package io.nightbeam.studio.xauctions.api;

import io.nightbeam.studio.xauctions.XAuctionsPlugin;
import io.nightbeam.studio.xauctions.api.service.AuctionService;
import io.nightbeam.studio.xauctions.api.service.EconomyService;

/**
 * Public API for xAuctions.
 */
public class XAuctionsAPI {

    private static XAuctionsAPI instance;
    private final XAuctionsPlugin plugin;

    private XAuctionsAPI(XAuctionsPlugin plugin) {
        this.plugin = plugin;
    }

    public static XAuctionsAPI getInstance() {
        if (instance == null) {
            if (XAuctionsPlugin.getInstance() == null) {
                throw new IllegalStateException("XAuctionsPlugin is not loaded!");
            }
            instance = new XAuctionsAPI(XAuctionsPlugin.getInstance());
        }
        return instance;
    }

    public AuctionService getAuctionService() {
        return plugin.getAuctionService();
    }

    public EconomyService getEconomyService() {
        return plugin.getEconomyManager();
    }
}
