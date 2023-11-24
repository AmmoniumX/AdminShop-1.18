package com.ammonium.adminshop.client.gui;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.money.MoneyFormat;
import com.ammonium.adminshop.shop.ShopItem;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Shop Item as a clickable button
 */
public class ShopButton extends Button {
    private final ShopItem item;
    private TextureAtlasSprite fluidTexture;
    private int fluidTextureId;
    private float fluidColorR, fluidColorG, fluidColorB, fluidColorA;
    public boolean isMouseOn = false;

    public ShopButton(ShopItem item, int x, int y, OnPress listener) {
        super(x, y, 16, 16, Component.literal(" "), listener, DEFAULT_NARRATION);
        this.item = item;
        if(!item.isItem()) {
            Function<ResourceLocation, TextureAtlasSprite> spriteAtlas = Minecraft.getInstance()
                    .getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
            IClientFluidTypeExtensions properties = IClientFluidTypeExtensions.of(item.getFluid().getFluid());
            ResourceLocation resource = properties.getStillTexture();
            fluidTexture = spriteAtlas.apply(resource);
            TextureManager manager = Minecraft.getInstance().getTextureManager();
            AbstractTexture abstractTexture = manager.getTexture(InventoryMenu.BLOCK_ATLAS);
            TextureAtlas atlas = null;
            if (abstractTexture instanceof TextureAtlas) {
                atlas = (TextureAtlas) abstractTexture;
            }
            assert atlas != null;
            fluidTextureId = atlas.getId();
            int fcol = properties.getTintColor();
            fluidColorR = ((fcol >> 16) & 0xFF) / 255.0F;
            fluidColorG = ((fcol >> 8) & 0xFF) / 255.0F;
            fluidColorB = (fcol & 0xFF) / 255.0F;
            fluidColorA = ((fcol >> 24) & 0xFF) / 255.0F;
        }
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        int x = getX();
        int y = getY();
        PoseStack matrix = guiGraphics.pose();
        if(!visible)
            return;
        matrix.pushPose();

        //Draw item or fluid
        if(item.isItem()) {
            guiGraphics.renderItem(item.getItem(), x, y);
        } else { // Render Fluid
            RenderSystem.bindTexture(fluidTextureId);
            RenderSystem.setShaderColor(fluidColorR, fluidColorG, fluidColorB, fluidColorA);
            RenderSystem.setShaderTexture(0,
                    fluidTexture.atlasLocation());
            guiGraphics.blit(x, y,0, 16, 16, fluidTexture);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        }

        //Highlight background and write item name if hovered or focused
        if(isHovered()){
            isMouseOn = true;
            guiGraphics.fill(x, y, x+width, y+height, 0xFFFFFFDF);
        }else{
            isMouseOn = false;
        }
        //Write quantity based on buttons pressed (sneak & run)
        matrix.pushPose();
//        matrix.translate(0, 0, itemRenderer.blitOffset+201);
        matrix.translate(0, 0, 201);
        matrix.scale(.5f, .5f, 1);
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        guiGraphics.drawString(font, getQuantity()+"", 2*(x+16)- font.width(getQuantity()+""), 2*(y)+24, 0xFFFFFF);
        if(item.isTag()) {
            guiGraphics.drawString(font, "#", 2 * x + width * 2 - font.width("#") - 1, 2 * y + 1, 0xFFC921);
        } else if (item.hasNBT()) {
            guiGraphics.drawString(font, "+NBT", 2 * x + width * 2 - font.width("+NBT") - 1, 2 * y + 1, 0xFF55FF);
        }
        matrix.popPose();

        matrix.popPose();
    }

    public int getQuantity(){
        if(Screen.hasControlDown() && Screen.hasShiftDown())
            return item.isItem() ? 64 : 1000;
        else if(Screen.hasControlDown() || Screen.hasShiftDown())
            return item.isItem() ? 16 : 100;
        else
            return 1;
    }

    public List<Component> getTooltipContent(){
        long price = item.getPrice() * getQuantity();
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.literal(I18n.get("gui.money_message")+(Screen.hasAltDown() ? MoneyFormat.forcedFormat(price, MoneyFormat.FormatType.RAW) :
                MoneyFormat.forcedFormat(price, MoneyFormat.FormatType.SHORT))+
                " "+getQuantity()+((item.isItem()) ? "x " : "mb ")+item));
        if (item.getPermitTier() != 0) {
            tooltip.add(Component.literal("Requires Permit Tier: "+item.getPermitTier()));
        }
        return tooltip;
    }

    public ShopItem getShopItem(){
        return item;
    }
}