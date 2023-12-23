package com.ammonium.adminshop.blocks;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.item.LoreBlockItem;
import com.ammonium.adminshop.item.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.MaterialColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public class ModBlocks {
    public static final Material machineBlock = new Material(MaterialColor.METAL, false, true, true, true, false, false, PushReaction.BLOCK);
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, AdminShop.MODID);

    public static final RegistryObject<Block> SHOP = registerLoreBlock("shop",
            ShopBlock::new, "Buy and Sell Items!");
    public static final RegistryObject<Block> BUYER_1 = registerLoreBlock("buyer_1",
            BuyerBlock::new, "Automatically buys up to 4 items/second");

    public static final RegistryObject<Block> BUYER_2 = registerLoreBlock("buyer_2",
            Buyer2Block::new, "Automatically buys up to 16 items/second");
    public static final RegistryObject<Block> BUYER_3 = registerLoreBlock("buyer_3",
            Buyer3Block::new, "Automatically buys up to 64 items/second");

    public static final RegistryObject<Block> SELLER = registerLoreBlock("seller",
            SellerBlock::new, "Automatically buys up to 64 items/second");

    public static final RegistryObject<Block> FLUID_BUYER = registerLoreBlock("fluid_buyer",
            FluidBuyerBlock::new, "Automatically buys up to 4000mb/second");

    public static final RegistryObject<Block> FLUID_SELLER = registerLoreBlock("fluid_seller",
            FluidSellerBlock::new, "Automatically sells up to 64000mb/second");

    public static final RegistryObject<Block> DETECTOR = registerLoreBlock("detector",
            BasicDetector::new, "Outputs full redstone signal if greater than threshold");

    public static final RegistryObject<Block> ADVANCED_DETECTOR = registerLoreBlock("adv_detector",
            AdvancedDetector::new, "Outputs analog redstone signal proportional on balance between 0 and threshold");

    private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> block) {
        RegistryObject<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <T extends Block> RegistryObject<Item> registerBlockItem(String name, RegistryObject<T> block) {
        return ModItems.ITEMS.register(name, () -> new BlockItem(block.get(),
                new Item.Properties()));
    }

    private static <T extends Block> RegistryObject<T> registerLoreBlock(String name, Supplier<T> block,
                                                                         String lore) {
        RegistryObject<T> toReturn = BLOCKS.register(name, block);
        registerLoreBlockItem(name, toReturn, lore);
        return toReturn;
    }

    private static <T extends Block> RegistryObject<Item> registerLoreBlockItem(String name, RegistryObject<T> block, String lore) {
        return ModItems.ITEMS.register(name, () -> new LoreBlockItem(block.get(), new Item.Properties(), lore));
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}