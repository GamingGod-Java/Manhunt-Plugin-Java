package me.champion.manhuntplugin;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public final class Manhunt extends JavaPlugin {

    private TeamManager teamManager;
    private MhStart mhStart; // Add this field

    File configFile = new File(getDataFolder(), "playerdata.yml");
    FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);

    @Override
    public void onLoad() {
        getLogger().info("Manhunt plugin is loading!");
    }

    @Override
    public void onEnable() {
        getLogger().info("Manhunt plugin has started, have a nice day! :)");

        // Clear the configuration
        config = new YamlConfiguration();

        // Save the empty configuration to the file
        try {
            config.save(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Emptied playerdata.yml");

        // Initialize TeamManager
        teamManager = new TeamManager(this);
        // Initialize MhStart as a local variable and store it in the field
        mhStart = new MhStart(teamManager);

        //Set world border for Overworld and Nether
        setWorldBorder();

        MhRestart mhRestart = new MhRestart(mhStart, teamManager);

        // Register commands and their executors
        getCommand("MhCreate").setExecutor(new MhCreate(this, teamManager));
        getCommand("MhMove").setExecutor(new TeamMove(teamManager));
        getCommand("MhPause").setExecutor(new MhPause(this, teamManager));
        getCommand("MhUnpause").setExecutor(new MhUnpause(teamManager));
        getCommand("MhReady").setExecutor(new MhReady(teamManager, this));
        getCommand("MhCompass").setExecutor(new MhCompass(teamManager, this));
        getCommand("MhStart").setExecutor(mhStart); // Use the mhStart instance
        getCommand("MhRestart").setExecutor(mhRestart); // Use the mhRestart instance

        Bukkit.getServer().getPluginManager().registerEvents(new TeamSelection(this, teamManager, mhStart), this);
        Bukkit.getServer().getPluginManager().registerEvents(new TeamManager(this), this);
        Bukkit.getServer().getPluginManager().registerEvents(new MhCompass(teamManager, this), this);
        Bukkit.getServer().getPluginManager().registerEvents(new WinCondition(teamManager, mhStart), this);
    }

    @Override
    public void onDisable() {
        getLogger().info("Manhunt plugin has stopped!");

        // Clear teams if TeamManager is initialized
        if (teamManager != null) {
            teamManager.clearTeams();
        }
    }

    public TeamManager getTeamManager() {
        return teamManager;
    }

    // Method to get an instance of the plugin
    public static Manhunt getPlugin() {
        return JavaPlugin.getPlugin(Manhunt.class);
    }

    // Set the world border size for both Overworld and Nether
    private void setWorldBorder() {
        World overworld = Bukkit.getWorlds().get(0); // Get the first world (Overworld)
        World nether = Bukkit.getWorlds().get(1); // Get the second world (Nether)

        double overworldSize = 5000.0;
        double netherSize = overworldSize / 8; // Nether is 1/8th the size of the Overworld

        // Set the Overworld world border size
        setWorldBorderSize(overworld, overworldSize);

        // Set the Nether world border size
        setWorldBorderSize(nether, netherSize);

        getLogger().info("World border has been set to " + overworldSize + " blocks in the Overworld and " + netherSize + " blocks in the Nether by Manhunt plugin.");
    }

    // Set the world border size for a specific world
    private void setWorldBorderSize(World world, double size) {
        WorldBorder worldBorder = world.getWorldBorder();
        worldBorder.setSize(size);
    }
}