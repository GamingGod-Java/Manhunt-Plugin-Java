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
import org.bukkit.SkullType;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

public class MhWheel implements CommandExecutor, Listener {
    private final Manhunt plugin;
    private final TeamManager teamManager;
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
        List<Player> teamPlayers = teamManager.getPlayersOnTeam(team);
        if (teamPlayers.isEmpty() && !teamMessageSent.contains(team)) {
            player.sendMessage("There are no players on the " + team + " team.");
            teamMessageSent.add(team); // Add the team to the set to avoid spamming the message
            return;
        } else if (!teamPlayers.isEmpty()) {
            // Remove the team from the set if there are players on it
            teamMessageSent.remove(team);
        }
        Inventory gui = Bukkit.createInventory(null, 27, "Select Buff/Debuff for " + team);

        // Buffs
        gui.setItem(11, createPotionItem(PotionEffectType.SPEED, "Speed Buff", team));
        gui.setItem(12, createPotionItem(PotionEffectType.INCREASE_DAMAGE, "Strength Buff", team));

        // Debuffs
        gui.setItem(14, createPotionItem(PotionEffectType.SLOW, "Slowness Debuff", team));
        gui.setItem(15, createPotionItem(PotionEffectType.WEAKNESS, "Weakness Debuff", team));

        gui.setItem(22, createItem(Material.ARROW, ChatColor.GRAY + "Back"));
        player.openInventory(gui);
    }

    private void openPlayerListGui(Player player, String team) {
        Inventory gui = Bukkit.createInventory(null, 54, "Players in " + team);
        List<Player> players = teamManager.getPlayersOnTeam(team);
        for (int i = 0; i < players.size() && i < 53; i++) {
            Player p = players.get(i);
            gui.setItem(i, createPlayerItem(p));
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

    private ItemStack createPotionItem(PotionEffectType effectType, String name, String team) {
        Material material = getPotionMaterial(effectType);
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            // Set potion effect names to white color
            meta.setDisplayName(ChatColor.WHITE + name + " - " + team);
            item.setItemMeta(meta);
        }
        return item;
    }

    private Material getPotionMaterial(PotionEffectType effectType) {
        switch (effectType.getName()) {
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
        ItemStack item = new ItemStack(Material.PLAYER_HEAD, 1, (short) SkullType.PLAYER.ordinal());
        SkullMeta skullMeta = (SkullMeta) item.getItemMeta();

        if (skullMeta != null) {
            skullMeta.setOwningPlayer(player);
            skullMeta.setDisplayName(player.getName());
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
                    String team = ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName()); // Remove color codes
                    openBuffsDebuffsGui(player, team);
                }
            } else if (guiTitle.startsWith("Select Buff/Debuff for ")) {
                String[] titleParts = guiTitle.split(" ");
                if (titleParts.length > 3) {
                    String team = titleParts[titleParts.length - 1]; // Get the last part of the title
                    if (clickedItem.getType() == Material.ARROW) {
                        openMainGui(player);
                    } else {
                        PotionEffectType effectType = getPotionEffectFromItem(clickedItem);
                        if (effectType != null) {
                            selectedEffects.put(player.getUniqueId(), effectType);
                            openPlayerListGui(player, team);
                        } else {
                            player.sendMessage(ChatColor.RED + "Please select a valid buff or debuff.");
                        }
                    }
                } else {
                    player.sendMessage(ChatColor.RED + "An error occurred. Please try again.");
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
        if (item != null) {
            Material material = item.getType();
            if (material == Material.SUGAR) return PotionEffectType.SPEED;
            if (material == Material.BLAZE_POWDER) return PotionEffectType.INCREASE_DAMAGE;
            if (material == Material.SOUL_SAND) return PotionEffectType.SLOW;
            if (material == Material.FERMENTED_SPIDER_EYE) return PotionEffectType.WEAKNESS;
        }
        return null;
    }
    private void applyEffectToPlayer(ItemStack item, Player operator) {
        UUID operatorUUID = operator.getUniqueId();
        PotionEffectType effectType = selectedEffects.get(operatorUUID);
        if (effectType != null) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                String itemName = ChatColor.stripColor(meta.getDisplayName()); // Remove color codes
                String[] parts = itemName.split(" - ");
                if (parts.length == 2) {
                    String playerName = parts[1]; // Extract player name
                    Player target = Bukkit.getServer().getPlayerExact(playerName);
                    if (target != null) {
                        PotionEffect effect = new PotionEffect(effectType, 600, 1); // 30 seconds duration, amplifier 1
                        target.addPotionEffect(effect);
                        String message = ChatColor.LIGHT_PURPLE + "Applied " + formatEffectName(effectType) + " to " + ChatColor.WHITE + playerName;
                        Bukkit.getServer().broadcastMessage(message);

                        // Debug messages
                        operator.sendMessage("Applied " + effectType.getName() + " to " + playerName);
                    } else {
                        // Debug message if target is null
                        operator.sendMessage("Target player is null.");
                    }
                } else {
                    // Debug message if parts.length is not 2
                    operator.sendMessage("Invalid item name format.");
                }
            } else {
                // Debug message if meta is null
                operator.sendMessage("ItemMeta is null.");
            }
            selectedEffects.remove(operatorUUID);
        } else {
            // Debug message if effectType is null
            operator.sendMessage("EffectType is null.");
        }
    }

    private String formatEffectName(PotionEffectType effectType) {
        String name = effectType.toString().toLowerCase().replace('_', ' ');
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
}
