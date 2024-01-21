package me.champion.manhuntplugin;

import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import java.util.stream.Collectors;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class TeamManager implements Listener {
    private final Map<Material, Team> teams = new HashMap<>();

    public boolean GameOver = false;
    public final Map<UUID, String> playerTeams = new HashMap<>();
    private final Set<UUID> frozenPlayers = new HashSet<>();
    private final Plugin plugin;
    private final File playerDataFile;
    private final FileConfiguration playerData;
    public Map<UUID, Map<PotionEffect, Integer>> playerPotionEffects = new HashMap<>();
    private final Map<UUID, Integer> originalAirLevels = new HashMap<>();
    private final Map<UUID, Integer> savedFireTicks = new HashMap<>();
    private final Map<UUID, BoatData> savedBoats = new HashMap<>();

    private BukkitTask potionEffectTask;



    private File statisticsFile;
    private FileConfiguration statisticsConfig;

    public void startPotionEffectLoop() {
        potionEffectTask = new BukkitRunnable() {
            @Override
            public void run() {
                applyPotionEffectsDuringPause();
            }
        }.runTaskTimer(plugin, 0, 20); // The second parameter (delay) is in ticks, so 20 ticks = 1 second
    }

    public void stopPotionEffectLoop() {
        if (potionEffectTask != null) {
            potionEffectTask.cancel();
            potionEffectTask = null;
        }
    }

    private void applyPotionEffectsDuringPause() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isGamePaused()) {
                restorePotionEffects(player);
            }
        }
    }

    private static class BoatData {
        private final Vehicle boat;
        private final List<UUID> passengers;

        public BoatData(Vehicle boat) {
            this.boat = boat;
            this.passengers = boat.getPassengers().stream()
                    .filter(e -> e instanceof Player)
                    .map(e -> e.getUniqueId())
                    .collect(Collectors.toList());
        }

        public Vehicle getBoat() {
            return boat;
        }

        public List<UUID> getPassengers() {
            return passengers;
        }
    }

    public void saveFireTicks(Player player) {
        savedFireTicks.put(player.getUniqueId(), player.getFireTicks());
    }

    // Restore the player's fire ticks to the saved value
    public void restoreFireTicks(Player player) {
        UUID playerUUID = player.getUniqueId();
        if (savedFireTicks.containsKey(playerUUID)) {
            int savedTicks = savedFireTicks.get(playerUUID);
            player.setFireTicks(savedTicks);
            savedFireTicks.remove(playerUUID);
        }
    }

    public List<Player> getRunners() {
        List<Player> runners = new ArrayList<>();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isOnTeam(player, "Runners")) {
                runners.add(player);
            }
        }
        //System.out.println("runners: "+runners);
        return runners;
    }


    public void savePotionEffects(Player player) {
        Map<PotionEffect, Integer> effectsMap = new HashMap<>();

        for (PotionEffect activeEffect : player.getActivePotionEffects()) {
            effectsMap.put(activeEffect, activeEffect.getDuration());
        } // save effects and durations of individual player to hashmap

        playerPotionEffects.put(player.getUniqueId(), effectsMap);
        //save hashmap to hashmap of all players
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

    public void restoreDebuffEffects(Player player) {
        UUID playerUUID = player.getUniqueId();
        System.out.println("restoring debuffs");
        if (playerPotionEffects.containsKey(playerUUID)) {
            // Clear existing potion effects
            player.getActivePotionEffects().clear();

            // Restore saved potion effects
            for (PotionEffect savedEffect : playerPotionEffects.get(playerUUID).keySet()) {
                if (savedEffect.getType() == PotionEffectType.WEAKNESS || savedEffect.getType() == PotionEffectType.SLOW) {
                    int amplifier = playerPotionEffects.get(playerUUID).get(savedEffect);
                    player.addPotionEffect(new PotionEffect(savedEffect.getType(), Integer.MAX_VALUE, amplifier));
                    System.out.println("applied "+savedEffect.toString());
                }
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



        statisticsFile = new File(plugin.getDataFolder(), "statistics.yml");
        statisticsConfig = YamlConfiguration.loadConfiguration(statisticsFile);

        if (!statisticsFile.exists()) {
            try {
                statisticsConfig.save(statisticsFile);
                System.out.println("Created statistics.yml");
            } catch (IOException e) {
                System.out.println("FAILED to create statistics.yml");
            }
        }

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
        System.out.println("adding " + player.getName() + " to " + team);
        UUID playerUUID = player.getUniqueId();
        String currentTeam = playerTeams.get(playerUUID);

        // If the player is already on the team, no need to add them again
        if (currentTeam != null && currentTeam.equalsIgnoreCase(team)) {
            return;
        }

        playerTeams.put(playerUUID, team);

        // Update their display name and nametag based on the team
        if (team.equalsIgnoreCase("Zombies")) {
            player.setDisplayName("§c" + player.getName()); // Red "Zombie" prefix
            player.setPlayerListName("§cZ " + player.getName()); // Red "Zombie" prefix
            sendTitle(player, ChatColor.RED + "You have joined the Zombies team");
        } else if (team.equalsIgnoreCase("Runners")) {
            player.setDisplayName("§b" + player.getName()); // Aqua "Runner" prefix
            player.setPlayerListName("§bR " + player.getName()); // Aqua "Runner" prefix
            sendTitle(player, ChatColor.AQUA + "You have joined the Runners team");
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();
        String name = player.getDisplayName(); // This retains the colored name

        message = message.replace("%", "%%"); //jank fix but it was all chatgpt had

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

                // Set the player to Adventure mode
                player.setGameMode(GameMode.ADVENTURE);

                // Set the walk speed to 0 - this makes the player unable to walk
                player.setWalkSpeed(0.0f);

                player.setInvulnerable(true);

                //player.sendMessage("Game paused by " + pausingPlayer.getName() + "!");

                // Invulnerability logic
                player.setInvulnerable(true);

                // Potion saving logic
                savePotionEffects(player);

                // Save air bubble progress
                saveOriginalAirLevels();

                // Save fire ticks
                saveFireTicks(player);

                //Start reapplying potion effects
                startPotionEffectLoop();
                // Execute /tick freeze command
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "tick freeze");
                if (player.getVehicle() instanceof Vehicle) {
                    Vehicle boat = (Vehicle) player.getVehicle();
                    savedBoats.put(boat.getUniqueId(), new BoatData(boat)); // Save the boat (vehicle) with passengers
                    boat.eject(); // Eject all passengers
                    player.sendMessage(ChatColor.DARK_PURPLE + "You were in a vehicle and will be remounted when the game resumes.");
                }
            }
        }
    }


    public void unpauseGame(Player unpausingPlayer) {
        if (isGamePaused()) {
            setGamePaused(false);

            for (Player player : Bukkit.getOnlinePlayers()) {
                frozenPlayers.remove(player.getUniqueId());
                /*if (unpausingPlayer != null) {
                    player.sendMessage("Game unpaused by " + unpausingPlayer.getName() + "!");
                } else {
                    player.sendMessage("Game unpaused by server!");
                }*/

                // Invulnerability, potion, air levels, and fire ticks logic...
                restorePotionEffects(player);
                restoreOriginalAirLevels();
                restoreFireTicks(player);
                player.setInvulnerable(false);

                // Reset the player's walk speed to the default Minecraft value (0.2)
                player.setWalkSpeed(0.2f);


                // Switch the player's game mode back to Survival
                player.setGameMode(GameMode.SURVIVAL);

                //Does this fix it?
                player.setInvulnerable(false);

                // Execute /tick unfreeze command

            }
            //Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "tick unfreeze");
            //Stop reapplying potion effects
            stopPotionEffectLoop();

            // Restoring boats and their passengers
            for (BoatData boatData : savedBoats.values()) {
                Vehicle boat = boatData.getBoat();
                List<UUID> passengers = boatData.getPassengers();

                if (!passengers.isEmpty()) {
                    Player firstPassenger = Bukkit.getPlayer(passengers.get(0));
                    if (firstPassenger != null) {
                        boat.teleport(firstPassenger.getLocation());
                        for (UUID passengerId : passengers) {
                            Player passenger = Bukkit.getPlayer(passengerId);
                            if (passenger != null && passenger.isOnline()) {
                                boat.addPassenger(passenger);
                            }
                        }
                    }
                }
            }

            savedBoats.clear(); // Clear the saved boats map after restoring them
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
                player.setInvulnerable(true);
                frozenPlayers.add(player.getUniqueId());
            }
        }
    }

    public void unpauseZombies() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isOnTeam(player, "Zombies")) {
                player.setInvulnerable(false);
                frozenPlayers.clear();
            }

            // Broadcast message or perform other actions when zombies are unpaused
        }
    }



    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!GameOver) {
            savePotionEffects(event.getEntity());
            Player player = event.getEntity();
            EntityDamageEvent lastDamageCause = player.getLastDamageCause();

            UUID playerUUID = player.getUniqueId(); // Declare playerUUID here
            String team = playerTeams.get(playerUUID);


            if (team != null && team.equalsIgnoreCase("Runners")) {

                // Check if the player was killed by another entity
                if (lastDamageCause instanceof EntityDamageByEntityEvent) {
                    EntityDamageByEntityEvent damageByEntityEvent = (EntityDamageByEntityEvent) lastDamageCause;

                    // Check if the killer is a player
                    if (damageByEntityEvent.getDamager() instanceof Player) {
                        Player killer = (Player) damageByEntityEvent.getDamager();

                        // Custom logic for when a player is killed by another player
                        event.setDeathMessage("§b" + player.getName() + "§f has been infected by §c" + killer.getName());
                        updatePlayerStatistics(player.getName(), "player_deaths");
                        updatePlayerStatistics(killer.getName(), "player_kills");

                    } else {
                        event.setDeathMessage("§b" + player.getName() + " §fhas been infected by the environment");
                        updatePlayerStatistics(player.getName(), "environment_deaths");
                        updatePlayerStatistics(damageByEntityEvent.getDamager().getName(), "player_kills");
                    }
                } else {
                    event.setDeathMessage("§b" + player.getName() + " §fhas been infected by the environment");
                    updatePlayerStatistics(player.getName(), "environment_deaths");
                }
                addToTeam(player, "Zombies");
                pauseGame(player);
            } else {
                event.setDeathMessage("§c" + event.getEntity() + " §f" +event.getDeathMessage());
            }


            // Remove the player from the saved boat data if they die while in a boat
            for (BoatData boatData : savedBoats.values()) {
                boatData.getPassengers().remove(playerUUID); // playerUUID is in scope here
            }
        }
    }

    private void updatePlayerStatistics(String player, String statistic) {

        int currentStatistic = statisticsConfig.getInt(player + "." + statistic, 0);
        statisticsConfig.set(player + "." + statistic, currentStatistic + 1);

        try {
            statisticsConfig.save(statisticsFile);
        } catch (IOException e) {
            System.out.println("FAILED to save statistics.yml");
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
        restoreDebuffEffects(player);


        if (team != null && team.equalsIgnoreCase("Zombies")) {
            System.out.println("Gave " + player.getName() + " compass and tools");

            // Give 1 stone axe
            ItemStack stoneAxe = new ItemStack(Material.STONE_AXE, 1);
            player.getInventory().addItem(stoneAxe);

            // Give 1 stone pickaxe
            ItemStack stonePickaxe = new ItemStack(Material.STONE_PICKAXE, 1);
            player.getInventory().addItem(stonePickaxe);

            // Give 20 bread
            ItemStack bread = new ItemStack(Material.BREAD, 20);
            player.getInventory().addItem(bread);

            // Create and give the compass
            ItemStack compass = new ItemStack(Material.COMPASS);
            compass.addUnsafeEnchantment(Enchantment.ARROW_INFINITE, 1);
            ItemMeta compassMeta = compass.getItemMeta();
            if (compassMeta != null) {
                compassMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                compassMeta.setDisplayName("§cTrack Runners");
                compass.setItemMeta(compassMeta);
            }
            player.getInventory().addItem(compass);
        }
    }
    public Player findNearestRunner(Location zombieLocation) {
        Player nearestRunner = null;
        double minDistance = Double.MAX_VALUE;

        for (Player runner : Bukkit.getOnlinePlayers()) {
            // Check if the player is a runner and in the same world as the zombie
            if (isOnTeam(runner, "Runners") && runner.getWorld().equals(zombieLocation.getWorld())) {
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
        if (isGamePaused()) {
            player.setWalkSpeed(0);
        }
        if (!isGamePaused()) {
            player.setWalkSpeed(0.2f);
        }

        if (playerData.contains(playerUUID.toString())) {
            String team = playerData.getString(playerUUID.toString());
            System.out.println("Added " + playerUUID + " to " + team);

            // Check if 'team' is not null before performing operations
            if (team != null) {
                addToTeam(player, team);
                // Reapply display name and prefix
                updatePlayerDisplayName(player, team);
            } else {
                // Handle the case where 'team' is null (e.g., provide a default behavior or log a message)
                player.sendMessage("No team found");
            }
        }
    }
    private void updatePlayerDisplayName(Player player, String team) {
        if (team.equalsIgnoreCase("Zombies")) {
            player.setDisplayName("§c" + player.getName());
            player.setPlayerListName("§cZ " + player.getName());
        } else if (team.equalsIgnoreCase("Runners")) {
            player.setDisplayName("§b" + player.getName());
            player.setPlayerListName("§bR " + player.getName());
        } else {
            // Default display name if the team is not recognized
            player.setDisplayName(player.getName());
            player.setPlayerListName(player.getName());
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
    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (isGamePaused() && event.getEntity() instanceof Vehicle) {
            // Cancel the event if the game is paused and a boat is being damaged
            event.setCancelled(true);
        }
    }
    @EventHandler
    public void onVehicleDamage(VehicleDamageEvent event) {
        if (isGamePaused()) {
            event.setCancelled(true);
        }
    }
    @EventHandler
    public void onVehicleEnter(VehicleEnterEvent event) {
        if (isGamePaused() && event.getVehicle() instanceof Vehicle) {
            // If the game is paused and a player tries to enter a boat, cancel the event
            event.setCancelled(true);
            if (event.getEntered() instanceof Player) {
                Player player = (Player) event.getEntered();
                player.sendMessage(ChatColor.RED + "Nice try Andre, you can not enter a boat while the game is paused.");

            }
        }
    }
    public List<Player> getPlayersOnTeam(String teamName) {
        List<Player> teamPlayers = new ArrayList<>();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isOnTeam(player, teamName)) {
                teamPlayers.add(player);
            }
        }

        return teamPlayers;
    }
}
