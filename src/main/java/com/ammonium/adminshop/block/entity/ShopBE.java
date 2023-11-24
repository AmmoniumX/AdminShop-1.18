package com.ammonium.adminshop.block.entity;

import com.ammonium.adminshop.setup.Registration;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ShopBE extends BlockEntity {

    public ShopBE(BlockPos pos, BlockState state){
        super(Registration.SHOP_BE.get(), pos, state);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
    }

}
