package me.champion.manhuntplugin;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.block.Block;

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

    private final List<Location> platformLocations = new ArrayList<>();

    public MhCreate(Manhunt plugin, TeamManager teamManager) {
        this.plugin = plugin;
        this.teamManager = teamManager;  // Initialize TeamManager instance
    }

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

        // Teleport the player to the exact center of the block they're standing on
        teleportPlayerToCenter(player);
        createGlassSphere(player);
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

        player.sendMessage("Runners and Zombies platforms spawned around you! Type /mhstart to start the countdown.");
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
    public void createGlassSphere(Player player) {
        int createRadius = 15; // Radius for creation
        int sphereHeight = -2; // Height below the player
        World world = player.getWorld();
        Location center = player.getLocation().add(0, sphereHeight, 0);

        Set<Material> flowers = new HashSet<>(Arrays.asList(
                Material.DANDELION, // Dandelion
                Material.POPPY, // Poppy
                Material.BLUE_ORCHID, // Blue Orchid
                Material.ALLIUM, // Allium
                Material.AZURE_BLUET, // Azure Bluet
                Material.RED_TULIP, // Red Tulip
                Material.ORANGE_TULIP, // Orange Tulip
                Material.WHITE_TULIP, // White Tulip
                Material.PINK_TULIP, // Pink Tulip
                Material.OXEYE_DAISY, // Oxeye Daisy
                Material.CORNFLOWER, // Cornflower
                Material.LILY_OF_THE_VALLEY, // Lily of the Valley
                Material.SUNFLOWER, // Sunflower
                Material.LILAC, // Lilac
                Material.ROSE_BUSH, // Rose Bush
                Material.PEONY // Peony
        ));

        for (int x = -createRadius; x <= createRadius; x++) {
            for (int y = -createRadius; y <= createRadius; y++) {
                for (int z = -createRadius; z <= createRadius; z++) {
                    double distance = Math.sqrt(x * x + y * y + z * z);
                    if (distance <= createRadius && distance >= createRadius - 1) {
                        Location blockLocation = center.clone().add(x, y, z);
                        Block block = world.getBlockAt(blockLocation);
                        Material blockType = block.getType();

                        if (blockType == Material.AIR ||
                                flowers.contains(blockType) || // Treat all flowers as air
                                blockType == Material.TALL_GRASS ||
                                blockType == Material.SHORT_GRASS ||

                                blockType == Material.FERN ||
                                blockType == Material.LARGE_FERN ||
                                blockType == Material.VINE ||
                                blockType == Material.SCUTE ||
                                blockType == Material.SNOW) { // Turn snow layers into regular glass
                            blockLocation.getBlock().setType(Material.GLASS, false);
                        } else if ( //Water based blocks
                                blockType == Material.WATER
                                        || blockType == Material.TALL_SEAGRASS
                                        || blockType == Material.SEAGRASS
                                        || blockType == Material.KELP
                                        || blockType == Material.KELP_PLANT
                        ) {
                            blockLocation.getBlock().setType(Material.BLUE_STAINED_GLASS, false);
                        } else if (blockType == Material.LAVA) {
                            blockLocation.getBlock().setType(Material.RED_STAINED_GLASS, false);
                        }
                    }
                }
            }
        }
    }


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
}