package com.ammonium.adminshop.setup;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.money.ClientLocalData;
import com.ammonium.adminshop.network.PacketChangeDefaultAccount;
import com.google.gson.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

@OnlyIn(Dist.CLIENT)
public class ClientConfig {
    private static final Path CLIENT_CONFIG_FOLDER = FMLPaths.CONFIGDIR.get().resolve("adminshop").resolve("client");
    private static final Gson GSON = new Gson();
    private static Pair<String, Integer> defaultAccount = null;

    private static String getServerName() {
        String name;
        try {
            // Attempt to get the save directory name
            MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();
            if (server != null) {
                // Integrated server (singleplayer)
                name = server.getWorldData().getLevelName();
            } else {
                // Dedicated server or multiplayer
                ServerData serverData = Minecraft.getInstance().getCurrentServer();
                if (serverData == null) {
                    name = "default";
                } else {
                    name = serverData.ip;
                }
            }
        } catch (Exception e) {
            AdminShop.LOGGER.error("Error getting server name: "+e.getLocalizedMessage());
            e.printStackTrace();
            name = "default";
        }
        return name;
    }

    private static JsonObject loadClientData() {
        assert Minecraft.getInstance().level != null && Minecraft.getInstance().level.isClientSide;
        File clientDataFile = new File(CLIENT_CONFIG_FOLDER.toFile(), getServerName() + "_client.json");
        AdminShop.LOGGER.debug("Loading client data from "+clientDataFile);
        JsonObject readObject = new JsonObject();
        if (clientDataFile.exists()) {
            try (FileReader reader = new FileReader(clientDataFile)) {
                readObject = JsonParser.parseReader(reader).getAsJsonObject();
            } catch (IOException | JsonParseException | IllegalStateException e) {
                AdminShop.LOGGER.debug("Exception while loading client data: "+e.getLocalizedMessage());
                e.printStackTrace();
            }
        }

        return readObject;
    }

    private static void saveClientData(JsonObject data) {
        assert Minecraft.getInstance().level != null && Minecraft.getInstance().level.isClientSide;
        File clientDataFile = new File(CLIENT_CONFIG_FOLDER.toFile(), getServerName() + "_client.json");
        AdminShop.LOGGER.debug("Saving client config data to "+clientDataFile);
        // Make sure the directories exist
        clientDataFile.getParentFile().mkdirs();

        try (FileWriter writer = new FileWriter(clientDataFile)) {
            GSON.toJson(data, writer);
        } catch (IOException | JsonIOException e) {
            AdminShop.LOGGER.debug("Exception while saving client data: "+e.getLocalizedMessage());
            e.printStackTrace();
        }
    }
    // Gets the default account of the player
    public static Pair<String, Integer> getDefaultAccount() {
        // Get account from memory if possible
        if (defaultAccount != null) {
//            AdminShop.LOGGER.debug("Getting default account: "+defaultAccount.toString());
            return defaultAccount;
        }
        // Obtain from reading config
        LocalPlayer player = Minecraft.getInstance().player;
        assert player != null;
        JsonObject clientData = ClientConfig.loadClientData();

        // Default account: personal account
        defaultAccount = Pair.of(player.getStringUUID(), 1);

        if (clientData == null || clientData.isJsonNull()) {
            AdminShop.LOGGER.info("No default account data found");
            return defaultAccount;
        }
        if (clientData.has("accOwner") && clientData.has("accId")) {
            String accOwner = clientData.get("accOwner").getAsString();
            int accId = clientData.get("accId").getAsInt();
            Pair<String, Integer> account = Pair.of(accOwner, accId);
            // Only set new account if player actually has access to it
            if (ClientLocalData.getUsableAccountsStatic().contains(account)) {
                defaultAccount = account;
            }
        }
//        AdminShop.LOGGER.debug("Getting default account: "+defaultAccount.toString());
        return defaultAccount;
    }

    public static void setDefaultAccount(Pair<String, Integer> account) {
        assert Minecraft.getInstance().level != null && Minecraft.getInstance().level.isClientSide;
        assert Minecraft.getInstance().player != null;

        defaultAccount = account;

        JsonObject clientData = new JsonObject();
        clientData.addProperty("accOwner", account.getKey());
        clientData.addProperty("accId", account.getValue());
        ClientConfig.saveClientData(clientData);
        // Send change packet to server
        Messages.sendToServer(new PacketChangeDefaultAccount(Minecraft.getInstance().player.getStringUUID(), account.getKey(), account.getValue()));
    }

}
