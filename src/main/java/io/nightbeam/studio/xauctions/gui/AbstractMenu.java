package io.nightbeam.studio.xauctions.gui;

import io.nightbeam.studio.xauctions.xAuctions;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractMenu implements InventoryHolder {

    protected xAuctions plugin;
    protected Inventory inventory;
    protected String title;
    protected int size;

    public AbstractMenu(xAuctions plugin, String title, int size) {
        this.plugin = plugin;
        this.title = title;
        this.size = size;
        this.inventory = Bukkit.createInventory(this, size, title); // Use adventure components in future
    }

    public abstract void onOpen(Player player);

    public abstract void onClose(Player player);

    public abstract void handleClick(InventoryClickEvent event);

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    protected void setItem(int slot, ItemStack item) {
        inventory.setItem(slot, item);
    }
}
