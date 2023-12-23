package com.ammonium.adminshop;

import com.ammonium.adminshop.blocks.ModBlocks;
import com.ammonium.adminshop.blocks.entity.ModBlockEntities;
import com.ammonium.adminshop.client.events.ServerEventListeners;
import com.ammonium.adminshop.item.ModItems;
import com.ammonium.adminshop.screen.ModMenuTypes;
import com.ammonium.adminshop.setup.ClientSetup;
import com.ammonium.adminshop.setup.Config;
import com.ammonium.adminshop.setup.ModSetup;
import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.CreativeModeTabEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(AdminShop.MODID)
public class AdminShop {
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final String MODID = "adminshop";

    public AdminShop() {

        IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();
        Config.register();

        eventBus.addListener(ModSetup::init);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> eventBus.addListener(ClientSetup::init));
        MinecraftForge.EVENT_BUS.register(ServerEventListeners.class);

        ModItems.register(eventBus);
        ModBlocks.register(eventBus);
        ModBlockEntities.register(eventBus);
        ModMenuTypes.register(eventBus);
    }

    private void setup(final FMLCommonSetupEvent event) {
        // some preinit code
//        LOGGER.info("HELLO FROM PREINIT");
    }

    // You can use EventBusSubscriber to automatically subscribe events on the contained class (this is subscribing to the FORGE
    // Event bus for receiving Forge Events)
    @Mod.EventBusSubscriber(modid = AdminShop.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ForgeEvents {

    }

    @Mod.EventBusSubscriber(modid = AdminShop.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModEvents {
        @SubscribeEvent
        public static void buildContents(CreativeModeTabEvent.Register event) {
            event.registerCreativeModeTab(new ResourceLocation(AdminShop.MODID, "creativetab"), builder ->
                    // Set name of tab to display
                    builder.title(Component.translatable("item_group." + AdminShop.MODID))
                            // Set icon of creative tab
                            .icon(() -> new ItemStack(ModBlocks.SHOP.get()))
                            // Add default items to tab
                            .displayItems((params, output) -> {
                                output.accept(ModItems.PERMIT.get());
                                output.accept(ModBlocks.SHOP.get());
                                output.accept(ModBlocks.BUYER_1.get());
                                output.accept(ModBlocks.BUYER_2.get());
                                output.accept(ModBlocks.BUYER_3.get());
                                output.accept(ModBlocks.SELLER.get());
                                output.accept(ModBlocks.FLUID_BUYER.get());
                                output.accept(ModBlocks.FLUID_SELLER.get());
                                output.accept(ModItems.TABLET.get());
                                output.accept(ModBlocks.DETECTOR.get());
                                output.accept(ModBlocks.ADVANCED_DETECTOR.get());
                            })
            );
        }
    }

}
