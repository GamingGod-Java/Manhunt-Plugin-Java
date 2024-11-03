package me.champion.manhuntplugin.commands;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.GameMode;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

public class MhSwap implements CommandExecutor, TabCompleter {

    private final JavaPlugin plugin;
    private final HashMap<String, HashMap<String, Location>> playerLastLocationMap = new HashMap<>();
    private final HashMap<String, String> playerLastVisitedMainWorld = new HashMap<>();
    private final HashMap<String, BukkitTask> soundTaskMap = new HashMap<>();

    public MhSwap(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2 || args.length > 3) {
            sender.sendMessage("Usage: /mhswap <username|@a> <world_name> [worldSpawn|time_in_seconds]");
            return true;
        }

        // Get target players
        List<Player> targetPlayers;
        if (args[0].equalsIgnoreCase("@a")) {
            targetPlayers = new ArrayList<>(Bukkit.getOnlinePlayers()); // All online players
        } else {
            Player player = Bukkit.getPlayer(args[0]);
            if (player == null) {
                sender.sendMessage("Player not found.");
                return true;
            }
            targetPlayers = List.of(player);
        }

        // Parse target world and delay or spawn location
        String targetWorld = args[1].toLowerCase();
        int delaySeconds = 0;
        boolean toWorldSpawn = args.length == 3 && args[2].equalsIgnoreCase("worldSpawn");

        // Check if the third argument is a delay in seconds
        if (!toWorldSpawn && args.length == 3) {
            try {
                delaySeconds = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                sender.sendMessage("Invalid delay time. Use 'worldSpawn' or a number for seconds.");
                return true;
            }
        }

        // Process each player
        for (Player player : targetPlayers) {
            if (targetWorld.equals("world") && toWorldSpawn) {
                stopHellSound(player);
                Location configWorldSpawn = getWorldSpawnFromConfig();
                if (configWorldSpawn != null) {
                    teleportPlayerWithReturn(player, configWorldSpawn, delaySeconds);
                } else {
                    sender.sendMessage("worldSpawn location not found in config.");
                }
            } else {
                switch (targetWorld) {
                    case "world":
                        stopHellSound(player);
                        teleportPlayerToLastKnownLocation(player, "world", delaySeconds);
                        break;
                    case "world_spawn":
                        stopHellSound(player);
                        teleportPlayerToWorldAndReturn(player, "world_spawn", delaySeconds, getFixedLocation("world_spawn"));
                        break;
                    case "customworld":
                        stopHellSound(player);
                        teleportPlayerToLastKnownLocation(player, "customworld", delaySeconds);
                        break;
                    case "world_hell":
                        teleportPlayerToWorldAndReturn(player, "world_hell", delaySeconds, getFixedLocation("world_hell"));
                        playHellSound(player, delaySeconds);
                        break;
                    default:
                        sender.sendMessage("Invalid world name. Use /mhswap <username|@a> <world|world_spawn|customworld|world_hell> [worldSpawn|time_in_seconds]");
                        return true;
                }
            }
        }
        return true;
    }

    private Location getWorldSpawnFromConfig() {
        String worldSpawnString = plugin.getConfig().getString("worldSpawn");
        if (worldSpawnString != null) {
            String[] parts = worldSpawnString.split(",");
            double x = Double.parseDouble(parts[0]);
            double y = Double.parseDouble(parts[1]);
            double z = Double.parseDouble(parts[2]);
            return new Location(Bukkit.getWorld("world"), x, y, z);
        }
        return null;
    }

    private Location getFixedLocation(String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            world = Bukkit.createWorld(new WorldCreator(worldName));
        }
        return new Location(world, 0, 101, 0);
    }

    private void savePlayerLocation(Player player) {
        String worldName = player.getWorld().getName();
        String playerUUID = player.getUniqueId().toString();

        if (worldName.equals("world") || worldName.equals("customworld")) {
            playerLastLocationMap.putIfAbsent(playerUUID, new HashMap<>());
            playerLastLocationMap.get(playerUUID).put(worldName, player.getLocation());
            playerLastVisitedMainWorld.put(playerUUID, worldName);
        }
    }

    private void teleportPlayerToLastKnownLocation(Player player, String targetWorldName, int delaySeconds) {
        savePlayerLocation(player);
        World targetWorld = Bukkit.getWorld(targetWorldName);
        if (targetWorld == null) {
            player.sendMessage("World " + targetWorldName + " is not loaded. Loading world...");
            targetWorld = Bukkit.createWorld(new WorldCreator(targetWorldName));
        }

        Location lastKnownLocation = playerLastLocationMap
                .getOrDefault(player.getUniqueId().toString(), new HashMap<>())
                .get(targetWorldName);

        if (lastKnownLocation == null) {
            lastKnownLocation = targetWorld.getSpawnLocation();
        }
        teleportPlayerWithReturn(player, lastKnownLocation, delaySeconds);
    }

    private void teleportPlayerToWorldAndReturn(Player player, String worldName, int delaySeconds, Location fixedLocation) {
        savePlayerLocation(player);
        World targetWorld = Bukkit.getWorld(worldName);
        if (targetWorld == null) {
            player.sendMessage("World " + worldName + " is not loaded. Loading world...");
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                World newWorld = Bukkit.createWorld(new WorldCreator(worldName));
                if (newWorld != null) {
                    teleportPlayerWithReturn(player, fixedLocation != null ? fixedLocation : newWorld.getSpawnLocation(), delaySeconds);
                } else {
                    player.sendMessage("Failed to load " + worldName + ".");
                }
            });
        } else {
            teleportPlayerWithReturn(player, fixedLocation != null ? fixedLocation : targetWorld.getSpawnLocation(), delaySeconds);
        }
    }

    private void teleportPlayerWithReturn(Player player, Location targetLocation, int delaySeconds) {
        player.teleport(targetLocation);
        if (delaySeconds > 0) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    String playerUUID = player.getUniqueId().toString();
                    String lastVisitedWorld = playerLastVisitedMainWorld.get(playerUUID);
                    if (lastVisitedWorld != null && (lastVisitedWorld.equals("world") || lastVisitedWorld.equals("customworld"))) {
                        Location originalLocation = playerLastLocationMap.get(playerUUID).get(lastVisitedWorld);
                        if (originalLocation != null) {
                            player.teleport(originalLocation);
                        }
                    }
                }
            }.runTaskLater(plugin, delaySeconds * 20L);
        }
    }

    private void playHellSound(Player player, int durationSeconds) {
        String playerUUID = player.getUniqueId().toString();
        if (soundTaskMap.containsKey(playerUUID)) {
            soundTaskMap.get(playerUUID).cancel();
            soundTaskMap.remove(playerUUID);
        }

        GameMode previousGameMode = player.getGameMode();
        player.setMetadata("previousGameMode", new FixedMetadataValue(plugin, previousGameMode));
        player.setGameMode(GameMode.ADVENTURE);
        player.playSound(player.getLocation(), "minecraft:hell", SoundCategory.MASTER, 1.0f, 1.0f);

        int blindnessDuration = (durationSeconds > 0 ? durationSeconds * 20 : Integer.MAX_VALUE) + 20;
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, blindnessDuration, 1, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, blindnessDuration, 255, false, false));
    }

    private void stopHellSound(Player player) {
        String playerUUID = player.getUniqueId().toString();
        if (soundTaskMap.containsKey(playerUUID)) {
            soundTaskMap.get(playerUUID).cancel();
            soundTaskMap.remove(playerUUID);
        }

        player.stopSound("minecraft:hell", SoundCategory.MASTER);
        if (player.hasMetadata("previousGameMode")) {
            GameMode previousGameMode = (GameMode) player.getMetadata("previousGameMode").get(0).value();
            player.setGameMode(previousGameMode);
            player.removeMetadata("previousGameMode", plugin);
        }
        player.removePotionEffect(PotionEffectType.BLINDNESS);
        player.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> playerNames = new ArrayList<>();
            playerNames.add("@a");
            for (Player player : Bukkit.getOnlinePlayers()) {
                playerNames.add(player.getName());
            }
            return playerNames;
        }

        if (args.length == 2) {
            return Arrays.asList("world", "world_spawn", "customworld", "world_hell");
        }

        if (args.length == 3 && args[1].equalsIgnoreCase("world")) {
            return Arrays.asList("worldSpawn", "30", "60", "120", "300", "600");
        }

        return null;
    }
}
