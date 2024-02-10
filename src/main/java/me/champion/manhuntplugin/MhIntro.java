package me.champion.manhuntplugin;

//to edit the audio clip https://www.veed.io/edit/6c0e89c8-c54c-46a4-a08d-54ab53165127
//to update the resourcepack for the server, upload it here first, then change values in server.properties https://mc-packs.net/
//I can share the login details with you

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

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be executed by a player.");
            return true;
        }

        // Iterate over all players and play the sound for each one
        for (Player player : Bukkit.getServer().getOnlinePlayers()) {
            player.playSound(player.getLocation(), "minecraft:hitmarker", 1.0F, 1.0F);
        }

        sender.sendMessage("Sound played successfully to all players.");
        return true;
    }
}
