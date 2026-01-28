package io.nightbeam.studio.xauctions.config.menu;

import org.bukkit.Material;
import java.util.List;

/**
 * Represents a single item in a menu configuration.
 */
public class MenuItemConfig {
    private final Material material;
    private final String name;
    private final List<String> lore;
    private final int customModelData;
    private final String action; // e.g. "OPEN:category", "CLOSE", "NEXT_PAGE"
    private final String actionValue;

    public MenuItemConfig(Material material, String name, List<String> lore, int customModelData, String action,
            String actionValue) {
        this.material = material;
        this.name = name;
        this.lore = lore;
        this.customModelData = customModelData;
        this.action = action;
        this.actionValue = actionValue;
    }

    public Material getMaterial() {
        return material;
    }

    public String getName() {
        return name;
    }

    public List<String> getLore() {
        return lore;
    }

    public int getCustomModelData() {
        return customModelData;
    }

    public String getAction() {
        return action;
    }

    public String getActionValue() {
        return actionValue;
    }
}
