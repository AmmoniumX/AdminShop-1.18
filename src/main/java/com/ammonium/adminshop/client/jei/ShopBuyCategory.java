package com.ammonium.adminshop.client.jei;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.setup.Registration;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class ShopBuyCategory implements IRecipeCategory<ShopBuyWrapper>{
    public static final RecipeType<ShopBuyWrapper> SHOP_RECIPE_TYPE =
            new RecipeType<>(new ResourceLocation(AdminShop.MODID, "buy_recipe_type"), ShopBuyWrapper.class);
    private final ResourceLocation GUI = new ResourceLocation(AdminShop.MODID, "textures/gui/jei_buy_category.png");
    private final IDrawable background;
    private final IDrawable icon;

    public ShopBuyCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createDrawable(GUI, 0, 0, 110, 100);
        this.icon = guiHelper.createDrawableItemStack(Registration.BUYER_1.get().asItem().getDefaultInstance());
    }

    @Override
    public @NotNull RecipeType<ShopBuyWrapper> getRecipeType() {
        return SHOP_RECIPE_TYPE;
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.translatable("jei.category.buy.title");
    }

    @Override
    public @NotNull IDrawable getBackground() {
        return background;
    }

    @Override
    public @NotNull IDrawable getIcon() {
        return icon;
    }

    @Override
    public void draw(ShopBuyWrapper recipe, IRecipeSlotsView recipeSlotsView,  GuiGraphics guiGraphics, double mouseX, double mouseY) {
        IRecipeCategory.super.draw(recipe, recipeSlotsView, guiGraphics, mouseX, mouseY);
        int priceX = 8;
        int priceY = 60;
        int tierX = 8;
        int tierY = 80;

        // Draw the price
        String priceText = "Buy Price: "+recipe.getPrice();
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        guiGraphics.drawString(font, priceText, priceX, priceY, 0xFF555555);

        // Draw the required tier
        String tierText = "Requires Tier: "+((recipe.getRequiresTier() == 0) ? "None" : recipe.getRequiresTier());
        guiGraphics.drawString(font, tierText, tierX, tierY, 0xFF555555);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ShopBuyWrapper recipe, IFocusGroup focuses) {
        IRecipeSlotBuilder slotBuilder = builder.addSlot(RecipeIngredientRole.OUTPUT, 67, 5);
        if (recipe.isItem()) {
            slotBuilder.addItemStack(recipe.getBuyItem());
        } else {
            slotBuilder.addFluidStack(recipe.getBuyFluid(), 1000);
        }
    }
}