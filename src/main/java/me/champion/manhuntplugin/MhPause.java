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
    public int continuousLookingTaskId = -1;

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
    private int startContinuousLooking(Player player) {
        float yaw = 0.0f;
        float pitch = 0.0f;

        continuousLookingTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            player.getLocation().setYaw(yaw);
            player.getLocation().setPitch(pitch);
            //System.out.println("mhpause restored potion effect");
            //teamManager.restorePotionEffects(player);
        }, 0L, 1L);

        return continuousLookingTaskId;
    }

}
