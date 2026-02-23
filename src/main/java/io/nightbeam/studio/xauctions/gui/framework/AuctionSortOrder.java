package io.nightbeam.studio.xauctions.gui.framework;

import org.bukkit.Material;

/**
 * Available sort orders for the auction house listing view.
 * Persisted per-player via {@link io.nightbeam.studio.xauctions.core.managers.PlayerPreferencesManager}.
 */
public enum AuctionSortOrder {

    NEWEST(Material.CLOCK, "Newest First"),
    OLDEST(Material.COBWEB, "Oldest First"),
    PRICE_ASC(Material.GOLD_NUGGET, "Price: Low → High"),
    PRICE_DESC(Material.GOLD_INGOT, "Price: High → Low"),
    EXPIRING_SOON(Material.TNT, "Expiring Soon");

    private final Material icon;
    private final String displayName;

    AuctionSortOrder(Material icon, String displayName) {
        this.icon = icon;
        this.displayName = displayName;
    }

    public Material getIcon() {
        return icon;
    }

    public String getDisplayName() {
        return displayName;
    }
}
