package me.champion.manhuntplugin;

import me.champion.manhuntplugin.commands.*;
import me.champion.manhuntplugin.listeners.*;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldBorder;
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
        getLogger().info("Manhunt plugin has started, have a nice day! :)");

        // Load configuration from PluginSetup
        config = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "config.yml"));

        // Log the contents of the loaded config file
        getLogger().info("Loaded config file contents:");
        for (String key : config.getKeys(true)) {
            getLogger().info(key + ": " + config.get(key));
        }

        enableFlightInServerProperties();
        setDifficultyHardInServerProperties();

        // Creating unique filenames based on date/time
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String currentDate = dateFormat.format(new Date());

        teamManager = new TeamManager(this, currentDate);
        mhStart = new MhStart(teamManager);
        teamChat = new TeamChat(teamManager);
        mhWheel = new MhWheel(this, teamManager); // Initialize MhWheel here
        winConditionListener = new WinConditionListener(teamManager, mhStart, this);
        mhIso = new MhIso(teamManager, this);

        MhSettings mhSettings = new MhSettings();

        MhRestart mhRestart = new MhRestart(mhStart, teamManager);

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
        registerCommand("MhTracker", new MhTracker(this, teamManager));
        registerCommand("MhSearch", new MhSearch(this));
        registerCommand("MhCoords", new TeamChat(teamManager));
        registerCommand("MhTp", new MhTp(config));

        // Register event listeners
        getServer().getPluginManager().registerEvents(new TeamSelection(this, teamManager, mhStart), this);
        getServer().getPluginManager().registerEvents(teamManager, this);
        getServer().getPluginManager().registerEvents(new MhCompass(teamManager, this), this);
        getServer().getPluginManager().registerEvents(winConditionListener, this);
        getServer().getPluginManager().registerEvents(teamChat, this);
        getServer().getPluginManager().registerEvents(mhWheel, this); // Register MhWheel as an event listener
        getServer().getPluginManager().registerEvents(mhWheel, this);
        getServer().getPluginManager().registerEvents(winConditionListener, this);
        getServer().getPluginManager().registerEvents(new EyeofEnderListener(teamManager, this), this);
        getServer().getPluginManager().registerEvents(new GameControlListener(mhStart), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(mhStart), this);
        getServer().getPluginManager().registerEvents(mhSettings, this);
        getServer().getPluginManager().registerEvents(new MhIso(teamManager, this), this);
        getServer().getPluginManager().registerEvents(new BedPlaceListener(teamManager, this), this);
        getServer().getPluginManager().registerEvents(new DamageListener(teamManager), this);
        winConditionListener.scheduleGameConditionCheck(); // Schedule the periodic check
    }

    @Override
    public void onDisable() {
        getLogger().info("Manhunt plugin has stopped!");
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
            getLogger().info("Difficulty hard. Server will restart shortly.");
            Bukkit.getScheduler().runTaskLater(this, () ->
                    Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "restart"), 100L);
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Failed to modify server.properties: " + e.getMessage(), e);
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
