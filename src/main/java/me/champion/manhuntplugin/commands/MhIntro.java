package me.champion.manhuntplugin.commands;

//to edit the audio clip https://www.veed.io/edit/6c0e89c8-c54c-46a4-a08d-54ab53165127
//to convert the audio from mp3/mp4 use https://www.freeconvert.com/mp4-to-ogg
//to update the resourcepack for the server, upload it here first, then change values in server.properties https://mc-packs.net/
//I can share the login details with you

import me.champion.manhuntplugin.Manhunt;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Location;
public class MhIntro implements CommandExecutor {
    private final JavaPlugin plugin;

    public MhIntro() {
        this.plugin = Manhunt.getPlugin();
    }

    private boolean globalCooldown = false;
    private long lastExecutionTime = 0;
    private final long cooldownTime = 50 * 1000; // 50 seconds in milliseconds


    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be executed by a player.");
            return true;
        }

        Player player = (Player) sender;

        // Check if player has OP
        if (!sender.isOp()) {
            player.sendMessage("§cYou do not have permission to use this command.");
            return true;
        }

        if (globalCooldown) {
            player.sendMessage("§cCommand is on cooldown. Please wait before using it again.");
            return true;
        }

        // Iterate over all players and play the sound for each one
        for (Player onlinePlayer : Bukkit.getServer().getOnlinePlayers()) {
            onlinePlayer.playSound(player.getLocation(), "minecraft:hitmarker", 1.0F, 1.0F);
        }

        globalCooldown = true;
        lastExecutionTime = System.currentTimeMillis();

        // Schedule task to reset global cooldown
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            globalCooldown = false;
            lastExecutionTime = 0;
        }, cooldownTime / 50);

        sender.sendMessage("Sound played successfully to all players.");
        return true;
    }
}