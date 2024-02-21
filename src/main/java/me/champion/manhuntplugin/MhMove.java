package me.champion.manhuntplugin;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class MhMove implements CommandExecutor, TabCompleter {

    private final TeamManager teamManager;

    public MhMove(TeamManager teamManager) {
        this.teamManager = teamManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player) || !sender.isOp()) {
            sender.sendMessage("Only players can use this command!");
            return true;
        }

        if (args.length != 2) {
            sender.sendMessage("Usage: /mhmove <playername> <team>");
            return true;
        }

        String playerName = args[0];
        String teamName = args[1];

        // Get the Player instance by name
        Player targetPlayer = Bukkit.getPlayer(playerName);

        if (targetPlayer == null) {
            sender.sendMessage("Player " + playerName + " not found!");
            return true;
        }

        // Check if the provided team name is valid
        if (!teamName.equalsIgnoreCase("zombies") && !teamName.equalsIgnoreCase("runners") && !teamName.equalsIgnoreCase("zombie") && !teamName.equalsIgnoreCase("runner") && !teamName.equalsIgnoreCase("none")) {
            sender.sendMessage("Valid team names are: Zombies, Runners, none");
            return true;
        }

        // Convert teamName to lowercase for internal logic
        String teamNameLower = teamName.toLowerCase();

        // Assign the player to the specified team or remove them from any team
        if (teamNameLower.equals("none")) {
            teamManager.removeFromTeam(targetPlayer);
            sender.sendMessage("Player " + playerName + " removed from all teams");
        } else {
            if (teamNameLower.equalsIgnoreCase("zombie")) {
                teamNameLower = "zombies";
            } else if (teamNameLower.equalsIgnoreCase("runner")) {
                teamNameLower = "runners";
            }

            teamManager.addToTeam(targetPlayer, teamNameLower);

            // Capitalize the first letter for display in the chat message
            String displayTeamName = teamNameLower.substring(0, 1).toUpperCase() + teamNameLower.substring(1);

            // Apply color codes to team names
            String coloredTeamName = displayTeamName.equalsIgnoreCase("Zombies") ? "§c" + displayTeamName : "§b" + displayTeamName;
            sender.sendMessage("Player " + playerName + " moved to team " + coloredTeamName);
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1 && sender instanceof Player) {
            // Autofill player names
            for (Player player : Bukkit.getOnlinePlayers()) {
                completions.add(player.getName());
            }
        } else if (args.length == 2) {
            // Autofill team names with the first letter in uppercase
            completions.add("Zombies");
            completions.add("Runners");
            completions.add("None");
        }

        return completions;
    }
}
