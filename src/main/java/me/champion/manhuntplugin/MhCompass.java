package me.champion.manhuntplugin;

import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.event.block.Action;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;


//import java.util.*;

//import org.bukkit.event.player.PlayerJoinEvent;
//import org.bukkit.event.player.PlayerItemHeldEvent;
//import org.bukkit.event.player.PlayerQuitEvent;
//import org.bukkit.event.entity.PlayerDeathEvent;
//import org.bukkit.event.player.PlayerDropItemEvent;
//import org.bukkit.inventory.Inventory;
//import org.bukkit.Bukkit;
//import org.bukkit.Particle;
//import org.bukkit.event.inventory.InventoryClickEvent;
//import org.bukkit.scheduler.BukkitRunnable;
//import org.bukkit.util.Vector;
//import org.bukkit.Color;
public class MhCompass implements CommandExecutor, Listener {
    private final TeamManager teamManager;
    private final Plugin plugin;
    //private final Map<UUID, BukkitRunnable> particleTasks;
    //private final Map<UUID, Double> previousOffsetDistances;
    //private final Map<UUID, Color> playerDyeColors;
    //private final Map<UUID, Inventory> dyeColorGUIs;
    private Map<UUID, BukkitRunnable> compassUpdateTasks = new HashMap<>();

    public MhCompass(TeamManager teamManager, Plugin plugin) {
        this.teamManager = teamManager;
        this.plugin = plugin;
        //this.particleTasks = new HashMap<>();
        //this.previousOffsetDistances = new HashMap<>();
        //this.playerDyeColors = new HashMap<>();
        //this.dyeColorGUIs = new HashMap<>();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            return false;
        }

        Player player = (Player) sender;
        /*if (!isZombie(player)) {
            player.sendMessage("§cYou need to be a zombie to use this command.");
            return true;
        }*/

        giveRunnerCompass(player);
        return true;
    }

    private boolean isZombie(Player player) {
        return teamManager.isOnTeam(player, "Zombies");
    }

    public void giveRunnerCompass(Player player) {
        ItemStack compass = new ItemStack(Material.COMPASS);
        compass.addUnsafeEnchantment(Enchantment.ARROW_INFINITE, 1);
        ItemMeta compassMeta = compass.getItemMeta();
        if (compassMeta != null) {
            compassMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            compassMeta.setDisplayName("§cTrack Runners");
            compass.setItemMeta(compassMeta);
            // Adding lore to the compass
            List<String> lore = new ArrayList<>();
            lore.add("§7Left click to update");
            compassMeta.setLore(lore);
        }
        player.getInventory().addItem(compass);
    }

    private boolean isHoldingRunnerCompass(ItemStack item) {
        if (item == null || item.getType() != Material.COMPASS) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        return meta != null && "§cTrack Runners".equals(meta.getDisplayName());
    }


    /*@EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        int newSlot = event.getNewSlot();
        ItemStack itemInHand = player.getInventory().getItemInMainHand();

        // Perform actions based on the new held item or slot
        // Example: Check if the player is holding a specific item
        if (player.getInventory().getItem(newSlot) != null) {
            if (isHoldingRunnerCompass(itemInHand)) {
                updateTrackingCompass(player);
                event.setCancelled(true); // Prevent normal left-click behavior
                System.out.println("updoot");
            }
            // Your additional actions here
        }
    }*/

@EventHandler
public void onPlayerInteract(PlayerInteractEvent event) {
    Player player = event.getPlayer();
    Action action = event.getAction();
    ItemStack itemInHand = player.getInventory().getItemInMainHand();

    // Check if the action is a left-click and the player is holding the "Track Runners" compass
    if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
        if (isHoldingRunnerCompass(itemInHand)) {

                // Only perform lodestone-related actions in the Nether
                updateTrackingCompass(player);
                event.setCancelled(true); // Prevent normal left-click behavior
                // Debug message for placing lodestone in the Nether
                //player.sendMessage(ChatColor.GREEN + "Placed lodestone in the Nether.");

        }
    }
}

    private void updateTrackingCompass(Player player) {
        Location playerLocation = player.getLocation();
        Player nearestRunner = teamManager.findNearestRunner(playerLocation);


        if (nearestRunner != null) {
            Location runnerLocation = nearestRunner.getLocation();

                stopCompassUpdaterTask(player);
                UUID playerUUID = player.getUniqueId();
                BukkitRunnable updaterTask = new BukkitRunnable() {

                    @Override
                    public void run() {
                        updateLodestoneLocation(player, runnerLocation);
                    }
                };
                updaterTask.runTaskTimer(plugin, 0, 1);
                compassUpdateTasks.put(playerUUID, updaterTask);

        } else {
            player.sendMessage(ChatColor.RED + "No runner found to track.");
        }
   }

    public void stopCompassUpdaterTask(Player player) {
        UUID playerUUID = player.getUniqueId();
        if (compassUpdateTasks.containsKey(playerUUID)) {
            BukkitRunnable updaterTask = compassUpdateTasks.remove(playerUUID);
            if (updaterTask != null) {
                updaterTask.cancel();
            }
        }
    }


   public void updateLodestoneLocation(Player player, Location runnerLocation) {
        if (isHoldingRunnerCompass(player.getInventory().getItemInMainHand())) {
            ItemStack compass = player.getInventory().getItemInMainHand();
            CompassMeta compassMeta = (CompassMeta) compass.getItemMeta();
            if (compassMeta != null) {
                compassMeta.setDisplayName("§cTrack Runners");
                compassMeta.setLodestoneTracked(false); // Clear any previous tracked lodestone
                compassMeta.setLodestone(runnerLocation);
                compassMeta.setLodestoneTracked(true);
                compass.setItemMeta(compassMeta);
                //System.out.println("Compass updated for player: " + player.getName() + " to track lodestone at " + runnerLocation.toString());
            }
        }
   }
}