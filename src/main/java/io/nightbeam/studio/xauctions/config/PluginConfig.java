package io.nightbeam.studio.xauctions.config;

import io.nightbeam.studio.xauctions.XAuctionsPlugin;
import org.bukkit.configuration.file.FileConfiguration;

public class PluginConfig {

    private final XAuctionsPlugin plugin;

    // Settings
    private AuctionSettings auctionSettings;
    private String storageType;
    private String economyProvider;
    private String discordWebhookUrl;
    private boolean debugMode;
    private boolean guiSoundsEnabled;
    private String soundOpen;
    private String soundClick;
    private String soundPurchase;
    private String soundError;
    private boolean enableTopLevelSell;

    public PluginConfig(XAuctionsPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        // General
        this.debugMode = config.getBoolean("general.debug", false);

        // Auction Settings
        this.auctionSettings = new AuctionSettings(
                config.getLong("auction-settings.default-duration", 86400),
                config.getDouble("auction-settings.listing-fee", 0.0),
                config.getInt("auction-settings.max-auctions", 15),
                config.getDouble("auction-settings.min-price", 10.0),
                config.getDouble("auction-settings.max-price", 1000000000.0),
                config.getBoolean("auction-settings.allow-creative", false),
                config.getBoolean("auction-settings.allow-damaged", true),
                config.getBoolean("auction-settings.broadcast-creation", true),
                config.getBoolean("auction-settings.broadcast-sale", true),
                config.getBoolean("auction-settings.auto-claim-online", true),
                config.getBoolean("auction-settings.auto-claim-on-join", true));

        // Storage
        this.storageType = config.getString("storage.type", "json");

        // Economy
        this.economyProvider = config.getString("economy.provider", "vault");

        // Integrations
        this.discordWebhookUrl = config.getString("integrations.discord.webhook-url", "");

        // GUI
        this.guiSoundsEnabled = config.getBoolean("gui.sounds.enabled", true);
        this.soundOpen = config.getString("gui.sounds.open-menu", "BLOCK_CHEST_OPEN");
        this.soundClick = config.getString("gui.sounds.click", "UI_BUTTON_CLICK");
        this.soundPurchase = config.getString("gui.sounds.purchase", "ENTITY_PLAYER_LEVELUP");
        this.soundError = config.getString("gui.sounds.error", "ENTITY_VILLAGER_NO");

        // Commands
        this.enableTopLevelSell = config.getBoolean("commands.enable-top-level-sell", false);
    }

    public void reload() {
        load();
    }

    public AuctionSettings getAuctionSettings() {
        return auctionSettings;
    }

    public String getStorageType() {
        return storageType;
    }

    public String getEconomyProvider() {
        return economyProvider;
    }

    public String getDiscordWebhookUrl() {
        return discordWebhookUrl;
    }

    public boolean isDebugMode() {
        return debugMode;
    }

    public boolean isGuiSoundsEnabled() {
        return guiSoundsEnabled;
    }

    public String getSound(String key) {
        return switch (key) {
            case "open" -> soundOpen;
            case "click" -> soundClick;
            case "purchase" -> soundPurchase;
            case "error" -> soundError;
            default -> null;
        };
    }

    public boolean isTopLevelSellEnabled() {
        return enableTopLevelSell;
    }
}
