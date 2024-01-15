package me.champion.manhuntplugin;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class TeamMove implements CommandExecutor, TabCompleter {

    private final TeamManager teamManager;

    public TeamMove(TeamManager teamManager) {
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
        if (!teamName.equalsIgnoreCase("zombies") && !teamName.equalsIgnoreCase("runners") && !teamName.equalsIgnoreCase("zombie") && !teamName.equalsIgnoreCase("runner")&& !teamName.equalsIgnoreCase("none")) {
            sender.sendMessage("Invalid team name! Valid team names are: zombies, runners, zombie, runner, none");
            return true;
        }

        // Assign the player to the specified team or remove them from any team
        if (teamName.equalsIgnoreCase("none")) {
            teamManager.removeFromTeam(targetPlayer);
        } else {
            if  (teamName.equalsIgnoreCase("zombie")) {
                teamName = "zombies";

            } if  (teamName.equalsIgnoreCase("runner")) {
                teamName = "runners";

            }
            teamManager.addToTeam(targetPlayer, teamName);
        }

        sender.sendMessage("Player " + playerName + " moved to team " + teamName);

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
            // Autofill team names
            completions.add("zombies");
            completions.add("runners");
            completions.add("none");
        }

        return completions;
    }
}
