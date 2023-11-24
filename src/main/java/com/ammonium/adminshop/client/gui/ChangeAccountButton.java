package com.ammonium.adminshop.client.gui;

import com.ammonium.adminshop.AdminShop;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class ChangeAccountButton extends Button {
    private final ResourceLocation GUI = new ResourceLocation(AdminShop.MODID, "textures/gui/shop_gui.png");
    private static final String name = "Change Account";

    public ChangeAccountButton(int x, int y, Button.OnPress listener) {
        super(x, y, 50, 16, Component.literal(name), listener, DEFAULT_NARRATION);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        if(!visible) {
            return;
        }
        int x = getX();
        int y = getY();

        RenderSystem.setShaderTexture(0, GUI);
        guiGraphics.blit(GUI, x, y, 195, 113, 50, 16);

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        drawSmallerCenteredString(guiGraphics, font, name, x + 25,
                y + 10 - font.lineHeight/2, 0xFFFFFF);
    }

    public void drawSmallerCenteredString(GuiGraphics guiGraphics, Font font, String text, int x, int y, int color) {
        float scale = 0.62f; // Adjust this value to set the desired font size
        PoseStack matrix = guiGraphics.pose();
        matrix.pushPose(); // Save the current pose

        // Apply the scaling
        matrix.scale(scale, scale, scale);

        // Calculate the new x and y positions based on the scaling
        int scaledX = (int) (x / scale);
        int scaledY = (int) (y / scale);

        // Draw the centered string with the new positions
        guiGraphics.drawString(font, text, (float) (scaledX - font.width(text) / 2), (float) scaledY, color, active);

        matrix.popPose(); // Restore the saved pose
    }
}
