package io.nightbeam.studio.xauctions;

import org.bukkit.plugin.java.JavaPlugin;

public class xAuctions extends JavaPlugin {

    private static xAuctions instance;

    @Override
    public void onEnable() {
        instance = this;

        getLogger().info("xAuctions (Nightbeam Studio) is loading...");

        // Initialize managers
        loadManagers();

        // Commands
        getCommand("ah").setExecutor(new io.nightbeam.studio.xauctions.core.commands.AuctionsCommand(this));
        // Actually, we should register "sell" if we want /ah sell to work or handle
        // args in main command
        // For simplicity let's register /sell separately if desired, or handle in
        // AuctionsCommand
        // But the previous step added SellCommand class. Let's register it as a
        // sub-command logic or separate.
        // User requested /ah sell <price>, so we need to handle arguments in
        // AuctionsCommand or register a sophisticated command handler.
        // For now, let's route /ah sell in AuctionsCommand? No wait, I created
        // SellCommand separately.
        // I'll register it as "sell" command for now or fix plugin.yml to have "sell"
        getCommand("sell").setExecutor(new io.nightbeam.studio.xauctions.core.commands.SellCommand(this));

        getLogger().info("xAuctions loaded successfully!");
    }

    @Override
    public void onDisable() {
        getLogger().info("xAuctions is saving data...");

        // Save logic

        getLogger().info("xAuctions disabled.");
    }

    public static xAuctions getInstance() {
        return instance;
    }

    private io.nightbeam.studio.xauctions.core.managers.EconomyManager economyManager;
    private io.nightbeam.studio.xauctions.gui.GuiManager guiManager;
    private io.nightbeam.studio.xauctions.api.storage.StorageProvider storageProvider;
    private io.nightbeam.studio.xauctions.core.managers.AuctionManager auctionManager;
    private io.nightbeam.studio.xauctions.security.AntiDupeManager antiDupeManager;
    private io.nightbeam.studio.xauctions.core.listeners.InputListener inputListener;
    private io.nightbeam.studio.xauctions.addons.DiscordWebhook discordWebhook;
    private io.nightbeam.studio.xauctions.core.managers.MessageManager messageManager;

    private void loadManagers() {
        // Config
        saveDefaultConfig();

        // Messages
        messageManager = new io.nightbeam.studio.xauctions.core.managers.MessageManager(this);

        // Economy
        economyManager = new io.nightbeam.studio.xauctions.core.managers.EconomyManager(this);
        economyManager.init();

        // Storage (TODO: Load config to decide type)
        storageProvider = new io.nightbeam.studio.xauctions.storage.json.JsonStorageProvider(this);
        storageProvider.init();

        // Auction Logic
        auctionManager = new io.nightbeam.studio.xauctions.core.managers.AuctionManager(this);

        // Listeners
        new io.nightbeam.studio.xauctions.core.listeners.PlayerConnectionListener(this);
        getServer().getPluginManager()
                .registerEvents(new io.nightbeam.studio.xauctions.core.listeners.PlayerConnectionListener(this), this);

        inputListener = new io.nightbeam.studio.xauctions.core.listeners.InputListener(this);
        getServer().getPluginManager().registerEvents(inputListener, this);

        // AntiDupe
        antiDupeManager = new io.nightbeam.studio.xauctions.security.AntiDupeManager(this);

        // Addons
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new io.nightbeam.studio.xauctions.addons.xAuctionsExpansion(this).register();
            getLogger().info("PlaceholderAPI expansion registered.");
        }

        // Discord
        String webhookUrl = getConfig().getString("discord-webhook-url", "");
        if (!webhookUrl.isEmpty()) {
            discordWebhook = new io.nightbeam.studio.xauctions.addons.DiscordWebhook(webhookUrl);
            getLogger().info("Discord integration enabled.");
        }
    }

    public io.nightbeam.studio.xauctions.core.managers.EconomyManager getEconomyManager() {
        return economyManager;
    }

    public io.nightbeam.studio.xauctions.gui.GuiManager getGuiManager() {
        return guiManager;
    }

    public io.nightbeam.studio.xauctions.api.storage.StorageProvider getStorageProvider() {
        return storageProvider;
    }

    public io.nightbeam.studio.xauctions.core.managers.AuctionManager getAuctionManager() {
        return auctionManager;
    }

    public io.nightbeam.studio.xauctions.security.AntiDupeManager getAntiDupeManager() {
        return antiDupeManager;
    }

    public io.nightbeam.studio.xauctions.core.listeners.InputListener getInputListener() {
        return inputListener;
    }

    public io.nightbeam.studio.xauctions.addons.DiscordWebhook getDiscordWebhook() {
        return discordWebhook;
    }

    public io.nightbeam.studio.xauctions.core.managers.MessageManager getMessageManager() {
        return messageManager;
    }
}
