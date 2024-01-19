package me.champion.manhuntplugin;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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
}
