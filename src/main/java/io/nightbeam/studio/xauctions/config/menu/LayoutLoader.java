package io.nightbeam.studio.xauctions.config.menu;

import io.nightbeam.studio.xauctions.XAuctionsPlugin;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class LayoutLoader {

    private final XAuctionsPlugin plugin;
    private final Map<String, MenuConfig> layouts = new HashMap<>();
    private final File menuDir;

    public LayoutLoader(XAuctionsPlugin plugin) {
        this.plugin = plugin;
        this.menuDir = new File(plugin.getDataFolder(), "menus");
    }

    public void loadLayouts() {
        if (!menuDir.exists()) {
            menuDir.mkdirs();
            // Create default menus if needed
            // saveDefaultMenu("main_menu.yml");
            // saveDefaultMenu("category_menu.yml");
        }

        layouts.clear();
        File[] files = menuDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null)
            return;

        for (File file : files) {
            try {
                loadLayout(file);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to load menu layout: " + file.getName(), e);
            }
        }
    }

    private void loadLayout(File file) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);

        String title = yaml.getString("title", "Menu");
        int size = yaml.getInt("size", 27);
        String type = yaml.getString("type", "CUSTOM"); // MAIN, AUCTIONS, etc.

        MenuConfig config = new MenuConfig(title, size, type);

        ConfigurationSection itemsSec = yaml.getConfigurationSection("items");
        if (itemsSec != null) {
            for (String key : itemsSec.getKeys(false)) {
                ConfigurationSection itemSec = itemsSec.getConfigurationSection(key);
                if (itemSec == null)
                    continue;

                // Slot parsing
                List<Integer> slots = new ArrayList<>();
                if (itemSec.isInt("slot")) {
                    slots.add(itemSec.getInt("slot"));
                } else if (itemSec.isList("slots")) {
                    slots.addAll(itemSec.getIntegerList("slots"));
                }

                // Item parsing
                String matName = itemSec.getString("material", "STONE");
                Material mat = Material.matchMaterial(matName);
                if (mat == null)
                    mat = Material.STONE;

                String name = itemSec.getString("name", " ");
                List<String> lore = itemSec.getStringList("lore");
                int modelData = itemSec.getInt("custom_model_data", 0);

                String action = itemSec.getString("action", "NONE");
                String actionValue = itemSec.getString("action_value", "");

                MenuItemConfig itemConfig = new MenuItemConfig(mat, name, lore, modelData, action, actionValue);

                for (int slot : slots) {
                    config.addItem(slot, itemConfig);
                }
            }
        }

        layouts.put(type.toUpperCase(), config); // Key by TYPE or Filename?
        // If we want multiple layouts per type, we might need a mapping.
        // For now, let's assume Type is unique key for determining which menu to use
        // specific structure.
        // OR we use filename without extension as key.
        String id = file.getName().replace(".yml", "");
        layouts.put(id, config);

        // Also map by type if it's a known internal type
        if (!type.equals("CUSTOM")) {
            layouts.put(type.toUpperCase(), config);
        }
    }

    public MenuConfig getLayout(String id) {
        return layouts.get(id);
    }
}
