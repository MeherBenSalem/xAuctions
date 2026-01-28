package io.nightbeam.studio.xauctions.gui.menus;

import io.nightbeam.studio.xauctions.XAuctionsPlugin;
import io.nightbeam.studio.xauctions.gui.framework.BaseMenu;
import io.nightbeam.studio.xauctions.gui.framework.MenuItem;
import io.nightbeam.studio.xauctions.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.function.Consumer;

/**
 * Generic Confirmation Menu.
 */
public class ConfirmationMenu extends BaseMenu {

    private final String description;
    private final Consumer<Player> onConfirm;
    private final Consumer<Player> onCancel;

    public ConfirmationMenu(XAuctionsPlugin plugin, String title, String description, Consumer<Player> onConfirm,
            Consumer<Player> onCancel) {
        super(plugin, title, 27);
        this.description = description;
        this.onConfirm = onConfirm;
        this.onCancel = onCancel;
    }

    @Override
    public void onOpen(Player player) {
        // Confirmation Info
        setItem(13, ItemBuilder.from(Material.PAPER)
                .name("§e§lInfo")
                .lore("§7" + description)
                .build());

        // Confirm Button
        setItem(11, new MenuItem(ItemBuilder.from(Material.LIME_TERRACOTTA)
                .name("§a§lCONFIRM")
                .lore("§7Click to confirm.")
                .build(), event -> {
                    player.closeInventory();
                    if (onConfirm != null)
                        onConfirm.accept(player);
                }));

        // Cancel Button
        setItem(15, new MenuItem(ItemBuilder.from(Material.RED_TERRACOTTA)
                .name("§c§lCANCEL")
                .lore("§7Click to cancel.")
                .build(), event -> {
                    player.closeInventory();
                    if (onCancel != null)
                        onCancel.accept(player);
                }));

        fillBorders(ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name(" ").build());
    }

    @Override
    public void onClose(Player player) {
        // Optional: Trigger cancel if closed without choice?
        // Usually safer to do nothing or treat as cancel depending on logic.
    }
}
