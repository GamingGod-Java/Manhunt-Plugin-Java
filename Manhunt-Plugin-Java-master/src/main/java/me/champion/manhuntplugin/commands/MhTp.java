package me.champion.manhuntplugin.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MhTp implements CommandExecutor {

    private final FileConfiguration config;

    public MhTp(FileConfiguration config) {
        this.config = config;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can execute this command.");
            return true;
        }

        Player player = (Player) sender;
        List<Player> playersToTeleport = new ArrayList<>();

        if (args.length == 2 && args[1].equalsIgnoreCase("random")) {
            if (args[0].equalsIgnoreCase("@a")) {
                playersToTeleport.addAll(Bukkit.getServer().getOnlinePlayers());
            } else {
                Player target = Bukkit.getPlayer(args[0]);
                if (target != null) {
                    playersToTeleport.add(target);
                } else {
                    player.sendMessage("Player not found.");
                    return true;
                }
            }
        } else {
            player.sendMessage("Usage: /mhtp <username> random or /mhtp @a random");
            return true;
        }

        if (config == null || !config.contains("overworldSize")) {
            player.sendMessage("Config file not found or 'overworldSize' value not specified.");
            return true;
        }

        int overworldSize = config.getInt("overworldSize");
        Random random = new Random();

        for (Player playerToTeleport : playersToTeleport) {
            int x = random.nextInt(overworldSize * 2) - overworldSize;
            int z = random.nextInt(overworldSize * 2) - overworldSize;
            int y = Bukkit.getWorlds().get(0).getHighestBlockYAt(x, z) + 1; // Teleport 1 block higher
            playerToTeleport.teleport(Bukkit.getWorlds().get(0).getBlockAt(x, y, z).getLocation());
        }

        player.sendMessage("Teleported players to random locations within the overworldSize.");
        return true;
    }
}
