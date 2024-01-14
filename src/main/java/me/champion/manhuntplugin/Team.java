package me.champion.manhuntplugin;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class Team {
    private final String name;
    private final List<Player> players = new ArrayList<>();
    private final List<Location> platforms = new ArrayList<>(); // Added platforms list

    public Team(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void addPlayer(Player player) {
        players.add(player);
    }

    public void removePlayer(Player player) {
        players.remove(player);
    }

    public List<Player> getPlayers() {
        return players;
    }

    public void clear() {
        players.clear();
    }

    // New method to register platforms
    public void registerPlatform(Location platformLocation) {
        platforms.add(platformLocation);
    }

    // You might want additional methods related to platforms, e.g., clearPlatforms

    public List<Location> getPlatforms() {
        return platforms;
    }
}
