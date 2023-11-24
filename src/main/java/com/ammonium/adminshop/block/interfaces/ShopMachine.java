package com.ammonium.adminshop.block.interfaces;

import net.minecraft.world.MenuProvider;
import net.minecraftforge.common.extensions.IForgeBlockEntity;
import org.apache.commons.lang3.tuple.Pair;

public interface ShopMachine extends MenuProvider, IForgeBlockEntity {
    void setOwnerUUID(String ownerUUID);
    String getOwnerUUID();
    void setAccount(Pair<String, Integer> account);
    Pair<String, Integer> getAccount();
    void sendUpdates();
}
