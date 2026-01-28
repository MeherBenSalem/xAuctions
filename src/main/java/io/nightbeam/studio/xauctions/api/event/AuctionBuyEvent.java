package io.nightbeam.studio.xauctions.api.event;

import io.nightbeam.studio.xauctions.api.model.Auction;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when an auction is purchased.
 */
public class AuctionBuyEvent extends AuctionEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Player buyer;
    private boolean cancelled;

    public AuctionBuyEvent(Auction auction, Player buyer) {
        super(auction);
        this.buyer = buyer;
    }

    public Player getBuyer() {
        return buyer;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
