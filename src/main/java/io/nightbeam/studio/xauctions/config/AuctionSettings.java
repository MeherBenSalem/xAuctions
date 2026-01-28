package io.nightbeam.studio.xauctions.config;

/**
 * Immutable configuration settings for auctions.
 */
public record AuctionSettings(
        long defaultDurationSeconds,
        double listingFee,
        int maxAuctionsPerPlayer,
        double minPrice,
        double maxPrice,
        boolean allowCreative,
        boolean allowDamaged,
        boolean broadcastCreation,
        boolean broadcastSale) {
}
