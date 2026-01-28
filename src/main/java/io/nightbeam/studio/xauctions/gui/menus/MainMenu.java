package io.nightbeam.studio.xauctions.gui.menus;

import io.nightbeam.studio.xauctions.XAuctionsPlugin;
import io.nightbeam.studio.xauctions.gui.framework.BaseMenu;
import io.nightbeam.studio.xauctions.gui.framework.MenuItem;
import io.nightbeam.studio.xauctions.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class MainMenu extends BaseMenu {

    public MainMenu(XAuctionsPlugin plugin) {
        super(plugin, "Auction House", 54);
    }

    @Override
    public void onOpen(Player player) {
        // Borders
        fillBorders(ItemBuilder.from(Material.BLACK_STAINED_GLASS_PANE).name(" ").build());

        // Browse Button (20)
        setItem(20, new MenuItem(ItemBuilder.from(Material.GOLD_INGOT)
                .name("§eBrowse Auctions")
                .lore("§7Click to view all active auctions")
                .build(), event -> {
                    plugin.getGuiManager().openMenu(player, new AuctionsMenu(plugin));
                }));

        // Search Button (22)
        setItem(22, new MenuItem(ItemBuilder.from(Material.COMPASS)
                .name("§aSearch")
                .lore("§aLeft-Click §7to search items", "§aRight-Click §7to clear search")
                .build(), event -> {
                    if (event.isRightClick()) {
                        plugin.getGuiManager().openMenu(player, new AuctionsMenu(plugin, null));
                    } else {
                        plugin.getInputListener().awaitSearch(player);
                    }
                }));

        // My Auctions (24)
        setItem(24, new MenuItem(ItemBuilder.from(Material.ENDER_CHEST)
                .name("§bMy Auctions")
                .lore("§7Manage your listings", "§7Collect earnings/items")
                .build(), event -> {
                    plugin.getGuiManager().openMenu(player, new PlayerSellingMenu(plugin, player, player));
                }));

        // Categories (31) - Keep for now, but link properly or remove if user prefers
        // direct.
        // User's image didn't show main menu, but let's keep it consistent.
        setItem(31, new MenuItem(ItemBuilder.from(Material.CHEST)
                .name("§6Categories")
                .lore("§7Browse by category")
                .build(), event -> {
                    plugin.getGuiManager().openMenu(player, new CategoryMenu(plugin));
                }));
    }

    @Override
    public void onClose(Player player) {
    }
}
