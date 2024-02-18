package me.champion.manhuntplugin;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.Set;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

public class MhCreate implements CommandExecutor {

    public static final int PLATFORM_SIZE = 5;
    private static final int PLATFORM_DISTANCE = 9;
    private final Manhunt plugin;
    private final TeamManager teamManager;  // Added TeamManager instance

    private final MhStart mhStart;

    private final MhIso mhIso;  // Added IsoUlt instance

    private final List<Location> platformLocations = new ArrayList<>();

    private static Location spawnLocation; // Declare the spawnLocation variable

    public MhCreate(Manhunt plugin, TeamManager teamManager, MhStart mhStart, MhIso mhIso) {
        this.plugin = plugin;
        this.teamManager = teamManager;  // Initialize TeamManager instance
        this.mhStart = mhStart;
        this.mhIso = mhIso;
    }

    private BukkitTask saturationTask;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command!");
            return true;
        }

        Player player = (Player) sender;

        // Check if player has OP
        if (!sender.isOp()) {
            player.sendMessage("§cYou do not have permission to use this command.");
            return true;
        }
        // Store the spawn location where the command is executed
        setSpawnLocation(player.getLocation());

        // Teleport the player to the exact center of the block they're standing on
        teleportPlayerToCenter(player);
        createGlassSphere(player);

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "gamerule doMobSpawning false");

        // Generate a 5x5x1 light blue concrete platform north of the player
        Location bluePlatformLocation = generatePlatform(player.getLocation().add(0, -1, -PLATFORM_DISTANCE / 2),
                Material.LIGHT_BLUE_CONCRETE, -4); // North
        platformLocations.add(bluePlatformLocation);

        // Generate a 5x5x1 red concrete platform south of the player
        Location redPlatformLocation = generatePlatform(player.getLocation().add(0, -1, PLATFORM_DISTANCE / 2),
                Material.RED_CONCRETE, 4); // South
        platformLocations.add(redPlatformLocation);

        // Register platforms with TeamManager
        teamManager.registerPlatform("Runners", bluePlatformLocation);
        teamManager.registerPlatform("Zombies", redPlatformLocation);

        player.sendMessage("§bRunner§r and §cZombie§r platforms spawned around you! Type /MhStart to start the countdown.");

        for (Player onlineplayer: Bukkit.getOnlinePlayers()) {
            onlineplayer.setInvulnerable(true);
        }

        mhIso.createBarrierBox(player);

        saturationTask = Bukkit.getScheduler().runTaskTimer(Bukkit.getPluginManager().getPlugin("Manhunt"), () -> {
            for (Player onlineplayer : Bukkit.getOnlinePlayers()) {
                // Assuming 'player' is the Player object you want to modify


                // Check if the player is in survival mode
                if (!mhStart.gameStarted) {
                    // Set the player's saturation to the maximum value
                    onlineplayer.setSaturation(Float.MAX_VALUE);
                }
                 else {
                    // Cancel the task if the player is not in survival mode
                    saturationTask.cancel();
                }
            }
        }, 0L, 20L * 60L); // Run the task every 60 seconds (adjust as needed)

        return true;
    }

    private void teleportPlayerToCenter(Player player) {
        Location currentLocation = player.getLocation();
        double x = currentLocation.getBlockX() + 0.5;
        double y = currentLocation.getBlockY();
        double z = currentLocation.getBlockZ() + 0.5;
        player.teleport(new Location(currentLocation.getWorld(), x, y, z));
    }

    private Location generatePlatform(Location centerLocation, Material platformMaterial, int zOffset) {
        World world = centerLocation.getWorld();
        int xOffset = (PLATFORM_SIZE - 1) / 2;

        for (int x = 0; x < PLATFORM_SIZE; x++) {
            for (int z = 0; z < PLATFORM_SIZE; z++) {
                Location platformLocation = centerLocation.clone().add(x - xOffset, 0, z - xOffset + zOffset);
                world.getBlockAt(platformLocation).setType(platformMaterial);
            }
        }

        // Return the center location of the generated platform
        return centerLocation.clone().add(0, 1, 0);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        /*Player player = event.getPlayer();
        if (!mhStart.gameStarted) {
            //Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "effect give "+ event.getPlayer().getName()+ " minecraft:saturation 1000000 255 true");
            player.setSaturation(Float.MAX_VALUE);
        }*/
    }

    public void createGlassSphere(Player player) {
        int createRadius = 15; // Radius for creation
        int sphereHeight = -2; // Height below the player
        World world = player.getWorld();
        Location center = player.getLocation().add(0, sphereHeight, 0);

        Set<Material> replaceableBlocks = new HashSet<>(Arrays.asList(
                Material.DANDELION, Material.POPPY, Material.BLUE_ORCHID, Material.ALLIUM,
                Material.AZURE_BLUET, Material.RED_TULIP, Material.ORANGE_TULIP, Material.WHITE_TULIP,
                Material.PINK_TULIP, Material.OXEYE_DAISY, Material.CORNFLOWER, Material.LILY_OF_THE_VALLEY,
                Material.SUNFLOWER, Material.LILAC, Material.ROSE_BUSH, Material.PEONY,
                Material.TALL_GRASS, Material.SHORT_GRASS, Material.FERN, Material.LARGE_FERN,
                Material.VINE, Material.SCUTE, Material.SNOW, Material.SUGAR_CANE
                // Add other non-solid blocks here as needed
        ));

        for (int x = -createRadius; x <= createRadius; x++) {
            for (int y = -createRadius; y <= createRadius; y++) {
                for (int z = -createRadius; z <= createRadius; z++) {
                    double distance = Math.sqrt(x * x + y * y + z * z);
                    if (distance <= createRadius && distance >= createRadius - 1) {
                        Location blockLocation = center.clone().add(x, y, z);
                        Block block = world.getBlockAt(blockLocation);
                        Material blockType = block.getType();

                        // Replace lava with red stained glass
                        if (blockType == Material.LAVA) {
                            blockLocation.getBlock().setType(Material.RED_STAINED_GLASS, false);
                        }
                        // Replace water and water-related blocks with blue stained glass
                        else if (blockType == Material.WATER || blockType == Material.SEAGRASS ||
                                blockType == Material.TALL_SEAGRASS || blockType == Material.KELP ||
                                blockType == Material.KELP_PLANT) {
                            blockLocation.getBlock().setType(Material.BLUE_STAINED_GLASS, false);
                        }
                        // Replace air and other replaceable non-solid blocks with regular glass
                        else if (blockType == Material.AIR || replaceableBlocks.contains(blockType)) {
                            blockLocation.getBlock().setType(Material.GLASS, false);
                        }
                    }
                }
            }
        }
    }

    // Method to remove the glass sphere created by createGlassSphere
    public void removeGlassSphere(Player player) {
        int removeRadius = 35; // Radius for removal
        World world = player.getWorld();
        Location center = player.getLocation().subtract(0, 2, 0); // Adjusted to be 2 blocks below the player

        for (int x = -removeRadius; x <= removeRadius; x++) {
            for (int y = -removeRadius; y <= removeRadius; y++) {
                for (int z = -removeRadius; z <= removeRadius; z++) {
                    double distance = Math.sqrt(x * x + y * y + z * z);
                    if (distance <= removeRadius) {
                        Location blockLocation = center.clone().add(x, y, z);
                        Block block = world.getBlockAt(blockLocation);
                        if (block.getType() == Material.GLASS) {
                            blockLocation.getBlock().setType(Material.AIR, false);
                        } else if (block.getType() == Material.BLUE_STAINED_GLASS) {
                            blockLocation.getBlock().setType(Material.WATER, false);
                        } else if (block.getType() == Material.RED_STAINED_GLASS) {
                            blockLocation.getBlock().setType(Material.LAVA, false);
                        }
                    }
                }
            }
        }
    }

    // Method to set the spawn location and log it to the console
    public static void setSpawnLocation(Location location) {
        spawnLocation = location;
        World world = location.getWorld();
        if (world != null) {
            world.setSpawnLocation(location.getBlockX(), location.getBlockY(), location.getBlockZ());
            Bukkit.getLogger().info("Spawn location set to: X=" + location.getBlockX() + ", Y=" + location.getBlockY() + ", Z=" + location.getBlockZ());
        }
    }

    // Method to get the spawn location
    public static Location getSpawnLocation() {
        return spawnLocation;
    }
}
