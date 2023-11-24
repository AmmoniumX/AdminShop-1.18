package com.ammonium.adminshop.client.screen.slot;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.shop.Shop;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

import java.util.Optional;

public class ModShopInputSlot extends SlotItemHandler {
    public ModShopInputSlot(IItemHandler itemHandler, int index, int x, int y) {
        super(itemHandler, index, x, y);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        boolean isShopItem = Shop.get().hasSellShopItem(stack.getItem());
        if (!isShopItem) {
            // Check if item tags are in item tags map
            Optional<TagKey<Item>> searchTag = stack.getTags().filter(itemTag -> Shop.get().hasSellShopItemTag(itemTag)).findFirst();
            isShopItem = searchTag.isPresent();
        }
        if (!isShopItem) {
            AdminShop.LOGGER.debug("Item is not in shop sell map: "+stack.getDisplayName().getString());
        }
        return isShopItem;
    }
    @Override
    public boolean mayPickup(Player playerIn) {
        return true;
    }

    @Override
    public int getMaxStackSize(ItemStack pStack) {
        return super.getMaxStackSize(pStack);
    }

}
