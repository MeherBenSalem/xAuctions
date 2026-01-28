package io.nightbeam.studio.xauctions.gui.menus;

import io.nightbeam.studio.xauctions.xAuctions;
import io.nightbeam.studio.xauctions.gui.AbstractMenu;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class CategoryMenu extends AbstractMenu {

    public CategoryMenu(xAuctions plugin) {
        super(plugin, "Categories", 27);
    }

    @Override
    public void onOpen(Player player) {
        // Blocks
        ItemStack blocks = new ItemStack(Material.GRASS_BLOCK);
        ItemMeta blockMeta = blocks.getItemMeta();
        blockMeta.setDisplayName("§aBlocks");
        blocks.setItemMeta(blockMeta);
        setItem(10, blocks);

        // Weapons
        ItemStack weapons = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta weaponMeta = weapons.getItemMeta();
        weaponMeta.setDisplayName("§cWeapons");
        weapons.setItemMeta(weaponMeta);
        setItem(12, weapons);

        // Tools
        ItemStack tools = new ItemStack(Material.DIAMOND_PICKAXE);
        ItemMeta toolMeta = tools.getItemMeta();
        toolMeta.setDisplayName("§bTools");
        tools.setItemMeta(toolMeta);
        setItem(14, tools);

        // Misc
        ItemStack misc = new ItemStack(Material.LAVA_BUCKET);
        ItemMeta miscMeta = misc.getItemMeta();
        miscMeta.setDisplayName("§eMisc");
        misc.setItemMeta(miscMeta);
        setItem(16, misc);

        // Back Button
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.setDisplayName("§cBack");
        back.setItemMeta(backMeta);
        setItem(22, back);
    }

    @Override
    public void onClose(Player player) {
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getSlot();
        if (slot == 22) {
            // Go back
            plugin.getGuiManager().openMenu((Player) event.getWhoClicked(), new MainMenu(plugin));
        }
        // Handle filter selection
        if (slot == 10 || slot == 12 || slot == 14 || slot == 16) {
            // Pass filter later, for now just open all
            plugin.getGuiManager().openMenu((Player) event.getWhoClicked(), new AuctionsMenu(plugin));
        }
    }
}
