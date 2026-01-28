package io.nightbeam.studio.xauctions.api.model;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Request to create a new auction.
 */
public record AuctionRequest(
        Player seller,
        ItemStack item,
        double price,
        long durationSeconds,
        String currency) {
    public AuctionRequest(Player seller, ItemStack item, double price, long durationSeconds) {
        this(seller, item, price, durationSeconds, "vault");
    }
}
