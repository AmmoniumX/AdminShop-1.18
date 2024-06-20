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
        shopData = new String(buf.readByteArray());
    }

    public void toBytes(FriendlyByteBuf buf){
        buf.writeByteArray(shopData.getBytes());
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier){
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            //Client side accessed here
            //Do NOT call client-only code though, since server needs to access this too
            AdminShop.LOGGER.info("Syncing shop data from Server...");
            Shop.get().loadFromFile(shopData, ctx.getSender());
        });
        return true;
    }
}
