package com.ammonium.adminshop.network;

import com.ammonium.adminshop.AdminShop;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class MojangAPI {

    private static final Map<String, String> storedResults = new HashMap<>();
    public static String getUsernameByUUID(String uuid) {
        // Search in stored results
        if (storedResults.containsKey(uuid)) {
            AdminShop.LOGGER.debug("Retrieving "+uuid+" to "+storedResults.get(uuid)+" from cache.");
            return storedResults.get(uuid);
        }
        // Search in mojang API
        AdminShop.LOGGER.debug("Name for "+uuid+" not found, using Mojang API...");
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
                // Extract the "name" field
                String name = jsonObj.get("name").getAsString();

                AdminShop.LOGGER.debug("Storing "+uuid+" to "+name+" in cache.");
                storedResults.put(uuid, name);
                return name;
            } else {
                AdminShop.LOGGER.error("Mojang API request failed: " + responseCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        AdminShop.LOGGER.error("No name found, returning UUID");
        return uuid;
    }
}
