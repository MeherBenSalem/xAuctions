package io.nightbeam.studio.xauctions.gui.menus;

import io.nightbeam.studio.xauctions.XAuctionsPlugin;
import io.nightbeam.studio.xauctions.api.model.Auction;
import io.nightbeam.studio.xauctions.core.managers.PlayerPreferences;
import io.nightbeam.studio.xauctions.gui.framework.AuctionFilter;
import io.nightbeam.studio.xauctions.gui.framework.AuctionSortOrder;
import io.nightbeam.studio.xauctions.gui.framework.MenuItem;
import io.nightbeam.studio.xauctions.gui.framework.PaginatedMenu;
import io.nightbeam.studio.xauctions.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import io.nightbeam.studio.xauctions.gui.menus.ShulkerPreviewMenu;

public class AuctionsMenu extends PaginatedMenu<Auction> {

    private final String searchQuery;
    private AuctionFilter currentFilter;
    private AuctionSortOrder currentSort;
    private List<Auction> cachedAuctions;
    /** UUID of the player who opened this menu, set in onOpen. */
    private UUID viewerUuid;

    public AuctionsMenu(XAuctionsPlugin plugin) {
        this(plugin, null, AuctionFilter.ALL, AuctionSortOrder.NEWEST);
    }

    public AuctionsMenu(XAuctionsPlugin plugin, String searchQuery) {
        this(plugin, searchQuery, AuctionFilter.ALL, AuctionSortOrder.NEWEST);
    }

    public AuctionsMenu(XAuctionsPlugin plugin, String searchQuery, AuctionFilter filter) {
        this(plugin, searchQuery, filter, AuctionSortOrder.NEWEST);
    }

    public AuctionsMenu(XAuctionsPlugin plugin, String searchQuery, AuctionFilter filter, AuctionSortOrder sort) {
        super(plugin, "AUCTION SITE", 54);
        this.searchQuery = searchQuery;
        this.currentFilter = filter;
        this.currentSort = sort;
        this.maxItemsPerPage = 45; // 5 rows
    }

    @Override
    public void onOpen(Player player) {
        this.viewerUuid = player.getUniqueId();
        // Load persisted filter + sort from the player's preferences
        PlayerPreferences prefs = plugin.getPlayerPreferencesManager().getOrCreate(viewerUuid);
        this.currentFilter = prefs.getFilter();
        this.currentSort = prefs.getSortOrder();
        update();
    }

    @Override
    public void update() {
        inventory.clear();
        // Do NOT call addMenuBorder() or logic that fills empty spaces with glass.
        // We want a clean interface as requested.

        List<Auction> data = getData();
        if (data == null) {
            data = Collections.emptyList();
        }

        int totalPages = (int) Math.ceil((double) data.size() / 45); // 45 items per page
        if (totalPages == 0)
            totalPages = 1;

        if (page >= totalPages && totalPages > 0) {
            page = totalPages - 1;
        }

        int startIndex = page * 45;
        int endIndex = Math.min(startIndex + 45, data.size());

        int slot = 0;
        for (int i = startIndex; i < endIndex; i++) {
            loopCode(data.get(i), slot);
            slot++;
        }

        addNavigationButtons(totalPages);
    }

    @Override
    protected void addMenuBorder() {
        // Intentionally empty to avoid filling empty slots with glass
    }

    @Override
    public List<Auction> getData() {
        if (cachedAuctions == null) {
            try {
                cachedAuctions = plugin.getStorageProvider().loadActiveAuctions().join();
            } catch (Exception e) {
                cachedAuctions = Collections.emptyList();
            }
        }

        return cachedAuctions.stream()
                .filter(auction -> {
                    // Search
                    if (searchQuery != null && !searchQuery.isEmpty()) {
                        String q = searchQuery.toLowerCase();
                        boolean match = auction.getSellerName().toLowerCase().contains(q) ||
                                auction.getItemStack().getType().toString().toLowerCase().contains(q);
                        if (auction.getItemStack().hasItemMeta()
                                && auction.getItemStack().getItemMeta().hasDisplayName()) {
                            match |= auction.getItemStack().getItemMeta().getDisplayName().toLowerCase().contains(q);
                        }
                        if (!match)
                            return false;
                    }

                    // Category Filter
                    if (currentFilter != AuctionFilter.ALL) {
                        return matchFilter(auction.getItemStack(), currentFilter);
                    }
                    return true;
                })
                .sorted(buildComparator())
                .collect(Collectors.toList());
    }

    /** Builds the {@link Comparator} that corresponds to {@link #currentSort}. */
    private Comparator<Auction> buildComparator() {
        return switch (currentSort) {
            case PRICE_ASC     -> Comparator.comparingDouble(Auction::getPrice);
            case PRICE_DESC    -> Comparator.comparingDouble(Auction::getPrice).reversed();
            case OLDEST        -> Comparator.comparingLong(Auction::getStartTime);
            case EXPIRING_SOON -> Comparator.comparingLong(Auction::getExpireTime);
            default            -> Comparator.comparingLong(Auction::getStartTime).reversed(); // NEWEST
        };
    }

    private boolean matchFilter(ItemStack item, AuctionFilter filter) {
        String type = item.getType().toString();
        switch (filter) {
            case BLOCKS:
                return item.getType().isBlock();
            case WEAPONS:
                return type.contains("SWORD") || type.contains("AXE") || type.contains("BOW")
                        || type.contains("TRIDENT") || type.contains("PICKAXE") || type.contains("SHOVEL")
                        || type.contains("HOE") || type.contains("FISHING_ROD") || type.contains("HELMET")
                        || type.contains("CHESTPLATE") || type.contains("LEGGINGS") || type.contains("BOOTS");
            case CONSUMABLES:
                return type.contains("POTION") || type.contains("FOOD") || type.contains("APPLE")
                        || type.contains("BREAD");
            case MISC:
                return !item.getType().isBlock() && !type.contains("SWORD") && !type.contains("PICKAXE");
            default:
                return true;
        }
    }

    @Override
    public void loopCode(Auction auction, int slot) {
        // Build item and add preview hint for shulker boxes
        ItemStack base = auction.getItemStack().clone();
        ItemBuilder builder = ItemBuilder.from(base)
                .lore(
                        " ",
                        "§bSELLER: §f" + auction.getSellerName(),
                        "§bPRICE: §f" + plugin.getEconomyManager().getDefaultProvider().format(auction.getPrice()),
                        "§bEXPIRE: §f" + formatTime(auction.getExpireTime() - System.currentTimeMillis()),
                        " ",
                        "§b▶ CLICK TO BUY THIS ITEM");

        boolean isShulker = base.getType().toString().contains("SHULKER");
        if (isShulker) {
            builder.addLore("§7(Right-click to preview contents)");
        }

        ItemStack item = builder.build();

        setItem(slot, new MenuItem(item, event -> {
            Player p = (Player) event.getWhoClicked();

            // Right-click to preview shulker box contents if applicable
            if (event.isRightClick() && isShulker) {
                try {
                    plugin.getGuiManager().openMenu(p, new ShulkerPreviewMenu(plugin, auction.getItemStack(), this));
                    return;
                } catch (Exception e) {
                    // If preview fails, fall back to standard behavior
                }
            }

            // Prevent buying your own auction (only for purchase attempts)
            if (auction.getSellerUuid().equals(p.getUniqueId())) {
                p.sendMessage("§cYou cannot buy your own auction.");
                return;
            }

            // Left-click (or other clicks) proceed to purchase confirmation
            plugin.getGuiManager().openMenu(p, new ConfirmationMenu(plugin, "Confirm Purchase",
                    "Buy " + item.getType().name() + " for " + auction.getPrice() + "?",
                    confirmPlayer -> {
                        plugin.getAuctionService().buyAuction(confirmPlayer, auction)
                                .thenAccept(result -> {
                                    if (result.isSuccess()) {
                                        confirmPlayer.sendMessage("§aItem purchased!");
                                        plugin.getGuiManager().openMenu(confirmPlayer, new AuctionsMenu(plugin));
                                    } else {
                                        confirmPlayer.sendMessage("§cError: " + result.getError());
                                    }
                                });
                    },
                    cancelPlayer -> plugin.getGuiManager().openMenu(cancelPlayer, this)));
        }));
    }

    @Override
    protected void addNavigationButtons(int totalPages) {
        // Layout:
        // [45: Arrow (Prev)] [46: Back] [49: Cycle Filter] [53: Arrow (Next)]

        // 45: Previous Page (Always Arrow)
        ItemStack prevItem = ItemBuilder.from(Material.ARROW)
                .name(page > 0 ? "§aPrevious Page" : "§7Previous Page")
                .build();
        setItem(45, new MenuItem(prevItem, e -> {
            if (page > 0) {
                page--;
                update();
            }
        }));

        // 46: Back to Main Menu
        setItem(46, new MenuItem(ItemBuilder.from(Material.BARRIER).name("§cGo Back").build(), e -> {
            plugin.getGuiManager().openMenu((Player) e.getWhoClicked(), new MainMenu(plugin));
        }));

        // 47: Cycle Sort
        ItemStack sortItem = ItemBuilder.from(currentSort.getIcon())
                .name("§6Sort: §f" + currentSort.getDisplayName())
                .lore("§7Click to cycle sort orders", "§eCurrent: §f" + currentSort.getDisplayName())
                .glow(true)
                .build();

        setItem(47, new MenuItem(sortItem, e -> {
            AuctionSortOrder[] sorts = AuctionSortOrder.values();
            int nextOrdinal = (currentSort.ordinal() + 1) % sorts.length;
            this.currentSort = sorts[nextOrdinal];
            this.page = 0;
            savePreferences();
            update();
        }));

        // 49: Cycle Filter
        ItemStack filterItem = ItemBuilder.from(currentFilter.getIcon())
                .name("§aFilter: §f" + currentFilter.getDisplayName())
                .lore("§7Click to cycle filters", "§eCurrent: §f" + currentFilter.getDisplayName())
                .glow(true)
                .build();

        setItem(49, new MenuItem(filterItem, e -> {
            AuctionFilter[] filters = AuctionFilter.values();
            int nextOrdinal = (currentFilter.ordinal() + 1) % filters.length;
            this.currentFilter = filters[nextOrdinal];
            this.page = 0;
            savePreferences();
            update();
        }));

        // 53: Next Page (Always Arrow)
        ItemStack nextItem = ItemBuilder.from(Material.ARROW)
                .name(page < totalPages - 1 ? "§aNext Page" : "§7Next Page")
                .build();
        setItem(53, new MenuItem(nextItem, e -> {
            if (page < totalPages - 1) {
                page++;
                update();
            }
        }));
    }

    private String formatTime(long millis) {
        if (millis <= 0)
            return "Expired";
        long days = millis / (24 * 60 * 60 * 1000);
        long hours = (millis / (60 * 60 * 1000)) % 24;
        long minutes = (millis / (60 * 1000)) % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0)
            sb.append(days).append("d ");
        sb.append(hours).append("h ");
        sb.append(minutes).append("m");
        return sb.toString();
    }

    /** Persists the current filter and sort order to the player's preferences. */
    private void savePreferences() {
        if (viewerUuid == null) return;
        PlayerPreferences prefs = plugin.getPlayerPreferencesManager().getOrCreate(viewerUuid);
        prefs.setFilter(currentFilter);
        prefs.setSortOrder(currentSort);
    }

    @Override
    public void onClose(Player player) {
        // Ensure preferences are persisted even if the player closes the menu
        // without clicking filter/sort (e.g. pressing Escape)
        savePreferences();
    }
}
