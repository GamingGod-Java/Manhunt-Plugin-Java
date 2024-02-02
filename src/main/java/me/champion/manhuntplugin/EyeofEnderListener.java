package me.champion.manhuntplugin;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.generator.structure.StructureType;
import org.bukkit.plugin.Plugin;
import org.bukkit.block.BlockState;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Structure;
import org.bukkit.entity.EnderSignal;

import java.util.ArrayList;
import java.util.List;

public class EyeofEnderListener implements Listener {
    private List<Location> closestStrongholds = new ArrayList<>();
    private final TeamManager teamManager;
    private final Plugin plugin;
    public EyeofEnderListener(TeamManager teamManager, Plugin plugin) {
        this.teamManager = teamManager;
        this.plugin = plugin;

        findStrongholds();
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (event.getItem() != null && event.getItem().getType() == Material.ENDER_EYE) {
                // Handle Eye of Ender usage
                if (handleEyeOfEnderUsage(event.getPlayer())) {
                    event.getPlayer().sendMessage("Stronghold inside world border!");
                    event.setCancelled(true);
                } if (!handleEyeOfEnderUsage(event.getPlayer())) {
                    event.getPlayer().sendMessage("Something has gone terribly wrong, please contact a server admin");
                    event.setCancelled(true);
                }
            }
        }
    }



    private boolean handleEyeOfEnderUsage(Player player) {
        Location playerLocation = player.getLocation();
        Location closestStronghold = findClosestStronghold(playerLocation);
        EnderSignal enderSignal = player.getWorld().spawn(playerLocation, EnderSignal.class);





        if (closestStronghold != null) {
            // Modify the behavior of the Eye of Ender to point to the specified stronghold location
            enderSignal.setTargetLocation(closestStronghold);
            Bukkit.getLogger().info("Eye of Ender points to " + closestStronghold);
            return true;

        } else {

            return false;

        }
    }

    private Location findClosestStronghold(Location playerLocation) {
        double closestDistanceSquared = Double.MAX_VALUE;
        Location closestStronghold = null;

        for (Location strongholdLocation : closestStrongholds) {
            double distanceSquared = playerLocation.distanceSquared(strongholdLocation);

            if (distanceSquared < closestDistanceSquared) {
                closestDistanceSquared = distanceSquared;
                closestStronghold = strongholdLocation;
            }
        }

        return closestStronghold;
    }

    private Location findStrongholds() {
        for (int i = 0; i < 5; i++){
            int X = -1500;
            int Z = -1500;
            if (i == 1) {
                X = 1500;
                Z = -1500;
            }
            if (i == 2) {
                X = -1500;
                Z = 1500;
            }
            if (i == 3) {
                X = 1500;
                Z = 1500;
            }
            //this code is a crime against for loops but idk how to programatically do a square
            Location Strongholdsearch = Bukkit.getWorld("world").locateNearestStructure(new Location(Bukkit.getWorld("world"), X, 0, Z), StructureType.STRONGHOLD, 2000, false).getLocation();
            if (Strongholdsearch.getBlockX() > 3000 || Strongholdsearch.getBlockX() < -3000 || Strongholdsearch.getBlockZ() > 3000 || Strongholdsearch.getBlockZ() < -3000) {
                continue;
            }
            if (!closestStrongholds.contains(Strongholdsearch)) {
                closestStrongholds.add(Strongholdsearch);
                System.out.println(Strongholdsearch);
            }
            if (closestStrongholds.size() > 2) {
                break;
            }
        }

        return null;
    }


}
