package com.ammonium.adminshop.block.entity;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.block.interfaces.BuyerMachine;
import com.ammonium.adminshop.client.screen.FluidBuyerMenu;
import com.ammonium.adminshop.money.BankAccount;
import com.ammonium.adminshop.money.MoneyManager;
import com.ammonium.adminshop.network.PacketSyncMoneyToClient;
import com.ammonium.adminshop.setup.Messages;
import com.ammonium.adminshop.setup.Registration;
import com.ammonium.adminshop.shop.Shop;
import com.ammonium.adminshop.shop.ShopItem;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.FluidHandlerBlockEntity;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class FluidBuyerBE extends FluidHandlerBlockEntity implements BuyerMachine {
    private String ownerUUID;
    private Pair<String, Integer> account;
    private ShopItem targetShopItem = null;
    private int tickCounter = 0;
    private final int buySize = 4000;
    private final int tankCapacity = 64000;

    public FluidBuyerBE(BlockPos pWorldPosition, BlockState pBlockState) {
        super(Registration.FLUID_BUYER_BE.get(), pWorldPosition, pBlockState);
        this.tank = new ExtractOnlyTank(tankCapacity, this::sendUpdates);
    }

    // Secure method for internal fluid insertion
    private int secureFill(FluidStack resource, IFluidHandler.FluidAction action) {
        return ((ExtractOnlyTank) this.tank).secureFill(resource, action);
    }

    public void setOwnerUUID(String ownerUUID) {
        this.ownerUUID = ownerUUID;
    }

    public String getOwnerUUID() {
        return ownerUUID;
    }

    public void setAccount(Pair<String, Integer> account) {
        this.account = account;
    }

    public Pair<String, Integer> getAccount() {
        return account;
    }
    public void setTargetShopItem(ShopItem item) {
        this.targetShopItem = item;
    }

    public ShopItem getTargetShopItem() {
        return this.targetShopItem;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.adminshop.fluid_buyer");
    }

    @Override
    public void setChanged() {
        super.setChanged();
    }

    public FluidTank getTank() {
        return this.tank;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, Inventory pInventory, Player pPlayer) {
        return new FluidBuyerMenu(pContainerId, pInventory, this);
    }

    public static void tick(Level pLevel, BlockPos pPos, BlockState pState, FluidBuyerBE pBlockEntity) {
        if(!pLevel.isClientSide) {
            pBlockEntity.tickCounter++;
            if (pBlockEntity.tickCounter > 20) {
                pBlockEntity.tickCounter = 0;
                // Send buy fluid transaction
                assert pLevel instanceof ServerLevel;
                buyerTransaction(pPos, (ServerLevel) pLevel, pBlockEntity, pBlockEntity.buySize);
            }
        }
    }

    public static void buyerTransaction(BlockPos pos, ServerLevel level, FluidBuyerBE buyerEntity, final int buySize) {
        // fluid logic
        // Attempt to insert the fluids, and only perform transaction on what can fit
        MoneyManager moneyManager = MoneyManager.get(level);
        // Check targetShopItem
        if (buyerEntity.targetShopItem == null) {
            return;
        }

        ShopItem shopItem = buyerEntity.getTargetShopItem();
        // Check shopItem is fluid and buy only
        if (!shopItem.isBuy() || shopItem.isItem()) {
            AdminShop.LOGGER.error("Fluid Buyer shopItem is not buy fluid!");
            return;
        }
        if (shopItem.getFluid().isEmpty()) {
            AdminShop.LOGGER.error("Fluid Buyer shopItem is empty!");
            return;
        }

        FluidStack toInsert = shopItem.getFluid().copy();
        toInsert.setAmount(buySize);
        LazyOptional<IFluidHandler> lazyFluidHandler = buyerEntity.getCapability(ForgeCapabilities.FLUID_HANDLER);
        if (!lazyFluidHandler.isPresent()) {
            AdminShop.LOGGER.error("FluidBuyer has no FluidHandler!");
            return;
        }
        lazyFluidHandler.ifPresent(fluidHandler -> {
            int canFill = buyerEntity.secureFill(toInsert, IFluidHandler.FluidAction.SIMULATE);
            if(canFill == 0) {
                return;
            }
            long itemCost = shopItem.getPrice();
            long price = (long) canFill * itemCost;
            // Get MoneyManager and attempt transaction

            // Check if account is set
            if (buyerEntity.account == null) {
                AdminShop.LOGGER.error("Fluid Buyer bankAccount is null");
                return;
            }

            // Check if account still exists
            if (!moneyManager.existsBankAccount(buyerEntity.account)) {
                AdminShop.LOGGER.error("Fluid Buyer machine account "+buyerEntity.account.getKey()+":"+buyerEntity.account
                        .getValue()+" does not exist");
                return;
            }
            String accOwner = buyerEntity.account.getKey();
            int accID = buyerEntity.account.getValue();
            // Check if account has enough money, if not reduce amount
            long balance = moneyManager.getBalance(accOwner, accID);
            if (price > balance) {
                if (itemCost > balance) {
                    // not enough money to buy one
                    return;
                }
                // Find max amount he can buy
                int maxBuySize = Math.min((int) (balance / itemCost), buySize);
                price = (long) maxBuySize * itemCost;
                toInsert.setAmount(maxBuySize);
            }
//            AdminShop.LOGGER.debug("Removing "+price+" from account for "+toInsert.getAmount()+"mb");
            boolean success = moneyManager.subtractBalance(accOwner, accID, price);
            if (success) {
                int filled = buyerEntity.secureFill(toInsert, IFluidHandler.FluidAction.EXECUTE);
//                AdminShop.LOGGER.debug("Successfully filled "+filled+"mb");
//                AdminShop.LOGGER.debug("Tank is now "+buyerEntity.tank.getFluid().getAmount()+"mb "+buyerEntity.tank.getFluid().getDisplayName().getString());
            } else {
                AdminShop.LOGGER.error("Error buying fluid.");
                return;
            }
            // Sync account data
            // Get current bank account
            BankAccount currentAccount = moneyManager.getBankAccount(accOwner, accID);
            // Sync money with bank account's members
            assert currentAccount.getMembers().contains(accOwner);
            currentAccount.getMembers().forEach(memberUUID -> {
                List<BankAccount> usableAccounts = moneyManager.getSharedAccounts().get(memberUUID);
                ServerPlayer playerByUUID = (ServerPlayer) level.getPlayerByUUID(UUID.fromString(memberUUID));
                if (playerByUUID != null) {
                    Messages.sendToPlayer(new PacketSyncMoneyToClient(usableAccounts), playerByUUID);
                }
            });
        });
    }


//    @Nonnull
//    @Override
//    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @javax.annotation.Nullable Direction side) {
//        if (cap == ForgeCapabilities.FLUID_HANDLER) {
//            return lazyItemHandler.cast();
//        }
//        return super.getCapability(cap, side);
//    }

    @Override
    public void onLoad() {
        super.onLoad();
//        lazyItemHandler = LazyOptional.of(() -> itemHandler);
    }

    @Override
    public void invalidateCaps()  {
        super.invalidateCaps();
//        lazyItemHandler.invalidate();
    }

    @Override
    public @NotNull CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
//        tag.put("inventory", this.itemHandler.serializeNBT());
        tank.writeToNBT(tag);
        if (this.ownerUUID != null) {
            tag.putString("ownerUUID", this.ownerUUID);
        }
        if (this.account != null) {
            tag.putString("accountUUID", this.account.getKey());
            tag.putInt("accountID", this.account.getValue());
        }
        if (this.targetShopItem != null) {
            ResourceLocation targetResource = ForgeRegistries.FLUIDS.getKey(this.targetShopItem.getFluid().getFluid());
            assert targetResource != null;
            tag.putString("targetResource", targetResource.toString());
//            AdminShop.LOGGER.debug("Creating update for FluidBuyer targetResource: "+targetResource);
        }
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        super.onDataPacket(net, pkt);
        this.load(Objects.requireNonNull(pkt.getTag()));
    }
    public void sendUpdates() {
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        super.handleUpdateTag(tag);
//        this.itemHandler.deserializeNBT(tag.getCompound("inventory"));
        this.tank.readFromNBT(tag);
        if (tag.contains("ownerUUID")) {
            this.ownerUUID = tag.getString("ownerUUID");
        }
        if (tag.contains("accountUUID") && tag.contains("accountID")) {
            String accountUUID = tag.getString("accountUUID");
            int accountID = tag.getInt("accountID");
            this.account = Pair.of(accountUUID, accountID);
        }
        ResourceLocation targetResource = null;
        if (tag.contains("targetResource")) {
            targetResource = new ResourceLocation(tag.getString("targetResource"));
        }
        if (targetResource == null) {
            AdminShop.LOGGER.debug("Fluid Buyer has no targetShopItem");
            this.targetShopItem = null;
        } else {
            Fluid targetFluid = ForgeRegistries.FLUIDS.getValue(targetResource);
            this.targetShopItem = Shop.get().getBuyShopFluid(targetFluid);
        }
//        AdminShop.LOGGER.debug("Updated FluidBuyer with targetShopItem "+((this.targetShopItem != null) ? this.targetShopItem.getFluid().getDisplayName() : "none"));
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
//        tank.writeToNBT(tag);
        if (this.ownerUUID != null) {
            tag.putString("ownerUUID", this.ownerUUID);
        }
        if (this.account != null) {
            tag.putString("accountUUID", this.account.getKey());
            tag.putInt("accountID", this.account.getValue());
        }
        if (this.targetShopItem != null) {
            ResourceLocation targetResource = ForgeRegistries.FLUIDS.getKey(this.targetShopItem.getFluid().getFluid());
            assert targetResource != null;
            tag.putString("targetResource", targetResource.toString());
//            AdminShop.LOGGER.debug("Saved FluidBuyer targetResource: "+targetResource);
        }
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
//        tank.readFromNBT(tag);
        if (tag.contains("ownerUUID")) {
            this.ownerUUID = tag.getString("ownerUUID");
        }
        if (tag.contains("accountUUID") && tag.contains("accountID")) {
            String accountUUID = tag.getString("accountUUID");
            int accountID = tag.getInt("accountID");
            this.account = Pair.of(accountUUID, accountID);
        }
        ResourceLocation targetResource = null;
        if (tag.contains("targetResource")) {
            targetResource = new ResourceLocation(tag.getString("targetResource"));
//            AdminShop.LOGGER.debug("Contains targetResource");
        }
        if (targetResource == null) {
            AdminShop.LOGGER.debug("FluidBuyer has no targetShopItem");
            this.targetShopItem = null;
        } else {
            Fluid targetFluid = ForgeRegistries.FLUIDS.getValue(targetResource);
            this.targetShopItem = Shop.get().getBuyShopFluid(targetFluid);
        }
//        AdminShop.LOGGER.debug("Loaded FluidBuyer with targetShopItem "+((this.targetShopItem != null) ? this.targetShopItem.getFluid().getDisplayName().getString() : "none"));
    }

//    public void drops() {
//        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
//        for (int i = 0; i < itemHandler.getSlots(); i++) {
//            inventory.setItem(i, itemHandler.getStackInSlot(i));
//        }
//
//        Containers.dropContents(this.level, this.worldPosition, inventory);
//    }
}
