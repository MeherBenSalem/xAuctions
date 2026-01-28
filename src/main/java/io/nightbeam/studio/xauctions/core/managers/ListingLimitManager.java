package io.nightbeam.studio.xauctions.core.managers;

import io.nightbeam.studio.xauctions.XAuctionsPlugin;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

public class ListingLimitManager {

    private final XAuctionsPlugin plugin;

    public ListingLimitManager(XAuctionsPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Gets the maximum number of listings a player is allowed to have.
     * Checks permissions: xauctions.limit.<number>
     * Defaults to config value if no specific permission found.
     */
    public int getMaxListings(Player player) {
        if (player.hasPermission("xauctions.limit.*")) {
            return 999; // Unlimited
        }

        int max = plugin.getPluginConfig().getAuctionSettings().maxAuctions(); // Default from config

        // Check for permission overrides
        for (PermissionAttachmentInfo info : player.getEffectivePermissions()) {
            String perm = info.getPermission();
            if (perm.startsWith("xauctions.limit.")) {
                try {
                    int limit = Integer.parseInt(perm.substring("xauctions.limit.".length()));
                    if (limit > max) {
                        max = limit; // Take the highest limit found
                    }
                } catch (NumberFormatException ignored) {
                    // Invalid permission format
                }
            }
        }

        return max;
    }

    public boolean canCreateListing(Player player, int currentActive) {
        return currentActive < getMaxListings(player);
    }
}
