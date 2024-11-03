package me.champion.manhuntplugin;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class SpawnSetup {

    private final JavaPlugin plugin;

    public SpawnSetup(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Ensures the world_spawn folder exists with the necessary region files in the server root.
     */
    public void setupWorldSpawn() {
        File serverWorldSpawnFolder = new File(Bukkit.getWorldContainer(), "world_spawn");
        File serverRegionFolder = new File(serverWorldSpawnFolder, "region");

        // Create world_spawn/region folder if it doesn't exist
        if (!serverRegionFolder.exists()) {
            serverRegionFolder.mkdirs();
            System.out.println("[Manhunt] Created world_spawn/region folder.");
        }

        // Copy region files from the plugin's resources if missing
        copyRegionFilesIfAbsent("world_spawn/region", serverRegionFolder);
    }

    /**
     * Copies missing region files from the JAR's resources to the world_spawn/region folder in the server root.
     *
     * @param resourcePath      The path within the JAR to copy from.
     * @param destinationFolder The destination folder on the server.
     */
    private void copyRegionFilesIfAbsent(String resourcePath, File destinationFolder) {
        try {
            File jarFile = new File(getClass().getProtectionDomain().getCodeSource().getLocation().getPath());

            if (jarFile.isFile()) { // Ensure it's a JAR file
                try (JarFile jar = new JarFile(jarFile)) {
                    Enumeration<JarEntry> entries = jar.entries();

                    while (entries.hasMoreElements()) {
                        JarEntry entry = entries.nextElement();
                        String entryName = entry.getName();

                        // Only process files in the resourcePath and skip if they already exist in the destination
                        if (entryName.startsWith(resourcePath) && !entry.isDirectory()) {
                            File destFile = new File(destinationFolder, entryName.substring(resourcePath.length() + 1));
                            if (!destFile.exists()) {
                                System.out.println("[Manhunt] Copying missing region file: " + entryName);
                                try (InputStream in = jar.getInputStream(entry)) {
                                    Files.copy(in, destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                                }
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("[Manhunt] Failed to copy region files: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
