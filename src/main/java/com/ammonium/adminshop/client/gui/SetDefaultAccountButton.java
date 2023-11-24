package com.ammonium.adminshop.client.gui;

import com.ammonium.adminshop.AdminShop;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class SetDefaultAccountButton extends Button {
    private final ResourceLocation GUI = new ResourceLocation(AdminShop.MODID, "textures/gui/shop_gui.png");
    private static final String name = "Change Account";

    public SetDefaultAccountButton(int x, int y, OnPress listener) {
        super(x, y, 50, 16, Component.literal(name), listener, DEFAULT_NARRATION);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        if(!visible) {
            return;
        }
        int x = getX();
        int y = getY();
        RenderSystem.setShaderTexture(0, GUI);
        guiGraphics.blit(GUI, x, y, 195, 129, 16, 16);

    }
}