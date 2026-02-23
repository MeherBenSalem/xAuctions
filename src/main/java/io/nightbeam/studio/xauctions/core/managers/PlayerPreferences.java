package io.nightbeam.studio.xauctions.core.managers;

import io.nightbeam.studio.xauctions.gui.framework.AuctionFilter;
import io.nightbeam.studio.xauctions.gui.framework.AuctionSortOrder;

/**
 * Stores per-player GUI preferences for the auction house.
 * Held in memory by {@link PlayerPreferencesManager} and evicted on player quit.
 */
public class PlayerPreferences {

    private AuctionFilter filter;
    private AuctionSortOrder sortOrder;

    public PlayerPreferences() {
        this.filter = AuctionFilter.ALL;
        this.sortOrder = AuctionSortOrder.NEWEST;
    }

    public AuctionFilter getFilter() {
        return filter;
    }

    public void setFilter(AuctionFilter filter) {
        this.filter = filter;
    }

    public AuctionSortOrder getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(AuctionSortOrder sortOrder) {
        this.sortOrder = sortOrder;
    }
}
