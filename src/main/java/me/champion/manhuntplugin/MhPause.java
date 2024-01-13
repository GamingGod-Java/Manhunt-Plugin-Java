package me.champion.manhuntplugin;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MhPause implements CommandExecutor {

    private final TeamManager teamManager;
    private final Manhunt plugin; // Add a reference to the Manhunt plugin

    public MhPause(Manhunt plugin, TeamManager teamManager) {
        this.plugin = plugin;
        this.teamManager = teamManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            // Call the pauseGame method in the TeamManager
            teamManager.pauseGame(player);

            // Set the player to Adventure mode
            player.setGameMode(GameMode.ADVENTURE);

            // Set the walk speed to 0 - this makes the player unable to walk
            player.setWalkSpeed(0.0f);

            // Execute /tick freeze command
            Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "tick freeze");

            // Start continuous looking behavior
            startContinuousLooking(player);

            return true;
        }

        return false;
    }

    // Method to start continuous looking
    private void startContinuousLooking(Player player) {
        // Define yaw and pitch angles (you can set them as needed)
        float yaw = 0.0f;  // Replace with the desired yaw angle
        float pitch = 0.0f;  // Replace with the desired pitch angle

        // Schedule a task to continuously set the player's view direction
        int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            player.getLocation().setYaw(yaw);
            player.getLocation().setPitch(pitch);
        }, 0L, 1L); // Adjust the delay and interval as needed

        // Store the task ID somewhere (e.g., in a Map) if you need to cancel it later
    }
}
