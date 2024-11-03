package me.champion.manhuntplugin.commands;

import me.champion.manhuntplugin.Manhunt;
import me.champion.manhuntplugin.TeamManager;
import me.champion.manhuntplugin.listeners.GameControlListener;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.Sound;

public class MhStart implements CommandExecutor {

    private final TeamManager teamManager;
    private GameControlListener gameControlListener; // Not final
    private final long gameTimerDuration;  // Game timer duration from config

    public boolean gameStarted = false;
    private BukkitTask countdownTask;
    private BossBar bossBar;
    public boolean timerExpired = false;
    private boolean initialCountdownInProgress = false;
    private BukkitTask initialCountdownTask;

    public MhStart(TeamManager teamManager) {
        this.teamManager = teamManager;
        // Load game timer duration from the config file
        this.gameTimerDuration = Manhunt.getPlugin().getConfig().getLong("gameTimer", 8990); // Default to 8990 seconds if not set
    }

    public void setGameControlListener(GameControlListener gameControlListener) {
        this.gameControlListener = gameControlListener;
    }

    public BossBar getBossBar() {
        return this.bossBar;
    }

    public boolean isGameStarted() {
        return gameStarted;
    }

    public void resetGame() {
        cancelInitialCountdown();

        if (countdownTask != null && !countdownTask.isCancelled()) {
            countdownTask.cancel();
            countdownTask = null;
        }

        hideBossBar();
        gameStarted = false;
        timerExpired = false;
        teamManager.unpauseZombies();
    }

    public void hideBossBar() {
        if (bossBar != null) {
            bossBar.setVisible(false);
            bossBar.removeAll();
            if (countdownTask != null && !countdownTask.isCancelled()) {
                countdownTask.cancel();
                countdownTask = null;
            }
        }
    }

    public void cancelInitialCountdown() {
        if (initialCountdownTask != null && !initialCountdownTask.isCancelled()) {
            initialCountdownTask.cancel();
            initialCountdownInProgress = false;
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player) || !sender.isOp()) {
            sender.sendMessage("§cYou do not have permission to use this command.");
            return true;
        }

        if (gameStarted) {
            sender.sendMessage("§cThe game has already started. Use /mhrestart to restart.");
            return true;
        }

        for (Player onlineplayer : Bukkit.getOnlinePlayers()) {
            onlineplayer.setGameMode(GameMode.SURVIVAL);

            onlineplayer.setHealth(20.0);

            onlineplayer.setFoodLevel(20);

            onlineplayer.setSaturation(20);

            onlineplayer.playSound(onlineplayer.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 1.0F);

            onlineplayer.setInvulnerable(false);
        }
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "effect clear @a");

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "time set 0");

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "gamerule doMobSpawning true");

        World world = ((Player) sender).getWorld();
        // Iterate through all entities in the world
        for (Entity entity : world.getEntities()) {
            // Check if the entity is an item
            if (entity instanceof Item) {
                // Remove the item entity
                entity.remove();
            }
        }

        MhCreate mhCreate = new MhCreate(Manhunt.getPlugin(), teamManager, this, new MhIso(teamManager, Manhunt.getPlugin()));
        mhCreate.removeGlassSphere((Player) sender);

        // Reset boss bar and game state
        gameStarted = true;

        // Remove the "Open Settings" command block from all operators
        if (gameControlListener != null) {
            gameControlListener.removeSettingsCommandBlocksFromAllOps();
        } else {
            Bukkit.getLogger().severe("GameControlListener is null in MhStart. Cannot remove command blocks.");
        }

        teamManager.pauseZombies();
        startInitialCountdown();
        giveCompassesToZombies();
        return true;
    }

    private void startInitialCountdown() {
        if (initialCountdownTask != null && !initialCountdownTask.isCancelled()) {
            initialCountdownTask.cancel();
        }

        initialCountdownInProgress = true;
        initialCountdownTask = new BukkitRunnable() {
            int secondsLeft = 10;

            @Override
            public void run() {
                if (secondsLeft <= 0) {
                    Bukkit.broadcastMessage("The §cZombies §fcan now move!");
                    teamManager.unpauseZombies();
                    this.cancel();
                    initialCountdownInProgress = false;

                    createAndStartBossBar(); // Start the boss bar timer after the initial countdown
                } else {
                    Bukkit.broadcastMessage("§cZombies §fcan move in " + secondsLeft + " seconds!");
                }
                secondsLeft--;
            }
        }.runTaskTimer(Manhunt.getPlugin(), 0L, 20L);
    }

    private void createAndStartBossBar() {
        if (bossBar != null) {
            bossBar.removeAll(); // Reset the boss bar for a new game
        }

        // Initialize the boss bar
        bossBar = Bukkit.createBossBar("Game Timer", BarColor.PURPLE, BarStyle.SEGMENTED_10);
        bossBar.setVisible(true); // Make the boss bar visible after the initial countdown

        // Debug: Check if boss bar is being created
        Bukkit.getLogger().info("Boss bar created and set to visible.");

        // Validate the timer value from the config
        if (gameTimerDuration <= 0) {
            Bukkit.getLogger().severe("Invalid gameTimer value from config: " + gameTimerDuration);
            return;
        }

        long totalSeconds = gameTimerDuration;  // Use the config value here
        countdownTask = new BukkitRunnable() {
            long secondsLeft = totalSeconds;

            @Override
            public void run() {
                if (secondsLeft <= 0) {
                    bossBar.setVisible(false);
                    timerExpired = true;
                    this.cancel();
                    return;
                }

                double progress = (double) secondsLeft / totalSeconds;
                bossBar.setProgress(progress);
                bossBar.setTitle(formatTime(secondsLeft));
                if (!teamManager.isGamePaused()) {
                    secondsLeft--;
                }
            }
        }.runTaskTimer(Manhunt.getPlugin(), 0L, 20L);

        // Debug: Log how many players are online
        Bukkit.getLogger().info("Adding boss bar to players. Online players count: " + Bukkit.getOnlinePlayers().size());

        // Add all online players to the boss bar
        Bukkit.getOnlinePlayers().forEach(bossBar::addPlayer);
    }

    private String formatTime(long totalSeconds) {
        int hours = (int) totalSeconds / 3600;
        int minutes = (int) (totalSeconds % 3600) / 60;
        int seconds = (int) totalSeconds % 60;
        return String.format("%dh %dm %ds", hours, minutes, seconds);
    }

    public void giveCompassesToZombies() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (teamManager.isOnTeam(player, "Zombies")) {
                MhCompass compassManager = new MhCompass(teamManager, Manhunt.getPlugin());
                compassManager.giveRunnerCompass(player);
            }
        }
    }


}
