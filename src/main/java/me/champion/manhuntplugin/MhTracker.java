package me.champion.manhuntplugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.inventory.ItemStack;
public class MhTracker implements CommandExecutor, Listener {

    private JavaPlugin plugin;
    private boolean trackingEnabled = false;
    private TeamManager teamManager;
    private Location zombieLodestoneLocation = null;

    public MhTracker(JavaPlugin plugin, TeamManager teamManager) {
        this.plugin = plugin;
        this.teamManager = teamManager;
        plugin.getServer().getPluginManager().registerEvents(this, plugin); // Register the listener
    }

    private boolean isZombie(Player player) {
        return teamManager != null && teamManager.isOnTeam(player, "Zombies");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be run by a player.");
            return false;
        }

        Player player = (Player) sender;

        // Check if the player is a zombie
        if (!isZombie(player)) {
            player.sendMessage("Only zombies can use this command.");
            return true;
        }

        // Toggle tracking
        trackingEnabled = !trackingEnabled;

        if (trackingEnabled) {
            // Start the sound update task if tracking is enabled
            startSoundTask(player);
            player.sendMessage(ChatColor.GREEN + "Sound has been enabled.");
        } else {
            player.sendMessage(ChatColor.RED + "Sound has been disabled.");
        }

        return true;
    }
/*
    private void sendActionBar(Player player, String message) {
        String command = "minecraft:title " + player.getName() + " actionbar [\"\",{\"text\":\"" + message + "\"}]";
        Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), command);
    }
*/
private void startSoundTask(Player zombie) {
    new BukkitRunnable() {
        @Override
        public void run() {
            // Check if the zombie is still online and tracking is still enabled
            if (!zombie.isOnline() || !trackingEnabled) {
                cancel(); // Stop the task if the zombie is offline or tracking is disabled
                return;
            }

            // Check if the zombie is holding a compass
            if (zombie.getInventory().getItemInMainHand().getType() == Material.COMPASS) {
                Player nearestRunner = findNearestRunner(zombie);
                if (nearestRunner != null && isLookingAt(zombie, nearestRunner)) {
                    // Play noteblock sound
                    zombie.playSound(zombie.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, SoundCategory.PLAYERS, 1, 1);
                }
            }
        }
    }.runTaskTimer(plugin, 0, 20); // Run the task every second (20 ticks)
}

    @EventHandler
    public void onCompassRightClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getAction().name().contains("RIGHT_CLICK") &&
                player.getInventory().getItemInMainHand().getType() == Material.COMPASS) {
            if (!isZombie(player)) {
                player.sendMessage(ChatColor.RED + "Only zombies can toggle tracking with the compass.");
                return;
            }
            trackingEnabled = !trackingEnabled;
            if (trackingEnabled) {
                startSoundTask(player);
                player.sendMessage(ChatColor.GREEN + "Sound has been enabled.");
            } else {
                player.sendMessage(ChatColor.RED + "Sound has been disabled.");
            }
            event.setCancelled(true); // Cancel the event to prevent compass usage
        }
    }

    private Player findNearestRunner(Player zombie) {
        Player nearestRunner = null;
        double minDistance = Double.MAX_VALUE;
        Location zombieLocation = zombie.getLocation();

        for (Player player : Bukkit.getServer().getOnlinePlayers()) {
            // Check if the player is on the "Runners" team and is in the same world as the zombie
            if (teamManager != null && teamManager.isOnTeam(player, "Runners") && player.getWorld().equals(zombieLocation.getWorld()) && !player.equals(zombie)) {
                double distance = zombieLocation.distance(player.getLocation());
                if (distance < minDistance) {
                    minDistance = distance;
                    nearestRunner = player;
                }
            }
        }

        // Update nearest runner's location and set it as the zombie's lodestone location if tracking is enabled
        if (trackingEnabled && nearestRunner != null) {
            zombieLodestoneLocation = nearestRunner.getLocation();
            zombie.setCompassTarget(nearestRunner.getLocation());
        }

        return nearestRunner;
    }


    private boolean isLookingAt(Player player, Player target) {
        // Get the direction vector from the player to the target
        Vector direction = target.getLocation().toVector().subtract(player.getLocation().toVector()).normalize();

        // Get the player's facing direction
        Vector facing = player.getEyeLocation().getDirection();

        // Calculate the dot product of the direction vectors
        double dot = direction.dot(facing);

        // If the dot product is close to 1, the player is looking in the direction of the target
        return dot > 0.99;
    }
}
