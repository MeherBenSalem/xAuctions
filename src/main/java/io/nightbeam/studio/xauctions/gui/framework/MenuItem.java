package io.nightbeam.studio.xauctions.gui.framework;

import org.bukkit.inventory.ItemStack;

/**
 * Represents an item in a menu with an associated action.
 */
public class MenuItem {

    private final ItemStack itemStack;
    private final MenuAction action;

    public MenuItem(ItemStack itemStack, MenuAction action) {
        this.itemStack = itemStack;
        this.action = action;
    }

    public MenuItem(ItemStack itemStack) {
        this(itemStack, null);
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public MenuAction getAction() {
        return action;
    }

    public boolean hasAction() {
        return action != null;
    }
}
