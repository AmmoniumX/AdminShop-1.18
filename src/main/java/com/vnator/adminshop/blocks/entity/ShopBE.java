package com.vnator.adminshop.blocks.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ShopBE extends BlockEntity {

    public ShopBE(BlockPos pos, BlockState state){
        super(ModBlockEntities.SHOP.get(), pos, state);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
    }

//    @Override
//    public Component getDisplayName() {
//        return new TextComponent("Shop");
//    }

//    @Nullable
//    @Override
//    public AbstractContainerMenu createMenu(int pContainerId, Inventory pInventory, Player pPlayer) {
//        return
//    }
}
