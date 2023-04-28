package com.vnator.adminshop.setup;

import com.vnator.adminshop.blocks.ModBlocks;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

public class ModSetup {

    public static final String TAB_NAME = "adminshop";

    public static final CreativeModeTab ITEM_GROUP = new CreativeModeTab(TAB_NAME) {
        @Override
        public ItemStack makeIcon() {
            return new ItemStack(ModBlocks.SHOP.get());
        }
    };

    public static void init(FMLCommonSetupEvent event){
        Messages.register();
    }
}
