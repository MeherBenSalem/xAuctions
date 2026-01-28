package io.nightbeam.studio.xauctions.api.service;

import io.nightbeam.studio.xauctions.api.model.Auction;
import io.nightbeam.studio.xauctions.api.model.AuctionRequest;
import io.nightbeam.studio.xauctions.api.model.Result;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;

/**
 * Service for managing auctions.
 */
public interface AuctionService {

    /**
     * Creates a new auction.
     *
     * @param request The auction creation request.
     * @return A future containing the result of the operation.
     */
    CompletableFuture<Result<Auction>> createAuction(AuctionRequest request);

    /**
     * Purchases an auction.
     *
     * @param buyer   The player buying the auction.
     * @param auction The auction to buy.
     * @return A future containing the result of the operation.
     */
    CompletableFuture<Result<Void>> buyAuction(Player buyer, Auction auction);

    /**
     * Cancels an auction.
     *
     * @param player  The player cancelling the auction.
     * @param auction The auction to cancel.
     * @return A future containing the result of the operation.
     */
    CompletableFuture<Result<Void>> cancelAuction(Player player, Auction auction);

    /**
     * Collects an auction (either money from sale or item from expiration).
     *
     * @param player  The player collecting the auction.
     * @param auction The auction to collect.
     * @return A future containing the result.
     */
    /**
     * Collects an auction (either money from sale or item from expiration).
     *
     * @param player  The player collecting the auction.
     * @param auction The auction to collect.
     * @return A future containing the result.
     */
    CompletableFuture<Result<Void>> collectAuction(Player player, Auction auction);

    /**
     * Returns the cached count of active auctions.
     */
    int getActiveCount();
}
