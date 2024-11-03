package me.champion.manhuntplugin;

import me.champion.manhuntplugin.commands.*;
import me.champion.manhuntplugin.listeners.*;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.WorldCreator;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public final class Manhunt extends JavaPlugin {

    private TeamManager teamManager;
    private MhStart mhStart;
    private TeamChat teamChat;
    private MhWheel mhWheel;
    private WinConditionListener winConditionListener;
    private MhIso mhIso;
    private GameControlListener gameControlListener; // Moved here
    private BackupManager backupManager;

    FileConfiguration config;

    @Override
    public void onLoad() {
        getLogger().info("Manhunt plugin is loading!");

        // Initialize PluginSetup and set up the folder
        PluginSetup pluginSetup = new PluginSetup();
        pluginSetup.setupFolder();
    }

    @Override
    public void onEnable() {

        // Load necessary worlds (make sure world_spawn and CustomWorld are loaded)
        getLogger().info("Attempting to load world_spawn...");
        loadWorld("world_spawn");

        getLogger().info("Attempting to load world_hell...");
        loadWorld("world_hell");

        // getLogger().info("Attempting to load CustomWorld...");
        // loadWorld("CustomWorld");

        getLogger().info("Manhunt plugin has started, have a nice day! :)");

        // Load configuration from PluginSetup
        config = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "config.yml"));

        // Log the contents of the loaded config file
        getLogger().info("Loaded config file contents:");
        for (String key : config.getKeys(true)) {
            getLogger().info(key + ": " + config.get(key));
        }

        // ** Set the world border ** //
        setWorldBorder();

        enableFlightInServerProperties();
        setDifficultyHardInServerProperties();

        // Creating unique filenames based on date/time
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String currentDate = dateFormat.format(new Date());

        teamManager = new TeamManager(this, currentDate);


        mhStart = new MhStart(teamManager);
        gameControlListener = new GameControlListener(mhStart);
        mhStart.setGameControlListener(gameControlListener);
        teamChat = new TeamChat(teamManager);
        mhWheel = new MhWheel(this, teamManager);
        winConditionListener = new WinConditionListener(teamManager, mhStart, this);
        mhIso = new MhIso(teamManager, this);
        MhSettings mhSettings = new MhSettings();
        MhRestart mhRestart = new MhRestart(mhStart, teamManager);
        backupManager = new BackupManager(this, teamManager);

        // Register commands
        registerCommand("MhCreate", new MhCreate(this, teamManager, mhStart, mhIso));
        registerCommand("MhMove", new MhMove(teamManager));
        registerCommand("MhPause", new MhPause(this, teamManager));
        registerCommand("MhUnpause", new MhUnpause(this, teamManager));
        registerCommand("MhReady", new MhReady(teamManager, this));
        registerCommand("MhCompass", new MhCompass(teamManager, this));
        registerCommand("MhStart", mhStart);
        registerCommand("MhRestart", mhRestart);
        registerCommand("MhTeamChat", teamChat);
        registerCommand("MhWheel", mhWheel);
        registerCommand("MhSettings", mhSettings);
        registerCommand("MhIntro", new MhIntro());
        registerCommand("MhIso", mhIso);
        // registerCommand("MhTracker", new MhTracker(this, teamManager));
        registerCommand("MhSearch", new MhSearch(this));
        registerCommand("MhCoords", new TeamChat(teamManager));
        registerCommand("MhTp", new MhTp(config));
        registerCommand("MhWorld", new MhWorld(this));
        registerCommand("MhSwap", new MhSwap(this));

        // Register event listeners
        getServer().getPluginManager().registerEvents(new TeamSelection(this, teamManager, mhStart), this);
        getServer().getPluginManager().registerEvents(teamManager, this);
        getServer().getPluginManager().registerEvents(new MhCompass(teamManager, this), this);
        getServer().getPluginManager().registerEvents(winConditionListener, this);
        getServer().getPluginManager().registerEvents(teamChat, this);
        getServer().getPluginManager().registerEvents(mhWheel, this); // Register MhWheel as an event listener
        getServer().getPluginManager().registerEvents(new EyeofEnderListener(teamManager, this), this);
        getServer().getPluginManager().registerEvents(gameControlListener, this); // Updated here
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(mhStart, teamManager), this);
        getServer().getPluginManager().registerEvents(mhSettings, this);
        getServer().getPluginManager().registerEvents(new MhIso(teamManager, this), this);
        getServer().getPluginManager().registerEvents(new BedPlaceListener(teamManager, this), this);
        getServer().getPluginManager().registerEvents(new DamageListener(teamManager), this);
        winConditionListener.scheduleGameConditionCheck(); // Schedule the periodic check
        // Schedule periodic backups every 10 minutes (12000 ticks)
        Bukkit.getScheduler().runTaskTimer(this, () -> backupManager.backupPlayerData(), 0L, 1200L);
    }

    @Override
    public void onDisable() {
        getLogger().info("Manhunt plugin has stopped!");

        // First, back up player data, ensuring we have team data saved
        if (backupManager != null) {
            backupManager.backupPlayerData();
        }

        // Now clear the teams after the backup is saved
        if (teamManager != null) {
            teamManager.clearTeams();
        }
    }

    public TeamManager getTeamManager() {
        return teamManager;
    }

    public static Manhunt getPlugin() {
        return JavaPlugin.getPlugin(Manhunt.class);
    }

    private void setWorldBorder() {
        // Default overworld size (in radius) is 3000, so we need to set diameter to 6000
        int defaultOverworldSize = 6000; // Diameter is twice the radius
        int overworldSize = config.getInt("overworldSize", defaultOverworldSize);

        // Log if using the default value or the config value
        if (overworldSize == defaultOverworldSize) {
            getLogger().info("Overworld size not found in config, using default value of " + defaultOverworldSize + " blocks diameter.");
        } else {
            getLogger().info("Overworld size loaded from config: " + overworldSize + " blocks diameter.");
        }

        // Get the overworld and set its border
        World overworld = Bukkit.getWorld("world"); // Default overworld name
        if (overworld != null) {
            WorldBorder overworldBorder = overworld.getWorldBorder();

            // Set the spawn point to (0, 0)
            overworld.setSpawnLocation(0, overworld.getHighestBlockYAt(0, 0), 0);
            getLogger().info("Overworld spawn point set to (0, 0)");

            // Set the world border center to (0, 0)
            overworldBorder.setCenter(0, 0);
            getLogger().info("Overworld border center set to (0, 0)");

            // Set the size (diameter) after the center is correctly set
            overworldBorder.setSize(overworldSize);
            getLogger().info("Overworld border size set to " + overworldSize + " blocks diameter.");
        } else {
            getLogger().warning("Overworld not found.");
        }

        // Get the nether and set its border (nether size is overworldSize / 8 by default)
        int netherSize = overworldSize / 8;
        getLogger().info("Nether border size is " + netherSize + " blocks diameter (1/8th of the overworld size).");

        World nether = Bukkit.getWorld("world_nether"); // Default nether world name
        if (nether != null) {
            WorldBorder netherBorder = nether.getWorldBorder();

            // Set the nether world border center to (0, 0)
            netherBorder.setCenter(0, 0);
            getLogger().info("Nether border center set to (0, 0)");

            // Set the size (diameter) after the center is correctly set
            netherBorder.setSize(netherSize);
            getLogger().info("Nether border size set to " + netherSize + " blocks diameter.");
        } else {
            getLogger().warning("Nether world not found.");
        }
    }

    private void enableFlightInServerProperties() {
        try {
            File propFile = new File("server.properties");
            if (!propFile.exists()) {
                getLogger().warning("server.properties not found!");
                return;
            }
            String content = new String(Files.readAllBytes(Paths.get(propFile.toURI())));
            if (!content.contains("allow-flight=false")) {
                getLogger().info("Flight already enabled or property not found.");
                return;
            }
            content = content.replaceAll("allow-flight=false", "allow-flight=true");
            Files.write(Paths.get(propFile.toURI()), content.getBytes());
            getLogger().info("Flight enabled. Server will restart shortly.");
            Bukkit.getScheduler().runTaskLater(this, () ->
                    Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "restart"), 100L);
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Failed to modify server.properties: " + e.getMessage(), e);
        }
    }

    private void setDifficultyHardInServerProperties() {
        try {
            File propFile = new File("server.properties");
            if (!propFile.exists()) {
                getLogger().warning("server.properties not found!");
                return;
            }
            String content = new String(Files.readAllBytes(Paths.get(propFile.toURI())));
            if (!content.contains("difficulty=easy")) {
                getLogger().info("Difficulty property not found.");
                return;
            }
            content = content.replaceAll("difficulty=easy", "difficulty=hard");
            Files.write(Paths.get(propFile.toURI()), content.getBytes());
            getLogger().info("Difficulty set to hard. Server will restart shortly.");
            Bukkit.getScheduler().runTaskLater(this, () ->
                    Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "restart"), 100L);
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Failed to modify server.properties: " + e.getMessage(), e);
        }
    }

    public void loadWorld(String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            getLogger().info("World " + worldName + " is not loaded, loading now...");
            world = Bukkit.createWorld(new WorldCreator(worldName));
            if (world != null) {
                getLogger().info("Successfully loaded world: " + worldName);
            } else {
                getLogger().severe("Failed to load world: " + worldName);
            }
        } else {
            getLogger().info("World " + worldName + " is already loaded.");
        }
    }

    public void registerCommand(String commandName, CommandExecutor commandExecutor) {
        PluginCommand pluginCommand = getCommand(commandName);
        if (pluginCommand != null) {
            pluginCommand.setExecutor(commandExecutor);
        } else {
            getLogger().warning(commandName + " command not found in plugin.yml");
        }
    }
}
