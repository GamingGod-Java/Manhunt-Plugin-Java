package me.champion.manhuntplugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class TeamChat implements Listener, CommandExecutor {
    private final TeamManager teamManager;
    private final Set<UUID> inTeamChat = new HashSet<>();

    public TeamChat(TeamManager teamManager) {
        this.teamManager = teamManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Handling the team chat toggle command
        if (command.getName().equalsIgnoreCase("MhTeamChat") && sender instanceof Player) {
            Player player = (Player) sender;
            toggleTeamChat(player);
            return true;
        }

        return false;
    }

    public void toggleTeamChat(Player player) {
        // Check if the player is on any team
        boolean isOnAnyTeam = false;
        for (String team : Arrays.asList("Zombies", "Runners")) {
            if (teamManager.isOnTeam(player, team)) {
                isOnAnyTeam = true;
                break;
            }
        }

        // If the player is not on any team, display an error message
        if (!isOnAnyTeam) {
            player.sendMessage(ChatColor.RED + "You need to select a team before enabling team chat.");
            return;
        }

        // Toggle team chat
        if (inTeamChat.contains(player.getUniqueId())) {
            inTeamChat.remove(player.getUniqueId());
            player.sendMessage(ChatColor.GREEN + "Team chat disabled.");
        } else {
            inTeamChat.add(player.getUniqueId());
            player.sendMessage(ChatColor.GREEN + "Team chat enabled.");
        }
    }


    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        if (inTeamChat.contains(player.getUniqueId())) {
            // Player has team chat enabled, redirect message to team only
            event.setCancelled(true);

            String team = null;
            for (String t : Arrays.asList("Zombies", "Runners")) {
                if (teamManager.isOnTeam(player, t)) {
                    team = t;
                    break;
                }
            }

            if (team != null) {
                String message = ChatColor.GRAY + "[" + team + " Chat] " + player.getName() + ": " + event.getMessage();
                sendTeamMessage(team, message);
            } else {
                player.sendMessage(ChatColor.RED + "You are not on a team. Cannot send message in team chat.");
            }
        }
        // If not in team chat, the message will go to all chat by default
    }
    // Send a message to all players in the specified team
    private void sendTeamMessage(String team, String message) {
        for (Player p : Bukkit.getServer().getOnlinePlayers()) {
            if (teamManager.isOnTeam(p, team)) {
                p.sendMessage(message);
            }
        }
    }
}
