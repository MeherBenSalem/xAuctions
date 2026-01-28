package io.nightbeam.studio.xauctions;

import io.nightbeam.studio.xauctions.addons.DiscordWebhook;
import io.nightbeam.studio.xauctions.addons.xAuctionsExpansion;
import io.nightbeam.studio.xauctions.api.storage.StorageProvider;
import io.nightbeam.studio.xauctions.core.commands.AuctionsCommand;
import io.nightbeam.studio.xauctions.core.commands.SellCommand;
import io.nightbeam.studio.xauctions.core.listeners.InputListener;
import io.nightbeam.studio.xauctions.core.listeners.PlayerConnectionListener;
import io.nightbeam.studio.xauctions.core.managers.AuctionManager;
import io.nightbeam.studio.xauctions.core.managers.EconomyManager;
import io.nightbeam.studio.xauctions.core.managers.MessageManager;
import io.nightbeam.studio.xauctions.gui.GuiManager;
import io.nightbeam.studio.xauctions.security.AntiDupeManager;
import io.nightbeam.studio.xauctions.storage.json.JsonStorageProvider;
import io.nightbeam.studio.xauctions.storage.sql.SqlStorageProvider;
import io.nightbeam.studio.xauctions.config.PluginConfig;
import io.nightbeam.studio.xauctions.api.service.AuctionService;
import io.nightbeam.studio.xauctions.internal.service.impl.AuctionServiceImpl;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

/**
 * xAuctions - Premium Auction House Plugin
 * <p>
 * A scalable, modular auction house system designed for SMP and network
 * environments.
 * </p>
 *
 * @author Nightbeam Studio
 * @version 1.0.0
 */
public class XAuctionsPlugin extends JavaPlugin {

    private static XAuctionsPlugin instance;

    // Managers & Services
    private EconomyManager economyManager;
    private GuiManager guiManager;
    private StorageProvider storageProvider;
    private AuctionManager auctionManager;
    private AntiDupeManager antiDupeManager;
    private InputListener inputListener;
    private DiscordWebhook discordWebhook;
    private MessageManager messageManager;
    private AuctionService auctionService;

    // Configuration
    private PluginConfig pluginConfig;

    @Override
    public void onEnable() {
        instance = this;
        long startTime = System.currentTimeMillis();

        getLogger().info("═══════════════════════════════════════════");
        getLogger().info("       xAuctions by Nightbeam Studio");
        getLogger().info("═══════════════════════════════════════════");

        try {
            // Load configuration
            loadConfiguration();

            // Initialize managers in proper order
            initializeManagers();

            // Register commands
            registerCommands();

            // Register listeners
            registerListeners();

            // Load integrations
            loadIntegrations();

            long loadTime = System.currentTimeMillis() - startTime;
            getLogger().info("xAuctions loaded successfully in " + loadTime + "ms!");

        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to initialize xAuctions!", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("xAuctions is shutting down...");

        // Save data
        if (storageProvider != null) {
            storageProvider.shutdown();
            getLogger().info("Storage provider shut down successfully.");
        }

        getLogger().info("xAuctions disabled. Goodbye!");
    }

    /**
     * Loads and validates plugin configuration.
     */
    private void loadConfiguration() {
        // Initialize PluginConfig which handles loading and reloading
        if (pluginConfig == null) {
            pluginConfig = new PluginConfig(this);
        } else {
            pluginConfig.reload();
        }

        if (pluginConfig.isDebugMode()) {
            getLogger().info("[DEBUG] Debug mode is enabled.");
        }
    }

    /**
     * Initializes all managers in the correct dependency order.
     */
    private void initializeManagers() {
        // 1. Message Manager (no dependencies)
        messageManager = new MessageManager(this);
        debug("MessageManager initialized.");

        // 2. Economy Manager (no dependencies)
        economyManager = new EconomyManager(this);
        economyManager.init();
        debug("EconomyManager initialized.");

        // 3. Storage Provider (depends on config)
        initializeStorage();
        debug("StorageProvider initialized.");

        // 4. GUI Manager (no dependencies)
        guiManager = new GuiManager(this);
        debug("GuiManager initialized.");

        // 5. Auction Manager (depends on storage, economy)
        auctionManager = new AuctionManager(this);
        auctionService = new AuctionServiceImpl(this);
        debug("AuctionManager initialized.");

        // 6. Anti-Dupe Manager (no dependencies)
        antiDupeManager = new AntiDupeManager(this);
        debug("AntiDupeManager initialized.");

        // 7. Input Listener (no dependencies)
        inputListener = new InputListener(this);
        debug("InputListener initialized.");
    }

    /**
     * Initializes the storage provider based on configuration.
     */
    private void initializeStorage() {
        String storageType = pluginConfig.getStorageType().toLowerCase();

        switch (storageType) {
            case "mysql", "mariadb", "postgresql", "sqlite" -> {
                storageProvider = new SqlStorageProvider(this);
                getLogger().info("Using SQL storage: " + storageType.toUpperCase());
            }
            default -> {
                storageProvider = new JsonStorageProvider(this);
                getLogger().info("Using JSON storage.");
            }
        }

        storageProvider.init();
    }

    /**
     * Registers plugin commands.
     */
    private void registerCommands() {
        var ahCommand = getCommand("ah");
        if (ahCommand != null) {
            ahCommand.setExecutor(new AuctionsCommand(this));
            debug("Command 'ah' registered.");
        } else {
            getLogger().warning("Failed to register 'ah' command - not defined in plugin.yml");
        }

        var sellCommand = getCommand("sell");
        if (sellCommand != null) {
            sellCommand.setExecutor(new SellCommand(this));
            debug("Command 'sell' registered.");
        } else {
            getLogger().warning("Failed to register 'sell' command - not defined in plugin.yml");
        }
    }

    /**
     * Registers event listeners.
     */
    private void registerListeners() {
        // Player Connection Listener
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
        debug("PlayerConnectionListener registered.");

        // Input Listener (already instantiated in initializeManagers)
        getServer().getPluginManager().registerEvents(inputListener, this);
        debug("InputListener registered.");

        // Note: AntiDupeManager and GuiManager register themselves in their
        // constructors
    }

    /**
     * Loads optional plugin integrations.
     */
    private void loadIntegrations() {
        // PlaceholderAPI
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new xAuctionsExpansion(this).register();
            getLogger().info("PlaceholderAPI expansion registered.");
        }

        // Discord Webhook
        String webhookUrl = pluginConfig.getDiscordWebhookUrl();

        if (webhookUrl != null && !webhookUrl.isEmpty()) {
            discordWebhook = new DiscordWebhook(webhookUrl);
            getLogger().info("Discord webhook integration enabled.");
        }
    }

    /**
     * Logs a debug message if debug mode is enabled.
     *
     * @param message The message to log
     */
    public void debug(String message) {
        if (pluginConfig.isDebugMode()) {
            getLogger().info("[DEBUG] " + message);
        }
    }

    /**
     * Logs a debug message with formatting if debug mode is enabled.
     *
     * @param message The message format
     * @param args    The format arguments
     */
    public void debug(String message, Object... args) {
        if (pluginConfig.isDebugMode()) {
            getLogger().info("[DEBUG] " + String.format(message, args));
        }
    }

    /**
     * Reloads the plugin configuration and managers.
     */
    public void reload() {
        loadConfiguration();
        messageManager.reload();
        getLogger().info("Configuration reloaded.");
    }

    // ═══════════════════════════════════════════════════════════════
    // GETTERS
    // ═══════════════════════════════════════════════════════════════

    public static XAuctionsPlugin getInstance() {
        return instance;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public GuiManager getGuiManager() {
        return guiManager;
    }

    public StorageProvider getStorageProvider() {
        return storageProvider;
    }

    public AuctionManager getAuctionManager() {
        return auctionManager;
    }

    public AntiDupeManager getAntiDupeManager() {
        return antiDupeManager;
    }

    public InputListener getInputListener() {
        return inputListener;
    }

    public DiscordWebhook getDiscordWebhook() {
        return discordWebhook;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public boolean isDebugMode() {
        return pluginConfig.isDebugMode();
    }

    public PluginConfig getPluginConfig() {
        return pluginConfig;
    }

    public AuctionService getAuctionService() {
        return auctionService;
    }
}
