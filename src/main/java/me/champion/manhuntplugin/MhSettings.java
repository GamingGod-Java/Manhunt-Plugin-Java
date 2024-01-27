package me.champion.manhuntplugin;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class MhSettings implements CommandExecutor, Listener {
    private final Inventory gui;

    public MhSettings() {
        // Create a 9-slot inventory for GUI
        gui = Bukkit.createInventory(null, 18, "Manhunt Settings"); // 18-slot inventory


        // Add items for each command
        addItem(Material.END_PORTAL_FRAME, "MhCreate", 0);
        addItem(Material.LEATHER_HELMET, "MhMove", 1);
        addItem(Material.RED_WOOL, "MhPause", 2); // Red Wool for MhPause
        addItem(Material.GREEN_WOOL, "MhUnpause", 3); // Green Wool for MhUnpause
        addItem(Material.LIME_WOOL, "MhReady", 4); // Lime Wool for MhReady
        addItem(Material.COMPASS, "MhCompass", 5);
        addItem(Material.FIREWORK_ROCKET, "MhStart", 6);
        addItem(Material.REPEATER, "MhRestart", 7);
        addItem(Material.WRITABLE_BOOK, "MhTeamChat", 8);
        addItem(Material.DROPPER, "MhWheel", 9);
        // Add more items as necessary
    }

    private void addItem(Material material, String name, int slot) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        gui.setItem(slot, item);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player && sender.hasPermission("manhunt.settings")) {
            Player player = (Player) sender;
            player.openInventory(gui);
        }
        return true;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory() != gui) return;

        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || !clickedItem.hasItemMeta()) return;

        ItemMeta itemMeta = clickedItem.getItemMeta();
        if (itemMeta == null || !itemMeta.hasDisplayName()) return;

        String commandLabel = itemMeta.getDisplayName();

        switch (commandLabel) {
            case "MhCreate":
                player.performCommand("MhCreate");
                break;
            case "MhMove":
                player.performCommand("MhMove");
                break;
            case "MhPause":
                player.performCommand("MhPause");
                break;
            case "MhUnpause":
                player.performCommand("MhUnpause");
                break;
            case "MhReady":
                player.performCommand("MhReady");
                break;
            case "MhCompass":
                player.performCommand("MhCompass");
                break;
            case "MhStart":
                player.performCommand("MhStart");
                break;
            case "MhRestart":
                player.performCommand("MhRestart");
                break;
            case "MhTeamChat":
                player.performCommand("MhTeamChat");
                break;
            case "MhWheel":
                player.performCommand("MhWheel");
                break;
            case "MhSettings":
                player.performCommand("MhSettings");
                break;
            default:
                player.sendMessage("Unknown command.");
                break;
        }
    }
}
