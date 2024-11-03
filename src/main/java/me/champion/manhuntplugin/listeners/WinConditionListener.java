package me.champion.manhuntplugin.listeners;

import me.champion.manhuntplugin.TeamManager;
import me.champion.manhuntplugin.commands.MhStart;
import org.bukkit.Bukkit;
import org.bukkit.entity.EnderDragon;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class WinConditionListener implements Listener {
    private final TeamManager teamManager;
    private final MhStart mhstart;
    private final Plugin plugin;
    private boolean ZombieWin = false;
    private boolean RunnerWin = false;

    public WinConditionListener(TeamManager teamManager, MhStart mhstart, Plugin plugin) {
        this.teamManager = teamManager;
        this.mhstart = mhstart;
        this.plugin = plugin;

        // Start the game condition checks when the listener is instantiated
        scheduleGameConditionCheck();
    }

    // Resets the win conditions and game state
    public void resetConditions() {
        ZombieWin = false;
        RunnerWin = false;
        teamManager.GameOver = false;
    }

    // This method schedules periodic checks on game conditions
    public void scheduleGameConditionCheck() {
        int delay = 0; // Initial delay (in ticks)
        int period = 20; // Delay between each run (in ticks) - 20 ticks = 1 second

        // Schedule a repeating task that checks game conditions every second
        new BukkitRunnable() {
            @Override
            public void run() {
                checkGameConditions();  // This method will contain the logic to check for win conditions
            }
        }.runTaskTimer(plugin, delay, period);  // Run the task repeatedly with the given period
    }

    // Logic to check if win conditions have been met
    private void checkGameConditions() {
        if (mhstart.isGameStarted()) {
            // Zombie Win Condition - Timer expired or all runners are dead
            if (!ZombieWin && !RunnerWin) {
                if (mhstart.timerExpired || teamManager.getRunners().isEmpty()) {
                    ZombieWin = true;
                    teamManager.GameOver = true;
                    Bukkit.broadcastMessage("§cZombies win on time!");

                    // Reset the game immediately
                    resetGame();
                }
            }
        }

        // If the game is no longer started, reset conditions
        if (!mhstart.isGameStarted()) {
            resetConditions();
        }
    }

    // Log when an Ender Dragon is spawned
    @EventHandler
    public void onEntitySpawn(CreatureSpawnEvent event) {
        if (event.getEntity() instanceof EnderDragon) {
            Bukkit.getLogger().info("An Ender Dragon has been summoned or spawned at location: "
                    + event.getLocation().toString());
        }
    }

    // Log when an Ender Dragon is killed and trigger Runner win condition
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        // Runner Win Condition - Any Ender Dragon is killed
        if (event.getEntity() instanceof EnderDragon && mhstart.isGameStarted()) {
            Bukkit.getLogger().info("An Ender Dragon has been killed at location: "
                    + event.getEntity().getLocation().toString());

            // Ensure the game win condition for Runners is triggered
            if (!RunnerWin) {
                RunnerWin = true;
                teamManager.GameOver = true;

                // Broadcast the proper "Runners Win!" message
                Bukkit.broadcastMessage("§bRunners Win!");
                Bukkit.broadcastMessage("The §5Ender Dragon §fhas been defeated!");

                // Reset the game immediately
                resetGame();
            }
        }
    }

    // Capture player commands (e.g., /kill @e[type=ender_dragon]) that could kill an Ender Dragon
    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        String command = event.getMessage().toLowerCase();
        if (command.contains("/kill") && command.contains("ender_dragon")) {
            Bukkit.getLogger().info("Detected a command to kill an Ender Dragon by player " + event.getPlayer().getName());

            // Trigger the Runners' win condition if the game is started
            if (mhstart.isGameStarted()) {
                triggerRunnersWin();
            }
        }
    }

    // Capture console commands (e.g., /kill @e[type=ender_dragon])
    @EventHandler
    public void onServerCommand(ServerCommandEvent event) {
        String command = event.getCommand().toLowerCase();
        if (command.contains("kill") && command.contains("ender_dragon")) {
            Bukkit.getLogger().info("Detected a command to kill an Ender Dragon via console.");

            // Trigger the Runners' win condition if the game is started
            if (mhstart.isGameStarted()) {
                triggerRunnersWin();
            }
        }
    }

    // Method to trigger Runners' win condition
    private void triggerRunnersWin() {
        if (!RunnerWin) {
            RunnerWin = true;
            teamManager.GameOver = true;
            Bukkit.broadcastMessage("§bRunners Win!");
            Bukkit.broadcastMessage("The §5Ender Dragon §fhas been defeated!");


            // Reset the game immediately
            resetGame();
        }
    }

    // Method to reset the game immediately
    private void resetGame() {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mhrestart confirm");
    }
}
