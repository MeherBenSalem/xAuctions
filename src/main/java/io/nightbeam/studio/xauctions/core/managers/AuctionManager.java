package io.nightbeam.studio.xauctions.core.managers;

import io.nightbeam.studio.xauctions.XAuctionsPlugin;
import io.nightbeam.studio.xauctions.api.model.Auction;
import io.nightbeam.studio.xauctions.api.model.AuctionRequest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class AuctionManager {

    private final XAuctionsPlugin plugin;

    public AuctionManager(XAuctionsPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Creates a new auction (BIN).
     */
    public void createAuction(Player player, ItemStack item, double price, long durationSeconds, String currency) {
        // Delegate to Service
        AuctionRequest request = new AuctionRequest(
                player,
                item,
                price,
                durationSeconds,
                currency != null ? currency : "vault",
                Auction.AuctionType.BIN);

        plugin.getAuctionService().createAuction(request).thenAccept(result -> {
            if (result.isSuccess()) {
                plugin.getMessageManager().send(player, "auction-created",
                        "%item%", item.getType().toString(),
                        "%price%", String.valueOf(price));
                // Discord handling is likely inside Service now?
                // Service logic (Step 129) had "Notify Discord" comment but Log.info.
                // We should move Discord logic to Service or listener.
                // For now, Service returns Auction, we can do extra stuff here if needed, but
                // keeping it DRY is better.
                // Ideally Service fires Event, and DiscordListener handles it.
            } else {
                player.sendMessage("§cFailed to create auction: " + result.getError());
            }
        });
    }

    /**
     * Atomic Buy Operation
     */
    public void buyAuction(Player buyer, Auction auction) {
        plugin.getAuctionService().buyAuction(buyer, auction).thenAccept(result -> {
            if (!result.isSuccess()) {
                buyer.sendMessage("§c" + result.getError());
            } else {
                // Success handled by Service (items given, etc in Step 129/136)
            }
        });
    }

    /**
     * Collect earnings or expired items.
     */
    public void collectAuction(Player player, Auction auction) {
        plugin.getAuctionService().collectAuction(player, auction).thenAccept(result -> {
            if (!result.isSuccess()) {
                player.sendMessage("§c" + result.getError());
            }
        });
    }

    /**
     * Cancel an active auction.
     */
    public void cancelAuction(Player player, Auction auction) {
        plugin.getAuctionService().cancelAuction(player, auction).thenAccept(result -> {
            if (result.isSuccess()) {
                plugin.getMessageManager().send(player, "auction-cancelled");
            } else {
                player.sendMessage("§c" + result.getError());
            }
        });
    }
}
