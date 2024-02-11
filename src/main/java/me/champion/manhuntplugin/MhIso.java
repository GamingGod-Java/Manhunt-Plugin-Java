package me.champion.manhuntplugin;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MhIso implements Listener, CommandExecutor {
    private TeamManager teamManager;
    private Plugin plugin;

    private Location runnerlocation;

    private Location zombielocation;

    private boolean fight = false;

    private final Map<UUID, Location> playerLocations = new HashMap<>();
    public  MhIso(TeamManager teamManager, Plugin plugin) {
        this.teamManager = teamManager;
        this.plugin = plugin;
    }

    public void createBarrierBox(Player player) {
        int boxRadius = 16; // Radius for bedrock box
        int boxHeight = 5;  // Height of the bedrock box
        World world = player.getWorld();
        Location center = player.getLocation().add(0, 20, 0);
        runnerlocation = player.getLocation().add(10, 21, 0);
        zombielocation = player.getLocation().add(-10, 21, 0);

        for (int x = -boxRadius; x <= boxRadius; x++) {
            for (int y = 0; y <= boxHeight; y++) {
                for (int z = -boxRadius; z <= boxRadius; z++) {
                    Location blockLocation = center.clone().add(x, y, z);

                    // Floor: Bedrock
                    if (y == 0) {
                        world.getBlockAt(blockLocation).setType(Material.BEDROCK, false);
                    }
                    // Walls and Roof: Barriers
                    else if (Math.abs(x) == boxRadius || Math.abs(z) == boxRadius || y == boxHeight) {
                        world.getBlockAt(blockLocation).setType(Material.BARRIER, false);
                    }
                }
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {
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

        if (args.length < 2 && !fight) {
            player.sendMessage("Usage: /isoult <player1> <player2>");
            return true;
        }

        if (!fight) {

            String runnername = args[0];
            String zombiename = args[1];

            Player runner = Bukkit.getPlayerExact(runnername);
            Player zombie = Bukkit.getPlayerExact(zombiename);

            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                savePlayerLocation(onlinePlayer);
                if ((!onlinePlayer.getName().equals(runnername) && !onlinePlayer.getName().equals(zombiename))) {
                    player.setGameMode(GameMode.SPECTATOR);
                }
            }

            runner.teleport(runnerlocation);
            zombie.teleport(zombielocation);

            //teamManager.pauseGame(player);
        }

        if (fight) {
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                restorePlayerLocation(onlinePlayer);
                player.setGameMode(GameMode.SURVIVAL);
            }
        }

        fight = !fight;

        return true;
    }

    private void savePlayerLocation(Player player) {
        playerLocations.put(player.getUniqueId(), player.getLocation());
        System.out.println(playerLocations);
    }

    private void restorePlayerLocation(Player player) {
        UUID playerId = player.getUniqueId();

        if (playerLocations.containsKey(playerId)) {
            Location location = playerLocations.get(playerId);

            // Check if the location is not null and the world is valid
            if (location != null && Bukkit.getWorld(location.getWorld().getName()) != null) {
                player.teleport(location);
            } else {
                player.sendMessage("Invalid saved location for " + player.getName());
            }
        } else {
            player.sendMessage("No saved location found for " + player.getName());
        }
    }
}
