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
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.bukkit.Particle;
import org.bukkit.Color;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.util.*;

public class MhCompass implements CommandExecutor, Listener {
    private final TeamManager teamManager;
    private final Plugin plugin;
    private final Map<UUID, BukkitRunnable> particleTasks;
    private final Map<UUID, Double> previousOffsetDistances;
    private final Map<UUID, Color> playerDyeColors;
    private final Map<UUID, Inventory> dyeColorGUIs;

    public MhCompass(TeamManager teamManager, Plugin plugin) {
        this.teamManager = teamManager;
        this.plugin = plugin;
        this.particleTasks = new HashMap<>();
        this.previousOffsetDistances = new HashMap<>();
        this.playerDyeColors = new HashMap<>();
        this.dyeColorGUIs = new HashMap<>();
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

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getAction().name().contains("RIGHT") && isHoldingRunnerCompass(player.getItemInHand())) {
            openDyeColorGUI(player);
        }
    }

    private void openDyeColorGUI(Player player) {
        Inventory gui = createDyeColorGUI(player);
        player.openInventory(gui);
        dyeColorGUIs.put(player.getUniqueId(), gui);
    }
    private Inventory createDyeColorGUI(Player player) {
        Inventory gui = plugin.getServer().createInventory(player, 18, "Colors");

        for (Material dyeMaterial : getDyeMaterials()) {
            ItemStack dyeItem = new ItemStack(dyeMaterial);
            ItemMeta meta = dyeItem.getItemMeta();
            if (meta != null) {
                // Set display name with lowercase first letter and remove "dye" from the name
                ChatColor color = getColorFromDyeMaterialAsChatColor(dyeMaterial);
                String displayName = color + dyeMaterial.name().toLowerCase().replace("_dye", "").substring(0, 1).toUpperCase() +
                        dyeMaterial.name().toLowerCase().replace("_dye", "").substring(1);
                meta.setDisplayName(displayName);
                dyeItem.setItemMeta(meta);
            }
            gui.addItem(dyeItem);
        }

        return gui;
    }

    private ChatColor getColorFromDyeMaterialAsChatColor(Material dyeMaterial) {
        switch (dyeMaterial) {
            case WHITE_DYE:
                return ChatColor.WHITE;
            case ORANGE_DYE:
                return ChatColor.GOLD;
            case MAGENTA_DYE:
                return ChatColor.LIGHT_PURPLE;
            case LIGHT_BLUE_DYE:
                return ChatColor.AQUA;
            case YELLOW_DYE:
                return ChatColor.YELLOW;
            case LIME_DYE:
                return ChatColor.GREEN;
            case PINK_DYE:
                return ChatColor.LIGHT_PURPLE;
            case GRAY_DYE:
                return ChatColor.GRAY;
            case LIGHT_GRAY_DYE:
                return ChatColor.GRAY;
            case CYAN_DYE:
                return ChatColor.DARK_AQUA;
            case PURPLE_DYE:
                return ChatColor.DARK_PURPLE;
            case BLUE_DYE:
                return ChatColor.BLUE;
            case BROWN_DYE:
                return ChatColor.GOLD;
            case GREEN_DYE:
                return ChatColor.DARK_GREEN;
            case RED_DYE:
                return ChatColor.RED;
            case BLACK_DYE:
                return ChatColor.BLACK;
            default:
                return ChatColor.WHITE;
        }
    }

    private List<Material> getDyeMaterials() {
        return Arrays.asList(
                Material.WHITE_DYE,
                Material.ORANGE_DYE,
                Material.MAGENTA_DYE,
                Material.LIGHT_BLUE_DYE,
                Material.YELLOW_DYE,
                Material.LIME_DYE,
                Material.PINK_DYE,
                Material.GRAY_DYE,
                Material.LIGHT_GRAY_DYE,
                Material.CYAN_DYE,
                Material.PURPLE_DYE,
                Material.BLUE_DYE,
                Material.BROWN_DYE,
                Material.GREEN_DYE,
                Material.RED_DYE,
                Material.BLACK_DYE
        );
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        UUID playerUUID = player.getUniqueId();

        if (!dyeColorGUIs.containsKey(playerUUID)) return;

        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(dyeColorGUIs.get(playerUUID))) return;

        event.setCancelled(true);

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        Material dyeMaterial = clickedItem.getType();
        Color dyeColor = getColorFromDyeMaterial(dyeMaterial);

        // Set the new color
        playerDyeColors.put(playerUUID, dyeColor);
        player.closeInventory();

        cancelExistingParticleTask(playerUUID);
        startParticleTask(player);
    }

    private Color getColorFromDyeMaterial(Material dyeMaterial) {
        switch (dyeMaterial) {
            case WHITE_DYE:
                return Color.fromRGB(255, 255, 255);
            case ORANGE_DYE:
                return Color.fromRGB(255, 165, 0);
            case MAGENTA_DYE:
                return Color.fromRGB(255, 0, 255);
            case LIGHT_BLUE_DYE:
                return Color.fromRGB(173, 216, 230);
            case YELLOW_DYE:
                return Color.fromRGB(255, 255, 0);
            case LIME_DYE:
                return Color.fromRGB(0, 255, 0);
            case PINK_DYE:
                return Color.fromRGB(255, 192, 203);
            case GRAY_DYE:
                return Color.fromRGB(128, 128, 128);
            case LIGHT_GRAY_DYE:
                return Color.fromRGB(192, 192, 192);
            case CYAN_DYE:
                return Color.fromRGB(0, 255, 255);
            case PURPLE_DYE:
                return Color.fromRGB(128, 0, 128);
            case BLUE_DYE:
                return Color.fromRGB(0, 0, 255);
            case BROWN_DYE:
                return Color.fromRGB(139, 69, 19);
            case GREEN_DYE:
                return Color.fromRGB(0, 128, 0);
            case RED_DYE:
                return Color.fromRGB(255, 0, 0);
            case BLACK_DYE:
                return Color.fromRGB(0, 0, 0);
            default:
                return Color.WHITE;
        }
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
        task.runTaskTimer(plugin, 0L, 1L);
        particleTasks.put(player.getUniqueId(), task);
    }

    private void cancelExistingParticleTask(UUID playerUUID) {
        if (particleTasks.containsKey(playerUUID)) {
            particleTasks.get(playerUUID).cancel();
            particleTasks.remove(playerUUID);
            previousOffsetDistances.remove(playerUUID);
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

        Location runnerLocation = nearestRunner.getLocation();

        if (!playerLocation.getWorld().equals(runnerLocation.getWorld())) {
            return;
        }

        double playerSpeed = player.getVelocity().length();
        double targetOffsetDistance;

        if (playerSpeed >= 0.1) {
            targetOffsetDistance = player.isInsideVehicle() ? 6.0 : 3.0; // Adjusted for moving
        } else {
            targetOffsetDistance = player.isInsideVehicle() ? 3.0 : 0.5; // Adjusted for standing still
        }

        double previousOffsetDistance = previousOffsetDistances.getOrDefault(player.getUniqueId(), 0.1);
        double interpolatedOffsetDistance = interpolate(previousOffsetDistance, targetOffsetDistance, 0.01);
        previousOffsetDistances.put(player.getUniqueId(), interpolatedOffsetDistance);

        Vector direction = runnerLocation.clone().add(0, 1.5, 0).subtract(playerLocation.clone().add(0, getPlayerYOffset(player), 0)).toVector().normalize();
        Location particleStartLocation = playerLocation.clone().add(0, getPlayerYOffset(player), 0);
        Vector offset = direction.clone().multiply(interpolatedOffsetDistance);
        Location particleLocation = particleStartLocation.clone().add(offset);

        // Calculate distance between particle location and player location
        double distance = particleLocation.distance(playerLocation);

        // Calculate particle size based on distance (adjust the values as needed)
        float particleSize = distance <= 1.0 ? 0.1f : 0.3f;

        // Get the color from the hashmap
        Color dyeColor = playerDyeColors.get(player.getUniqueId());

        // Spawn particles with the selected color (or null if no color is selected)
        if (dyeColor != null) {
            player.getWorld().spawnParticle(Particle.REDSTONE, particleLocation, 1, 0, 0, 0, 0, new Particle.DustOptions(dyeColor, particleSize));
        }
    }

    private double getPlayerYOffset(Player player) {
        if (player.isInsideVehicle()) {
            return 1; // Adjust as needed when player is in a boat
        } else if (player.isSneaking()) {
            return 1.25; // Adjust as needed when player is shifting
        } else if (player.isSwimming()) {
            return 0.25; // Adjust as needed when player is swimming
        } else {
            return 1.5; // Default eye level
        }
    }

    private double interpolate(double startValue, double endValue, double ratio) {
        return startValue + (endValue - startValue) * ratio;
    }
}
