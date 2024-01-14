package me.champion.manhuntplugin;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EnderDragonChangePhaseEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;

public class WinCondition implements Listener {
    private final TeamManager teamManager;
    private final MhStart mhstart;
    private boolean ZombieWin = false;
    private boolean RunnerWin = false;
    public WinCondition(TeamManager teamManager, MhStart mhstart) {
        this.teamManager = teamManager;
        this.mhstart = mhstart;
    }
    public static boolean endEntered = false;
    @EventHandler
    public void onDragonDeath(EnderDragonChangePhaseEvent event) {
        System.out.println("entity died");
        if (mhstart.isGameStarted()) {
            if (event.getNewPhase() == EnderDragon.Phase.DYING) {
                // The Ender Dragon has died
                Bukkit.broadcastMessage("The §5Ender Dragon " + "§fhas been defeated!");
                RunnerWin = true;
                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.sendTitle("§bRunners win", "Game Over", 20, 40, 10);
                }
            }
        }
    }
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (mhstart.isGameStarted()) {
            if (!ZombieWin && !RunnerWin) {
                if (mhstart.timerExpired || teamManager.getRunners().isEmpty()) {
                    System.out.println(teamManager.playerTeams);
                    System.out.println("Zombie Win");
                    ZombieWin = true;
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        player.sendTitle("§cZombies Win", "Game Over", 20, 40, 10);
                    }
                }
            }
        }
        if (!mhstart.isGameStarted()) {
            ZombieWin = false;
            RunnerWin = false;
        }
    }


    @EventHandler
    public void onPlayerPortal(PlayerPortalEvent event) {
        Player player = event.getPlayer();

        // Check if the player is a runner and has entered the End portal
        if (teamManager.isOnTeam(player, "Runners") && event.getTo().getWorld().getEnvironment() == World.Environment.THE_END) {
            endEntered = true;  // Set the flag to true
        }


    }
}