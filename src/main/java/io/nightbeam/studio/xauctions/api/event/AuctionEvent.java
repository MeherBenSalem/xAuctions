package io.nightbeam.studio.xauctions.api.event;

import io.nightbeam.studio.xauctions.api.model.Auction;
import org.bukkit.event.Event;

/**
 * Base class for all auction events.
 */
public abstract class AuctionEvent extends Event {

    protected final Auction auction;

    public AuctionEvent(Auction auction) {
        this.auction = auction;
    }

    public AuctionEvent(Auction auction, boolean isAsync) {
        super(isAsync);
        this.auction = auction;
    }

    public Auction getAuction() {
        return auction;
    }
}
