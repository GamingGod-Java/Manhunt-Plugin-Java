package me.champion.manhuntplugin;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class PluginSetup {

    public void setupFolder() {
        // Get the server's plugins directory
        File pluginsFolder = new File("plugins");

        // Create a new folder called "Manhunt" if it doesn't exist
        File manhuntFolder = new File(pluginsFolder, "Manhunt");
        if (!manhuntFolder.exists()) {
            manhuntFolder.mkdirs(); // Create the folder and any necessary parent folders
            System.out.println("Folder created by PluginSetup");
        }

        // Create a "sessions" folder inside "Manhunt" if it doesn't exist
        File sessionsFolder = new File(manhuntFolder, "sessions");
        if (!sessionsFolder.exists()) {
            sessionsFolder.mkdirs();
            System.out.println("Sessions folder created successfully.");
        }

        // Create a "playerdata.yml" file inside "Manhunt" if it doesn't exist
        File playerDataFile = new File(manhuntFolder, "playerdata.yml");
        if (!playerDataFile.exists()) {
            try {
                playerDataFile.createNewFile();
                System.out.println("playerdata.yml created successfully.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Create or load the "Config.yml" file inside "Manhunt"
        File configFile = new File(manhuntFolder, "config.yml");
        if (!configFile.exists()) {
            // Config file doesn't exist, create it and set default values
            createDefaultConfig(configFile);
        } else {
            // Log a message indicating that the config file exists
            System.out.println("config.yml already exists.");
        }
    }

    private void createDefaultConfig(File configFile) {
        try {
            configFile.createNewFile();
            System.out.println("Config.yml created successfully.");

            // Initialize default configuration values
            FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);

            int overworldSize = config.getInt("overworldSize", 3000); // Get overworld size from config or default to 3000

            config.set("overworldSize", overworldSize); // Set default overworld size to 3000 in radius
            config.set("netherSize", overworldSize / 8); // Set nether size to overworldSize / 8
            config.save(configFile); // Save the configuration
            System.out.println("Default configuration values set successfully.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
