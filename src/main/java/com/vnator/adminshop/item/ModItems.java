package com.vnator.adminshop.item;

import com.vnator.adminshop.AdminShop;
import com.vnator.adminshop.setup.ModSetup;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, AdminShop.MODID);

    public static final RegistryObject<Item> CHECK = ITEMS.register("check",
            () -> new Item(new Item.Properties().tab(ModSetup.ITEM_GROUP)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}