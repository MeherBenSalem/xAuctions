package io.nightbeam.studio.xauctions.core.managers;

import io.nightbeam.studio.xauctions.XAuctionsPlugin;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ItemBlacklistManager {

    private final XAuctionsPlugin plugin;
    private final Set<Material> blacklistedMaterials = new HashSet<>();
    private final Set<String> blacklistedNames = new HashSet<>(); // Regex?

    public ItemBlacklistManager(XAuctionsPlugin plugin) {
        this.plugin = plugin;
    }

    public void init() {
        loadConfig();
    }

    public void loadConfig() {
        blacklistedMaterials.clear();
        blacklistedNames.clear();

        List<String> materials = plugin.getConfig().getStringList("blacklist.materials");
        for (String s : materials) {
            Material mat = Material.matchMaterial(s);
            if (mat != null)
                blacklistedMaterials.add(mat);
        }

        List<String> names = plugin.getConfig().getStringList("blacklist.names");
        blacklistedNames.addAll(names);
    }

    public boolean isBlacklisted(ItemStack item) {
        if (item == null)
            return false;

        if (blacklistedMaterials.contains(item.getType()))
            return true;

        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            String name = item.getItemMeta().getDisplayName();
            for (String regex : blacklistedNames) {
                if (name.matches(regex))
                    return true;
            }
        }

        return false;
    }
}
