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

public class PacketAccountRemovePermit {
    private final int permit;
    private final String accOwner;
    private final int accID;

    public PacketAccountRemovePermit(BankAccount bankAccount, int permit) {
        this.accOwner = bankAccount.getOwner();
        this.accID = bankAccount.getId();
        this.permit = permit;
    }

    public PacketAccountRemovePermit(Pair<String, Integer> bankAccount, int permit) {
        this.accOwner = bankAccount.getKey();
        this.accID = bankAccount.getValue();
        this.permit = permit;
    }

    public PacketAccountRemovePermit(String owner, int ownerId, int permit) {
        this.accOwner = owner;
        this.accID = ownerId;
        this.permit = permit;
    }

    public PacketAccountRemovePermit(FriendlyByteBuf buf) {
        this.accOwner = buf.readUtf();
        this.accID = buf.readInt();
        this.permit = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(accOwner);
        buf.writeInt(accID);
        buf.writeInt(permit);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            //Client side accessed here
            //Do NOT call client-only code though, since server needs to access this too
            AdminShop.LOGGER.info("Removing permit tier "+permit+" from "+accOwner+":"+accID);
            ServerPlayer player = ctx.getSender();
            assert player != null;
            MoneyManager moneyManager = MoneyManager.get(player.level());
            boolean success = moneyManager.removePermit(accOwner, accID, permit);
            if (!success) {
                AdminShop.LOGGER.error("Error removing permit from account!");
            } else {

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
                        serverPlayer.playNotifySound(SoundEvents.ZOMBIE_VILLAGER_CONVERTED, SoundSource.PLAYERS, 1.0f, 1.0f);
                        serverPlayer.sendSystemMessage(Component.literal("Removing permit tier " + permit + " from account " +
                                MojangAPI.getUsernameByUUID(accOwner) + ":" + accID));
                        Messages.sendToPlayer(new PacketSyncMoneyToClient(usableAccounts), serverPlayer);
                    }
                });
            }
        });
        return true;
    }
}