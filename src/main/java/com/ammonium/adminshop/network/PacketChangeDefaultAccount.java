package com.ammonium.adminshop.network;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.money.BankAccount;
import com.ammonium.adminshop.money.MoneyManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.function.Supplier;

public class PacketChangeDefaultAccount {
    private final String player;
    private final String accOwner;
    private final int accID;

    public PacketChangeDefaultAccount(String player, String accOwner, int accID) {
        this.player = player;
        this.accOwner = accOwner;
        this.accID = accID;
    }
    public PacketChangeDefaultAccount(FriendlyByteBuf buf) {
        this.player = buf.readUtf();
        this.accOwner = buf.readUtf();
        this.accID = buf.readInt();
    }
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(this.player);
        buf.writeUtf(this.accOwner);
        buf.writeInt(this.accID);

    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier){
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            //Client side accessed here
            //Do NOT call client-only code though, since server needs to access this too

            // Change machine's account
            ServerPlayer player = ctx.getSender();

            if (player != null) {
                AdminShop.LOGGER.info("Setting default account for "+this.player+" to "+this.accOwner+":"+this.accID);
                // Check if chosen new account is in player's usable accounts
                MoneyManager moneyManager = MoneyManager.get(player.level());
                List<BankAccount> usableAccounts = moneyManager.getSharedAccounts().get(player.getStringUUID());
                boolean hasAccess = usableAccounts.stream().anyMatch(account ->
                        (account.getOwner().equals(this.accOwner) && account.getId() == this.accID));
                if (!hasAccess) {
                    AdminShop.LOGGER.error("Player does not have access to that account");
                    return;
                }
                AdminShop.LOGGER.debug("Saving default account");

                // Apply to MoneyManager
                moneyManager.setDefaultAccount(this.player, Pair.of(this.accOwner, this.accID));
            }
        });
        return true;
    }
}