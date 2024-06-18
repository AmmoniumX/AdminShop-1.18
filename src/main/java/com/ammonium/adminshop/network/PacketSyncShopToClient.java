package com.ammonium.adminshop.network;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.shop.Shop;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketSyncShopToClient {
    private final String shopData;

    public PacketSyncShopToClient(String shopData){
        this.shopData = shopData;
    }

    public PacketSyncShopToClient(FriendlyByteBuf buf){
        shopData = buf.readUtf(32767);
    }

    public void toBytes(FriendlyByteBuf buf){
        buf.writeUtf(shopData, 32767);
    }


    public boolean handle(Supplier<NetworkEvent.Context> supplier){
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            //Client side accessed here
            // Sync shop
            AdminShop.LOGGER.info("Syncing shop data from Server...");
            Shop.get().loadFromFile(shopData, ctx.getSender(), "PlayerLogin");

        });
        return true;
    }

}
