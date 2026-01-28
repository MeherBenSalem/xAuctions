package io.nightbeam.studio.xauctions.gui.menus;

import io.nightbeam.studio.xauctions.XAuctionsPlugin;
import io.nightbeam.studio.xauctions.gui.framework.AuctionFilter;
import io.nightbeam.studio.xauctions.gui.framework.BaseMenu;
import io.nightbeam.studio.xauctions.gui.framework.MenuItem;
import io.nightbeam.studio.xauctions.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class CategoryMenu extends BaseMenu {

    public CategoryMenu(XAuctionsPlugin plugin) {
        super(plugin, "Categories", 27);
    }

    @Override
    public void onOpen(Player player) {
        fillBorders(ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name(" ").build());

        // Helper to add filter button
        addFilterButton(10, AuctionFilter.BLOCKS);
        addFilterButton(12, AuctionFilter.WEAPONS);
        addFilterButton(14, AuctionFilter.CONSUMABLES);
        addFilterButton(16, AuctionFilter.MISC);

        // Back
        setItem(22, new MenuItem(ItemBuilder.from(Material.ARROW).name("§cBack").build(), event -> {
            plugin.getGuiManager().openMenu(player, new MainMenu(plugin));
        }));
    }

    private void addFilterButton(int slot, AuctionFilter filter) {
        ItemStack item = ItemBuilder.from(filter.getIcon())
                .name("§a" + filter.getDisplayName())
                .lore("§7Click to view " + filter.getDisplayName())
                .build();

        setItem(slot, new MenuItem(item, event -> {
            plugin.getGuiManager().openMenu((Player) event.getWhoClicked(), new AuctionsMenu(plugin, null, filter));
        }));
    }

    @Override
    public void onClose(Player player) {
    }
}
