package com.ammonium.adminshop.setup;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public class Config {

    public static ForgeConfigSpec.LongValue STARTING_MONEY;
    public static ForgeConfigSpec.BooleanValue balanceDisplay;
    public static ForgeConfigSpec.BooleanValue displayFormat;

    public static void register(){
        ForgeConfigSpec.Builder serverConfig = new ForgeConfigSpec.Builder();
        registerServerConfigs(serverConfig);
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, serverConfig.build());
        ForgeConfigSpec.Builder clientConfig = new ForgeConfigSpec.Builder();
        registerClientConfigs(clientConfig);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, clientConfig.build());
    }

    private static void registerServerConfigs(ForgeConfigSpec.Builder config){
        config.comment("General configurations. Shop contents stored in \"adminshop.csv\"")
                .push("server_config");
        STARTING_MONEY = config
                .comment("Amount of money each player starts with. Must be a whole number.")
                .defineInRange("starting_money", 100, 0, Long.MAX_VALUE);
        config.pop();
    }

    private static void registerClientConfigs(ForgeConfigSpec.Builder config){
        config.comment("Client configurations. Options for changing display view")
                .push("display_config");

        balanceDisplay = config
                .comment("Displays your current balance and gained balance per second in the top left corner")
                .define("Balance Display", true);

        displayFormat = config
                .comment("If monetary values should be formatted as M/B/T/etc (Short) instead of Million/Billion/Trillion/etc (Full)")
                .define("Short mode ", true );
        config.pop();
    }

}
