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
import io.nightbeam.studio.xauctions.core.managers.TaxManager;
import io.nightbeam.studio.xauctions.core.managers.ListingLimitManager;
import io.nightbeam.studio.xauctions.utils.PlatformAdapter;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

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
    private io.nightbeam.studio.xauctions.core.managers.TaxManager taxManager;
    private io.nightbeam.studio.xauctions.core.managers.ListingLimitManager listingLimitManager;
    private io.nightbeam.studio.xauctions.core.managers.ItemBlacklistManager itemBlacklistManager;
    private io.nightbeam.studio.xauctions.core.managers.PlayerBlacklistManager playerBlacklistManager;
    private PlatformAdapter platformAdapter;

    // Configuration
    private PluginConfig pluginConfig;

    // Vault economy flag and reference (optional)
    private boolean vaultEnabled = false;
    private Economy vaultEconomy = null;

    // ... [skipping to getters]

    @Override
    public void onEnable() {
        instance = this;
        platformAdapter = new PlatformAdapter(this);
        long startTime = System.currentTimeMillis();

        // Colored ASCII-art startup banner (uses ChatColor constants, not '&' codes)
        String ver = fetchRemoteVersion();
        String server = getServer().getVersion();

        // Normalize version display (ensure single leading 'v')
        String displayVer = (ver != null && ver.startsWith("v")) ? ver : "v" + ver;

        // Try to load ASCII banner from resource to avoid escape issues.
        try (var is = getResource("banner.txt")) {
            if (is != null) {
                var br = new java.io.BufferedReader(
                        new java.io.InputStreamReader(is, java.nio.charset.StandardCharsets.UTF_8));
                String line;
                while ((line = br.readLine()) != null) {
                    String colored = ChatColor.AQUA + line;
                    Bukkit.getConsoleSender().sendMessage(colored); // Log to console
                }
                // Single plain log entry summarizing banner (avoids per-line duplication in
                // server logs)
                getLogger().info("Premium Auction House " + displayVer + " - Running on " + server);
            } else {
                // fallback: simple single-line banner
                String colored = ChatColor.AQUA + "=== xAuctions ===";
                Bukkit.getConsoleSender().sendMessage(colored);
                getLogger().info("Premium Auction House " + displayVer + " - Running on " + server);
            }
        } catch (Exception e) {
            // if resource read fails, fallback gracefully
            String colored = ChatColor.AQUA + "=== xAuctions ===";
            Bukkit.getConsoleSender().sendMessage(colored);
            getLogger().info("xAuctions startup banner displayed (console-only). Version: " + displayVer);
        }

        try {
            // Load configuration
            loadConfiguration();

            // Setup optional Vault economy (safe, non-throwing)
            setupEconomy();

            // Initialize managers in proper order
            initializeManagers();

            // Register commands
            registerCommands();

            // Register listeners
            registerListeners();

            // Load integrations
            loadIntegrations();

            // Check for updates asynchronously (safe, non-blocking)
            checkForUpdates();

            long loadTime = System.currentTimeMillis() - startTime;
            getLogger().info("xAuctions loaded successfully in " + loadTime + "ms!");

        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to initialize xAuctions!", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    /**
     * Asynchronously checks the remote version and logs if an update is available.
     */
    private void checkForUpdates() {
        platformAdapter.runAsync(() -> {
            try {
                String remote = fetchRemoteVersion();
                if (remote == null || remote.isBlank())
                    return;
                String remoteClean = remote.startsWith("v") ? remote.substring(1) : remote;
                String local = getDescription().getVersion();
                if (!remoteClean.equals(local)) {
                    String msg = "A new xAuctions version is available: " + remote + " (installed: v" + local + ").";
                    getLogger().info(msg);
                    // also print a colored console message for visibility
                    Bukkit.getConsoleSender().sendMessage(ChatColor.YELLOW + "[xAuctions] " + msg);
                } else {
                    String msg = "xAuctions is up to date (v" + local + ").";
                    getLogger().info(msg);
                }
            } catch (Throwable t) {
                // ignore failures silently
            }
        });
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
     * Safely attempts to hook into Vault's Economy via ServicesManager.
     * This method never throws; it only sets {@code vaultEnabled} to true
     * when a valid provider is found.
     */
    private void setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            getLogger().warning("Vault not found — disabling Vault economy support.");
            vaultEnabled = false;
            return;
        }

        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);

        if (rsp == null || rsp.getProvider() == null) {
            getLogger().warning(
                    "No economy provider found — install EssentialsX, CMI, or another Vault-compatible economy.");
            vaultEnabled = false;
            return;
        }

        try {
            vaultEconomy = rsp.getProvider();
            vaultEnabled = true;
            getLogger().info("Hooked into economy: " + vaultEconomy.getName());
        } catch (Throwable t) {
            vaultEnabled = false;
            getLogger().warning("Failed to initialize Vault economy provider — disabling Vault economy support.");
        }
    }

    /**
     * Attempts to fetch the public version string from the remote text file.
     * Falls back to the plugin's own version on any error or timeout.
     */
    private String fetchRemoteVersion() {
        String remoteUrl = "https://raw.githubusercontent.com/MeherBenSalem/VersionChecker/main/versions_xAuctions.txt";
        try {
            URL url = new URL(remoteUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(2000);
            conn.setRequestMethod("GET");
            int code = conn.getResponseCode();
            if (code != 200)
                return getDescription().getVersion();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line = br.readLine();
                if (line != null && !line.isBlank()) {
                    return line.trim();
                }
            }
        } catch (Exception ignored) {
            // ignore and fall back
        }
        return getDescription().getVersion();
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

        // 8. Tax Manager
        taxManager = new TaxManager(this);
        taxManager.init();
        debug("TaxManager initialized.");

        // 9. Listing Limit Manager
        listingLimitManager = new ListingLimitManager(this);
        debug("ListingLimitManager initialized.");

        // 10. Item Blacklist Manager
        itemBlacklistManager = new io.nightbeam.studio.xauctions.core.managers.ItemBlacklistManager(this);
        itemBlacklistManager.init();
        debug("ItemBlacklistManager initialized.");

        // 11. Player Blacklist Manager
        playerBlacklistManager = new io.nightbeam.studio.xauctions.core.managers.PlayerBlacklistManager(this);
        playerBlacklistManager.init();
        debug("PlayerBlacklistManager initialized.");
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

        // Register top-level /sell only if enabled in config
        if (pluginConfig.isTopLevelSellEnabled()) {
            var sellCommand = getCommand("sell");
            if (sellCommand != null) {
                sellCommand.setExecutor(new SellCommand(this));
                debug("Command 'sell' registered.");
            } else {
                getLogger().warning("Failed to register 'sell' command - not defined in plugin.yml");
            }
        } else {
            debug("Top-level 'sell' command disabled via config; use '/ah sell <price>' instead.");
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

    public io.nightbeam.studio.xauctions.core.managers.TaxManager getTaxManager() {
        return taxManager;
    }

    public io.nightbeam.studio.xauctions.core.managers.ListingLimitManager getListingLimitManager() {
        return listingLimitManager;
    }

    public io.nightbeam.studio.xauctions.core.managers.ItemBlacklistManager getItemBlacklistManager() {
        return itemBlacklistManager;
    }

    public io.nightbeam.studio.xauctions.core.managers.PlayerBlacklistManager getPlayerBlacklistManager() {
        return playerBlacklistManager;
    }

    public PlatformAdapter getPlatformAdapter() {
        return platformAdapter;
    }
}
