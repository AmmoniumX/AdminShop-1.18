package com.ammonium.adminshop.client.gui;

import com.ammonium.adminshop.AdminShop;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * To swap between Buy and Sell modes
 */
public class BuySellButton extends Button {

    private final ResourceLocation GUI = new ResourceLocation(AdminShop.MODID, "textures/gui/shop_gui.png");
    private static final int TYPE_BUY = 0;
    private static final int TYPE_SELL = 1;

    private final String GUI_BUY = "gui.buy";
    private final String GUI_SELL = "gui.sell";

    private boolean isBuy;

    public BuySellButton(int x, int y, String buyText, String sellText, boolean isBuy, OnPress listener) {
        super(x, y, 50, 12, Component.literal(isBuy ? buyText : sellText), listener, DEFAULT_NARRATION);
        this.isBuy = isBuy;
    }

    /**
     * Switches buy and sell mode
     * @return Whether in Buy mode. false = sell mode
     */
    public boolean switchBuySell() {
        isBuy = !isBuy;
        return isBuy;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        if(!visible) {
            return;
        }
        int x = getX();
        int y = getY();
        RenderSystem.setShaderTexture(0, GUI);
        if (isBuy) {
            guiGraphics.blit(GUI, x, y, 195, 68, 50, 12);
        } else {
            guiGraphics.blit(GUI, x, y, 195, 56, 50, 12);
        }

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        guiGraphics.drawCenteredString(font, I18n.get(GUI_BUY), x + 12,
                y + 6 - font.lineHeight/2, 0xFFFFFF);
        guiGraphics.drawCenteredString(font, I18n.get(GUI_SELL), x + 37,
                y + 6 - font.lineHeight/2, 0xFFFFFF);
    }
}
