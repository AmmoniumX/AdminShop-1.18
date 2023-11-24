package com.ammonium.adminshop.network;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.money.BankAccount;
import com.ammonium.adminshop.money.MoneyManager;
import com.ammonium.adminshop.setup.Messages;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.network.NetworkEvent;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class PacketAccountAddPermit {
    private final int permit;
    private final int slotIndex;
    private final String accOwner;
    private final int accID;

    public PacketAccountAddPermit(BankAccount bankAccount, int permit, int slotIndex) {
        this.accOwner = bankAccount.getOwner();
        this.accID = bankAccount.getId();
        this.permit = permit;
        this.slotIndex = slotIndex;
    }

    public PacketAccountAddPermit(Pair<String, Integer> bankAccount, int permit, int slotIndex) {
        this.accOwner = bankAccount.getKey();
        this.accID = bankAccount.getValue();
        this.permit = permit;
        this.slotIndex = slotIndex;
    }

    public PacketAccountAddPermit(String owner, int ownerId, int permit, int slotIndex) {
        this.accOwner = owner;
        this.accID = ownerId;
        this.permit = permit;
        this.slotIndex = slotIndex;
    }

    public PacketAccountAddPermit(FriendlyByteBuf buf) {
        this.accOwner = buf.readUtf();
        this.accID = buf.readInt();
        this.permit = buf.readInt();
        this.slotIndex = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(accOwner);
        buf.writeInt(accID);
        buf.writeInt(permit);
        buf.writeInt(slotIndex);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            //Client side accessed here
            //Do NOT call client-only code though, since server needs to access this too
            AdminShop.LOGGER.info("Adding permit tier "+permit+" to "+accOwner+":"+accID);
            ServerPlayer player = ctx.getSender();
            assert player != null;
            MoneyManager moneyManager = MoneyManager.get(player.level());
            boolean success = moneyManager.addPermit(accOwner, accID, permit);
            if (!success) {
                AdminShop.LOGGER.error("Error adding permit to account!");
            } else {
                // Remove item from user
                player.getInventory().removeItem(slotIndex, 1);
                // Sync money with affected clients
                AdminShop.LOGGER.info("Syncing money with clients");

                // Get current bank account
                BankAccount currentAccount = moneyManager.getBankAccount(this.accOwner, this.accID);

                // Sync money with bank account's members
                assert currentAccount.getMembers().contains(this.accOwner);
                currentAccount.getMembers().forEach(memberUUID -> {

                    List<BankAccount> usableAccounts = moneyManager.getSharedAccounts().get(memberUUID);
                    ServerPlayer serverPlayer = (ServerPlayer) player.level()
                            .getPlayerByUUID(UUID.fromString(memberUUID));
                    if (serverPlayer != null) {
                        serverPlayer.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.0f);
                        serverPlayer.sendSystemMessage(Component.literal("Adding permit tier " + permit + " to account " +
                                        MojangAPI.getUsernameByUUID(accOwner) + ":" + accID));
                        Messages.sendToPlayer(new PacketSyncMoneyToClient(usableAccounts), serverPlayer);
                    }
                });
            }
        });
        return true;
    }
}