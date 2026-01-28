package io.nightbeam.studio.xauctions.gui.menus;

import io.nightbeam.studio.xauctions.xAuctions;
import io.nightbeam.studio.xauctions.gui.AbstractMenu;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class MainMenu extends AbstractMenu {

    public MainMenu(xAuctions plugin) {
        super(plugin, "Auction House", 54); // TODO: Load from config
    }

    @Override
    public void onOpen(Player player) {
        // Initialize Default Items (Borders)
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        meta.setDisplayName(" ");
        filler.setItemMeta(meta);

        for (int i = 0; i < 54; i++) {
            if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) {
                setItem(i, filler);
            }
        }

        // Browse Button
        ItemStack browse = new ItemStack(Material.GOLD_INGOT);
        ItemMeta bMeta = browse.getItemMeta();
        bMeta.setDisplayName("§eBrowse Auctions");
        bMeta.setLore(Arrays.asList("§7Click to view all active auctions"));
        browse.setItemMeta(bMeta);
        setItem(20, browse);

        // Search Button
        ItemStack search = new ItemStack(Material.COMPASS);
        ItemMeta sMeta = search.getItemMeta();
        sMeta.setDisplayName("§aSearch");
        sMeta.setLore(Arrays.asList("§7Click to search for items"));
        search.setItemMeta(sMeta);
        setItem(22, search);

        // My Auctions
        ItemStack myAuctions = new ItemStack(Material.ENDER_CHEST);
        ItemMeta mMeta = myAuctions.getItemMeta();
        mMeta.setDisplayName("§bMy Auctions");
        mMeta.setLore(Arrays.asList("§7Manage your listings", "§7Collect earnings/items"));
        myAuctions.setItemMeta(mMeta);
        setItem(24, myAuctions);

        // Categories
        ItemStack cats = new ItemStack(Material.CHEST);
        ItemMeta cMeta = cats.getItemMeta();
        cMeta.setDisplayName("§6Categories");
        cMeta.setLore(Arrays.asList("§7Browse by category"));
        cats.setItemMeta(cMeta);
        setItem(31, cats);
    }

    private void loadAuctions() {
        // No-op here
    }

    @Override
    public void onClose(Player player) {
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getSlot();
        Player player = (Player) event.getWhoClicked();

        if (slot == 20) {
            plugin.getGuiManager().openMenu(player, new AuctionsMenu(plugin));
        } else if (slot == 22) {
            plugin.getInputListener().awaitSearch(player);
        } else if (slot == 24) {
            plugin.getGuiManager().openMenu(player, new PlayerSellingMenu(plugin));
        } else if (slot == 31) {
            plugin.getGuiManager().openMenu(player, new CategoryMenu(plugin));
        }
    }
}
