package me.champion.manhuntplugin;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.entity.Player;

public class GameControlListener implements Listener {

    private final MhStart mhStart;

    public GameControlListener(MhStart mhStart) {
        this.mhStart = mhStart;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!mhStart.isGameStarted() && !event.getPlayer().isOp()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!mhStart.isGameStarted() && !event.getPlayer().isOp()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (!mhStart.isGameStarted() && event.getEntity() instanceof Player && !event.getEntity().isOp()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        if (!mhStart.isGameStarted() && !player.isOp()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        //Removed check for whom the attacker is, as don't want players to die of fall damage before
        if (!mhStart.isGameStarted() && event.getEntity() instanceof Player) {
            event.setCancelled(true);
        }
    }
}
