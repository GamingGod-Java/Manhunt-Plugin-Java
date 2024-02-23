package me.champion.manhuntplugin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.bukkit.Bukkit.getLogger;

public class MhTracker implements CommandExecutor {
    private final JavaPlugin plugin;
    private final Map<UUID, Boolean> actionBarStates; // HashMap to store action bar states for each player

    public MhTracker(JavaPlugin plugin) {
        this.plugin = plugin;
        this.actionBarStates = new HashMap<>();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;
        boolean actionBarEnabled = toggleActionBar(player);

        // Log in chat
        if (actionBarEnabled) {
            getLogger().info("Action bar has been toggled ON for player: " + player.getName());
            player.sendMessage("Action bar has been toggled ON.");
        } else {
            getLogger().info("Action bar has been toggled OFF for player: " + player.getName());
            player.sendMessage("Action bar has been toggled OFF.");
        }

        return true;
    }

    private boolean toggleActionBar(Player player) {
        UUID playerId = player.getUniqueId();
        boolean currentSetting = !actionBarStates.getOrDefault(playerId, false); // Get current state, default to false if not found
        actionBarStates.put(playerId, currentSetting); // Update the state
        return currentSetting;
    }
}
