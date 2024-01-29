package me.champion.manhuntplugin;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;

import java.util.logging.Level;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public final class Manhunt extends JavaPlugin {

    private TeamManager teamManager;
    private MhStart mhStart;
    private TeamChat teamChat;
    private MhWheel mhWheel; // Added this line
    private WinCondition winCondition;
    File configFile = new File(getDataFolder(), "playerdata.yml");
    FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);

    File statisticsFile = new File(getDataFolder(), "statistics.yml");
    FileConfiguration statisticsConfig = YamlConfiguration.loadConfiguration(statisticsFile);

    @Override
    public void onLoad() {
        getLogger().info("Manhunt plugin is loading!");
    }

    @Override
    public void onEnable() {
        getLogger().info("Manhunt plugin has started, have a nice day! :)");

        enableFlightInServerProperties();
        setDifficultyHardInServerProperties();

        try {
            statisticsConfig.save(statisticsFile);
            getLogger().info("Created and saved statistics.yml");
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Failed to save statistics.yml: " + e.getMessage(), e);
        }

        config = new YamlConfiguration();
        try {
            config.save(configFile);
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Failed to save configuration file: " + e.getMessage(), e);
        }
        getLogger().info("Emptied playerdata.yml");

        teamManager = new TeamManager(this);
        mhStart = new MhStart(teamManager);
        teamChat = new TeamChat(teamManager);
        mhWheel = new MhWheel(this, teamManager); // Initialize MhWheel here
        winCondition = new WinCondition(teamManager, mhStart);
        MhSettings mhSettings = new MhSettings();

        setWorldBorder();

        MhRestart mhRestart = new MhRestart(mhStart, teamManager);

        // Register commands
        registerCommand("MhCreate", new MhCreate(this, teamManager));
        registerCommand("MhMove", new TeamMove(teamManager));
        registerCommand("MhPause", new MhPause(this, teamManager));
        registerCommand("MhUnpause", new MhUnpause(this, teamManager));
        registerCommand("MhReady", new MhReady(teamManager, this));
        registerCommand("MhCompass", new MhCompass(teamManager, this));
        registerCommand("MhStart", mhStart);
        registerCommand("MhRestart", mhRestart);
        registerCommand("MhTeamChat", teamChat);
        registerCommand("MhWheel", mhWheel);
        registerCommand("MhSettings", mhSettings);
        // Register event listeners

        getServer().getPluginManager().registerEvents(new TeamSelection(this, teamManager, mhStart), this);
        getServer().getPluginManager().registerEvents(teamManager, this);
        getServer().getPluginManager().registerEvents(new MhCompass(teamManager, this), this);
        getServer().getPluginManager().registerEvents(new WinCondition(teamManager, mhStart), this);
        getServer().getPluginManager().registerEvents(teamChat, this);
        getServer().getPluginManager().registerEvents(mhWheel, this); // Register MhWheel as an event listener
        getServer().getPluginManager().registerEvents(mhWheel, this);
        getServer().getPluginManager().registerEvents(winCondition, this);
        getServer().getPluginManager().registerEvents(new EyeofEnderListener(teamManager, this), this);
        winCondition.scheduleGameConditionCheck(); // Schedule the periodic check
        getServer().getPluginManager().registerEvents(new GameControlListener(mhStart), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(mhStart), this);
        getServer().getPluginManager().registerEvents(mhSettings, this);
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

    private void setWorldBorder() {
        World overworld = Bukkit.getWorlds().get(0);
        World nether = Bukkit.getWorlds().get(1);
        double overworldSize = 6000.0;
        double netherSize = overworldSize / 8;

        setWorldBorderSize(overworld, overworldSize);
        setWorldBorderSize(nether, netherSize);
        getLogger().info("World border set for Overworld and Nether.");
    }

    private void setWorldBorderSize(World world, double size) {
        WorldBorder worldBorder = world.getWorldBorder();
        worldBorder.setSize(size);
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

    private void registerCommand(String commandName, CommandExecutor commandExecutor) {
        PluginCommand pluginCommand = getCommand(commandName);
        if (pluginCommand != null) {
            pluginCommand.setExecutor(commandExecutor);
        } else {
            getLogger().warning(commandName + " command not found in plugin.yml");
        }
    }
}
