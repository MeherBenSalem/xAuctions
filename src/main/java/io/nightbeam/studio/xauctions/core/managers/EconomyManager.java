package io.nightbeam.studio.xauctions.core.managers;

import io.nightbeam.studio.xauctions.api.economy.EconomyProvider;
import io.nightbeam.studio.xauctions.xAuctions;
import io.nightbeam.studio.xauctions.economy.impl.VaultProvider;
import io.nightbeam.studio.xauctions.economy.impl.PlayerPointsProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class EconomyManager {

    private final xAuctions plugin;
    private final Map<String, EconomyProvider> providers = new HashMap<>();

    public EconomyManager(xAuctions plugin) {
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
    }

    public void registerProvider(EconomyProvider provider) {
        providers.put(provider.getId().toLowerCase(), provider);
    }

    public EconomyProvider getProvider(String id) {
        return providers.get(id.toLowerCase());
    }

    public EconomyProvider getDefaultProvider() {
        // Return Vault if available, otherwise first available
        if (providers.containsKey("vault"))
            return providers.get("vault");
        return providers.values().stream().findFirst().orElse(null);
    }
}
