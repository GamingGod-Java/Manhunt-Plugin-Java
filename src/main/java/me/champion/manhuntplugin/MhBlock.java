package me.champion.manhuntplugin;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.block.Block;

public class MhBlock implements CommandExecutor {

    private final Manhunt plugin;

    public MhBlock(Manhunt plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("mhblock")) {
            if (sender instanceof Player) {
                detectBlock((Player) sender);
            } else {
                sender.sendMessage("Only players can use this command!");
            }
            return true;
        }
        return false;
    }

    private void detectBlock(Player player) {
        Block block = player.getLocation().subtract(0, 1, 0).getBlock(); // Get block at -y level

        if (block.getType() == Material.RED_WOOL) {
            player.sendMessage("You are standing on a Zombie block!");
        } else if (block.getType() == Material.BLUE_WOOL) {
            player.sendMessage("You are standing on a Runner block!");
        } else {
            player.sendMessage("You are not standing on a team block.");
        }
    }
}
