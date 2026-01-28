package io.nightbeam.studio.xauctions.gui;

import io.nightbeam.studio.xauctions.XAuctionsPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GuiManager implements Listener {

    private final XAuctionsPlugin plugin;
    private final Map<UUID, AbstractMenu> openMenus = new HashMap<>();
    private final io.nightbeam.studio.xauctions.config.menu.LayoutLoader layoutLoader;

    public GuiManager(XAuctionsPlugin plugin) {
        this.plugin = plugin;
        this.layoutLoader = new io.nightbeam.studio.xauctions.config.menu.LayoutLoader(plugin);
        this.layoutLoader.loadLayouts();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public io.nightbeam.studio.xauctions.config.menu.LayoutLoader getLayoutLoader() {
        return layoutLoader;
    }

    public void openMenu(Player player, AbstractMenu menu) {
        // Close previous if needed, typically setInventory handles it
        player.openInventory(menu.getInventory());
        openMenus.put(player.getUniqueId(), menu);
        menu.onOpen(player);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player))
            return;

        // Check if the inventory holder is one of ours
        if (event.getInventory().getHolder() instanceof AbstractMenu menu) {
            event.setCancelled(true); // Default cancel
            menu.handleClick(event);
        }
    }

    @EventHandler
    public void onDrag(org.bukkit.event.inventory.InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof AbstractMenu) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            AbstractMenu menu = openMenus.remove(player.getUniqueId());
            if (menu != null) {
                menu.onClose(player);
            }
        }
    }

    public void refreshMenu(Player player) {
        AbstractMenu menu = openMenus.get(player.getUniqueId());
        if (menu != null) {
            menu.update();
        }
    }
}
