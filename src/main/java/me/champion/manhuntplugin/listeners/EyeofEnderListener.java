package me.champion.manhuntplugin.listeners;


import me.champion.manhuntplugin.TeamManager;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.generator.structure.StructureType;
import org.bukkit.plugin.Plugin;
import org.bukkit.entity.EnderSignal;
import org.bukkit.util.StructureSearchResult;

import java.util.ArrayList;
import java.util.List;

public class EyeofEnderListener implements Listener {
    private final List<Location> closestStrongholds = new ArrayList<>();
    private final TeamManager teamManager;
    private final Plugin plugin;

    public EyeofEnderListener(TeamManager teamManager, Plugin plugin) {
        this.teamManager = teamManager;
        this.plugin = plugin;

        findStrongholds();
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
            // Check if the player is right-clicking on an end portal frame with an Eye of Ender
            if (event.getClickedBlock().getType() == Material.END_PORTAL_FRAME &&
                    event.getItem() != null && event.getItem().getType() == Material.ENDER_EYE) {
                // Allow the normal behavior (placing the eye in the portal frame)
                return;
            }
        }

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (event.getItem() != null && event.getItem().getType() == Material.ENDER_EYE) {
                // Handle Eye of Ender usage
                if (handleEyeOfEnderUsage(event.getPlayer())) {
                    event.setCancelled(true);
                } else {
                    event.getPlayer().sendMessage("§cSomething has gone terribly wrong, please contact a server admin");
                    event.setCancelled(true);
                }
            }
        }
    }

    private boolean handleEyeOfEnderUsage(Player player) {
        Location playerLocation = player.getLocation().add(0, 1, 0); // Add 1 block to the Y-coordinate
        World playerWorld = playerLocation.getWorld();

        // Check if the world is not null
        if (playerWorld != null) {
            World.Environment playerEnvironment = playerWorld.getEnvironment();

            // Check if the player is in the Nether or the End
            if (playerEnvironment == World.Environment.NETHER || playerEnvironment == World.Environment.THE_END) {
                player.sendMessage("§cYou can't use Eye of Ender in the Nether or the End!");
                return true; // Prevent the usage
            }

            // Find the closest stronghold
            Location closestStronghold = findClosestStronghold(playerLocation);

            // Spawn the Ender Signal
            EnderSignal enderSignal = playerWorld.spawn(playerLocation, EnderSignal.class);

            if (teamManager.isGamePaused()) {
                return true; // Do nothing if the game is paused
            }

            // Check if the closet stronghold was found
            if (closestStronghold != null) {
                enderSignal.setTargetLocation(closestStronghold); // Set the target location for the Ender Signal

                // Decrement the Eye of Ender from the player's inventory if not in Creative mode
                if (player.getGameMode() != GameMode.CREATIVE) {
                    decrementEnderEye(player);
                }
                return true;
            } else {
                player.sendMessage("§cNo strongholds found nearby.");
                return false;
            }
        } else {
            player.sendMessage("§cError: World not found!");
            return false; // World is null, so return false
        }
    }


    private void decrementEnderEye(Player player) {
        if (player.getInventory().getItemInMainHand().getType() == Material.ENDER_EYE) {
            int amount = player.getInventory().getItemInMainHand().getAmount();
            player.getInventory().getItemInMainHand().setAmount(amount - 1);
        } else if (player.getInventory().getItemInOffHand().getType() == Material.ENDER_EYE) {
            int amount = player.getInventory().getItemInOffHand().getAmount();
            player.getInventory().getItemInOffHand().setAmount(amount - 1);
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
        for (int i = 0; i < 5; i++) {
            int X = -1500;
            int Z = -1500;
            if (i == 1) {
                X = 1500;
                Z = -1500;
            } else if (i == 2) {
                X = -1500;
                Z = 1500;
            } else if (i == 3) {
                X = 1500;
                Z = 1500;
            }

            // Perform structure search and get the result
            StructureSearchResult searchResult = Bukkit.getWorld("world").locateNearestStructure(
                    new Location(Bukkit.getWorld("world"), X, 0, Z), StructureType.STRONGHOLD, 800, false
            );

            // Check if the search result found a structure
            if (searchResult != null) {
                Location strongholdLocation = searchResult.getLocation();

                // Validate coordinates to keep within specified bounds
                if (Math.abs(strongholdLocation.getBlockX()) <= 3000 && Math.abs(strongholdLocation.getBlockZ()) <= 3000) {
                    if (!closestStrongholds.contains(strongholdLocation)) {
                        closestStrongholds.add(strongholdLocation);
                    }
                }
            }

            // Stop if we've found enough strongholds
            if (closestStrongholds.size() > 2) {
                break;
            }
        }
        return null;
    }
}
