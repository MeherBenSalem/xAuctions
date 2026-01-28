package io.nightbeam.studio.xauctions.gui.framework;

import io.nightbeam.studio.xauctions.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public enum AuctionFilter {
    ALL(Material.CHEST, "All Items"), // Chest
    BLOCKS(Material.OAK_SIGN, "Blocks"), // Sign
    WEAPONS(Material.ANVIL, "Weapons & Tools"), // Anvil
    CONSUMABLES(Material.CAULDRON, "Consumables"), // Cauldron
    MISC(Material.HOPPER, "Redstone & Misc"); // Hopper

    private final Material icon;
    private final String displayName;

    AuctionFilter(Material icon, String displayName) {
        this.icon = icon;
        this.displayName = displayName;
    }

    public Material getIcon() {
        return icon;
    }

    public String getDisplayName() {
        return displayName;
    }

    public ItemStack getDisplayItem(boolean selected) {
        return ItemBuilder.from(icon)
                .name((selected ? "§a§l" : "§7") + displayName)
                .lore(selected ? "§eCurrently Selected" : "§7Click to filter")
                .glow(selected)
                .build();
    }
}
