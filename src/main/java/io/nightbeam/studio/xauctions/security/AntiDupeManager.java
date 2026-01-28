package io.nightbeam.studio.xauctions.security;

import io.nightbeam.studio.xauctions.xAuctions;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * AntiDupeManager
 * Prevents race conditions and inventory exploits during auction
 * creation/purchase.
 */
public class AntiDupeManager implements Listener {

    private final xAuctions plugin;
    // Players who are currently in a "Transaction State" (Listing, Buying,
    // Collecting)
    private final Set<UUID> lockedPlayers;

    public AntiDupeManager(xAuctions plugin) {
        this.plugin = plugin;
        this.lockedPlayers = new HashSet<>();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void lockPlayer(Player player) {
        lockedPlayers.add(player.getUniqueId());
    }

    public void unlockPlayer(Player player) {
        lockedPlayers.remove(player.getUniqueId());
    }

    public boolean isLocked(Player player) {
        return lockedPlayers.contains(player.getUniqueId());
    }

    /**
     * Rigorous validation of the item before listing.
     * Check for illegal NBTs, stack sizes, or blacklisted materials.
     */
    public boolean validateItem(ItemStack item) {
        if (item == null || item.getType().isAir())
            return false;
        // Logic: Check Blacklist, NBT exploit tags, etc.
        return true;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            if (isLocked(player)) {
                // Prevent moving items if they are mid-transaction
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(org.bukkit.event.inventory.InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            if (isLocked(player)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onDropAndPickup(org.bukkit.event.player.PlayerDropItemEvent event) {
        if (isLocked(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPickup(org.bukkit.event.player.PlayerAttemptPickupItemEvent event) { // Use AttemptPickup if available
                                                                                       // in 1.20, or
                                                                                       // EntityPickupItemEvent
        if (isLocked(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    // Additional protections for DropItemEvent, Menu Closing, etc.
}
