package io.nightbeam.studio.xauctions.internal.service.impl;

import io.nightbeam.studio.xauctions.XAuctionsPlugin;
import io.nightbeam.studio.xauctions.api.economy.EconomyProvider;
import io.nightbeam.studio.xauctions.api.model.Auction;
import io.nightbeam.studio.xauctions.api.model.AuctionRequest;
import io.nightbeam.studio.xauctions.api.model.Result;
import io.nightbeam.studio.xauctions.api.service.AuctionService;
import io.nightbeam.studio.xauctions.api.event.AuctionBuyEvent;
import io.nightbeam.studio.xauctions.api.event.AuctionCreateEvent;
import io.nightbeam.studio.xauctions.config.PluginConfig;
import io.nightbeam.studio.xauctions.utils.Log;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Implementation of the AuctionService.
 */
public class AuctionServiceImpl implements AuctionService {

    private final XAuctionsPlugin plugin;

    public AuctionServiceImpl(XAuctionsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public CompletableFuture<Result<Auction>> createAuction(AuctionRequest request) {
        Player seller = request.seller();
        PluginConfig config = plugin.getPluginConfig();

        // 1. Validation
        // TODO: Move validation to a Validator class or simpler checks here
        if (config.getAuctionSettings().listingFee() > 0) {
            EconomyProvider eco = plugin.getEconomyManager().getDefaultProvider();
            if (eco == null) {
                return CompletableFuture.completedFuture(Result.error("Economy not available"));
            }
            if (!eco.has(seller, config.getAuctionSettings().listingFee())) {
                return CompletableFuture.completedFuture(Result.error("Not enough money for listing fee"));
            }
            eco.withdraw(seller, config.getAuctionSettings().listingFee());
        }

        request.item().setAmount(request.item().getAmount()); // Ensure amount is correct?

        // 2. Create Auction Object
        long start = System.currentTimeMillis();
        long expire = start + (request.durationSeconds() * 1000);

        Auction auction = Auction.builder()
                .auctionId(UUID.randomUUID())
                .sellerUuid(seller.getUniqueId())
                .sellerName(seller.getName())
                .itemStack(request.item().clone())
                .price(request.price())
                .startTime(start)
                .expireTime(expire)
                .type(Auction.AuctionType.BIN)
                .currency(request.currency())
                .sold(false)
                .collected(false)
                .deleted(false)
                .deleted(false)
                .build();

        // Fire Event
        AuctionCreateEvent event = new AuctionCreateEvent(auction, seller);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return CompletableFuture.completedFuture(Result.error("Cancelled by plugin"));
        }

        // Remove item from inventory
        seller.getInventory().removeItem(request.item());

        // 3. Save to Storage
        return plugin.getStorageProvider().saveAuction(auction)
                .thenApply(v -> {
                    // Notify Discord, etc.
                    Log.info("Auction created: " + auction.getAuctionId());
                    return Result.success(auction);
                })
                .exceptionally(ex -> Result.error("Storage error: " + ex.getMessage()));
    }

    @Override
    public CompletableFuture<Result<Void>> buyAuction(Player buyer, Auction auction) {
        // Basic implementation for now, mirroring manager logic
        if (auction.isSold() || auction.isExpired()) {
            return CompletableFuture.completedFuture(Result.error("Auction not available"));
        }

        EconomyProvider eco = plugin.getEconomyManager().getDefaultProvider();
        if (eco == null) {
            return CompletableFuture.completedFuture(Result.error("Economy not available"));
        }

        if (!eco.has(buyer, auction.getPrice())) {
            return CompletableFuture.completedFuture(Result.error("Not enough money"));
        }

        // Fire Event
        AuctionBuyEvent event = new AuctionBuyEvent(auction, buyer);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return CompletableFuture.completedFuture(Result.error("Cancelled by plugin"));
        }

        eco.withdraw(buyer, auction.getPrice());
        auction.setSold(true);

        buyer.getInventory().addItem(auction.getItemStack());

        return plugin.getStorageProvider().updateAuction(auction)
                .thenApply(v -> {
                    // Deposit to seller logic would go here or be handled by a separate
                    // process/lazy load
                    return Result.<Void>success();
                });
    }

    @Override
    public CompletableFuture<Result<Void>> cancelAuction(Player player, Auction auction) {
        if (!auction.getSellerUuid().equals(player.getUniqueId())) {
            return CompletableFuture.completedFuture(Result.error("Not your auction"));
        }

        auction.setExpireTime(0L); // Expire it
        return plugin.getStorageProvider().updateAuction(auction)
                .thenApply(v -> Result.success());
    }

    @Override
    public CompletableFuture<Result<Void>> collectAuction(Player player, Auction auction) {
        // TODO: Implement collection logic
        return CompletableFuture.completedFuture(Result.success());
    }
}
