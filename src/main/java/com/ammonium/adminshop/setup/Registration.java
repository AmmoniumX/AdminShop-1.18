package com.ammonium.adminshop.setup;

import com.ammonium.adminshop.block.*;
import com.ammonium.adminshop.block.entity.*;
import com.ammonium.adminshop.client.screen.*;
import com.ammonium.adminshop.item.LoreBlockItem;
import com.ammonium.adminshop.item.LoreItem;
import com.ammonium.adminshop.item.ShopTablet;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

import static com.ammonium.adminshop.AdminShop.MODID;

public class Registration {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static void init() {
        IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();

        BLOCKS.register(eventBus);
        BLOCK_ENTITIES.register(eventBus);
        ITEMS.register(eventBus);
        MENUS.register(eventBus);
        CREATIVE_MODE_TABS.register(eventBus);
        registerCreativeTabItems();
    }

    public static final RegistryObject<Block> SHOP = registerLoreBlock("shop", ShopBlock::new, "Buy and Sell Items!");
    public static final RegistryObject<Block> BUYER_1 = registerLoreBlock("buyer_1", BuyerBlock::new, "Automatically buys up to 4 items/second");
    public static final RegistryObject<Block> BUYER_2 = registerLoreBlock("buyer_2", Buyer2Block::new, "Automatically buys up to 16 items/second");
    public static final RegistryObject<Block> BUYER_3 = registerLoreBlock("buyer_3", Buyer3Block::new, "Automatically buys up to 64 items/second");
    public static final RegistryObject<Block> SELLER = registerLoreBlock("seller", SellerBlock::new, "Automatically sells up to 4 items/second");
    public static final RegistryObject<Block> FLUID_BUYER = registerLoreBlock("fluid_buyer", FluidBuyerBlock::new, "Automatically buys up to 4000mb/second");
    public static final RegistryObject<Block> FLUID_SELLER = registerLoreBlock("fluid_seller", FluidSellerBlock::new, "Automatically sells up to 64000mb/second");
    public static final RegistryObject<Block> DETECTOR = registerLoreBlock("detector", BasicDetector::new, "Outputs full redstone signal if greater than threshold");
    public static final RegistryObject<Block> ADVANCED_DETECTOR = registerLoreBlock("adv_detector", AdvancedDetector::new, "Outputs analog redstone signal proportional on balance between 0 and threshold");

    public static final RegistryObject<BlockEntityType<SellerBE>> SELLER_BE = BLOCK_ENTITIES.register("seller",
        () -> BlockEntityType.Builder.of(SellerBE::new, SELLER.get()).build(null));
    public static final RegistryObject<BlockEntityType<ShopBE>> SHOP_BE = BLOCK_ENTITIES.register("shop",
         () -> BlockEntityType.Builder.of(ShopBE::new, SHOP.get()).build(null));

    public static final RegistryObject<BlockEntityType<BuyerBE>> BUYER_1_BE = BLOCK_ENTITIES.register("buyer_1",
        () -> BlockEntityType.Builder.of(BuyerBE::new, BUYER_1.get()).build(null));
    public static final RegistryObject<BlockEntityType<Buyer2BE>> BUYER_2_BE = BLOCK_ENTITIES.register("buyer_2",
        () -> BlockEntityType.Builder.of(Buyer2BE::new, BUYER_2.get()).build(null));
    public static final RegistryObject<BlockEntityType<Buyer3BE>> BUYER_3_BE = BLOCK_ENTITIES.register("buyer_3",
        () -> BlockEntityType.Builder.of(Buyer3BE::new, BUYER_3.get()).build(null));
    public static final RegistryObject<BlockEntityType<FluidBuyerBE>> FLUID_BUYER_BE = BLOCK_ENTITIES.register("fluid_buyer",
        () -> BlockEntityType.Builder.of(FluidBuyerBE::new, FLUID_BUYER.get()).build(null));
    public static final RegistryObject<BlockEntityType<FluidSellerBE>> FLUID_SELLER_BE = BLOCK_ENTITIES.register("fluid_seller",
        () -> BlockEntityType.Builder.of(FluidSellerBE::new, FLUID_SELLER.get()).build(null));
    public static final RegistryObject<BlockEntityType<BasicDetectorBE>> BASIC_DETECTOR_BE = BLOCK_ENTITIES.register("detector",
            () -> BlockEntityType.Builder.of(BasicDetectorBE::new, DETECTOR.get()).build(null));

    public static final RegistryObject<BlockEntityType<AdvancedDetectorBE>> ADVANCED_DETECTOR_BE = BLOCK_ENTITIES.register("adv_detector",
            () -> BlockEntityType.Builder.of(AdvancedDetectorBE::new, ADVANCED_DETECTOR.get()).build(null));

    public static final RegistryObject<MenuType<ShopMenu>> SHOP_MENU = MENUS.register("shop_menu",
        () -> IForgeMenuType.create(((windowId, inv, data) -> new ShopMenu(windowId, inv, inv.player))));
    public static final RegistryObject<MenuType<SellerMenu>> SELLER_MENU = MENUS.register("seller_menu",
        () -> IForgeMenuType.create((SellerMenu::new)));
    public static final RegistryObject<MenuType<BuyerMenu>> BUYER_MENU = MENUS.register("buyer_menu",
        () -> IForgeMenuType.create((BuyerMenu::new)));
    public static final RegistryObject<MenuType<Buyer2Menu>> BUYER_2_MENU = MENUS.register("buyer_2_menu",
        () -> IForgeMenuType.create((Buyer2Menu::new)));
    public static final RegistryObject<MenuType<Buyer3Menu>> BUYER_3_MENU = MENUS.register("buyer_3_menu",
        () -> IForgeMenuType.create((Buyer3Menu::new)));
    public static final RegistryObject<MenuType<FluidBuyerMenu>> FLUID_BUYER_MENU = MENUS.register("fluid_buyer_menu",
        () -> IForgeMenuType.create((FluidBuyerMenu::new)));
    public static final RegistryObject<MenuType<FluidSellerMenu>> FLUID_SELLER_MENU = MENUS.register("fluid_seller_menu",
        () -> IForgeMenuType.create((FluidSellerMenu::new)));
    public static final RegistryObject<MenuType<BasicDetectorMenu>> BASIC_DETECTOR_MENU = MENUS.register("basic_detector_menu",
            () -> IForgeMenuType.create((BasicDetectorMenu::new)));
    public static final RegistryObject<MenuType<AdvancedDetectorMenu>> ADVANCED_DETECTOR_MENU = MENUS.register("advanced_detector_menu",
            () -> IForgeMenuType.create((AdvancedDetectorMenu::new)));

    public static final RegistryObject<Item> PERMIT = ITEMS.register("permit",
        () -> new LoreItem(new Item.Properties(), "Shift-click inside a shop to unlock new trades"));
    public static final RegistryObject<Item> TABLET = ITEMS.register("tablet", ShopTablet::new);

    private static <T extends Block> RegistryObject<T> registerLoreBlock(String name, Supplier<T> block, String lore) {
        RegistryObject<T> toReturn = BLOCKS.register(name, block);
        registerLoreBlockItem(name, toReturn, lore);
        return toReturn;
    }

    private static <T extends Block> RegistryObject<Item> registerLoreBlockItem(String name, RegistryObject<T> block, String lore) {
        return ITEMS.register(name, () -> new LoreBlockItem(block.get(), new Item.Properties(), lore));
    }

    private static void registerCreativeTabItems() {
        CREATIVE_MODE_TABS.register("creativetab", () -> CreativeModeTab.builder()
        // Set name of tab to display
            .title(Component.translatable("item_group." + MODID + ".creativetab"))
            // Set icon of creative tab
            .icon(() -> new ItemStack(Registration.SHOP.get()))
            // Add default items to tab
            .displayItems((params, output) -> {
                Registration.ITEMS.getEntries().forEach(e -> output.accept(e.get()));
            })
            .build());
    }
}
