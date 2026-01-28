package io.nightbeam.studio.xauctions.api.storage;

import io.nightbeam.studio.xauctions.api.model.Auction;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface StorageProvider {

    void init();

    void shutdown();

    /**
     * Save a new auction to the database.
     */
    CompletableFuture<Void> saveAuction(Auction auction);

    /**
     * Update an existing auction state.
     */
    CompletableFuture<Void> updateAuction(Auction auction);

    /**
     * Delete an auction permanently (or soft delete).
     */
    CompletableFuture<Void> deleteAuction(UUID auctionId);

    /**
     * Load all active auctions for the global list.
     */
    CompletableFuture<List<Auction>> loadActiveAuctions();

    /**
     * Load expired or sold auctions for a specific player (Collection Bin).
     */
    CompletableFuture<List<Auction>> loadPlayerAuctions(UUID sellerUuid);

    /**
     * Invalidate local cache for an auction (used by multi-server sync).
     */
    void invalidateCache(UUID auctionId);

    /**
     * Atomic buy operation.
     * 
     * @return true if successful, false if already sold/unavailable.
     */
    CompletableFuture<Boolean> attemptBuy(UUID auctionId, UUID buyerUuid);
}
