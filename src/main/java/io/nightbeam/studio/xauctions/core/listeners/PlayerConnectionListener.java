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
            boolean autoClaim = plugin.getPluginConfig().getAuctionSettings().autoClaimOnJoin();

            if (autoClaim) {
                // Auto-claim logic
                double totalCollected = 0;
                int count = 0;

                io.nightbeam.studio.xauctions.api.economy.EconomyProvider eco = plugin.getEconomyManager()
                        .getDefaultProvider();
                // Note: We might have multiple currencies, so we should group by currency or
                // handle individually.
                // For simplicity/MVP, let's assumne default currency or handle per auction.

                for (Auction auction : auctions) {
                    if (auction.isSold() && !auction.isCollected()) {
                        io.nightbeam.studio.xauctions.api.economy.EconomyProvider auctionEco = plugin
                                .getEconomyManager().getProvider(auction.getCurrency());
                        if (auctionEco == null)
                            auctionEco = eco; // Fallback

                        if (auctionEco != null) {
                            // Calculate tax again? Or trust stored price? Tax is calculated on collection
                            // usually.
                            double tax = plugin.getTaxManager().calculateTax(auction.getPrice(),
                                    auction.getItemStack());
                            double finalAmount = auction.getPrice() - tax;

                            auctionEco.deposit(player, finalAmount);
                            auction.setCollected(true);
                            plugin.getStorageProvider().updateAuction(auction); // Async update, fire and forget for
                                                                                // join speed? Or wait?
                            // Better to fire and forget here to not block join too much, or batch update if
                            // possible.
                            // We will just do individal updates for now.

                            // We can't easily sum total collected if currencies differ.
                            // So maybe just notify "You collected X from Y sales".
                            player.sendMessage("§a[Auto-Claim] Sold " + auction.getItemStack().getType() + " for "
                                    + auctionEco.format(finalAmount));
                        }
                    }
                }
            }

            // 2. Notify if they have money to collect (if not auto-claimed or failed) or
            // expired items
            // Re-count after auto-claim attempt (local objects updated)
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
        Player player = event.getPlayer();

        // Evict player preferences from cache to prevent unbounded memory growth
        plugin.getPlayerPreferencesManager().evict(player.getUniqueId());

        // Unlock anti-dupe just in case
        if (plugin.getAntiDupeManager() != null) {
            plugin.getAntiDupeManager().unlockPlayer(player);
        }
    }
}
