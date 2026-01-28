package io.nightbeam.studio.xauctions.core.listeners;

import io.nightbeam.studio.xauctions.XAuctionsPlugin;
import io.nightbeam.studio.xauctions.api.model.Auction;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;

public class PlayerConnectionListener implements Listener {

    private final XAuctionsPlugin plugin;

    public PlayerConnectionListener(XAuctionsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // 1. Pre-load data if using caching strategy
        plugin.getStorageProvider().loadPlayerAuctions(player.getUniqueId()).thenAccept(auctions -> {
            // 2. Notify if they have money to collect or expired items
            long soldCount = auctions.stream().filter(a -> a.isSold() && !a.isCollected()).count();
            long expiredCount = auctions.stream().filter(a -> (a.isExpired() || a.isDeleted()) && !a.isCollected())
                    .count();

            if (soldCount > 0 || expiredCount > 0) {
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    player.sendMessage("§8§m--------------------------------");
                    player.sendMessage("§6§lxAuctions Notification");
                    if (soldCount > 0) {
                        player.sendMessage("§aYou have " + soldCount + " sold auctions to collect!");
                    }
                    if (expiredCount > 0) {
                        player.sendMessage("§cYou have " + expiredCount + " expired/cancelled items to retrieve!");
                    }
                    player.sendMessage("§7Type §e/ah §7-> §eMy Auctions §7to manage.");
                    player.sendMessage("§8§m--------------------------------");
                }, 60L); // 3 seconds delay
            }
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // Clear cache if needed
        // plugin.getStorageProvider().unloadPlayer(event.getPlayer().getUniqueId());

        // Unlock anti-dupe just in case
        if (plugin.getAntiDupeManager() != null) {
            plugin.getAntiDupeManager().unlockPlayer(event.getPlayer());
        }
    }
}
