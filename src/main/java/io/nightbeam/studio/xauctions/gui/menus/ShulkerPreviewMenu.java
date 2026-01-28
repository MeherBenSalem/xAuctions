package io.nightbeam.studio.xauctions.gui.menus;

import io.nightbeam.studio.xauctions.XAuctionsPlugin;
import io.nightbeam.studio.xauctions.gui.AbstractMenu;
import io.nightbeam.studio.xauctions.gui.framework.MenuItem;
import io.nightbeam.studio.xauctions.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;

import java.util.HashMap;
import java.util.Map;

public class ShulkerPreviewMenu extends AbstractMenu {

    private final ItemStack shulkerItem;
    private final AbstractMenu previousMenu;
    private final Map<Integer, MenuItem> menuItems = new HashMap<>();

    public ShulkerPreviewMenu(XAuctionsPlugin plugin, ItemStack shulkerItem, AbstractMenu previousMenu) {
        super(plugin, "Shulker Preview", 45); // 27 contents + padding + back button
        this.shulkerItem = shulkerItem;
        this.previousMenu = previousMenu;
    }

    @Override
    public void update() {
        inventory.clear();
        menuItems.clear();

        if (shulkerItem.getItemMeta() instanceof BlockStateMeta bsm) {
            if (bsm.getBlockState() instanceof ShulkerBox shulker) {
                ItemStack[] contents = shulker.getInventory().getContents();

                // Slots 0-26 are contents
                // We display them 1:1
                for (int i = 0; i < contents.length; i++) {
                    if (contents[i] != null && !contents[i].getType().isAir()) {
                        // Read-only item
                        setItem(i, new MenuItem(contents[i].clone(), e -> {
                            // Cancelled by default
                        }));
                    }
                }
            }
        }

        // Back Button at bottom center (slot 40)
        setItem(40, new MenuItem(ItemBuilder.from(Material.BARRIER).name("§cBack to Auction").build(), e -> {
            if (previousMenu != null) {
                plugin.getGuiManager().openMenu((Player) e.getWhoClicked(), previousMenu);
            } else {
                e.getWhoClicked().closeInventory();
            }
        }));
    }

    public void setItem(int slot, MenuItem item) {
        inventory.setItem(slot, item.getItemStack());
        menuItems.put(slot, item);
    }

    @Override
    public void onOpen(Player player) {
        update();
    }

    @Override
    public void onClose(Player player) {
        // No cleanup needed
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        // Handle menu item clicks
        if (menuItems.containsKey(event.getSlot())) {
            MenuItem item = menuItems.get(event.getSlot());
            if (item.hasAction()) {
                item.getAction().execute(event);
            }
        }
    }
}
