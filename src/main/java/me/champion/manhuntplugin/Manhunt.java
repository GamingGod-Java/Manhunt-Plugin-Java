package me.champion.manhuntplugin;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.plugin.java.JavaPlugin;
public final class Manhunt extends JavaPlugin {
    private TeamManager teamManager;
    @Override
    public void onLoad() {
        getLogger().info("Manhunt plugin is loading!");
    }

    @Override
    public void onEnable() {
        getLogger().info("Manhunt plugin has started, have a nice day! :)");

        // Initialize TeamManager
        teamManager = new TeamManager(this);

        MhPause mhPause = new MhPause(this, teamManager);
        // Initialize MhStart as a local variable
        MhStart mhStart = new MhStart(teamManager);

        //Set world border for Overworld and Nether
        setWorldBorder();


        // Register commands and their executors
        getCommand("MhCreate").setExecutor(new MhCreate(this, teamManager));
        getCommand("MhBlock").setExecutor(new MhBlock(this));
        getCommand("MhMove").setExecutor(new TeamMove(teamManager));
        getCommand("MhPause").setExecutor(new MhPause(this, teamManager));
        getCommand("MhUnpause").setExecutor(new MhUnpause(teamManager));
        getCommand("MhReady").setExecutor(new MhReady(teamManager, this));
        getCommand("MhCompass").setExecutor(new MhCompass(teamManager, this));
        getCommand("MhStart").setExecutor(mhStart);
        getCommand("MhRestart").setExecutor(new MhRestart(mhStart));

        // Register TeamSelection listener
        new TeamSelection(this, teamManager);

        Bukkit.getServer().getPluginManager().registerEvents(new TeamManager(this), this);

        Bukkit.getServer().getPluginManager().registerEvents(new MhCompass(teamManager, this), this);

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

        double overworldSize = 5000.0; //maybe should set this dynamically? unsure
        double netherSize = overworldSize / 8; // Nether is 1/8th the size of the Overworld

        // Set the Overworld world border size
        setWorldBorderSize(overworld, overworldSize);

        // Set the Nether world border size
        setWorldBorderSize(nether, netherSize);

        getLogger().info("World border has been set to " + overworldSize + " blocks in Overworld and " + netherSize + " blocks in Nether by Manhunt plugin.");
    }

    // Set the world border size for a specific world
    private void setWorldBorderSize(World world, double size) {
        WorldBorder worldBorder = world.getWorldBorder();
        worldBorder.setSize(size);
    }
}
