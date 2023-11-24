package com.ammonium.adminshop;

import org.slf4j.Logger;

import com.ammonium.adminshop.client.events.ServerEventListeners;
import com.ammonium.adminshop.setup.ClientSetup;
import com.ammonium.adminshop.setup.Config;
import com.ammonium.adminshop.setup.ModSetup;
import com.ammonium.adminshop.setup.Registration;
import com.mojang.logging.LogUtils;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(AdminShop.MODID)
public class AdminShop {
    public static final String MODID = "adminshop";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();
    
    public AdminShop() {
        Registration.init();
        IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();
        Config.register();

        eventBus.addListener(ModSetup::init);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> eventBus.addListener(ClientSetup::init));
        MinecraftForge.EVENT_BUS.register(ServerEventListeners.class);
    }
}
