package me.champion.manhuntplugin.listeners;

import me.champion.manhuntplugin.Manhunt;
import me.champion.manhuntplugin.TeamManager;
import me.champion.manhuntplugin.commands.MhPause;
import me.champion.manhuntplugin.commands.MhUnpause;
import me.champion.manhuntplugin.commands.MhStart;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.World;
import org.bukkit.entity.Firework;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.plugin.Plugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class WinConditionListener implements Listener {
    private final TeamManager teamManager;
    private final MhStart mhstart;
    private final Plugin plugin;
    private boolean ZombieWin = false;
    private boolean RunnerWin = false;
    private boolean gamePaused = false;

    public WinConditionListener(TeamManager teamManager, MhStart mhstart, Plugin plugin) {
        this.teamManager = teamManager;
        this.mhstart = mhstart;
        this.plugin = plugin;
    }

    public static boolean endEntered = false;

    public void resetConditions() {
        ZombieWin = false;
        RunnerWin = false;
        teamManager.GameOver = false;
        gamePaused = false;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof EnderDragon && mhstart.isGameStarted()) {
            RunnerWin = true;
            teamManager.GameOver = true;
            Bukkit.broadcastMessage("The §5Ender Dragon §fhas been defeated!");

            new BukkitRunnable() {
                int count = 10; // Runs for 5 seconds (10 times with 0.5s interval)

                @Override
                public void run() {
                    if (count <= 0) {
                        this.cancel();
                        return;
                    }

                    for (Player player : Bukkit.getOnlinePlayers()) {
                        spawnFirework(player, Color.AQUA);
                    }

                    count--;
                }
            }.runTaskTimer(plugin, 0, 10); // 10 ticks = 0.5 seconds

            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendTitle("§bRunners win", "Game Over", 60, 40, 60);
            }

            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mhrestart confirm");

            // Pause and unpause the game with a delay of 2 seconds
            new BukkitRunnable() {
                @Override
                public void run() {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mhpause");
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mhunpause");
                    Bukkit.getLogger().info("Manually paused and unpaused using WinCondition");
                }
            }.runTaskLater(plugin, 40); // 2 seconds (20 ticks per second)
        }
    }

    public void scheduleGameConditionCheck() {
        int delay = 0; // Initial delay (in ticks)
        int period = 20; // Delay between each run (in ticks)
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!gamePaused) {
                    checkGameConditions();
                }
            }
        }.runTaskTimer(Manhunt.getPlugin(), delay, period);
    }

    private void checkGameConditions() {
        if (mhstart.isGameStarted()) {
            if (!ZombieWin && !RunnerWin) {
                if (mhstart.timerExpired || teamManager.getRunners().isEmpty()) {
                    System.out.println(teamManager.playerTeams);
                    System.out.println("Zombie Win");
                    teamManager.GameOver = true;
                    ZombieWin = true;

                    // Increase title duration to 60 ticks (3 seconds)
                    int titleDuration = 60;

                    for (Player player : Bukkit.getOnlinePlayers()) {
                        player.sendTitle("§cZombies Win", "Game Over", titleDuration, 40, titleDuration);
                    }

                    new BukkitRunnable() {
                        int count = 10; // Runs for 5 seconds (10 times with 0.5s interval)

                        @Override
                        public void run() {
                            if (count <= 0) {
                                this.cancel();
                                // Delay for 0.5 seconds before confirming restart
                                Bukkit.getScheduler().runTaskLater(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mhrestart confirm"), 10); // 10 ticks = 0.5 seconds
                                return;
                            }

                            for (Player player : teamManager.getPlayersOnTeam("Zombies")) {
                                spawnFirework(player, Color.RED);
                            }

                            count--;
                        }
                    }.runTaskTimer(plugin, 0, 10); // 10 ticks = 0.5 seconds

                    // Delay for 2 seconds before pausing the game
                    Bukkit.getScheduler().runTaskLater(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mhpause"), 40); // 40 ticks = 2 seconds

                    // Delay for 3 seconds before unpausing the game
                    Bukkit.getScheduler().runTaskLater(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mhunpause"), 60); // 60 ticks = 3 seconds
                }
            }
        }
        if (!mhstart.isGameStarted()) {
            resetConditions();
            //System.out.println("reset conditions");
        }
    }

    @EventHandler
    public void onPlayerPortal(PlayerPortalEvent event) {
        Player player = event.getPlayer();

        // Check if the player is a runner and has entered the End portal
        if (teamManager.isOnTeam(player, "Runners") && event.getTo().getWorld().getEnvironment() == World.Environment.THE_END) {
            endEntered = true;  // Set the flag to true
            mhstart.hideBossBar();
        }
    }

    private void spawnFirework(Player player, Color color) {
        Firework firework = player.getWorld().spawn(player.getLocation(), Firework.class);
        FireworkMeta fireworkMeta = firework.getFireworkMeta();
        FireworkEffect effect = FireworkEffect.builder().withColor(color)
                .with(FireworkEffect.Type.BALL_LARGE)
                .trail(true).flicker(true).build();
        fireworkMeta.addEffect(effect);
        fireworkMeta.setPower(1);
        firework.setFireworkMeta(fireworkMeta);
    }
}

