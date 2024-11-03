package me.champion.manhuntplugin.commands;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import me.champion.manhuntplugin.Manhunt;

import java.io.File;

public class MhWorld implements CommandExecutor {

    private final Manhunt plugin;
    private static final String CUSTOM_WORLD_NAME = "CustomWorld";  // The name of the custom world folder

    public MhWorld(Manhunt plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // No need to take world name as an argument
        if (args.length > 0) {
            sender.sendMessage("Usage: /MhWorld");
            return false;
        }

        // Unload existing world if it exists
        World existingWorld = Bukkit.getWorld(CUSTOM_WORLD_NAME);
        if (existingWorld != null) {
            Bukkit.unloadWorld(existingWorld, false);
            deleteWorld(existingWorld.getWorldFolder());
        }

        // Create a new world in the folder named "CustomWorld"
        WorldCreator worldCreator = new WorldCreator(CUSTOM_WORLD_NAME);
        World newWorld = Bukkit.createWorld(worldCreator);

        if (newWorld != null) {
            sender.sendMessage("World '" + CUSTOM_WORLD_NAME + "' has been generated successfully.");
        } else {
            sender.sendMessage("Failed to generate world '" + CUSTOM_WORLD_NAME + "'.");
        }

        return true;
    }

    // Utility method to delete the world folder
    private void deleteWorld(File path) {
        if (path.exists()) {
            File[] files = path.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteWorld(file);  // Recursively delete directories
                    } else {
                        file.delete();  // Delete files
                    }
                }
            }
        }
        path.delete();  // Delete the root folder
    }
}
