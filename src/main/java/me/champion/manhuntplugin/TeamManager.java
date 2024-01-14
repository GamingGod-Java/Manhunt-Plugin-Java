package me.champion.manhuntplugin;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.ChatColor;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class TeamManager implements Listener {
    private final Map<Material, Team> teams = new HashMap<>();
    private final Map<UUID, String> playerTeams = new HashMap<>();
    private final Set<UUID> frozenPlayers = new HashSet<>();
    private final Plugin plugin;
    private final File playerDataFile;
    private final FileConfiguration playerData;
    public Map<UUID, Map<PotionEffect, Integer>> playerPotionEffects = new HashMap<>();
    private final Map<UUID, Integer> originalAirLevels = new HashMap<>();


    public void savePotionEffects(Player player) {
        Map<PotionEffect, Integer> effectsMap = new HashMap<>();

        for (PotionEffect activeEffect : player.getActivePotionEffects()) {
            effectsMap.put(activeEffect, activeEffect.getDuration());
        } // save effects and durations of individual player to hashmap

        playerPotionEffects.put(player.getUniqueId(), effectsMap);
        //save hashmap to hashmap of all players
        System.out.println(playerPotionEffects);
    }

    public void restorePotionEffects(Player player) {
        UUID playerUUID = player.getUniqueId();

        if (playerPotionEffects.containsKey(playerUUID)) {
            // Clear existing potion effects
            player.getActivePotionEffects().clear();

            // Restore saved potion effects
            for (Map.Entry<PotionEffect, Integer> entry : playerPotionEffects.get(playerUUID).entrySet()) {
                player.addPotionEffect(new PotionEffect(entry.getKey().getType(), entry.getValue(), entry.getKey().getAmplifier()));
            }
        }
    }

    private void saveOriginalAirLevels() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            originalAirLevels.put(player.getUniqueId(), player.getRemainingAir());
        }
    }

    private void restoreOriginalAirLevels() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID playerId = player.getUniqueId();
            if (originalAirLevels.containsKey(playerId)) {
                int originalAir = originalAirLevels.get(playerId);
                player.setRemainingAir(originalAir);
            }
        }
        originalAirLevels.clear();
    }

    public TeamManager(Plugin plugin) {
        teams.put(Material.BLUE_WOOL, new Team("Runners"));
        teams.put(Material.RED_WOOL, new Team("Zombies"));

        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
        this.plugin = plugin;

        playerDataFile = new File(plugin.getDataFolder(), "playerdata.yml");
        playerData = YamlConfiguration.loadConfiguration(playerDataFile);
    }

    public boolean isGamePaused() {
        return !frozenPlayers.isEmpty();
    }

    public void clearTeams() {
        teams.values().forEach(Team::clear);
    }

    public void addToTeam(Player player, String team) {
        System.out.println("adding"+player.getName()+" to "+team);
        UUID playerUUID = player.getUniqueId();
        String currentTeam = playerTeams.get(playerUUID);

        if (currentTeam != null && currentTeam.equalsIgnoreCase(team)) {
            if (team.equalsIgnoreCase("Zombies")) {
                player.setDisplayName("§c" + player.getName());
                player.setPlayerListName("§c" + player.getName());
                //sendTitle(player, ChatColor.RED + "You have joined the Zombies team");
            } else if (team.equalsIgnoreCase("Runners")) {
                player.setDisplayName("§9" + player.getName());
                player.setPlayerListName("§9" + player.getName());
                //sendTitle(player, ChatColor.BLUE + "You have joined the Runners team");
            }
            return; // Player is already on the team, no need to add them again
        }

        playerTeams.put(playerUUID, team);

        // Update their display name and nametag based on the team
        if (team.equalsIgnoreCase("Zombies")) {
            player.setDisplayName("§c" + player.getName());
            player.setPlayerListName("§c" + player.getName());
            sendTitle(player, ChatColor.RED + "You have joined the Zombies team");
        } else if (team.equalsIgnoreCase("Runners")) {
            player.setDisplayName("§9" + player.getName());
            player.setPlayerListName("§9" + player.getName());
            sendTitle(player, ChatColor.BLUE + "You have joined the Runners team");
        }

        // You can add additional logic here if needed
    }
    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();
        String name = player.getDisplayName(); // This retains the colored name

        // Format the chat message so that the player's name is colored but the message is default
        event.setFormat(name + ChatColor.WHITE + ": " + message);
    }

    private void sendTitle(Player player, String message) {
        final int fadeInTime = 10; // Time in ticks for the title to fade in
        final int stayTime = 40;   // Time in ticks for the title to stay on the screen
        final int fadeOutTime = 10; // Time in ticks for the title to fade out

        new BukkitRunnable() {
            @Override
            public void run() {
                player.sendTitle("", message, fadeInTime, stayTime, fadeOutTime);
            }
        }.runTaskLater(plugin, 1); // Send the title with a slight delay to ensure it displays correctly
    }

    public void removeFromTeam(Player player) {
        playerTeams.remove(player.getUniqueId());
        player.setDisplayName(player.getName()); // Resetting the display name to default
        player.setPlayerListName(player.getName()); // Resetting the list name to default
    }

    public void registerPlatform(String teamName, Location platformLocation) {
        Team team = getTeamByName(teamName);
        if (team != null) {
            team.registerPlatform(platformLocation);
        }
    }

    public boolean isOnTeam(Player player, String teamName) {
        String playerTeam = playerTeams.get(player.getUniqueId());
        return playerTeam != null && playerTeam.equalsIgnoreCase(teamName);
    }

    public void pauseGame(Player pausingPlayer) {
        if (!isGamePaused()) {
            setGamePaused(true);

            for (Player player : Bukkit.getOnlinePlayers()) {
                frozenPlayers.add(player.getUniqueId());
                player.sendMessage("Game paused by " + pausingPlayer.getName() + "!");

                //Invulnerability logic
                player.setInvulnerable(true);

                //Potion saving logic
                savePotionEffects(player);

                //air buble saving logic apparnetly
                saveOriginalAirLevels();
            }
        }
    }

    public void unpauseGame(Player unpausingPlayer) {
        if (isGamePaused()) {
            setGamePaused(false);

            for (Player player : Bukkit.getOnlinePlayers()) {
                frozenPlayers.remove(player.getUniqueId());
                player.sendMessage("Game unpaused by " + unpausingPlayer.getName() + "!");

                //Invulnerability logic
                player.setInvulnerable(false);

                //Potion restore logic
                System.out.println("teammanager restored potion effect");
                restorePotionEffects(player);
                restoreOriginalAirLevels();

            }
        }
    }

    public void setGamePaused(boolean paused) {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (paused) {
                onlinePlayer.sendMessage("");
            } else {
                onlinePlayer.sendMessage("");
            }
        }
    }

    private Team getTeamByName(String teamName) {
        for (Team team : teams.values()) {
            if (team.getName().equalsIgnoreCase(teamName)) {
                return team;
            }
        }
        return null;
    }
    public void pauseZombies() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isOnTeam(player, "Zombies")) {
                frozenPlayers.add(player.getUniqueId());
            }
        }
    }

    public void unpauseZombies() {
        frozenPlayers.clear();
        // Broadcast message or perform other actions when zombies are unpaused
    }
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        String team = playerTeams.get(player.getUniqueId());

        if (team != null && team.equalsIgnoreCase("Runners")) {
            addToTeam(player, "Zombies");
            player.sendMessage("Better luck next time! You've become a zombie!");
            pauseGame(player);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (isGamePaused() && frozenPlayers.contains(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }
    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        String team = playerTeams.get(player.getUniqueId());

        if (team != null && team.equalsIgnoreCase("Zombies")) {
            System.out.println("Gave "+player.getName()+"compass");
            ItemStack compass = new ItemStack(Material.COMPASS);

            compass.addUnsafeEnchantment(Enchantment.ARROW_INFINITE, 1);


            // Set unstackable flag
            ItemMeta compassMeta = compass.getItemMeta();
            compassMeta.setUnbreakable(true);
            compass.setItemMeta(compassMeta);

            // Add custom lore
            List<String> lore = new ArrayList<>();
            lore.add("This enchanted compass points to the nearest runner.");
            lore.add("Use it wisely!");
            compassMeta.setLore(lore);

            compassMeta.setDisplayName("§cTrack Runners");
            compass.setItemMeta(compassMeta);
            player.getInventory().addItem(compass);
            player.sendMessage("You have been given a compass to track runners.");
        }
    }

    public Player findNearestRunner(Location zombieLocation) {
        Player nearestRunner = null;
        double minDistance = Double.MAX_VALUE;

        for (Player runner : Bukkit.getOnlinePlayers()) {
            if (isOnTeam(runner, "Runners")) {
                double distance = zombieLocation.distance(runner.getLocation());
                if (distance < minDistance) {
                    minDistance = distance;
                    nearestRunner = runner;
                }
            }
        }

        return nearestRunner;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        if (playerData.contains(playerUUID.toString())) {

            String team = playerData.getString(playerUUID.toString());
            System.out.println("Added "+ playerUUID +"to" +team);
            addToTeam(player, team);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        if (playerTeams.containsKey(playerUUID)) {
            String team = playerTeams.get(playerUUID);
            playerData.set(playerUUID.toString(), team);

            try {
                playerData.save(playerDataFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        if (isGamePaused() && frozenPlayers.contains(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }
}
