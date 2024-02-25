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
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.ChatColor;
import org.bukkit.event.block.Action;
import org.bukkit.World;
import org.bukkit.block.Block;

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

    private boolean isHoldingRunnerCompass(ItemStack item) {
        if (item == null || item.getType() != Material.COMPASS) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        return meta != null && "§cTrack Runners".equals(meta.getDisplayName());
    }


@EventHandler
public void onPlayerInteract(PlayerInteractEvent event) {
    Player player = event.getPlayer();
    Action action = event.getAction();
    ItemStack itemInHand = player.getInventory().getItemInMainHand();

    // Check if the action is a left-click and the player is holding the "Track Runners" compass
    if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
        if (isHoldingRunnerCompass(itemInHand)) {
            // Check if the click occurred in the Nether

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
            Location lodestoneLocation = new Location(runnerLocation.getWorld(), runnerLocation.getBlockX() + 0.5, 0, runnerLocation.getBlockZ() + 0.5);

            if (player.getWorld().getEnvironment() == World.Environment.NETHER) {

                // Debug print when a runner is found
                //System.out.println("Runner found: " + nearestRunner.getName());

                // Remove old lodestone if exists
                removeOldLodestone(lodestoneLocation);

                // Place new lodestone
                placeLodestone(lodestoneLocation);

                // Set compass to track new lodestone
                setTrackingCompass(player, lodestoneLocation);
            } if (player.getWorld().getEnvironment() == World.Environment.NORMAL) {
                player.setCompassTarget(runnerLocation);
            }

            // Debug print when lodestone is set
            //System.out.println("Lodestone set for player: " + player.getName());
        } else {
            player.sendMessage(ChatColor.RED + "No runner found to track.");
        }
    }

    private void removeOldLodestone(Location location) {
        if (location.getBlock().getType() == Material.LODESTONE) {
            location.getBlock().setType(Material.AIR);
            // Debug print when old lodestone is removed
            //System.out.println("Old lodestone removed at location: " + location.toString());
        } else {
            // Debug print if no old lodestone is found at the specified location
            //System.out.println("No old lodestone found at location: " + location.toString());
        }
    }

    private void placeLodestone(Location location) {
        World world = location.getWorld();
        if (world != null) {
            Block lodestoneBlock = world.getBlockAt(location.getBlockX(), location.getBlockY(), location.getBlockZ());
            lodestoneBlock.setType(Material.LODESTONE);

            Block bedrockBlock = world.getBlockAt(location.getBlockX(), location.getBlockY() + 1, location.getBlockZ());
            bedrockBlock.setType(Material.BEDROCK);
        }
    }

    private void setTrackingCompass(Player player, Location lodestoneLocation) {
        ItemStack compass = player.getInventory().getItemInMainHand();
        CompassMeta compassMeta = (CompassMeta) compass.getItemMeta();
        if (compassMeta != null) {
            compassMeta.setDisplayName("§cTrack Runners");
            compassMeta.setLodestoneTracked(false); // Clear any previous tracked lodestone
            compassMeta.setLodestone(lodestoneLocation);
            compassMeta.setLodestoneTracked(true);
            //player.sendMessage(ChatColor.GREEN + "Compass is now tracking the nearest runner.");
            compass.setItemMeta(compassMeta);
            // Debug print when compass is updated
            //System.out.println("Compass updated for player: " + player.getName() + " to track lodestone at " + lodestoneLocation.toString());
        }
    }
}