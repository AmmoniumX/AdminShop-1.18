package com.ammonium.adminshop.network;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.money.BankAccount;
import com.ammonium.adminshop.money.MoneyManager;
import com.ammonium.adminshop.setup.Messages;
import com.ammonium.adminshop.shop.Shop;
import com.ammonium.adminshop.shop.ShopItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.PlayerMainInvWrapper;
import net.minecraftforge.network.NetworkEvent;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static java.lang.Math.ceil;

public class PacketBuyRequest {
    private final int quantity;
    private final String accOwner;
    private final int accID;
    private final ShopItem shopItem; // final

    public PacketBuyRequest(BankAccount bankAccount, ShopItem shopItem, int quantity){
        this.accOwner = bankAccount.getOwner();
        this.accID = bankAccount.getId();
        this.shopItem = shopItem;
        this.quantity = quantity;
    }

    public PacketBuyRequest(Pair<String, Integer> bankAccount, ShopItem shopItem, int quantity){
        this.accOwner = bankAccount.getKey();
        this.accID = bankAccount.getValue();
        this.shopItem = shopItem;
        this.quantity = quantity;
    }

    public PacketBuyRequest(String owner, int ownerId, ShopItem shopItem, int quantity){
        this.accOwner = owner;
        this.accID = ownerId;
        this.shopItem = shopItem;
        this.quantity = quantity;
    }

    public PacketBuyRequest(FriendlyByteBuf buf){
        this.accOwner = buf.readUtf();
        this.accID = buf.readInt();
        int shopItemIndex = buf.readInt();
        List<ShopItem> shopItemList = Shop.get().getShopStockBuy();
        this.shopItem = shopItemList.get(shopItemIndex);
        this.quantity = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf){
        buf.writeUtf(accOwner);
        buf.writeInt(accID);
        List<ShopItem> shopItemList = Shop.get().getShopStockBuy();
        buf.writeInt(shopItemList.indexOf(shopItem));
        buf.writeInt(quantity);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier){
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            //Client side accessed here
            //Do NOT call client-only code though, since server needs to access this too
            if (shopItem.isTag()) {
                AdminShop.LOGGER.error("Illegal transaction: buy transactions may not be done with tags");
                return;
            }
            AdminShop.LOGGER.debug("Performing buy transaction: ");
            if (shopItem.isItem()) {
                AdminShop.LOGGER.debug("Item: "+shopItem.getItem().getDisplayName().getString()+", nbt:"+(shopItem.hasNBT() ? shopItem.getItem().getTag() : "false"));
            } else {
                AdminShop.LOGGER.debug("Fluid: "+shopItem.getFluid().getDisplayName().getString());
            }

            ServerPlayer player = ctx.getSender();
            assert player != null;
            MoneyManager moneyManager = MoneyManager.get(player.level());

            // Check if account has permit requirement
            BankAccount bankAccount = moneyManager.getBankAccount(this.accOwner, this.accID);
            if (!bankAccount.hasPermit(shopItem.getPermitTier())) {
                AdminShop.LOGGER.error("Account "+accOwner+":"+accID+" does not have permit tier "+ shopItem.getPermitTier());
                player.sendSystemMessage(Component.literal( MojangAPI.getUsernameByUUID(accOwner)+":"+accID+" does not " +
                                "have permit tier "+ shopItem.getPermitTier()));
                return;
            }

            if (shopItem.isItem()) {
                buyItemTransaction(supplier, shopItem, quantity);
            } else {
                buyFluidTransaction(supplier, shopItem, quantity);
            }

            // Sync money with affected clients
            AdminShop.LOGGER.info("Syncing money with clients");
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
        return true;
    }

    private void buyItemTransaction(Supplier<NetworkEvent.Context> supplier, ShopItem item, int quantity) {
        assert(item.isItem() && item.isBuy() && !item.isTag());
        // IItemHandler inventory = player.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
        NetworkEvent.Context ctx = supplier.get();
        ServerPlayer player = ctx.getSender();
        // Get item handler
        assert player != null;
        Inventory playerInventory = player.getInventory();
        LazyOptional<IItemHandler> mainInventoryHandler = LazyOptional.of(() -> new PlayerMainInvWrapper(playerInventory));
        mainInventoryHandler.ifPresent(iItemHandler -> {
            // item logic
            // Attempt to insert the items, and only perform transaction on what can fit
            AdminShop.LOGGER.info("Buying Item");
            ItemStack toInsert = item.getItem().copy();
            toInsert.setCount(quantity);
            ItemStack returned = ItemHandlerHelper.insertItemStacked(iItemHandler, toInsert, true);
            if(returned.getCount() == quantity) {
                player.sendSystemMessage(Component.literal("Not enough inventory space for item!"));
            }
            long itemCost = item.getPrice();
            long price = (long) ceil((quantity - returned.getCount()) * itemCost);

            MoneyManager moneyManager = MoneyManager.get(player.level());
            boolean success = moneyManager.subtractBalance(accOwner, accID, price);
            if (success) {
                ItemHandlerHelper.insertItemStacked(iItemHandler, toInsert, false);
            } else {
                player.sendSystemMessage(Component.literal("Not enough money in account!"));
                AdminShop.LOGGER.error("Not enough money in account to perform transaction.");
            }
        });
    }
    private void buyFluidTransaction(Supplier<NetworkEvent.Context> supplier, ShopItem shopItem, int quantity) {
        assert(!shopItem.isItem() && shopItem.isBuy());
        NetworkEvent.Context ctx = supplier.get();
        ServerPlayer player = ctx.getSender();
        // Get item handler
        assert player != null;
        Inventory playerInventory = player.getInventory();
        LazyOptional<IItemHandler> mainInventoryHandler = LazyOptional.of(() -> new PlayerMainInvWrapper(playerInventory));
        mainInventoryHandler.ifPresent(itemHandler -> {
            // fluid logic
            // Attempt to insert the fluid into a IFluidContainerItem, and only perform transaction on what can fit (up to 1000mb)
            AdminShop.LOGGER.info("Buying Fluid");
            FluidStack toInsert = shopItem.getFluid().copy();
            toInsert.setAmount(quantity);
            int fillableContainerIdx = getFillableFluidContainer(itemHandler, toInsert.getFluid(), quantity);
            if(fillableContainerIdx == -1) {
                player.sendSystemMessage(Component.literal("No container found for fluid!"));
                AdminShop.LOGGER.error("No container found for fluid.");
                return;
            }
            ItemStack ogContainer = itemHandler.getStackInSlot(fillableContainerIdx);
            AtomicReference<ItemStack> newContainer = new AtomicReference<>(ogContainer);
            // If stacked buckets, make sure you have an empty slot
            if (ogContainer.getItem().equals(Items.BUCKET) && ogContainer.getCount() != 1) {
                if (!hasEmptySlot(itemHandler)) {
                    player.sendSystemMessage(Component.literal("Trying to fill into a bucket, but wouldn't have space for filled bucket"));
                    AdminShop.LOGGER.error("Trying to fill into a bucket, but wouldn't have space for filled bucket");
                    return;
                }
                // Set new container to be single item
                ItemStack singleStack = ogContainer.copy();
                singleStack.setCount(1);
                newContainer.set(singleStack);
            }
            AtomicInteger filledAmount = new AtomicInteger();
            newContainer.get().getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).ifPresent(handler ->
                    filledAmount.set(handler.fill(toInsert, IFluidHandler.FluidAction.SIMULATE)));
            toInsert.setAmount(filledAmount.get());
            long fluidCost = shopItem.getPrice();
            long price = (long) ceil(filledAmount.get() * fluidCost);


            MoneyManager moneyManager = MoneyManager.get(player.level());
            boolean success = moneyManager.subtractBalance(accOwner, accID, price);
            if (success) {
                newContainer.get().getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).ifPresent(fluidHandler -> {
                    AdminShop.LOGGER.debug("Attempt to fill with "+toInsert.getDisplayName().getString()+", "+toInsert.getAmount());
                    int filled = fluidHandler.fill(toInsert, IFluidHandler.FluidAction.EXECUTE);
                    AdminShop.LOGGER.debug("Filled with "+filled+" mb");

                    // Replace item
                    ItemStack newBucket = fluidHandler.getContainer();
//                    AdminShop.LOGGER.debug("New container: "+newBucket);
                    if (!newBucket.equals(ogContainer)) {
//                        AdminShop.LOGGER.debug("Giving new container");
                        itemHandler.extractItem(fillableContainerIdx, 1, false);
                        ItemStack inserted = ItemHandlerHelper.insertItemStacked(itemHandler, newBucket, false);
//                        AdminShop.LOGGER.debug("Inserted: "+inserted);
                        if (inserted.getCount() != 0) {
                            player.sendSystemMessage(Component.literal("Error inserting fluid container, this shouldn't happen!"));
                            AdminShop.LOGGER.error("Error inserting fluid container, this shouldn't happen! "+inserted.getCount());
                        }
                    }
                    });
            } else {
                player.sendSystemMessage(Component.literal("Not enough money in account!"));
                AdminShop.LOGGER.error("Not enough money in account to perform transaction.");
            }
        });
    }

    /**
     * Finds first possible container that can get filled with flujd
     * @param itemHandler handler for player inventory
     * @param fluid type of fluid
     * @return pair where first value is the slot index and second value is max fillable fluid
     */
    public static int getFillableFluidContainer(IItemHandler itemHandler, Fluid fluid, int quantity) {
        // Iterate over all slots in the IItemHandler
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            // Get the ItemStack in the current slot
            ItemStack ogStack = itemHandler.getStackInSlot(i);
            // return if stack is empty
            if (ogStack.isEmpty()) {continue;}
            AtomicReference<ItemStack> stack = new AtomicReference<>(ogStack);
            AtomicInteger result = new AtomicInteger(-1);
            // Check if the ItemStack has the IFluidHandlerItem capability
            int finalI = i;
            ogStack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).ifPresent(ogHandler -> {
                AdminShop.LOGGER.debug("Found fluid container on slot "+finalI+": "+ stack.get().getDisplayName().getString());
                // If stacked buckets, check if you can insert into single bucket
                if (ogStack.getItem().equals(Items.BUCKET) && ogStack.getCount() > 1) {
                    ItemStack singleItem = ogStack.copy();
                    singleItem.setCount(1);
                    stack.set(singleItem);
                }
                stack.get().getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).ifPresent(newHandler -> {
                    // Check if we can fill the fluid into the container
                    int canFillAmount = newHandler.fill(new FluidStack(fluid, quantity), IFluidHandler.FluidAction.SIMULATE);
                    if (canFillAmount > 0) {
                        // If we can fill, set the result
                        AdminShop.LOGGER.debug("Container can fill: " + canFillAmount);
                        result.set(finalI);
                    }
                });
            });

            // If result is set, return it
            if (result.get() != -1) {
                return result.get();
            }
        }
        // If no suitable fluid containers were found, return -1
        return -1;
    }
    public boolean hasEmptySlot(IItemHandler itemHandler) {
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (stack.isEmpty()) {
                return true;
            }
        }
        return false;
    }

}
