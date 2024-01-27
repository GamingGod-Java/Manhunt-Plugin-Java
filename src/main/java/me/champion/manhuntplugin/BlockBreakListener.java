package me.champion.manhuntplugin;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class BlockBreakListener implements Listener {

    private final MhStart mhStart;

    public BlockBreakListener(MhStart mhStart) {
        this.mhStart = mhStart;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        // Check if the game has not started before canceling block break
        if (!mhStart.isGameStarted() &&
                (event.getBlock().getType() == Material.GLASS ||
                        event.getBlock().getType() == Material.RED_STAINED_GLASS ||
                        event.getBlock().getType() == Material.BLUE_STAINED_GLASS)) {
            event.setCancelled(true); // Cancel the block break event
        }
    }
}
