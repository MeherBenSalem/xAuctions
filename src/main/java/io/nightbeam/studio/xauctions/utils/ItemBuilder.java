package io.nightbeam.studio.xauctions.utils;

import io.nightbeam.studio.xauctions.XAuctionsPlugin;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Fluent builder for creating ItemStacks.
 */
public class ItemBuilder {

    private final ItemStack item;
    private final ItemMeta meta;

    public ItemBuilder(Material material) {
        this.item = new ItemStack(material);
        this.meta = item.getItemMeta();
    }

    public ItemBuilder(ItemStack item) {
        this.item = item.clone();
        this.meta = this.item.getItemMeta();
    }

    public static ItemBuilder from(Material material) {
        return new ItemBuilder(material);
    }

    public static ItemBuilder from(ItemStack item) {
        return new ItemBuilder(item);
    }

    public ItemBuilder name(String name) {
        if (meta != null) {
            meta.setDisplayName(name);
        }
        return this;
    }

    public ItemBuilder lore(String... lore) {
        return lore(Arrays.asList(lore));
    }

    public ItemBuilder lore(List<String> lore) {
        if (meta != null) {
            meta.setLore(lore);
        }
        return this;
    }

    public ItemBuilder addLore(String... lines) {
        if (meta != null) {
            List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
            lore.addAll(Arrays.asList(lines));
            meta.setLore(lore);
        }
        return this;
    }

    public ItemBuilder amount(int amount) {
        item.setAmount(amount);
        return this;
    }

    public ItemBuilder enchant(Enchantment enchantment, int level) {
        if (meta != null) {
            meta.addEnchant(enchantment, level, true);
        }
        return this;
    }

    public ItemBuilder flags(ItemFlag... flags) {
        if (meta != null) {
            meta.addItemFlags(flags);
        }
        return this;
    }

    public ItemBuilder skullOwner(UUID playerUuid) {
        if (meta instanceof SkullMeta skullMeta) {
            skullMeta.setOwningPlayer(org.bukkit.Bukkit.getOfflinePlayer(playerUuid));
        }
        return this;
    }

    public ItemBuilder persistentData(NamespacedKey key, String value) {
        if (meta != null) {
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, value);
        }
        return this;
    }

    public ItemBuilder persistentData(NamespacedKey key, int value) {
        if (meta != null) {
            meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, value);
        }
        return this;
    }

    public ItemBuilder apply(Consumer<ItemMeta> consumer) {
        if (meta != null) {
            consumer.accept(meta);
        }
        return this;
    }

    public ItemBuilder glow(boolean glow) {
        if (glow && meta != null) {
            // Use DURABILITY (Unbreaking) as it's common and has no effect on most items in
            // GUI
            meta.addEnchant(Enchantment.DURABILITY, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        return this;
    }

    public ItemStack build() {
        if (meta != null) {
            item.setItemMeta(meta);
        }
        return item;
    }
}
