package me.champion.manhuntplugin;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class TeamSelection implements Listener {

    private final TeamManager teamManager;
    private final Manhunt plugin;

    public TeamSelection(Manhunt plugin, TeamManager teamManager) {
        this.plugin = plugin;
        this.teamManager = teamManager;

        // Register the listener
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        // Check if the player stepped on a wool block
        if (isWoolBlock(event.getPlayer().getLocation().subtract(0, 1, 0).getBlock().getType())) {
            // Get the color of the wool block (assuming wool is used for team selection)
            Material woolType = event.getPlayer().getLocation().subtract(0, 1, 0).getBlock().getType();

            // Add the player to the corresponding team based on wool color
            addToTeam(event.getPlayer(), woolType);
        }
    }

    private boolean isWoolBlock(Material blockType) {
        // Check if the block type is wool (you can modify this based on your actual implementation)
        return blockType.name().contains("WOOL");
    }

    private void addToTeam(Player player, Material woolType) {
        if (woolType == null) {
            return; // Handle the case when woolType is null
        }

        String team;

        if (woolType == Material.RED_WOOL) {
            team = "Zombies";
        } else if (woolType == Material.BLUE_WOOL) {
            team = "Runners";
        } else {
            // Handle other block types if needed
            return;
        }

        teamManager.addToTeam(player, team);
    }
}
