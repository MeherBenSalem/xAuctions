package io.nightbeam.studio.xauctions.core.managers;

import io.nightbeam.studio.xauctions.XAuctionsPlugin;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class TaxManager {

    private final XAuctionsPlugin plugin;
    private double globalTaxRate = 0.0;
    private final Map<Material, Double> itemTaxRates = new HashMap<>();

    public TaxManager(XAuctionsPlugin plugin) {
        this.plugin = plugin;
    }

    public void init() {
        loadConfig();
    }

    public void loadConfig() {
        // Load from config.yml
        // Assuming structure:
        // tax:
        // global: 5.0 # Percent
        // items:
        // DIAMOND_SWORD: 10.0

        globalTaxRate = plugin.getConfig().getDouble("tax.global", 0.0);
        itemTaxRates.clear();

        ConfigurationSection itemsSection = plugin.getConfig().getConfigurationSection("tax.items");
        if (itemsSection != null) {
            for (String key : itemsSection.getKeys(false)) {
                try {
                    Material mat = Material.valueOf(key.toUpperCase());
                    double rate = itemsSection.getDouble(key);
                    itemTaxRates.put(mat, rate);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid material in tax config: " + key);
                }
            }
        }
    }

    public double calculateTax(double price, ItemStack item) {
        double rate = globalTaxRate;

        if (item != null && itemTaxRates.containsKey(item.getType())) {
            rate = Math.max(rate, itemTaxRates.get(item.getType())); // Use higher tax? Or specific overrides global?
            // Let's say specific overrides global if present.
            rate = itemTaxRates.get(item.getType());
        }

        // Rate is percentage (e.g. 5.0 for 5%)
        return (price * rate) / 100.0;
    }

    public double getTaxRate(ItemStack item) {
        if (item != null && itemTaxRates.containsKey(item.getType())) {
            return itemTaxRates.get(item.getType());
        }
        return globalTaxRate;
    }
}
