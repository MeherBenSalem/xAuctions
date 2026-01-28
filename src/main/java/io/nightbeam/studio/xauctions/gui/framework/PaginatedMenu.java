package io.nightbeam.studio.xauctions.gui.framework;

import io.nightbeam.studio.xauctions.XAuctionsPlugin;
import io.nightbeam.studio.xauctions.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Menu with pagination support.
 * 
 * @param <T> The type of item being paginated.
 */
public abstract class PaginatedMenu<T> extends BaseMenu {

    protected int page = 0;
    protected int maxItemsPerPage = 28;
    // Slots in the middle: 10-16, 19-25, 28-34, 37-43 (4 rows of 7 = 28)
    protected final int[] itemSlots = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    public PaginatedMenu(XAuctionsPlugin plugin, String title, int size) {
        super(plugin, title, size);
    }

    public abstract List<T> getData();

    public abstract void loopCode(T object, int index);

    @Override
    public void onOpen(Player player) {
        // Redraw
        update();
    }

    public void update() {
        inventory.clear();
        addMenuBorder();

        List<T> data = getData();
        if (data == null || data.isEmpty()) {
            return;
        }

        int totalPages = (int) Math.ceil((double) data.size() / maxItemsPerPage);

        if (page >= totalPages && totalPages > 0) {
            page = totalPages - 1;
        }

        int startIndex = page * maxItemsPerPage;
        int endIndex = Math.min(startIndex + maxItemsPerPage, data.size());

        int slotIndex = 0;
        for (int i = startIndex; i < endIndex; i++) {
            if (slotIndex >= itemSlots.length)
                break;

            loopCode(data.get(i), itemSlots[slotIndex]);
            slotIndex++;
        }

        // Navigation buttons
        addNavigationButtons(totalPages);
    }

    protected void addNavigationButtons(int totalPages) {
        if (page > 0) {
            setItem(45, new MenuItem(ItemBuilder.from(Material.ARROW).name("§aPrevious Page").build(), event -> {
                page--;
                update();
            }));
        } else {
            // Maybe a gray arrow?
        }

        if (page < totalPages - 1) {
            setItem(53, new MenuItem(ItemBuilder.from(Material.ARROW).name("§aNext Page").build(), event -> {
                page++;
                update();
            }));
        } else {
            // Maybe a gray arrow?
        }

        // Close/Back button at 49 usually
        setItem(49, new MenuItem(ItemBuilder.from(Material.BARRIER).name("§cClose").build(), event -> {
            ((Player) event.getWhoClicked()).closeInventory();
        }));
    }

    protected void addMenuBorder() {
        fillBorders(ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name(" ").build());
    }

    public int getPage() {
        return page;
    }
}
