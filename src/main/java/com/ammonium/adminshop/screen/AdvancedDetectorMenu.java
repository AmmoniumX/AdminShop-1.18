package com.ammonium.adminshop.screen;

import com.ammonium.adminshop.blocks.entity.AdvancedDetectorBE;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class AdvancedDetectorMenu extends DetectorMenu<AdvancedDetectorBE> {
    public AdvancedDetectorMenu(int windowId, Inventory inv, FriendlyByteBuf extraData) {
        super(windowId, inv, extraData, ModMenuTypes.ADVANCED_DETECTOR_MENU.get(), AdvancedDetectorBE.class);
    }

    public AdvancedDetectorMenu(int windowId, Inventory inv, BlockEntity entity) {
        super(windowId, inv, entity, ModMenuTypes.ADVANCED_DETECTOR_MENU.get(), AdvancedDetectorBE.class);
    }
    public AdvancedDetectorMenu(int id, Inventory playerInventory, Level pLevel, BlockPos pPos) {
        this(id, playerInventory, pLevel.getBlockEntity(pPos));
    }
}
