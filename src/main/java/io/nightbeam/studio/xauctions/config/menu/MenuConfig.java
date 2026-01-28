package io.nightbeam.studio.xauctions.config.menu;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a loaded menu configuration.
 */
public class MenuConfig {
    private final String title;
    private final int size;
    private final Map<Integer, MenuItemConfig> items;
    private final String layoutType; // e.g. "MAIN", "CATEGORY", "AUCTIONS"

    public MenuConfig(String title, int size, String layoutType) {
        this.title = title;
        this.size = size;
        this.items = new HashMap<>();
        this.layoutType = layoutType;
    }

    public void addItem(int slot, MenuItemConfig item) {
        items.put(slot, item);
    }

    public MenuItemConfig getItem(int slot) {
        return items.get(slot);
    }

    public String getTitle() {
        return title;
    }

    public int getSize() {
        return size;
    }

    public Map<Integer, MenuItemConfig> getItems() {
        return items;
    }

    public String getLayoutType() {
        return layoutType;
    }
}
