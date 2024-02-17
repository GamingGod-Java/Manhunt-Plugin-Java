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
        Location center = player.getLocation().add(40, 20, 40);
        runnerlocation = player.getLocation().add(50, 21, 40);
        zombielocation = player.getLocation().add(30, 21, 40);

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

        // Handle the command to end the game
        if (args.length == 1 && args[0].equalsIgnoreCase("end")) {
            if (fight) {
                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    restorePlayerLocation(onlinePlayer);
                    onlinePlayer.setGameMode(GameMode.SURVIVAL);
                }
                fight = false;
                player.sendMessage("Game has been ended.");
            } else {
                player.sendMessage("The game is not currently running.");
            }
            return true;
        }

        // Check if game is already going, prevent starting a new one
        if (fight) {
            player.sendMessage("A game is already in progress. Use /mhiso end to end the current game.");
            return true;
        }

        // Existing game start logic
        if (args.length < 2) {
            player.sendMessage("Usage: /MhIso <player1> <player2>");
            return true;
        }

        String runnername = args[0];
        String zombiename = args[1];

        // Check if the runner and zombie are the same player
        if (runnername.equalsIgnoreCase(zombiename)) {
            player.sendMessage("Error: A player cannot fight themselves.");
            return true;
        }

        Player runner = Bukkit.getPlayerExact(runnername);
        Player zombie = Bukkit.getPlayerExact(zombiename);

        if (runner == null || zombie == null) {
            player.sendMessage("One or both specified players are not online.");
            return true;
        }

        if (runnerlocation == null || zombielocation == null) {
            player.sendMessage("Error: Game locations not set up correctly.");
            return true;
        }

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            savePlayerLocation(onlinePlayer);
            if ((!onlinePlayer.getName().equals(runnername) && !onlinePlayer.getName().equals(zombiename))) {
                onlinePlayer.setGameMode(GameMode.SPECTATOR);
            }
        }

        runner.teleport(runnerlocation);
        zombie.teleport(zombielocation);

        // Additional setup or game logic can be added here

        fight = true;
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

            // Check if the location and world are not null
            if (location != null && location.getWorld() != null) {
                if (Bukkit.getWorld(location.getWorld().getName()) != null) {
                    player.teleport(location);
                } else {
                    player.sendMessage("Invalid world in saved location for " + player.getName());
                }
            } else {
                player.sendMessage("Invalid saved location for " + player.getName());
            }
        } else {
            player.sendMessage("No saved location found for " + player.getName());
        }
    }
}