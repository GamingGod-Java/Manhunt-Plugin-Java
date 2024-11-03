package me.champion.manhuntplugin.listeners;

import me.champion.manhuntplugin.commands.MhStart;
import me.champion.manhuntplugin.Manhunt;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.event.player.PlayerPortalEvent;

import java.util.Map;

public class GameControlListener implements Listener {

    private final MhStart mhStart;

    public GameControlListener(MhStart mhStart) {
        this.mhStart = mhStart;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!mhStart.isGameStarted() && !event.getPlayer().isOp()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!mhStart.isGameStarted() && !event.getPlayer().isOp()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        ItemStack item = event.getItem().getItemStack();
        if (item.getType() == Material.COMMAND_BLOCK) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasDisplayName() && meta.getDisplayName().equals("Open Settings")) {
                Entity entity = event.getEntity();
                if (entity instanceof Player) {
                    Player player = (Player) entity;
                    if (!player.isOp()) {
                        // Non-operators cannot pick up the command block
                        event.setCancelled(true);
                    }
                } else {
                    // Non-player entities cannot pick up the command block
                    event.setCancelled(true);
                }
            }
        } else if (!mhStart.isGameStarted() && event.getEntity() instanceof Player) {
            // Before the game starts, prevent non-operators from picking up items
            Player player = (Player) event.getEntity();
            if (!player.isOp()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        if (!mhStart.isGameStarted() && !player.isOp()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // Don't want players to die of fall damage before the game starts
        if (!mhStart.isGameStarted() && event.getEntity() instanceof Player) {
            event.setCancelled(true);
        }
    }

    // Added methods and event handlers for the "Open Settings" command block
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Check if the game has started
        if (!mhStart.isGameStarted()) {
            // Game has not started: set player to Adventure mode
            player.setGameMode(GameMode.ADVENTURE);
            Bukkit.getLogger().info("Game has not started. Player " + player.getName() + " set to Adventure mode.");

            // If the player is an operator, give them an enchanted command block with custom name
            if (player.isOp()) {
                ItemStack commandBlockItem = new ItemStack(Material.COMMAND_BLOCK);
                ItemMeta meta = commandBlockItem.getItemMeta();
                meta.setDisplayName("Open Settings");

                // Add dummy enchantment to make it glow
                meta.addEnchant(Enchantment.DURABILITY, 1, true);
                // Hide enchantment from item lore
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

                commandBlockItem.setItemMeta(meta);

                Map<Integer, ItemStack> leftover = player.getInventory().addItem(commandBlockItem);
                if (!leftover.isEmpty()) {
                    // Inventory is full, drop the item on the ground
                    Item droppedItem = player.getWorld().dropItem(player.getLocation(), commandBlockItem);

                    // Set metadata to identify it as the special command block
                    droppedItem.setMetadata("OpenSettingsCommandBlock", new FixedMetadataValue(Manhunt.getPlugin(), true));

                    Bukkit.broadcastMessage("Operator " + player.getName() + " has a full inventory. The 'Open Settings' command block has been dropped on the ground.");
                    Bukkit.getLogger().info("Operator " + player.getName() + " has a full inventory. Dropped 'Open Settings' command block on the ground.");
                } else {
                    Bukkit.getLogger().info("Operator " + player.getName() + " given enchanted command block 'Open Settings'.");
                }
            }

            // Teleport the player to the world_spawn using the /mhswap command
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mhswap " + player.getName() + " world_spawn");
            Bukkit.getLogger().info("Player " + player.getName() + " teleported to world_spawn.");
        } else {
            // Game has started: set player to Spectator mode
            player.setGameMode(GameMode.SPECTATOR);
            Bukkit.getLogger().info("Game has started. Player " + player.getName() + " set to Spectator mode.");
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Remove the command block from OPs when they leave, to prevent duplication
        Player player = event.getPlayer();
        if (player.isOp()) {
            removeSettingsCommandBlockFromPlayer(player);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Check if action is right-clicking air or block
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Player player = event.getPlayer();

            ItemStack item = event.getItem();
            if (item != null && item.getType() == Material.COMMAND_BLOCK) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null && meta.hasDisplayName() && meta.getDisplayName().equals("Open Settings")) {
                    // Execute /MhSettings command as the player
                    player.performCommand("MhSettings");
                    event.setCancelled(true); // Prevent any default action
                    Bukkit.getLogger().info("Operator " + player.getName() + " used 'Open Settings' command block.");
                }
            }
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        Player player = event.getPlayer();
        if (item.getType() == Material.COMMAND_BLOCK) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasDisplayName() && meta.getDisplayName().equals("Open Settings")) {
                // Prevent dropping the command block
                event.setCancelled(true);
                player.sendMessage("You cannot drop the 'Open Settings' command block.");
            }
        }
    }

    // Method to remove the "Open Settings" command block from a player
    public void removeSettingsCommandBlockFromPlayer(Player player) {
        ItemStack[] contents = player.getInventory().getContents();
        boolean removed = false;
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item != null && item.getType() == Material.COMMAND_BLOCK) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null && meta.hasDisplayName() && meta.getDisplayName().equals("Open Settings")) {
                    // Remove this item
                    player.getInventory().setItem(i, null);
                    removed = true;
                }
            }
        }
        if (removed) {
            player.updateInventory(); // Update the inventory to reflect changes
            Bukkit.getLogger().info("Removed 'Open Settings' command block from " + player.getName() + "'s inventory.");
        }
    }

    @EventHandler
    public void onPlayerPortal(PlayerPortalEvent event) {
        // Check if the player is entering the End portal
        if (event.getTo().getWorld().getName().equals("world_the_end")) {
            // Remove the boss bar from all players
            if (mhStart.getBossBar() != null) {
                mhStart.getBossBar().setVisible(false);
                mhStart.getBossBar().removeAll(); // Optionally remove players from the boss bar
            }

            // Notify players
            Bukkit.broadcastMessage("[GameControlListener.java] §5The game timer has been hidden as a player entered the End.");
        }
    }

    // Method to remove the command blocks from all operators
    public void removeSettingsCommandBlocksFromAllOps() {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.isOp()) {
                removeSettingsCommandBlockFromPlayer(onlinePlayer);
            }
        }
        // Also remove any dropped "Open Settings" command blocks in the world
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof Item) {
                    Item itemEntity = (Item) entity;
                    ItemStack itemStack = itemEntity.getItemStack();
                    if (itemStack.getType() == Material.COMMAND_BLOCK) {
                        ItemMeta meta = itemStack.getItemMeta();
                        if (meta != null && meta.hasDisplayName() && meta.getDisplayName().equals("Open Settings")) {
                            itemEntity.remove();
                            Bukkit.getLogger().info("Removed dropped 'Open Settings' command block from the world.");
                        }
                    }
                }
            }
        }
    }
}
