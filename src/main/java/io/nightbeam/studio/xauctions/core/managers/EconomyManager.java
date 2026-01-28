package io.nightbeam.studio.xauctions.core.managers;

import io.nightbeam.studio.xauctions.api.economy.EconomyProvider;
import io.nightbeam.studio.xauctions.XAuctionsPlugin;
import io.nightbeam.studio.xauctions.economy.impl.VaultProvider;
import io.nightbeam.studio.xauctions.economy.impl.PlayerPointsProvider;

import io.nightbeam.studio.xauctions.economy.impl.LevelEconomyProvider;
import io.nightbeam.studio.xauctions.economy.impl.ItemEconomyProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import io.nightbeam.studio.xauctions.api.service.EconomyService;
import org.bukkit.OfflinePlayer;

public class EconomyManager implements EconomyService {

    private final XAuctionsPlugin plugin;
    private final Map<String, EconomyProvider> providers = new HashMap<>();

    public EconomyManager(XAuctionsPlugin plugin) {
        this.plugin = plugin;
    }

    public void init() {
        // Try to load Vault
        if (plugin.getServer().getPluginManager().getPlugin("Vault") != null) {
            try {
                registerProvider(new VaultProvider());
                plugin.getLogger().info("Woohoo! Vault found and hooked.");
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to hook Vault", e);
            }
        }

        // Try to load PlayerPoints
        if (plugin.getServer().getPluginManager().getPlugin("PlayerPoints") != null) {
            try {
                registerProvider(new PlayerPointsProvider());
                plugin.getLogger().info("Woohoo! PlayerPoints found and hooked.");
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to hook PlayerPoints", e);
            }
        }

        // Register Internal Providers
        registerProvider(new LevelEconomyProvider());
        // Example: Register Emerald as currency. In real config, this would be dynamic.
        registerProvider(new ItemEconomyProvider(org.bukkit.Material.EMERALD, "Emeralds"));
    }

    public void registerProvider(EconomyProvider provider) {
        providers.put(provider.getId().toLowerCase(), provider);
    }

    public EconomyProvider getProvider(String id) {
        return providers.get(id.toLowerCase());
    }

    public EconomyProvider getDefaultProvider() {
        // Get configured provider from PluginConfig
        String providerId = plugin.getPluginConfig().getEconomyProvider();
        if (providers.containsKey(providerId)) {
            return providers.get(providerId);
        }

        // Return Vault if available, otherwise first available
        if (providers.containsKey("vault"))
            return providers.get("vault");
        return providers.values().stream().findFirst().orElse(null);
    }

    // ----------------------------------------------------------------
    // EconomyService Implementation
    // ----------------------------------------------------------------

    @Override
    public boolean withdraw(OfflinePlayer player, double amount) {
        EconomyProvider provider = getDefaultProvider();
        if (provider == null)
            return false;
        return provider.withdraw(player, amount);
    }

    @Override
    public boolean deposit(OfflinePlayer player, double amount) {
        EconomyProvider provider = getDefaultProvider();
        if (provider == null)
            return false;
        return provider.deposit(player, amount);
    }

    @Override
    public boolean has(OfflinePlayer player, double amount) {
        EconomyProvider provider = getDefaultProvider();
        if (provider == null)
            return false;
        return provider.has(player, amount);
    }

    @Override
    public String format(double amount) {
        EconomyProvider provider = getDefaultProvider();
        if (provider == null)
            return String.valueOf(amount);
        return provider.format(amount);
    }
}
