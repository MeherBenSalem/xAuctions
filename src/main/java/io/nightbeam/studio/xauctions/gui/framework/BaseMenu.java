package io.nightbeam.studio.xauctions.gui.framework;

import io.nightbeam.studio.xauctions.XAuctionsPlugin;
import io.nightbeam.studio.xauctions.gui.AbstractMenu;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * Enhanced base menu class with action handling.
 */
public abstract class BaseMenu extends AbstractMenu {

    protected final Map<Integer, MenuItem> menuItems = new HashMap<>();

    public BaseMenu(XAuctionsPlugin plugin, String title, int size) {
        super(plugin, title, size);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        // Handle action if item exists
        int slot = event.getSlot();
        if (menuItems.containsKey(slot)) {
            MenuItem menuItem = menuItems.get(slot);
            if (menuItem.hasAction()) {
                // Play sound
                if (plugin.getPluginConfig().isGuiSoundsEnabled() && event.getWhoClicked() instanceof Player player) {
                    try {
                        String soundName = plugin.getPluginConfig().getSound("click");
                        player.playSound(player.getLocation(), Sound.valueOf(soundName), 1.0f, 1.0f);
                    } catch (IllegalArgumentException ignored) {
                        // Invalid sound
                    }
                }

                menuItem.getAction().execute(event);
            }
        }
    }

    protected void setItem(int slot, MenuItem menuItem) {
        this.inventory.setItem(slot, menuItem.getItemStack());
        this.menuItems.put(slot, menuItem);
    }

    @Override
    protected void setItem(int slot, ItemStack item) {
        setItem(slot, new MenuItem(item));
    }

    protected void setItem(int slot, ItemStack item, MenuAction action) {
        setItem(slot, new MenuItem(item, action));
    }

    protected void fillBorders(ItemStack glass) {
        // Top
        for (int i = 0; i < 9; i++) {
            if (inventory.getItem(i) == null)
                setItem(i, glass);
        }
        // Bottom
        for (int i = size - 9; i < size; i++) {
            if (inventory.getItem(i) == null)
                setItem(i, glass);
        }
        // Sides
        for (int i = 9; i < size - 9; i += 9) {
            if (inventory.getItem(i) == null)
                setItem(i, glass);
            if (inventory.getItem(i + 8) == null)
                setItem(i + 8, glass);
        }
    }
}
