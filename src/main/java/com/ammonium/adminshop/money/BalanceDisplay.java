package com.ammonium.adminshop.money;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.setup.ClientConfig;
import com.ammonium.adminshop.setup.Config;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = AdminShop.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class BalanceDisplay {
    private static long balance = 0;
    private static final long[] history = new long[]{0, 0};
    private static long lastBalance = 0;
    private static int tick = 0;

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent event) {
        if (!Config.balanceDisplay.get()) return;
        if (Minecraft.getInstance().player == null) return;
        tick++;
        if (event.phase == TickEvent.Phase.END && tick >= 20) {
            tick = 0;
            balance = ClientLocalData.getMoney(ClientConfig.getDefaultAccount());
            history[1] = history[0];
            history[0] = balance - lastBalance;
            lastBalance = balance;
        }
    }

    private static void reset() {
        balance = lastBalance = 0;
        tick = 0;
    }

    @SubscribeEvent
    public static void clientDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        if (!Config.balanceDisplay.get()) return;
        reset();
    }

    @SubscribeEvent
    public static void onRenderGUI(CustomizeGuiOverlayEvent.DebugText event) {
        if (!Config.balanceDisplay.get()) return;
        long avg = history[0] + history[1];
        String str = MoneyFormat.cfgformat(balance);
        if (avg != 0) str += " " + (avg > 0 ? (ChatFormatting.GREEN + "+") : (ChatFormatting.RED)) + MoneyFormat.format(avg, MoneyFormat.FormatType.SHORT, MoneyFormat.FormatType.RAW) + "/s";
        event.getLeft().add(Component.translatable("gui.balance", str).getString());

    }

}