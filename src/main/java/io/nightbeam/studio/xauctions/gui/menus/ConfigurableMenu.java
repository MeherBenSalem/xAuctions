package io.nightbeam.studio.xauctions.gui.menus;

import io.nightbeam.studio.xauctions.XAuctionsPlugin;
import io.nightbeam.studio.xauctions.config.menu.MenuConfig;
import io.nightbeam.studio.xauctions.config.menu.MenuItemConfig;
import io.nightbeam.studio.xauctions.gui.framework.BaseMenu;
import io.nightbeam.studio.xauctions.gui.framework.MenuItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A menu built entirely from a MenuConfig layout.
 */
public class ConfigurableMenu extends BaseMenu {

    private final MenuConfig config;

    public ConfigurableMenu(XAuctionsPlugin plugin, MenuConfig config) {
        super(plugin, config.getTitle(), config.getSize());
        this.config = config;
    }

    @Override
    public void onOpen(Player player) {
        update();
    }

    @Override
    public void onClose(Player player) {
    }

    @Override
    public void update() {
        for (Map.Entry<Integer, MenuItemConfig> entry : config.getItems().entrySet()) {
            int slot = entry.getKey();
            MenuItemConfig itemConfig = entry.getValue();

            ItemStack item = new ItemStack(itemConfig.getMaterial());
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                if (itemConfig.getName() != null) {
                    meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', itemConfig.getName()));
                }
                if (itemConfig.getLore() != null) {
                    List<String> coloredLore = new ArrayList<>();
                    for (String line : itemConfig.getLore()) {
                        coloredLore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', line));
                    }
                    meta.setLore(coloredLore);
                }
                if (itemConfig.getCustomModelData() != 0) {
                    meta.setCustomModelData(itemConfig.getCustomModelData());
                }
                item.setItemMeta(meta);
            }

            MenuItem menuItem;
            if (!"NONE".equalsIgnoreCase(itemConfig.getAction())) {
                menuItem = new MenuItem(item, event -> handleAction((Player) event.getWhoClicked(),
                        itemConfig.getAction(), itemConfig.getActionValue()));
            } else {
                menuItem = new MenuItem(item);
            }

            setItem(slot, menuItem);
        }
    }

    private void handleAction(Player player, String action, String value) {
        switch (action.toUpperCase()) {
            case "OPEN_MENU":
                if ("categories".equalsIgnoreCase(value)) {
                    new CategoryMenu(plugin).open(player);
                } else if ("my_auctions".equalsIgnoreCase(value)) {
                    // new AuctionHistoryMenu(plugin)... // Use standard open logic if available
                } else {
                    MenuConfig targetConfig = plugin.getGuiManager().getLayoutLoader().getLayout(value);
                    if (targetConfig != null) {
                        new ConfigurableMenu(plugin, targetConfig).open(player);
                    }
                }
                break;
            case "MESSAGE":
                player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', value));
                break;
            case "CLOSE":
                player.closeInventory();
                break;
        }
    }
}
