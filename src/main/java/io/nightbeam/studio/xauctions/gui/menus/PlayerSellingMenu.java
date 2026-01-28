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
import java.util.UUID;

public class PlayerSellingMenu extends AbstractMenu {

    private final Map<Integer, Auction> auctionMap = new HashMap<>();

    public PlayerSellingMenu(xAuctions plugin) {
        super(plugin, "My Auctions", 54);
    }

    @Override
    public void onOpen(Player player) {
        // Load player's auctions
        plugin.getStorageProvider().loadPlayerAuctions(player.getUniqueId()).thenAccept(auctions -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                int slot = 0;
                auctionMap.clear();

                // Sort by status: Ready to Collect first, then Active, then History
                // Or just iterate

                for (Auction auction : auctions) {
                    if (slot >= 45)
                        break;

                    ItemStack displayItem = auction.getItemStack().clone();
                    ItemMeta meta = displayItem.getItemMeta();
                    List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();

                    lore.add(" ");
                    lore.add("§8§m----------------");

                    if (auction.isCollected()) {
                        // History
                        lore.add("§7Status: §8Collected / Archived");
                        if (auction.isSold()) {
                            lore.add("§7Sold for: §6"
                                    + plugin.getEconomyManager().getDefaultProvider().format(auction.getPrice()));
                        } else {
                            lore.add("§7Result: §cExpired/Cancelled");
                        }
                    } else if (auction.isSold()) {
                        // Sold, not collected
                        lore.add("§7Status: §aSOLD!");
                        lore.add("§7Sold for: §6"
                                + plugin.getEconomyManager().getDefaultProvider().format(auction.getPrice()));
                        lore.add(" ");
                        lore.add("§eClick to Collect Money");
                    } else if (auction.isExpired() || auction.isDeleted()) {
                        // Expired, not collected
                        lore.add("§7Status: §cEXPIRED");
                        lore.add(" ");
                        lore.add("§eClick to Retreive Item");
                    } else {
                        // Active
                        lore.add("§7Status: §bActive");
                        lore.add("§7Price: §6"
                                + plugin.getEconomyManager().getDefaultProvider().format(auction.getPrice()));
                        lore.add("§7Expires in: §b" + formatTime(auction.getExpireTime() - System.currentTimeMillis()));
                        lore.add(" ");
                        lore.add("§cClick to Cancel");
                    }

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
        if (millis <= 0)
            return "Expired";
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

            if (auction.isCollected()) {
                player.sendMessage("§7This item is already in your history.");
                return;
            }

            if (auction.isSold() || auction.isExpired() || auction.isDeleted()) {
                // Collect
                plugin.getAuctionManager().collectAuction(player, auction);
                // Refresh
                player.closeInventory();
                plugin.getGuiManager().openMenu(player, new PlayerSellingMenu(plugin));
            } else {
                // Active -> Cancel
                plugin.getAuctionManager().cancelAuction(player, auction);
                player.closeInventory();
                plugin.getGuiManager().openMenu(player, new PlayerSellingMenu(plugin));
            }
        }
    }
}
