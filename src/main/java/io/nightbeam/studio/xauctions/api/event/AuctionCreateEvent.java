package io.nightbeam.studio.xauctions.api.event;

import io.nightbeam.studio.xauctions.api.model.Auction;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when an auction is created.
 */
public class AuctionCreateEvent extends AuctionEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Player seller;
    private boolean cancelled;

    public AuctionCreateEvent(Auction auction, Player seller) {
        super(auction); // Defaults to sync, but we might arguably want async if storage is involved?
        // Usually creation initiation is sync (command), actual storage save is async.
        // Let's keep it sync for safety with Bukkit API unless explicitly needed async.
        this.seller = seller;
    }

    public Player getSeller() {
        return seller;
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
