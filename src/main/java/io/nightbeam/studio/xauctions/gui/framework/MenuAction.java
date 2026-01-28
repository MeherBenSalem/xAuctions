package io.nightbeam.studio.xauctions.gui.framework;

import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * Functional interface for menu actions.
 */
@FunctionalInterface
public interface MenuAction {
    void execute(InventoryClickEvent event);
}
