package me.champion.manhuntplugin;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

public class MhWheel implements CommandExecutor, Listener {
    private final Manhunt plugin;
    private final TeamManager teamManager;
    private final Map<UUID, String> selectedTeams = new HashMap<>();
    private final Map<UUID, PotionEffectType> selectedEffects = new HashMap<>();
    private final Set<String> teamMessageSent = new HashSet<>();

    public MhWheel(Manhunt plugin, TeamManager teamManager) {
        this.plugin = plugin;
        this.teamManager = teamManager;
        Bukkit.getPluginManager().registerEvents(this, plugin); // Register events in the constructor
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            openMainGui(player);
        }
        return true;
    }

    private void openMainGui(Player player) {
        Inventory gui = Bukkit.createInventory(null, 9, "Select a Team");
        gui.setItem(3, createItem(Material.RED_CONCRETE, ChatColor.RED + "Zombies"));
        gui.setItem(5, createItem(Material.LIGHT_BLUE_CONCRETE, ChatColor.BLUE + "Runners"));
        player.openInventory(gui);
    }

    private void openBuffsDebuffsGui(Player player, String team) {
        Inventory gui = Bukkit.createInventory(null, 18, "Select Buff/Debuff for " + team);

        // Buffs
        gui.setItem(10, createPotionItem(PotionEffectType.SPEED, "Speed Buff"));
        gui.setItem(11, createPotionItem(PotionEffectType.INCREASE_DAMAGE, "Strength Buff"));

        // Debuffs
        gui.setItem(13, createPotionItem(PotionEffectType.SLOW, "Slowness Debuff"));
        gui.setItem(14, createPotionItem(PotionEffectType.WEAKNESS, "Weakness Debuff"));

        // Show the selected effect if one is already selected
        PotionEffectType selectedEffect = selectedEffects.get(player.getUniqueId());
        if (selectedEffect != null) {
            gui.setItem(16, createPotionItem(selectedEffect, "Selected Effect"));
        }

        gui.setItem(17, createItem(Material.ARROW, ChatColor.GRAY + "Back"));
        player.openInventory(gui);
    }

    private void openPlayerListGui(Player player, String team) {
        Inventory gui = Bukkit.createInventory(null, 54, "Players in " + team);
        List<Player> players = teamManager.getPlayersOnTeam(team);
        for (int i = 0; i < players.size() && i < 53; i++) {
            Player p = players.get(i);
            ItemStack playerHead = createPlayerItem(p);
            ItemMeta playerMeta = playerHead.getItemMeta();
            if (playerMeta != null) {
                playerMeta.setDisplayName(p.getName());
                playerHead.setItemMeta(playerMeta);
            }
            gui.setItem(i, playerHead);
        }
        gui.setItem(53, createItem(Material.ARROW, ChatColor.GRAY + "Back"));
        player.openInventory(gui);
    }

    private ItemStack createItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createPotionItem(PotionEffectType effectType, String name) {
        Material material = getPotionMaterial(effectType);
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            // Set potion effect names to white color
            meta.setDisplayName(ChatColor.WHITE + name);
            item.setItemMeta(meta);
        }
        return item;
    }

    private Material getPotionMaterial(PotionEffectType effectType) {
        String effectName = effectType.toString();

        switch (effectName) {
            case "SPEED":
                return Material.SUGAR;
            case "SLOW":
                return Material.SOUL_SAND;
            case "INCREASE_DAMAGE":
                return Material.BLAZE_POWDER;
            case "WEAKNESS":
                return Material.FERMENTED_SPIDER_EYE;
            default:
                return Material.POTION;
        }
    }

    private ItemStack createPlayerItem(Player player) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) item.getItemMeta();

        if (skullMeta != null) {
            skullMeta.setOwningPlayer(player);
            item.setItemMeta(skullMeta);
        }

        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getCurrentItem() == null || event.getClickedInventory() == null) return;

        Player player = (Player) event.getWhoClicked();
        Inventory clickedInventory = event.getClickedInventory();

        if (clickedInventory.equals(player.getOpenInventory().getTopInventory())) {
            ItemStack clickedItem = event.getCurrentItem();
            String guiTitle = event.getView().getTitle();
            event.setCancelled(true);

            if (guiTitle.equals("Select a Team")) {
                if (clickedItem.getType() == Material.RED_CONCRETE || clickedItem.getType() == Material.LIGHT_BLUE_CONCRETE) {
                    ItemMeta itemMeta = clickedItem.getItemMeta();
                    if (itemMeta != null && itemMeta.hasDisplayName()) {
                        String team = ChatColor.stripColor(itemMeta.getDisplayName()); // Remove color codes
                        selectedTeams.put(player.getUniqueId(), team);
                        openBuffsDebuffsGui(player, team);
                    }
                }
            } else if (guiTitle.startsWith("Select Buff/Debuff for ")) {
                if (clickedItem.getType() == Material.ARROW) {
                    openMainGui(player);
                } else {
                    PotionEffectType effectType = getPotionEffectFromItem(clickedItem);
                    if (effectType != null) {
                        selectedEffects.put(player.getUniqueId(), effectType);
                        String team = selectedTeams.get(player.getUniqueId());
                        openPlayerListGui(player, team);
                    } else {
                        player.sendMessage(ChatColor.RED + "Please select a valid buff or debuff.");
                    }
                }
            } else if (guiTitle.startsWith("Players in ")) {
                if (clickedItem.getType() == Material.ARROW) {
                    String team = guiTitle.substring(11);
                    openBuffsDebuffsGui(player, team);
                } else {
                    applyEffectToPlayer(clickedItem, player);
                }
            }
        }
    }

    private PotionEffectType getPotionEffectFromItem(ItemStack item) {
        if (item != null && item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
                String displayName = ChatColor.stripColor(meta.getDisplayName()); // Remove color codes
                if (displayName.endsWith(" - " + ChatColor.WHITE + "Speed Buff")) {
                    return PotionEffectType.SPEED;
                } else if (displayName.endsWith(" - " + ChatColor.WHITE + "Strength Buff")) {
                    return PotionEffectType.INCREASE_DAMAGE;
                } else if (displayName.endsWith(" - " + ChatColor.WHITE + "Slowness Debuff")) {
                    return PotionEffectType.SLOW;
                } else if (displayName.endsWith(" - " + ChatColor.WHITE + "Weakness Debuff")) {
                    return PotionEffectType.WEAKNESS;
                }
            }
        }
        return null;
    }

    private void applyEffectToPlayer(ItemStack item, Player operator) {
        UUID operatorUUID = operator.getUniqueId();
        PotionEffectType effectType = getPotionEffectFromItem(item);
        if (effectType != null) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
                String displayName = ChatColor.stripColor(meta.getDisplayName()); // Remove color codes
                // Parse the player name from the display name
                String playerName = displayName.replace(" - " + ChatColor.WHITE + "Speed Buff", "")
                        .replace(" - " + ChatColor.WHITE + "Strength Buff", "")
                        .replace(" - " + ChatColor.WHITE + "Slowness Debuff", "")
                        .replace(" - " + ChatColor.WHITE + "Weakness Debuff", "");
                Player target = Bukkit.getServer().getPlayerExact(playerName);
                if (target != null) {
                    PotionEffect effect = new PotionEffect(effectType, 600, 1); // 30 seconds duration, amplifier 1
                    target.addPotionEffect(effect);
                    String message = ChatColor.LIGHT_PURPLE + "Applied " + formatEffectName(effectType) + " to " + ChatColor.WHITE + playerName;
                    Bukkit.getServer().broadcastMessage(message);
                } else {
                    operator.sendMessage("Target player is null.");
                }
            } else {
                operator.sendMessage("ItemMeta is null.");
            }
            selectedEffects.remove(operatorUUID);
        } else {
            operator.sendMessage("EffectType is null.");
        }
    }

    private String formatEffectName(PotionEffectType effectType) {
        String name = effectType.toString().toLowerCase().replace('_', ' ');
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
}
