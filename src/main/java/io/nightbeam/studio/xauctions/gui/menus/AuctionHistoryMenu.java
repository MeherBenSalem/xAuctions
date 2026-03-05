package io.nightbeam.studio.xauctions.gui.menus;

import io.nightbeam.studio.xauctions.XAuctionsPlugin;
import io.nightbeam.studio.xauctions.api.model.Auction;
import io.nightbeam.studio.xauctions.gui.framework.MenuItem;
import io.nightbeam.studio.xauctions.gui.framework.PaginatedMenu;
import io.nightbeam.studio.xauctions.utils.ItemBuilder;
import io.nightbeam.studio.xauctions.utils.SchedulerUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class AuctionHistoryMenu extends PaginatedMenu<Auction> {

    private List<Auction> cachedData;

    public AuctionHistoryMenu(XAuctionsPlugin plugin) {
        super(plugin, "Manage Listings", 54);
    }

    @Override
    public List<Auction> getData() {
        if (cachedData == null) {
            return Collections.emptyList();
        }
        return cachedData;
    }

    @Override
    public void onOpen(Player player) {
        update();
    }

    @Override
    public void update() {
        if (inventory.getViewers().isEmpty())
            return;
        Player player = (Player) inventory.getViewers().get(0);

        plugin.getStorageProvider().loadPlayerAuctions(player.getUniqueId()).thenAccept(auctions -> {
            this.cachedData = auctions;

            this.cachedData.sort((a, b) -> {
                boolean aCollect = (a.isSold() || a.isExpired()) && !a.isCollected();
                boolean bCollect = (b.isSold() || b.isExpired()) && !b.isCollected();
                if (aCollect && !bCollect)
                    return -1;
                if (!aCollect && bCollect)
                    return 1;
                return Long.compare(b.getStartTime(), a.getStartTime());
            });

            SchedulerUtils.run(plugin, () -> {
                super.update(); // Call PaginatedMenu update which calls getData() and loopCode
            });
        });
    }

    @Override
    public void loopCode(Auction auction, int slot) {
        boolean canCollect = (auction.isSold() || auction.isExpired()) && !auction.isCollected();

        ItemBuilder builder = ItemBuilder.from(auction.getItemStack().clone());

        if (auction.isSold()) {
            builder.lore(" ", "§a§lSOLD", "§7Price: §f" + auction.getPrice(),
                    "§7Buyer: " + (auction.getBuyerUuid() != null ? "Unknown" : "Unknown"));
            if (canCollect) {
                builder.lore("§e▶ CLICK TO COLLECT MONEY");
                builder.glow(true);
            } else {
                builder.lore("§8(Money Collected)");
            }
        } else if (auction.isExpired()) {
            builder.lore(" ", "§c§lEXPIRED");
            if (canCollect) {
                builder.lore("§e▶ CLICK TO RECLAIM ITEM");
                builder.glow(true);
            } else {
                builder.lore("§8(Item Reclaimed)");
            }
        } else {
            builder.lore(" ", "§b§lACTIVE", "§7Price: §f" + auction.getPrice(), "§e▶ CLICK TO CANCEL");
        }

        setItem(slot, new MenuItem(builder.build(), e -> {
            Player p = (Player) e.getWhoClicked();
            if (auction.isSold() && canCollect) {
                plugin.getAuctionManager().collectAuction(p, auction);
                update(); // Refresh
            } else if (auction.isExpired() && canCollect) {
                plugin.getAuctionManager().collectAuction(p, auction);
                update(); // Refresh
            } else if (!auction.isSold() && !auction.isExpired()) {
                plugin.getAuctionManager().cancelAuction(p, auction);
                update(); // Refresh
            }
        }));
    }

    @Override
    protected void addNavigationButtons(int totalPages) {
        super.addNavigationButtons(totalPages);
        setItem(49, new MenuItem(ItemBuilder.from(Material.OAK_DOOR).name("§cBack to ᴀᴜᴄᴛɪᴏɴ ʜᴏᴜꜱᴇ").build(), e -> {
            plugin.getGuiManager().openMenu((Player) e.getWhoClicked(), new AuctionsMenu(plugin));
        }));
    }

    @Override
    public void onClose(Player player) {
        cachedData = null;
    }
}
