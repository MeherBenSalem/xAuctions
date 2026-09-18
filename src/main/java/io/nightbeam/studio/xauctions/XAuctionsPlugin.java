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
import io.nightbeam.studio.xauctions.util.ModrinthUpdateChecker;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Bukkit;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;

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
    private io.nightbeam.studio.xauctions.core.managers.PlayerPreferencesManager playerPreferencesManager;
    private PlatformAdapter platformAdapter;

    // Configuration
    private PluginConfig pluginConfig;

    // Vault economy flag and reference (optional)
    private boolean vaultEnabled = false;
    private Economy vaultEconomy = null;
    // track whether we've already warned about missing economy so we don't spam the console
    private boolean vaultCheckWarned = false;

    // ... [skipping to getters]

    @Override
    public void onEnable() {
        instance = this;
        platformAdapter = new PlatformAdapter(this);
        long startTime = System.currentTimeMillis();

        logStartupBanner();

        int step = 1;
        final int totalSteps = 15;

        try {
            // Step 1 — Configuration
            loadConfiguration();
            consoleStep(step++, totalSteps, "Configuration loaded");

            // Step 2 — Economy provider
            setupEconomy();
            // listen for other plugins enabling so we can pick up late-registered
            // Vault economy providers (e.g. DonutCore) and retry hooking.
            Bukkit.getPluginManager().registerEvents(new org.bukkit.event.Listener() {
                @org.bukkit.event.EventHandler
                public void onPluginEnable(org.bukkit.event.server.PluginEnableEvent e) {
                    // small optimization: only retry if Vault isn't hooked yet
                    if (!vaultEnabled) {
                        setupEconomy();
                    }
                    // also attempt to hook any DonutCore economy service when plugins come online
                    hookDonutCoreIfAvailable();
                }
            }, this);

            // schedule a fallback check in case the economy service registers asynchronously (license validation, etc.)
            platformAdapter.runGlobalTaskLater(this::hookDonutCoreIfAvailable, 40L);

            economyManager = new EconomyManager(this);
            economyManager.init();
            // after initializing the manager re-run our Vault check; the manager may have
            // re-registered a provider or DonutCore could have added its service already.
            hookDonutCoreIfAvailable();
            setupEconomy();
            // also schedule a second attempt a few ticks later in case provider registration
            // happens very late (e.g. DonutCore enabling after our listener registration).
            platformAdapter.runGlobalTaskLater(this::setupEconomy, 20L);

            String econDesc;
            var defaultProv = economyManager.getDefaultProvider();
            if (defaultProv != null) {
                econDesc = defaultProv.getId();
                // if we're using Vault we can include the hooked economy name too
                if ("vault".equalsIgnoreCase(econDesc) && vaultEconomy != null) {
                    econDesc = "Vault (" + vaultEconomy.getName() + ")";
                }
            } else {
                econDesc = "no economy provider hooked";
            }
            consoleStep(step++, totalSteps, "Economy provider resolved (" + econDesc + ")");

            // Environment info (server version, Java, integrations)
            logRuntimeInfo();

            // Step 3 — Message Manager
            messageManager = new MessageManager(this);
            consoleStep(step++, totalSteps, "Message manager initialized");

            // Step 4 — Storage provider
            initializeStorage();
            consoleStep(step++, totalSteps, "Storage initialized (" + pluginConfig.getStorageType().toUpperCase() + ")");

            // Step 5 — GUI Manager
            guiManager = new GuiManager(this);
            consoleStep(step++, totalSteps, "GUI system initialized");

            // Step 6 — Auction Manager + Service
            auctionManager = new AuctionManager(this);
            auctionService = new AuctionServiceImpl(this);
            consoleStep(step++, totalSteps, "Auction engine initialized");

            // Step 7 — Anti-Dupe Manager
            antiDupeManager = new AntiDupeManager(this);
            consoleStep(step++, totalSteps, "Anti-dupe protection active");

            // Step 8 — Input Listener
            inputListener = new InputListener(this);
            consoleStep(step++, totalSteps, "Chat input listener ready");

            // Step 9 — Tax Manager
            taxManager = new TaxManager(this);
            taxManager.init();
            consoleStep(step++, totalSteps, "Tax system initialized");

            // Step 10 — Listing Limit Manager
            listingLimitManager = new ListingLimitManager(this);
            consoleStep(step++, totalSteps, "Listing limit manager initialized");

            // Step 11 — Item Blacklist
            itemBlacklistManager = new io.nightbeam.studio.xauctions.core.managers.ItemBlacklistManager(this);
            itemBlacklistManager.init();
            consoleStep(step++, totalSteps, "Item blacklist loaded");

            // Step 12 — Player Blacklist
            playerBlacklistManager = new io.nightbeam.studio.xauctions.core.managers.PlayerBlacklistManager(this);
            playerBlacklistManager.init();
            consoleStep(step++, totalSteps, "Player blacklist loaded");

            // Step 13 — Player Preferences
            playerPreferencesManager = new io.nightbeam.studio.xauctions.core.managers.PlayerPreferencesManager();
            consoleStep(step++, totalSteps, "Player preferences manager initialized");

            // Step 14 — Commands
            registerCommands();
            consoleStep(step++, totalSteps, "Commands registered");

            // Step 15 — Listeners
            registerListeners();
            consoleStep(step++, totalSteps, "Event listeners registered");

            // Optional integrations (PlaceholderAPI, Discord)
            loadIntegrations();

            startMetrics();

            // Async version check (non-blocking)
            new ModrinthUpdateChecker(this).checkAsync();

            long loadTime = System.currentTimeMillis() - startTime;
            consoleSend("§2§l╠═════════════════════════════════════════════════════════════╣");
            consoleSend("§2§l║  §a§l✔ §r§fxAuctions §av" + getDescription().getVersion() + " §floaded in §b" + loadTime + "ms");
            consoleSend("§2§l║  §7  Powered by NightBeam Studio §8— §7premium quality guaranteed");
            consoleSend("§2§l╚═════════════════════════════════════════════════════════════╝");

        } catch (Exception e) {
            consoleSend("§4§l╠═════════════════════════════════════════════════════════════╣");
            consoleSend("§4§l║  §c§l✘ §fFailed to initialize xAuctions! Plugin has been disabled.");
            consoleSend("§4§l╚═════════════════════════════════════════════════════════════╝");
            getLogger().log(Level.SEVERE, "Initialization error:", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    /**
     * Registers anonymous usage metrics with bStats (non-fatal).
     */
    private void startMetrics() {
        try {
            Metrics metrics = new Metrics(this, 34108);
            metrics.addCustomChart(new SimplePie("server_software", () -> Bukkit.getServer().getName()));
        } catch (Exception ex) {
            getLogger().warning("Failed to start bStats metrics: " + ex.getMessage());
        }
    }

    @Override
    public void onDisable() {
        consoleSend("§4§l╔═════════════════════════════════════════════════════════════╗");
        consoleSend("§4§l║  §cShutting down §fxAuctions §8— §7thank you for using NightBeam Studio  §4§l║");
        consoleSend("§4§l╠═════════════════════════════════════════════════════════════╣");

        try {
            if (storageProvider != null) {
                storageProvider.shutdown();
                consoleSend("  §8[--] §a✔ §fStorage connections closed");
            }
        } catch (Exception e) {
            consoleSend("  §8[--] §c✘ §fStorage shutdown error: " + e.getMessage());
            getLogger().severe("Error during shutdown: " + e.getMessage());
        }

        consoleSend("§4§l╚══════════════════════════ §cxAuctions disabled §4§l══════════════╝");
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
            if (!vaultCheckWarned) {
                getLogger().warning(
                        "No economy provider found — install EssentialsX, CMI, or another Vault-compatible economy.");
                vaultCheckWarned = true;
            }
            vaultEnabled = false;
            return;
        }

        try {
            // avoid re-initializing if we already have the same provider
            Economy provider = rsp.getProvider();
            if (vaultEnabled && vaultEconomy != null && vaultEconomy.getName().equals(provider.getName())) {
                // already hooked to this economy
                return;
            }
            vaultEconomy = provider;
            vaultEnabled = true;
            vaultCheckWarned = false;
            getLogger().info("Hooked into economy: " + vaultEconomy.getName());
            // if the economy manager has been created already, register a fresh Vault
            // provider instance so it picks up the newly-available service.
            if (economyManager != null) {
                economyManager.registerProvider(new io.nightbeam.studio.xauctions.economy.impl.VaultProvider());
                getLogger().info("EconomyManager updated with Vault provider.");
            }
        } catch (Throwable t) {
            vaultEnabled = false;
            getLogger().warning("Failed to initialize Vault economy provider — disabling Vault economy support.");
        }
    }

    /**
     * Try to look up a DonutCore economy service and register an adapter provider.
     * This is done via reflection so that xAuctions does not hard-depend on DonutCore.
     */
    private void hookDonutCoreIfAvailable() {
        try {
            Class<?> donutClass = Class.forName("io.nightbeam.donutcore.modules.economy.EconomyService");
            RegisteredServiceProvider<?> reg = getServer().getServicesManager().getRegistration(donutClass);
            if (reg != null && reg.getProvider() != null && economyManager != null) {
                Object donutSvc = reg.getProvider();
                try {
                    economyManager.registerProvider(
                            new io.nightbeam.studio.xauctions.economy.impl.DonutCoreProvider(donutSvc));
                    getLogger().info("EconomyManager updated with DonutCore provider.");
                } catch (ReflectiveOperationException ex) {
                    getLogger().warning("Failed to register DonutCore economy provider: " + ex.getMessage());
                }
            }
        } catch (ClassNotFoundException ignored) {
            // DonutCore not installed, ignore
        }
    }

    /**
     * Premium console startup banner.
     */
    private void logStartupBanner() {
        consoleSend("");
        consoleSend("§6  __  __    _             _   _                 ");
        consoleSend("§6  \\ \\/ /   / \\  _   _  ___| |_(_) ___  _ __  ___ ");
        consoleSend("§6   \\  /   / _ \\| | | |/ __| __| |/ _ \\| '_ \\/ __|");
        consoleSend("§6   /  \\  / ___ \\ |_| | (__| |_| | (_) | | | \\__ \\");
        consoleSend("§6  /_/\\_\\/_/   \\_\\__,_|\\___|\\__|_|\\___/|_| |_|___/");
        consoleSend("§e         Premium Auction House  §8|  §fv" + getDescription().getVersion() + "  §8|  §7NightBeam Studio");
        consoleSend("");
        consoleSend("§2§l╔═════════════════════════════════════════════════════════════╗");
        consoleSend("§2§l║            §a§l  xAuctions §2§l— Initializing...                 ║");
        consoleSend("§2§l╠═════════════════════════════════════════════════════════════╣");
    }

    /**
     * Prints environment info (server version, Java, active integrations) to console.
     */
    private void logRuntimeInfo() {
        String serverVer = Bukkit.getBukkitVersion();
        String javaVer   = System.getProperty("java.version");
        boolean hasPAPI  = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
        boolean hasVault = Bukkit.getPluginManager().getPlugin("Vault") != null;
        String storage   = pluginConfig.getStorageType().toUpperCase();
        consoleSend("§8  Platform : §fMC " + serverVer + "   §8Java: §f" + javaVer);
        consoleSend("§8  Hooks    : §fVault=" + hasVault + "  PlaceholderAPI=" + hasPAPI + "  Storage=" + storage);
    }

    /**
     * Sends a raw console message (supports § color codes).
     */
    private void consoleSend(String message) {
        Bukkit.getConsoleSender().sendMessage(message);
    }

    /**
     * Sends a formatted step line: {@code  [X/Y] ✔ Description}.
     */
    private void consoleStep(int step, int total, String description) {
        consoleSend(String.format("  §8[§f%2d§8/§f%2d§8] §a✔ §f%s", step, total, description));
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
     * Loads optional plugin integrations (PlaceholderAPI, Discord webhook).
     */
    private void loadIntegrations() {
        // PlaceholderAPI
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new xAuctionsExpansion(this).register();
            consoleSend("  §8[opt] §a✔ §fPlaceholderAPI expansion registered");
        } else {
            consoleSend("  §8[opt] §e→ §fPlaceholderAPI not found (expansion skipped)");
        }

        // Discord Webhook
        String webhookUrl = pluginConfig.getDiscordWebhookUrl();
        if (webhookUrl != null && !webhookUrl.isEmpty()) {
            discordWebhook = new DiscordWebhook(webhookUrl);
            consoleSend("  §8[opt] §a✔ §fDiscord webhook integration enabled");
        } else {
            consoleSend("  §8[opt] §e→ §fDiscord webhook not configured (skipped)");
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

    public io.nightbeam.studio.xauctions.core.managers.PlayerPreferencesManager getPlayerPreferencesManager() {
        return playerPreferencesManager;
    }

    public PlatformAdapter getPlatformAdapter() {
        return platformAdapter;
    }
}
