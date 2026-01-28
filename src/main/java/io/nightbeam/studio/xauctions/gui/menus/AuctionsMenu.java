package io.nightbeam.studio.xauctions.gui.menus;

import io.nightbeam.studio.xauctions.xAuctions;
import io.nightbeam.studio.xauctions.api.model.Auction;
import io.nightbeam.studio.xauctions.gui.AbstractMenu;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AuctionsMenu extends AbstractMenu {

    private final Map<Integer, Auction> auctionMap = new HashMap<>();
    private final String filter;

    public AuctionsMenu(xAuctions plugin) {
        this(plugin, null);
    }

    public AuctionsMenu(xAuctions plugin, String filter) {
        super(plugin, filter == null ? "Auctions" : "Search: " + filter, 54);
        this.filter = filter;
    }

    @Override
    public void onOpen(Player player) {
        // Load active auctions
        plugin.getStorageProvider().loadActiveAuctions().thenAccept(auctions -> {

            plugin.getServer().getScheduler().runTask(plugin, () -> {
                int slot = 0;
                auctionMap.clear();

                for (Auction auction : auctions) {
                    // Filter Logic
                    if (filter != null) {
                        String query = filter.toLowerCase();
                        boolean match = false;
                        if (auction.getSellerName().toLowerCase().contains(query))
                            match = true;
                        if (auction.getItemStack().getType().toString().toLowerCase().contains(query))
                            match = true;
                        if (auction.getItemStack().hasItemMeta() &&
                                auction.getItemStack().getItemMeta().hasDisplayName() &&
                                auction.getItemStack().getItemMeta().getDisplayName().toLowerCase().contains(query))
                            match = true;

                        if (!match)
                            continue;
                    }

                    if (slot >= 45)
                        break;

                    // Display Item with extra lore (Price, Seller, Time)
                    ItemStack displayItem = auction.getItemStack().clone();
                    ItemMeta meta = displayItem.getItemMeta();
                    List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();

                    lore.add(" ");
                    lore.add("§8§m----------------");
                    lore.add(
                            "§7Price: §6" + plugin.getEconomyManager().getDefaultProvider().format(auction.getPrice()));
                    lore.add("§7Seller: §e" + auction.getSellerName());
                    lore.add("§7Expires in: §b" + formatTime(auction.getExpireTime() - System.currentTimeMillis()));
                    lore.add(" ");
                    lore.add("§aClick to Buy!");
                    lore.add("§8§m----------------");

                    meta.setLore(lore);
                    displayItem.setItemMeta(meta);

                    setItem(slot, displayItem);
                    auctionMap.put(slot, auction);
                    slot++;
                }

                // Back Button
                ItemStack back = new ItemStack(Material.ARROW);
                ItemMeta backMeta = back.getItemMeta();
                backMeta.setDisplayName("§cBack");
                back.setItemMeta(backMeta);
                setItem(49, back);
            });
        });
    }

    private String formatTime(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        return hours + "h " + (minutes % 60) + "m";
    }

    @Override
    public void onClose(Player player) {
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getSlot();

        if (slot == 49) {
            plugin.getGuiManager().openMenu((Player) event.getWhoClicked(), new MainMenu(plugin));
            return;
        }

        if (auctionMap.containsKey(slot)) {
            Auction auction = auctionMap.get(slot);
            Player player = (Player) event.getWhoClicked();

            plugin.getAuctionManager().buyAuction(player, auction);
            player.closeInventory();
            // Or refresh: plugin.getGuiManager().openMenu(player, new
            // AuctionsMenu(plugin));
        }
    }
}
