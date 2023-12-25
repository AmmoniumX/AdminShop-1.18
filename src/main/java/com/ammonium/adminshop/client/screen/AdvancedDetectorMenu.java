package com.ammonium.adminshop.client.screen;

import com.ammonium.adminshop.block.entity.AdvancedDetectorBE;
import com.ammonium.adminshop.setup.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class AdvancedDetectorMenu extends DetectorMenu<AdvancedDetectorBE> {
    public AdvancedDetectorMenu(int windowId, Inventory inv, FriendlyByteBuf extraData) {
        super(windowId, inv, extraData, Registration.ADVANCED_DETECTOR_MENU.get(), AdvancedDetectorBE.class);
    }

    public AdvancedDetectorMenu(int windowId, Inventory inv, BlockEntity entity) {
        super(windowId, inv, entity, Registration.ADVANCED_DETECTOR_MENU.get(), AdvancedDetectorBE.class);
    }
    public AdvancedDetectorMenu(int id, Inventory playerInventory, Level pLevel, BlockPos pPos) {
        this(id, playerInventory, pLevel.getBlockEntity(pPos));
    }
}