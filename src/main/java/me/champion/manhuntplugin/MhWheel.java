package me.champion.manhuntplugin;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.List;

public class MhWheel implements CommandExecutor, Listener {

    private final JavaPlugin plugin;
    private final TeamManager teamManager;
    private final String zombiesTeamName = "Zombies";
    private final String runnersTeamName = "Runners";
    private final String[] buffNames = {"Speed", "Jump Boost", "Strength"}; // Add more buffs if needed
    private final String[] debuffNames = {"Slowness", "Weakness"}; // Add more debuffs if needed
    private String selectedTeam = "";
    private String selectedBuffDebuff = "";

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

                if (slot >= 2 && slot <= 4) {
                    // Buffs were clicked, open the Buffs list
                    selectedBuffDebuff = buffNames[slot - 2];
                    openTeamMembersGUI(player, selectedTeam);
                } else if (slot >= 6 && slot <= 7) {
                    // Debuffs were clicked, open the Debuffs list
                    selectedBuffDebuff = debuffNames[slot - 6];
                    openTeamMembersGUI(player, selectedTeam);
                } else if (slot == 9) {
                    // Go back to the Main Menu
                    openMainMenu(player);
                }
            } else if (title.startsWith(selectedTeam + " Team Members")) {
                event.setCancelled(true);

                if (slot == event.getInventory().getSize() - 1) {
                    // Back button was clicked, go back to Buffs/Debuffs Menu
                    openBuffsDebuffsMenu(player);
                } else {
                    // Apply the selected buff or debuff to the player
                    applyBuffDebuff(player, slot);
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

    public void openBuffsDebuffsMenu(Player player) {
        Inventory menu = Bukkit.createInventory(player, 27, "Buffs/Debuffs Menu");

        for (int i = 0; i < buffNames.length; i++) {
            ItemStack buffsItem = createMenuItem("Buff: " + buffNames[i], Material.DIAMOND_SWORD);
            menu.setItem(2 + i, buffsItem);
        }

        for (int i = 0; i < debuffNames.length; i++) {
            ItemStack debuffsItem = createMenuItem("Debuff: " + debuffNames[i], Material.IRON_SWORD);
            menu.setItem(6 + i, debuffsItem);
        }

        ItemStack backButton = createMenuItem("Back", Material.ARROW);
        menu.setItem(9, backButton);

        player.openInventory(menu);
    }

    public void openTeamMembersGUI(Player player, String teamName) {
        List<Player> teamPlayers = getPlayersOnTeam(teamName);

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
        String message = "You have applied " + selectedBuffDebuff + " to " + player.getName();
        ItemStack buffItem = null;

        if (selectedBuffDebuff.startsWith("Buff:")) {
            int buffIndex = slot - 2;
            if (buffIndex >= 0 && buffIndex < buffNames.length) {
                String buffName = buffNames[buffIndex];
                message += " with " + buffName;
                buffItem = createBuffItem(buffName);
                // Apply the buff here
                applyPotionEffect(player, buffName);
            }
        } else if (selectedBuffDebuff.startsWith("Debuff:")) {
            int debuffIndex = slot - 6;
            if (debuffIndex >= 0 && debuffIndex < debuffNames.length) {
                String debuffName = debuffNames[debuffIndex];
                message += " with " + debuffName;
                buffItem = createDebuffItem(debuffName);
                // Apply the debuff here
                applyPotionEffect(player, debuffName);
            }
        }

        player.sendMessage(message);

        if (buffItem != null) {
            player.getInventory().removeItem(buffItem);
        }

        player.sendMessage("You have applied " + selectedBuffDebuff + " to " + player.getName() + ".");
        openTeamMembersGUI(player, selectedTeam);
    }

    // Method to apply a PotionEffect to a player
    public void applyPotionEffect(Player player, String effectName) {
        PotionEffectType effectType = null;
        int duration = 1200; // 1200 ticks = 60 seconds = 2 minutes (adjust as needed)

        if (effectName.equals("Speed")) {
            effectType = PotionEffectType.SPEED;
        } else if (effectName.equals("Jump Boost")) {
            effectType = PotionEffectType.JUMP;
        } else if (effectName.equals("Strength")) {
            effectType = PotionEffectType.INCREASE_DAMAGE;
        } else if (effectName.equals("Slowness")) {
            effectType = PotionEffectType.SLOW;
        } else if (effectName.equals("Weakness")) {
            effectType = PotionEffectType.WEAKNESS;
        }

        if (effectType != null) {
            player.addPotionEffect(new PotionEffect(effectType, duration, 1));
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
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createTeamItem(String teamName, Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(teamName);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createPlayerHeadItem(Player player) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwningPlayer(player);
        meta.setDisplayName(player.getName());
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createBuffItem(String buffName) {
        // Implement how you want to create a buff item
        // For example, you can create custom items or use specific materials
        return createMenuItem("Buff: " + buffName, Material.DIAMOND_SWORD);
    }

    public ItemStack createDebuffItem(String debuffName) {
        // Implement how you want to create a debuff item
        // For example, you can create custom items or use specific materials
        return createMenuItem("Debuff: " + debuffName, Material.IRON_SWORD);
    }
}
