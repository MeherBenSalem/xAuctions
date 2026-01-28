package io.nightbeam.studio.xauctions.addons;

import io.nightbeam.studio.xauctions.xAuctions;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class xAuctionsExpansion extends PlaceholderExpansion {

    private final xAuctions plugin;

    public xAuctionsExpansion(xAuctions plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "xauctions";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Nightbeam Studio";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (params.equalsIgnoreCase("active_count")) {
            // Need a method in AuctionManager/Storage to get count efficiently
            // For now, load active and count (not efficient but works for MVP)
            // Or better, just return "..." if async loading is needed, or cache it.
            return "Loading..."; // PlaceholderAPI requires sync usually
        }

        if (player == null)
            return null;

        if (params.equalsIgnoreCase("my_active_count")) {
            return "0"; // Implementation requires sync storage access
        }

        return null;
    }
}
