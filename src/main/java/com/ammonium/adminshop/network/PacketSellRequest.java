package com.ammonium.adminshop.network;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.money.BankAccount;
import com.ammonium.adminshop.money.MoneyManager;
import com.ammonium.adminshop.setup.Messages;
import com.ammonium.adminshop.shop.Shop;
import com.ammonium.adminshop.shop.ShopItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.PlayerMainInvWrapper;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.tags.ITag;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class PacketSellRequest {
    private final int quantity;
    private final String accOwner;
    private final int accID;
    private final boolean sellBySlot;
    private int slotIndex; // final
    private ShopItem shopItem;

    public PacketSellRequest(BankAccount bankAccount, int slotIndex, int quantity){
        this.accOwner = bankAccount.getOwner();
        this.accID = bankAccount.getId();
        this.sellBySlot = true;
        this.slotIndex = slotIndex;
        this.shopItem = null;
        this.quantity = quantity;
    }

    public PacketSellRequest(BankAccount bankAccount, ShopItem item, int quantity){
        this.accOwner = bankAccount.getOwner();
        this.accID = bankAccount.getId();
        this.sellBySlot = false;
        this.slotIndex = -1;
        this.shopItem = item;
        this.quantity = quantity;
    }

    public PacketSellRequest(Pair<String, Integer> bankAccount, int slotIndex, int quantity){
        this.accOwner = bankAccount.getKey();
        this.accID = bankAccount.getValue();
        this.sellBySlot = true;
        this.slotIndex = slotIndex;
        this.shopItem = null;
        this.quantity = quantity;
    }
    public PacketSellRequest(Pair<String, Integer> bankAccount, ShopItem item, int quantity){
        this.accOwner = bankAccount.getKey();
        this.accID = bankAccount.getValue();
        this.sellBySlot = false;
        this.slotIndex = -1;
        this.shopItem = item;
        this.quantity = quantity;
    }

    public PacketSellRequest(String owner, int ownerId, int slotIndex, int quantity){
        this.accOwner = owner;
        this.accID = ownerId;
        this.sellBySlot = true;
        this.slotIndex = slotIndex;
        this.shopItem = null;
        this.quantity = quantity;
    }
    public PacketSellRequest(String owner, int ownerId, ShopItem item, int quantity){
        this.accOwner = owner;
        this.accID = ownerId;
        this.sellBySlot = false;
        this.slotIndex = -1;
        this.shopItem = item;
        this.quantity = quantity;
    }

    public PacketSellRequest(FriendlyByteBuf buf){
        this.accOwner = buf.readUtf();
        this.accID = buf.readInt();
        this.sellBySlot = buf.readBoolean();
        if (this.sellBySlot) {
            this.slotIndex = buf.readInt();
            this.shopItem = null;
        } else {
            this.slotIndex = -1;
            int shopItemIndex = buf.readInt();
            List<ShopItem> shopItemList = Shop.get().getShopStockSell();
            this.shopItem = shopItemList.get(shopItemIndex);
        }
        this.quantity = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf){
        buf.writeUtf(accOwner);
        buf.writeInt(accID);
        buf.writeBoolean(sellBySlot);
        if (sellBySlot) {
            buf.writeInt(slotIndex);
        } else {
            List<ShopItem> shopItemList = Shop.get().getShopStockSell();
            buf.writeInt(shopItemList.indexOf(shopItem));
        }
        buf.writeInt(quantity);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier){
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            //Client side accessed here
            //Do NOT call client-only code though, since server needs to access this too
            AdminShop.LOGGER.debug("Performing sell transaction, sellBySlot="+sellBySlot);
            ServerPlayer player = ctx.getSender();
            // Get item handler
            assert player != null;
            Inventory playerInventory = player.getInventory();
            LazyOptional<IItemHandler> mainInventoryHandler = LazyOptional.of(() -> new PlayerMainInvWrapper(playerInventory));
            mainInventoryHandler.ifPresent(iItemHandler -> {
                ItemStack sellStack = ItemStack.EMPTY;
                if (sellBySlot) {
                    sellStack = iItemHandler.getStackInSlot(slotIndex);
                    // Check if sellStack is container
                    boolean isContainer = sellStack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).isPresent();
                    AtomicReference<FluidStack> sellFluid = new AtomicReference<>(FluidStack.EMPTY);
                    sellStack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).ifPresent(iFluidHandlerItem -> {
                        sellFluid.set(iFluidHandlerItem.getFluidInTank(0));
                    });
                    // Search for valid ShopItem, be it item or fluid
                    List<ShopItem> sellList = Shop.get().getShopStockSell();
                    for (ShopItem sItem: sellList) {
                        // Search for valid fluid, either by tag or not
                        if (!sItem.isItem() && isContainer && !sellFluid.get().isEmpty()) {
                            // Search by fluid tag
                            if (sItem.isTag()) {
                                ITag<Fluid> fluidTagManager = ForgeRegistries.FLUIDS.tags().getTag(sItem.getFluidTag());
                                if (fluidTagManager.contains(sellFluid.get().getFluid())) {
                                    shopItem = sItem;
                                    break;
                                }
                                // Search by Fluid
                            } else {
                                if(sellFluid.get().getFluid().equals(sItem.getFluid().getFluid())) {
                                    shopItem = sItem;
                                    break;
                                }
                            }
                            // Search by item tag
                        }
                        // Search for valid item, either by tag or not
                        if (sItem.isItem()) {
                            if (sItem.isTag()) {
                                ITag<Item> itemTagManager = ForgeRegistries.ITEMS.tags().getTag(sItem.getItemTag());
                                if (itemTagManager.contains(sellStack.getItem())) {
                                    shopItem = sItem;
                                    break;
                                }
                            } else {
                                if (sellStack.getItem().equals(sItem.getItem().getItem())) {
                                    shopItem = sItem;
                                    break;
                                }
                            }
                        }
                    }
                }
                if (sellBySlot && shopItem == null) {
                    AdminShop.LOGGER.error("Could not find a valid ShopItem for the ItemStack of "+sellStack.getDisplayName().getString());
                    return;
                }
                
                if (shopItem.isItem() && !shopItem.isTag()) {
                    AdminShop.LOGGER.debug("Item: "+shopItem.getItem().getDisplayName().getString());
                } else if (shopItem.isItem() && shopItem.isTag()) {
                    AdminShop.LOGGER.debug("Item tag: "+shopItem.getItemTag().location());
                } else if (!shopItem.isItem() && !shopItem.isTag()) {
                    AdminShop.LOGGER.debug("Fluid: "+shopItem.getFluid().getDisplayName().getString());
                } else if (!shopItem.isItem() && shopItem.isTag()) {
                    AdminShop.LOGGER.debug("Fluid tag: "+shopItem.getFluidTag().location());
                }

                MoneyManager moneyManager = MoneyManager.get(player.level());
                // Check if account has permit requirement
                BankAccount bankAccount = moneyManager.getBankAccount(this.accOwner, this.accID);
                if (!bankAccount.hasPermit(shopItem.getPermitTier())) {
                    AdminShop.LOGGER.error("Account "+accOwner+":"+accID+" does not have permit tier "+ shopItem.getPermitTier());
                    player.sendSystemMessage(Component.literal( MojangAPI.getUsernameByUUID(accOwner)+":"+accID+" does not " +
                            "have permit tier "+ shopItem.getPermitTier()));
                    return;
                }
                if (sellBySlot) {
                    if (shopItem.isItem()){
                        sellItemTransaction(supplier, slotIndex, shopItem, quantity);
                    } else {
                        sellFluidTransaction(supplier, slotIndex, shopItem, quantity);
                    }
                } else {
                    if (shopItem.isItem()){
                        sellItemTransaction(supplier, shopItem, quantity);
                    } else {
                        sellFluidTransaction(supplier, shopItem, quantity);
                    }
                }

                // Sync money with affected clients
                AdminShop.LOGGER.debug("Syncing money with clients");
                // Get current bank account
                BankAccount currentAccount = moneyManager.getBankAccount(this.accOwner, this.accID);

                // Sync money with bank account's members
                assert currentAccount.getMembers().contains(this.accOwner);
                currentAccount.getMembers().forEach(memberUUID -> {
                    List<BankAccount> usableAccounts = moneyManager.getSharedAccounts().get(memberUUID);
                    Messages.sendToPlayer(new PacketSyncMoneyToClient(usableAccounts), (ServerPlayer) player.level()
                            .getPlayerByUUID(UUID.fromString(memberUUID)));
                });
            });
        });
        return true;
    }

    private void sellItemTransaction(Supplier<NetworkEvent.Context> supplier, int slotIndex, ShopItem item, int quantity) {
        assert(item.isItem() && !item.isBuy());
        NetworkEvent.Context ctx = supplier.get();
        ServerPlayer player = ctx.getSender();
        assert player != null;
        Inventory playerInventory = player.getInventory();
        LazyOptional<IItemHandler> mainInventoryHandler = LazyOptional.of(() -> new PlayerMainInvWrapper(playerInventory));
        mainInventoryHandler.ifPresent(iItemHandler -> {
            // Check that we are doing a valid extraction
            ItemStack toExtract = iItemHandler.getStackInSlot(slotIndex);
            if (!item.isTag() && !toExtract.getItem().equals(item.getItem().getItem())) {
                AdminShop.LOGGER.error("Invalid extraction: non-tag item and Items don't match: "+
                        item.getItem().getDisplayName().getString()+", "+toExtract.getDisplayName().getString());
                return;
            }
            if (item.isTag()) {
                ITag<Item> itemTagManager = ForgeRegistries.ITEMS.tags().getTag(item.getItemTag());
                if (!itemTagManager.contains(toExtract.getItem())) {
                    AdminShop.LOGGER.error("Invalid extraction: item doesn't have tag: "+
                            toExtract.getDisplayName().getString()+", "+item.getItemTag().location());
                }
            }
            int numSold = iItemHandler.extractItem(slotIndex, quantity, false).getCount();
            long itemCost = item.getPrice();
            long price = (long) numSold * itemCost;
            if (numSold == 0) {
                player.sendSystemMessage(Component.literal("No valid item found."));
                AdminShop.LOGGER.error("No valid item found.");
                return;
            }

            boolean success = MoneyManager.get(player.level()).addBalance(accOwner, accID, price);
            if (success) {
                AdminShop.LOGGER.debug("Sold item.");
            } else {
                AdminShop.LOGGER.error("Error selling item.");
            }
        });
    }
    private void sellItemTransaction(Supplier<NetworkEvent.Context> supplier, ShopItem item, int quantity) {
        assert(item.isItem() && !item.isBuy());
        NetworkEvent.Context ctx = supplier.get();
        ServerPlayer player = ctx.getSender();
        assert player != null;
        Inventory playerInventory = player.getInventory();
        LazyOptional<IItemHandler> mainInventoryHandler = LazyOptional.of(() -> new PlayerMainInvWrapper(playerInventory));
        mainInventoryHandler.ifPresent(iItemHandler -> {
            // Check that we are doing a valid extraction
            ItemStack toExtract = ItemStack.EMPTY;

            if (item.isTag()) {
                // If tag, search inventory for matching tag
                ITag<Item> itemTag = ForgeRegistries.ITEMS.tags().getTag(item.getItemTag());
                for (int i = 0; i < iItemHandler.getSlots(); i++) {
                    ItemStack currentStack = iItemHandler.getStackInSlot(i);
                    if (itemTag.contains(currentStack.getItem())) {
                        toExtract = currentStack.copy();
                        break;
                    }
                }
            } else {
                // If not tag, get item from ShopItem
                toExtract = item.getItem().copy();
            }
            if (toExtract.isEmpty()) {
                AdminShop.LOGGER.error("Could not find item tag: "+item.getItemTag().location());
                return;
            }
            toExtract.setCount(quantity);
            if (!item.isTag() && !toExtract.getItem().equals(item.getItem().getItem())) {
                AdminShop.LOGGER.error("Invalid extraction: non-tag item and Items don't match: "+
                        item.getItem().getDisplayName().getString()+", "+toExtract.getDisplayName().getString());
                return;
            }
            int numSold = removeItemsFromInventory(iItemHandler, toExtract);
            long itemCost = item.getPrice();
            long price = (long) numSold * itemCost;
            if (numSold == 0) {
                player.sendSystemMessage(Component.literal("No matching item found."));
                AdminShop.LOGGER.error("No matching item found.");
                return;
            }
            boolean success = MoneyManager.get(player.level()).addBalance(accOwner, accID, price);
            if (success) {
                AdminShop.LOGGER.debug("Sold item.");
            } else {
                AdminShop.LOGGER.error("Error selling item.");
            }
        });
    }
    private void sellFluidTransaction(Supplier<NetworkEvent.Context> supplier, int slotIndex, ShopItem item, int quantity) {
        assert(!item.isItem() && !item.isBuy());
        NetworkEvent.Context ctx = supplier.get();
        ServerPlayer player = ctx.getSender();
        assert player != null;
        Inventory playerInventory = player.getInventory();
        LazyOptional<IItemHandler> mainInventoryHandler = LazyOptional.of(() -> new PlayerMainInvWrapper(playerInventory));
        mainInventoryHandler.ifPresent(itemHandler -> {
            // Check that we are doing a valid extraction
            ItemStack toExtract = itemHandler.getStackInSlot(slotIndex);
            if (!toExtract.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).isPresent()) {
                AdminShop.LOGGER.error("Item isn't a fluid handler");
                return;
            }
            toExtract.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).ifPresent(fluidHanlder -> {
                FluidStack toSellFluid = fluidHanlder.getFluidInTank(0);
                if (!item.isTag() && !toSellFluid.getFluid().equals(item.getFluid().getFluid())) {
                    AdminShop.LOGGER.error("Invalid extraction: fluids don't match: "+
                            toSellFluid.getDisplayName().getString()+", "+item.getFluid().getDisplayName().getString());
                    return;
                }
                if (item.isTag()) {
                    ITag<Fluid> fluidTagManager = ForgeRegistries.FLUIDS.tags().getTag(item.getFluidTag());
                    if (!fluidTagManager.contains(toSellFluid.getFluid())) {
                        AdminShop.LOGGER.error("Invalid extraction: fluid doesn't have tag: "+
                                toSellFluid.getDisplayName().getString()+", "+item.getFluidTag().location());
                    }
                }
                FluidStack trySellFluid = toSellFluid.copy();
                trySellFluid.setAmount(quantity);
                int numSold = fluidHanlder.drain(trySellFluid, IFluidHandler.FluidAction.EXECUTE).getAmount();
                long itemCost = item.getPrice();
                long price = (long) numSold * itemCost;
                if (numSold == 0) {
                    player.sendSystemMessage(Component.literal("No valid fluid found."));
                    AdminShop.LOGGER.error("No valid fluid found.");
                    return;
                }

                // Replace item
                ItemStack returned = fluidHanlder.getContainer();
                if (!returned.equals(toExtract)) {
                    itemHandler.extractItem(slotIndex, 1, false);
                    ItemStack inserted = ItemHandlerHelper.insertItemStacked(itemHandler, returned, false);
                    if (inserted.getCount() != 0) {
                        player.sendSystemMessage(Component.literal("Error inserting fluid container, this shouldn't happen!"));
                        AdminShop.LOGGER.error("Error inserting fluid container, this shouldn't happen! "+inserted.getCount());
                    }
                }

                boolean success = MoneyManager.get(player.level()).addBalance(accOwner, accID, price);
                if (success) {
                    AdminShop.LOGGER.debug("Sold fluid.");
                } else {
                    AdminShop.LOGGER.error("Error selling fluid.");
                }
            });
        });
    }
    private void sellFluidTransaction(Supplier<NetworkEvent.Context> supplier, ShopItem item, int quantity) {
        assert(!item.isItem() && !item.isBuy());
        NetworkEvent.Context ctx = supplier.get();
        ServerPlayer player = ctx.getSender();
        assert player != null;
        Inventory playerInventory = player.getInventory();
        LazyOptional<IItemHandler> mainInventoryHandler = LazyOptional.of(() -> new PlayerMainInvWrapper(playerInventory));
        mainInventoryHandler.ifPresent(itemHandler -> {
            AdminShop.LOGGER.debug("Searching for valid extraction");
            // Check that we are doing a valid extraction
            MutableObject<ItemStack> toExtract = new MutableObject<>(ItemStack.EMPTY);
            ITag<Fluid> fluidTag = item.isTag() ? ForgeRegistries.FLUIDS.tags().getTag(item.getFluidTag()) : null;
            MutableBoolean found = new MutableBoolean(false);
            for (int i = 0; i < itemHandler.getSlots(); i++) {
                if (found.getValue()) break;
                ItemStack currentStack = itemHandler.getStackInSlot(i);
                int finalI = i;
                currentStack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).ifPresent(fluidHandler -> {
                    if (found.getValue()) return;

                    if (item.isTag()) {
                        // If tag, search for matching tag
                        assert fluidTag != null;
                        if (fluidTag.contains(fluidHandler.getFluidInTank(0).getFluid())) {
                            AdminShop.LOGGER.debug("Found valid container: "+currentStack.getDisplayName().getString()+" and tag: "+fluidTag.getKey().location());
                            toExtract.setValue(currentStack);
                            slotIndex = finalI;
                            found.setValue(true);
                        }
                    } else {
                        // If not tag, match with fluid from ShopItem
                        if (fluidHandler.getFluidInTank(0).getFluid().equals(item.getFluid().getFluid())) {
                            AdminShop.LOGGER.debug("Found valid container: "+currentStack.getDisplayName().getString()+" and fluid: "+item.getFluid().getDisplayName().getString());
                            toExtract.setValue(currentStack);
                            slotIndex = finalI;
                            found.setValue(true);
                        }
                    }
                });
            }
            if (toExtract.getValue().isEmpty()) {
                AdminShop.LOGGER.error("Could not find fluid handler item with ShopItem");
                return;
            }
            if (!toExtract.getValue().getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).isPresent()) {
                AdminShop.LOGGER.error("Item isn't a fluid handler");
                return;
            }
            toExtract.getValue().getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).ifPresent(fluidHanlder -> {
                AdminShop.LOGGER.debug("Item is fluid handler");
                if (fluidHanlder.getTanks() < 1) {
                    AdminShop.LOGGER.error("Fluid handler has no tanks");
                    return;
                }
                FluidStack toSellFluid = fluidHanlder.getFluidInTank(0);
                if (!item.isTag() && !toSellFluid.getFluid().equals(item.getFluid().getFluid())) {
                    AdminShop.LOGGER.error("Invalid extraction: fluids don't match: "+
                            toSellFluid.getDisplayName().getString()+", "+item.getFluid().getDisplayName().getString());
                    return;
                }
                if (item.isTag()) {
                    ITag<Fluid> fluidTagManager = ForgeRegistries.FLUIDS.tags().getTag(item.getFluidTag());
                    if (!fluidTagManager.contains(toSellFluid.getFluid())) {
                        AdminShop.LOGGER.error("Invalid extraction: fluid doesn't have tag: "+
                                toSellFluid.getDisplayName().getString()+", "+item.getFluidTag().location());
                    }
                }
                FluidStack trySellFluid = toSellFluid.copy();
                trySellFluid.setAmount(quantity);
                int numSold = fluidHanlder.drain(trySellFluid, IFluidHandler.FluidAction.EXECUTE).getAmount();
                AdminShop.LOGGER.debug("Drained "+numSold+"mb");
                long itemCost = item.getPrice();
                long price = (long) numSold * itemCost;
                if (numSold == 0) {
                    player.sendSystemMessage(Component.literal("No valid fluid found."));
                    AdminShop.LOGGER.error("No valid fluid found.");
                    return;
                }

                // Replace item
                ItemStack returned = fluidHanlder.getContainer();
                if (!returned.equals(toExtract.getValue())) {
                    itemHandler.extractItem(slotIndex, 1, false);
                    ItemStack inserted = ItemHandlerHelper.insertItemStacked(itemHandler, returned, false);
                    if (inserted.getCount() != 0) {
                        player.sendSystemMessage(Component.literal("Error inserting fluid container, this shouldn't happen!"));
                        AdminShop.LOGGER.error("Error inserting fluid container, this shouldn't happen! "+inserted.getCount());
                    }
                }

                boolean success = MoneyManager.get(player.level()).addBalance(accOwner, accID, price);
                if (success) {
                    AdminShop.LOGGER.debug("Sold fluid.");
                } else {
                    AdminShop.LOGGER.error("Error selling fluid.");
                }
            });
        });
    }

    private boolean itemstacksEqual(ItemStack a, ItemStack b){
        if(a.getItem() == b.getItem() && a.getDamageValue() == b.getDamageValue()) {
            CompoundTag atag = a.getOrCreateTag();
            CompoundTag btag = b.getOrCreateTag();
            if(atag == btag) {
                return true;
            } else {
                return a.getOrCreateTag().equals(b.getOrCreateTag());
            }
        }
        return false;
    }

    /**
     * Removes the equivalent parameter itemstack from the inventory (item and count)
     * @param inv Inventory to remove from
     * @param item ItemStack that represents what is to be removed. item.count() = number of items to be removed
     * @return Number of items actually removed.
     */
    private int removeItemsFromInventory(IItemHandler inv, ItemStack item) {
        int count = item.getCount();
        System.out.println("Removing items from inventory! # to remove: "+count);
        for(int i = 0; i < inv.getSlots(); i++){
            ItemStack comp = inv.getStackInSlot(i);
            if(itemstacksEqual(comp, item)){ //Found items we can remove
                System.out.println("Found item at slot "+i);
                if(count > comp.getCount()){ //Remove entirety of this stack
                    count -= comp.getCount();
                    inv.extractItem(i, comp.getCount(), false);
                }else{ //Remove what is left of stack, return number removed
                    inv.extractItem(i, count, false);
                    System.out.println("Removed count: "+item.getCount());
                    return item.getCount();
                }
            }
        }
        //Removed as much as possible, return count
//        System.out.println("Removed count: "+(item.getCount() - count));
        return item.getCount() - count;
    }
}
