package com.ammonium.adminshop.client.screen;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.block.entity.AdvancedDetectorBE;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class AdvancedDetectorScreen extends DetectorScreen<AdvancedDetectorMenu, AdvancedDetectorBE> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(AdminShop.MODID, "textures/gui/adv_detector.png");

    public AdvancedDetectorScreen(AdvancedDetectorMenu pMenu, Inventory pPlayerInventory, Component pTitle, BlockPos blockPos) {
        super(pMenu, pPlayerInventory, pTitle, blockPos, AdvancedDetectorBE.class);
    }

    public AdvancedDetectorScreen(AdvancedDetectorMenu pMenu, Inventory inventory, Component pTitle) {
        super(pMenu, inventory, pTitle, AdvancedDetectorBE.class);
    }

    @Override
    protected ResourceLocation getTexture() {
        return TEXTURE;
    }
}