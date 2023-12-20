package com.ammonium.adminshop.blocks.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class BalanceDetectorBE extends BlockEntity {
    public BalanceDetectorBE(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.DETECTOR.get(), pPos, pBlockState);
    }
}
