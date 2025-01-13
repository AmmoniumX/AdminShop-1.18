package com.ammonium.adminshop.network;

import com.ammonium.adminshop.AdminShop;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class MojangAPI {
    private static final Map<String, String> storedResults = new HashMap<>();

    private static void storeInCache(String uuid, String name) {
        AdminShop.LOGGER.debug("Storing {} to {} in cache.", uuid, name);
        storedResults.put(uuid, name);
    }

    public static String getUsernameByUUID(String uuid) {
        // Search in cached results
        if (storedResults.containsKey(uuid)) {
            AdminShop.LOGGER.debug("Retrieving {} to {} from cache.", uuid, storedResults.get(uuid));
            return storedResults.get(uuid);
        }

        // Search in online players
        IntegratedServer server = Minecraft.getInstance().getSingleplayerServer();
        if (server == null) {
            AdminShop.LOGGER.error("Server is null, cannot get player list.");
        } else {
            PlayerList playerList = server.getPlayerList();
            for (ServerPlayer player : playerList.getPlayers()) {
                String playerUUID = player.getUUID().toString();
                String playerName = player.getName().getString();
                if (playerUUID.equals(uuid)) {
                    storeInCache(uuid, playerName);
                    return playerName;
                }
            }
        }

        // Search in mojang API
        AdminShop.LOGGER.debug("Name for {} not found, using Mojang API...", uuid);
        String apiResult = makeMojangAPIRequest(uuid);
        if (apiResult != null) {
            storeInCache(uuid, apiResult);
            return apiResult;
        }

        // If all else fails, return the UUID
        AdminShop.LOGGER.info("No name found, returning UUID");
        return uuid;
    }

    private static String makeMojangAPIRequest(String uuid) {
        try {
            URL url = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                // Extract the username from the response JSON
                String jsonResponse = response.toString();
                JsonObject jsonObj = JsonParser.parseString(jsonResponse).getAsJsonObject();
                if (jsonObj.has("name")) {
                    return jsonObj.get("name").getAsString();
                } else {
                    AdminShop.LOGGER.warn("Mojang API response does not contain 'name' field: {}", jsonResponse);
                }
            } else {
                AdminShop.LOGGER.warn("Mojang API request failed: {}", responseCode);
            }
        }
        catch (IOException e) {
            AdminShop.LOGGER.warn("Network exception while getting username. Will use UUID for the remainder of the session!");
            return uuid;
        } catch (Exception e) {
            AdminShop.LOGGER.error("Unhandled exception while getting username: {}", e.toString());
        }
        return null;
    }
}