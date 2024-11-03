package me.champion.manhuntplugin;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.Location;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.IOException;

public class PluginSetup {

    public void setupFolder() {
        File pluginsFolder = new File("plugins");
        File manhuntFolder = new File(pluginsFolder, "Manhunt");
        if (!manhuntFolder.exists()) {
            manhuntFolder.mkdirs();
            System.out.println("Folder created by PluginSetup");
        }

        File sessionsFolder = new File(manhuntFolder, "sessions");
        if (!sessionsFolder.exists()) {
            sessionsFolder.mkdirs();
            System.out.println("Sessions folder created successfully.");
        }

        File playerDataFile = new File(manhuntFolder, "playerdata.yml");
        if (!playerDataFile.exists()) {
            try {
                playerDataFile.createNewFile();
                System.out.println("playerdata.yml created successfully.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        File configFile = new File(manhuntFolder, "config.yml");
        if (!configFile.exists()) {
            createDefaultConfig(configFile);
        } else {
            System.out.println("config.yml already exists.");
        }
    }

    private void createDefaultConfig(File configFile) {
        try {
            configFile.createNewFile();
            System.out.println("Config.yml created successfully.");

            // Initialize default configuration values
            FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);

            int overworldSize = 3000; // Default overworld size in radius
            long gameTimer = 8990; // Default game time in seconds (2 hours, 29 minutes, and 50 seconds)

            config.set("overworldSize", overworldSize);
            config.set("netherSize", overworldSize / 8);
            config.set("gameTimer", gameTimer); // Store default game timer directly as seconds
            // Define default world spawn coordinates
            Location defaultWorldSpawn = new Location(Bukkit.getWorld("world"), 0, 100, 0);

            // Save as a single string in the format "x,y,z"
            String worldSpawnString = defaultWorldSpawn.getBlockX() + "," + defaultWorldSpawn.getBlockY() + "," + defaultWorldSpawn.getBlockZ();
            config.set("worldSpawn", worldSpawnString);

            config.save(configFile);
            System.out.println("Default configuration values set successfully.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
