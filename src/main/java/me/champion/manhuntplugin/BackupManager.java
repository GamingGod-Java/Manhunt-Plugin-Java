package me.champion.manhuntplugin;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;
import java.util.logging.Level;

public class BackupManager {

    private final Manhunt plugin;
    private final TeamManager teamManager;
    private final File backupFile;
    private final FileConfiguration backupConfig;

    public BackupManager(Manhunt plugin, TeamManager teamManager) {
        this.plugin = plugin;
        this.teamManager = teamManager;
        this.backupFile = new File(plugin.getDataFolder(), "backup.yml");
        this.backupConfig = YamlConfiguration.loadConfiguration(backupFile);
    }

    /**
     * Saves all online players' data, including username, world, location (last location in each world),
     * timestamp, and team to the backup file.
     */
    public void backupPlayerData() {
        // Check if there are any online players
        if (Bukkit.getOnlinePlayers().isEmpty()) {
            plugin.getLogger().info("No players online. Backup not performed.");
            return; // Exit the method if there are no players online
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String timestamp = dateFormat.format(new Date());

        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID playerUUID = player.getUniqueId();
            String playerName = player.getName();
            World currentWorld = player.getWorld();
            Location currentLocation = player.getLocation();

            // Get the player's team from TeamManager
            String team = teamManager.isOnTeam(player, "Runners") ? "Runners" :
                    teamManager.isOnTeam(player, "Zombies") ? "Zombies" : "None";

            // Create a section for each player using UUID as the unique identifier
            String path = "players." + playerUUID.toString();
            backupConfig.set(path + ".username", playerName);
            backupConfig.set(path + ".team", team);
            backupConfig.set(path + ".timestamp", timestamp);

            // Save only the location for the current world the player is in
            String currentWorldPath = path + ".worlds." + currentWorld.getName();
            backupConfig.set(currentWorldPath + ".x", currentLocation.getX());
            backupConfig.set(currentWorldPath + ".y", currentLocation.getY());
            backupConfig.set(currentWorldPath + ".z", currentLocation.getZ());
            backupConfig.set(currentWorldPath + ".pitch", currentLocation.getPitch());
            backupConfig.set(currentWorldPath + ".yaw", currentLocation.getYaw());
        }

        // Save changes to backup.yml
        try {
            backupConfig.save(backupFile);
            plugin.getLogger().info("Player data successfully backed up to backup.yml.");
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save backup.yml", e);
        }
    }
}
