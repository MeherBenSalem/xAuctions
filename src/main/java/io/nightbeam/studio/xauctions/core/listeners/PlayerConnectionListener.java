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

        plugin.getStorageProvider().loadPlayerAuctions(player.getUniqueId()).thenAccept(auctions -> {
            boolean autoClaim = plugin.getPluginConfig().getAuctionSettings().autoClaimOnJoin();

            if (autoClaim) {
                double totalCollected = 0;
                int count = 0;

                io.nightbeam.studio.xauctions.api.economy.EconomyProvider eco = plugin.getEconomyManager()
                        .getDefaultProvider();
                for (Auction auction : auctions) {
                    if (auction.isSold() && !auction.isCollected()) {
                        io.nightbeam.studio.xauctions.api.economy.EconomyProvider auctionEco = plugin
                                .getEconomyManager().getProvider(auction.getCurrency());
                        if (auctionEco == null)
                            auctionEco = eco; // Fallback

                        if (auctionEco != null) {
                            double tax = plugin.getTaxManager().calculateTax(auction.getPrice(),
                                    auction.getItemStack());
                            double finalAmount = auction.getPrice() - tax;

                            auctionEco.deposit(player, finalAmount);
                            auction.setCollected(true);
                            plugin.getStorageProvider().updateAuction(auction); // Async update, fire and forget for
                                                                                // join speed? Or wait?
                            player.sendMessage("§a[Auto-Claim] Sold " + auction.getItemStack().getType() + " for "
                                    + auctionEco.format(finalAmount));
                        }
                    }
                }
            }

            long soldCount = auctions.stream().filter(a -> a.isSold() && !a.isCollected()).count();
            long expiredCount = auctions.stream().filter(a -> (a.isExpired() || a.isDeleted()) && !a.isCollected())
                    .count();

            if (soldCount > 0 || expiredCount > 0) {
                plugin.getPlatformAdapter().runEntityTaskLater(player, () -> {
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
                }, 60L);
            }
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        plugin.getPlayerPreferencesManager().evict(player.getUniqueId());

        if (plugin.getAntiDupeManager() != null) {
            plugin.getAntiDupeManager().unlockPlayer(player);
        }
    }
}
