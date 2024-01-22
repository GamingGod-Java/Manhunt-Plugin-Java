package me.champion.manhuntplugin;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.OfflinePlayer;

import java.util.*;

public class MhWheel implements CommandExecutor, Listener {

    private final JavaPlugin plugin;
    private final TeamManager teamManager;
    private final String zombiesTeamName = "Zombies";
    private final String runnersTeamName = "Runners";
    private final String[] buffNames = {"Speed 1 inf", "Resistance 1 inf", "Resistance 2 inf", "Strength 1 inf", "FireResistance 1 inf", "Give totem", "Give diamond pants", "Give god apple", "LAST MAN STANDING"};
    private final String[] debuffNames = {"Slowness 1 20", "Weakness 1 20", "Clear Player"};
    private String selectedTeam = "";
    private String selectedBuffDebuff = "";

    private final Map<UUID, Long> clickCooldowns = new HashMap<>();
    private static final long COOLDOWN_TIME_MS = 10;

    private final Map<UUID, Map<PotionEffectType, Integer>> playerDebuffs = new HashMap<>();


    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        // Save debuffs before death
        //saveDebuffs(player);

        // Remove all potion effects on death (optional)
        //clearEffects(player);
    }

    // Method to save the debuffs of a player


    // Method to clear all potion effects from a player
    private void clearEffects(Player player) {
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
    }

    // ... (rest of the class)

    // Method to restore debuffs on respawn
    private void restoreDebuffs(Player player) {
        Map<PotionEffectType, Integer> debuffs = playerDebuffs.get(player.getUniqueId());
        if (debuffs != null) {
            for (Map.Entry<PotionEffectType, Integer> entry : debuffs.entrySet()) {
                player.addPotionEffect(new PotionEffect(entry.getKey(), Integer.MAX_VALUE, entry.getValue()));
            }
        }
    }

    public MhWheel(JavaPlugin plugin, TeamManager teamManager) {
        this.plugin = plugin;
        this.teamManager = teamManager;
    }



    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.isOp()) {
            player.sendMessage("You need to be an operator (op) to use this command.");
            return true;
        }

        openMainMenu(player);
        return true;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        long currentTime = System.currentTimeMillis();

        // Check if the player is in the cooldown period
        if (clickCooldowns.containsKey(player.getUniqueId()) && currentTime - clickCooldowns.get(player.getUniqueId()) < COOLDOWN_TIME_MS) {
            return;
        }

        // Set the cooldown for the player
        clickCooldowns.put(player.getUniqueId(), currentTime);


        Inventory clickedInventory = event.getClickedInventory();

        if (clickedInventory != null) {
            String title = event.getView().getTitle();
            int slot = event.getSlot();

            if (title.equals("MhWheel Menu")) {
                event.setCancelled(true);

                if (slot == 2) {
                    selectedTeam = zombiesTeamName;
                    openBuffsDebuffsMenu(player);
                } else if (slot == 6) {
                    selectedTeam = runnersTeamName;
                    openBuffsDebuffsMenu(player);
                }
            } else if (title.equals("Buffs/Debuffs Menu")) {
                event.setCancelled(true);

                if (slot >= 0 && slot <= 8) {
                    selectedBuffDebuff = buffNames[slot];
                    openTeamMembersGUI(player, selectedTeam);
                } else if (slot >= 10 && slot <= 13) {
                    selectedBuffDebuff = debuffNames[slot - 11];
                    openTeamMembersGUI(player, selectedTeam);
                } else if (slot == 9) {
                    openMainMenu(player);
                }
            } else if (title.startsWith(selectedTeam + " Team Members")) {
                event.setCancelled(true);

                if (slot == event.getInventory().getSize() - 1) {
                    openBuffsDebuffsMenu(player);
                } else {
                    if (slot < getPlayersOnTeam(selectedTeam).size()) {
                        applyBuffDebuff(player, slot);
                    }

                }
            }
        }
    }

    public void openMainMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(player, 9, "MhWheel Menu");

        ItemStack redConcrete = createTeamItem(zombiesTeamName, Material.RED_CONCRETE);
        ItemStack blueConcrete = createTeamItem(runnersTeamName, Material.LIGHT_BLUE_CONCRETE);

        inventory.setItem(2, redConcrete);
        inventory.setItem(6, blueConcrete);

        player.openInventory(inventory);
    }
    //private final String[] buffNames = {"Speed 1 inf", "Resistance 1 inf", "Resistance 2 inf", "Strength 1 inf", "FireResistance 1 inf", "Give totem", "Give diamond pants", "Give god apple"};
    //private final String[] debuffNames = {"Slowness 1 20", "Weakness 1 20", "Clear Player"};
    public void openBuffsDebuffsMenu(Player player) {

        List<Material> buffMaterials = Arrays.asList(Material.DIAMOND_BOOTS, Material.SHIELD, Material.SHIELD, Material.DIAMOND_SWORD, Material.LAVA_BUCKET, Material.TOTEM_OF_UNDYING, Material.DIAMOND_LEGGINGS, Material.ENCHANTED_GOLDEN_APPLE, Material.BEACON);
        List<Material> debuffMaterials = Arrays.asList(Material.LEATHER_BOOTS, Material.WOODEN_SWORD, Material.BARRIER);
        Inventory menu = Bukkit.createInventory(player, 27, "Buffs/Debuffs Menu");
        for (String buff: buffNames) {
            System.out.println(buff);
        }
        for (int i = 0; i < buffNames.length; i++) {
            ItemStack buffsItem = createMenuItem("Buff: " + buffNames[i], buffMaterials.get(i));
            if (i == 2) {
                buffsItem.setAmount(2);
            } else {
                buffsItem.setAmount(1);
            }
            menu.setItem(i, buffsItem);
        }

        for (int i = 0; i < debuffNames.length; i++) {
            ItemStack debuffsItem = createMenuItem("Debuff: " + debuffNames[i], debuffMaterials.get(i));
            menu.setItem(11 + i, debuffsItem);
        }

        ItemStack backButton = createMenuItem("Back", Material.ARROW);
        menu.setItem(9, backButton);

        player.openInventory(menu);
    }

    public void openTeamMembersGUI(Player player, String teamName) {
        List<Player> teamPlayers = getPlayersOnTeam(teamName);

        if (teamPlayers.isEmpty()) {
            player.sendMessage("There are no players on the " + teamName + " team.");
            return;
        }

        int inventorySize = (int) Math.ceil((double) teamPlayers.size() / 9) * 9;
        Inventory teamMembersGUI = Bukkit.createInventory(player, inventorySize, teamName + " Team Members");

        for (Player teamPlayer : teamPlayers) {
            ItemStack playerHead = createPlayerHeadItem(teamPlayer);
            teamMembersGUI.addItem(playerHead);
        }

        ItemStack backButton = createMenuItem("Back", Material.ARROW);
        teamMembersGUI.setItem(inventorySize - 1, backButton);

        player.openInventory(teamMembersGUI);
    }



    public void applyBuffDebuff(Player player, int slot) {
        System.out.println("applying buff");
        /*if (slot < 0 || slot >= player.getOpenInventory().getTopInventory().getSize()) {
            return; // Invalid slot, return early
        }*/

        Player targetPlayer = getSelectedPlayer(player, slot);

        if (targetPlayer == null) {
            player.sendMessage("Invalid target player.");
            return;
        }
        String[] parts = selectedBuffDebuff.split(" ");

        String effectName = parts[0];
        
        if (effectName.equalsIgnoreCase("last")) {
            targetPlayer.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 0));
            targetPlayer.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, Integer.MAX_VALUE, 0));
            targetPlayer.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, Integer.MAX_VALUE, 0));
            
        }
        if (effectName.equalsIgnoreCase("clear")) {
            //clearEffects(targetPlayer);
            player.getInventory().clear();
            player.sendMessage("Cleared inventory from " + targetPlayer.getName() + ".");
            return;
        }

        if (effectName.equalsIgnoreCase("give")) {
            if (isInventoryFull(player)) {
                targetPlayer.sendMessage("§cYour inventory is too full to receive an item!");
                player.sendMessage("§c" + player.getName()+" could not receive item as their inventory was full");
                return;
            }
            ItemStack itemtogive = null;
            if (parts[1].equalsIgnoreCase("totem")) {
                itemtogive = new ItemStack(Material.TOTEM_OF_UNDYING, 1);
                targetPlayer.sendMessage("You've received a totem!");
                player.sendMessage(player.getName()+" receieved totem");

            } if (parts[1].equalsIgnoreCase("diamond")) {
                itemtogive = new ItemStack(Material.DIAMOND_LEGGINGS, 1);
                targetPlayer.sendMessage("You've received diamond leggings!");
                player.sendMessage(player.getName()+" receieved diamonds leggings");
            }  if (parts[1].equalsIgnoreCase("god")) {
            itemtogive = new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 1);
            targetPlayer.sendMessage("You've received a god apple!");
            player.sendMessage(player.getName()+" receieved god apple");
        }
            if (itemtogive != null) {
                targetPlayer.getInventory().addItem(itemtogive);
            }
            return;
        }


        if (parts.length < 3) {
            System.out.println(("Invalid effect format: " + selectedBuffDebuff));
            return;
        }






        int amplifier;

        try {
            amplifier = Integer.parseInt(parts[1])-1;
        } catch (NumberFormatException e) {
            player.sendMessage("Invalid amplifier format: " + parts[1]);
            return;
        }

        PotionEffectType effectType = getEffectType(effectName);

        if (effectType != null) {
            String durationString = parts[2];
            int duration;
            if ("inf".equalsIgnoreCase(durationString)) {
                duration = Integer.MAX_VALUE;
            } else {
                try {
                    duration = Integer.parseInt(durationString)*20*60;
                } catch (NumberFormatException e) {
                    player.sendMessage("Invalid duration format: " + durationString);
                    return;
                }
            }


            targetPlayer.addPotionEffect(new PotionEffect(effectType, duration, amplifier));
            targetPlayer.sendMessage("You got "+effectName);

            // Crafting the broadcast message
            String teamColor = selectedTeam.equals(zombiesTeamName) ? "§c" : "§b"; // Example: Red for Zombies, Blue for Runners
            String message = teamColor + targetPlayer.getName() + "§f has received " + selectedBuffDebuff;
            Bukkit.broadcastMessage(message);
        } else {
            player.sendMessage("Unknown effect: " + effectName);
        }
    }

    public Player getSelectedPlayer(Player player, int slot) {
        ItemStack clickedItem = player.getOpenInventory().getTopInventory().getItem(slot);
        if (clickedItem != null && clickedItem.getType() == Material.PLAYER_HEAD) {
            SkullMeta meta = (SkullMeta) clickedItem.getItemMeta();

            if (meta != null && meta.getOwningPlayer() != null) {
                OfflinePlayer offlinePlayer = meta.getOwningPlayer();
                if (offlinePlayer.isOnline()) {
                    return offlinePlayer.getPlayer();
                } else {
                    player.sendMessage(offlinePlayer.getName() + " is not currently online.");
                }
            }
        }
        return null;
    }


    public PotionEffectType getEffectType(String effectName) {
        switch (effectName) {
            case "Speed":
                return PotionEffectType.SPEED;
            case "Resistance":
                return PotionEffectType.DAMAGE_RESISTANCE; // Change this to the appropriate Resistance effect*/
            case "FireResistance":
                return PotionEffectType.FIRE_RESISTANCE;
            case "Strength":
                return PotionEffectType.INCREASE_DAMAGE;
            case "Jump Boost":
                return PotionEffectType.JUMP;
            case "Slowness":
                return PotionEffectType.SLOW;
            case "Weakness":
                return PotionEffectType.WEAKNESS;
            default:
                return null;
        }
    }

    public List<Player> getPlayersOnTeam(String teamName) {
        List<Player> teamPlayers = new ArrayList<>();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (teamManager.isOnTeam(player, teamName)) {
                teamPlayers.add(player);
            }
        }

        return teamPlayers;
    }

    public ItemStack createMenuItem(String name, Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            meta = Bukkit.getItemFactory().getItemMeta(material);
        }

        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }

        return item;
    }

    public ItemStack createTeamItem(String teamName, Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            meta = Bukkit.getItemFactory().getItemMeta(material);
        }

        if (meta != null) {
            meta.setDisplayName(teamName);
            item.setItemMeta(meta);
        }

        return item;
    }

    public ItemStack createPlayerHeadItem(Player player) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();

        if (meta != null) {
            meta.setOwningPlayer(player);
            meta.setDisplayName(player.getName());
            item.setItemMeta(meta);
        }

        return item;
    }

    public boolean isInventoryFull(Player player) {
        int emptySlots = 0;

        // Get the player's inventory
        Inventory playerInventory = player.getInventory();

        // Count the number of empty slots in the inventory
        for (int slot = 0; slot < playerInventory.getSize() - 5; slot++) {
            ItemStack item = playerInventory.getItem(slot);

            // Check if the slot is empty
            if (item == null || item.getType() == Material.AIR) {
                emptySlots++;
                System.out.println("free inv slot");
            }
        }
        if (emptySlots == 0) {
            System.out.println("no free inv slots");
        }
        // Check if there are no empty slots (inventory is full)
        return emptySlots == 0;
    }
}
