package me.champion.manhuntplugin;


import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.Plugin;

public class DisableBedBomb implements Listener {
    private final TeamManager teamManager;
    private final Plugin plugin;
    public DisableBedBomb(TeamManager teamManager, Plugin plugin) {
        this.teamManager = teamManager;
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Check if the action is a right-click and if the player has an item
        if (event.getAction().name().contains("RIGHT") && event.hasItem() && event.getItem() != null) {
            // Check if the item is a bed
            if (isBed(event.getItem().getType())) {
                // Check if the player is in the Nether or the End
                if (event.getPlayer().getWorld().getEnvironment() == World.Environment.NETHER ||
                        event.getPlayer().getWorld().getEnvironment() == World.Environment.THE_END) {
                    event.setCancelled(true); // Cancel the bed placement
                    event.getPlayer().sendMessage("§cYou can't place beds in the Nether or the End!");
                }
            }
        }
    }


    private boolean isBed(Material material) {
        return material == Material.WHITE_BED ||
                material == Material.ORANGE_BED ||
                material == Material.MAGENTA_BED ||
                material == Material.LIGHT_BLUE_BED ||
                material == Material.YELLOW_BED ||
                material == Material.LIME_BED ||
                material == Material.PINK_BED ||
                material == Material.GRAY_BED ||
                material == Material.LIGHT_GRAY_BED ||
                material == Material.CYAN_BED ||
                material == Material.PURPLE_BED ||
                material == Material.BLUE_BED ||
                material == Material.BROWN_BED ||
                material == Material.GREEN_BED ||
                material == Material.RED_BED ||
                material == Material.BLACK_BED;
    }
}
