package io.nightbeam.studio.xauctions.addons;

import io.nightbeam.studio.xauctions.XAuctionsPlugin;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class xAuctionsExpansion extends PlaceholderExpansion {

    private final XAuctionsPlugin plugin;

    public xAuctionsExpansion(XAuctionsPlugin plugin) {
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
            return String.valueOf(plugin.getAuctionService().getActiveCount());
        }

        if (player == null)
            return null;

        if (params.equalsIgnoreCase("my_active_count")) {
            return "0"; // Implementation requires sync storage access
        }

        return null;
    }
}
