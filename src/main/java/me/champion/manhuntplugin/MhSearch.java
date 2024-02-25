package me.champion.manhuntplugin;

import org.bukkit.block.Biome;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Listener;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.generator.structure.StructureType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Bukkit;

public class MhSearch implements Listener, CommandExecutor {
    private JavaPlugin plugin;
    public MhSearch (JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] strings) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be executed by a player.");
            return true;
        }

        World netherWorld = Bukkit.getWorld("world_nether");
        int minX = -375;
        int minZ = -375;
        int maxX = 375;
        int maxZ = 375;
        Biome targetBiome = Biome.WARPED_FOREST;
        Location FortressSearch = netherWorld.locateNearestStructure(new Location(netherWorld, 0, 0, 0), StructureType.FORTRESS, 2000, false).getLocation();
        if (FortressSearch == null) {
            sender.sendMessage("No fortress found within the specified region, somehow?????????????");

        }
        boolean biomeFound = false;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                Biome currentBiome = netherWorld.getBiome(x, z);

                if (currentBiome == targetBiome) {
                    biomeFound = true;
                    break;
                }
            }
            if (biomeFound) {
                break;
            }
        }

        if (biomeFound) {
            sender.sendMessage("§aWarped forest found");
        } else {
            sender.sendMessage("§cNo warped forest found");
        }
        //String message = String.format("@ %d %d %d", FortressSearch.getBlockX(), FortressSearch.getBlockY(), FortressSearch.getBlockZ());
        if (FortressSearch.getBlockX() < maxX && FortressSearch.getBlockX() > minX && FortressSearch.getBlockZ() < maxZ && FortressSearch.getBlockZ() > minZ) {
            sender.sendMessage("§aFortress found");
            return true;
        } else {
            sender.sendMessage("§cNo fortress found");
            return true;
        }
    }
}
