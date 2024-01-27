package me.champion.manhuntplugin;

import org.bukkit.enchantments.Enchantment;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.bukkit.Particle;
import org.bukkit.inventory.ItemFlag;

import java.util.*;

//Important giveRunnerCompass in MhCompass.java and onPlayerRespawn in TeamManager.java both create the same compass, but with different code.
//If you make changes to either compass, make sure you update it for both.
public class MhCompass implements CommandExecutor, Listener {
    private final TeamManager teamManager;
    private final Plugin plugin;
    private final Map<UUID, BukkitRunnable> particleTasks; // Map to store particle tasks for players

    public MhCompass(TeamManager teamManager, Plugin plugin) {
        this.teamManager = teamManager;
        this.plugin = plugin;
        this.particleTasks = new HashMap<>();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            return false;
        }

        Player player = (Player) sender;
        if (!isZombie(player)) {
            player.sendMessage("§cYou need to be a zombie to use this command.");
            return true;
        }

        giveRunnerCompass(player);
        return true;
    }

    private boolean isZombie(Player player) {
        return teamManager.isOnTeam(player, "Zombies");
    }

    public void giveRunnerCompass(Player player) {
        System.out.println("Gave " + player.getName() + " compass");
        ItemStack compass = new ItemStack(Material.COMPASS);
        compass.addUnsafeEnchantment(Enchantment.ARROW_INFINITE, 1);
        ItemMeta compassMeta = compass.getItemMeta();
        if (compassMeta != null) {
            compassMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            compassMeta.setDisplayName("§cTrack Runners");
            compass.setItemMeta(compassMeta);
        }
        player.getInventory().addItem(compass);
    }


    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack newItem = player.getInventory().getItem(event.getNewSlot());

        cancelExistingParticleTask(player.getUniqueId());
        if (isHoldingRunnerCompass(newItem)) {
            startParticleTask(player);
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        ItemStack droppedItem = event.getItemDrop().getItemStack();

        if (isHoldingRunnerCompass(droppedItem)) {
            cancelExistingParticleTask(player.getUniqueId());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        cancelExistingParticleTask(player.getUniqueId());
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        cancelExistingParticleTask(player.getUniqueId());
    }

    private boolean isHoldingRunnerCompass(ItemStack item) {
        if (item == null || item.getType() != Material.COMPASS) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        return meta != null && "§cTrack Runners".equals(meta.getDisplayName());
    }

    private void startParticleTask(Player player) {
        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                spawnParticlesNearLine(player);
            }
        };
        task.runTaskTimer(plugin, 0L, 2L); // Spawn every 2 ticks
        particleTasks.put(player.getUniqueId(), task);
    }

    private void cancelExistingParticleTask(UUID playerUUID) {
        if (particleTasks.containsKey(playerUUID)) {
            particleTasks.get(playerUUID).cancel();
            particleTasks.remove(playerUUID);
        }
    }

    private void spawnParticlesNearLine(Player player) {
        if (!teamManager.isOnTeam(player, "Zombies")) {
            return;
        }

        Location playerLocation = player.getLocation();
        Player nearestRunner = teamManager.findNearestRunner(playerLocation);

        if (nearestRunner == null) {
            return;
        }

        Location runnerLocation = nearestRunner.getEyeLocation();

        // Check if the zombie and runner are in the same dimension
        if (!playerLocation.getWorld().equals(runnerLocation.getWorld())) {
            return;
        }


        Vector direction = runnerLocation.toVector().subtract(playerLocation.toVector()).normalize();


        //Unsure how to "unsave" location if runner enters nether, additionally, compass working in overworld and not in nether may cause confusion
        //player.setCompassTarget(nearestRunner.getEyeLocation());

        Location particleStartLocation = playerLocation.clone().add(0, 1.3, 0);
        Vector offset = direction.clone().multiply(0.75); // Offset distance
        Location particleLocation = particleStartLocation.clone().add(offset);

        player.getWorld().spawnParticle(Particle.DRAGON_BREATH, particleLocation, 1, 0, 0, 0, 0, null);
    }
}
