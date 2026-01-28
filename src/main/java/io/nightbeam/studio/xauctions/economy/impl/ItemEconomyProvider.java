package io.nightbeam.studio.xauctions.economy.impl;

import io.nightbeam.studio.xauctions.api.economy.EconomyProvider;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;

public class ItemEconomyProvider implements EconomyProvider {

    private final Material currencyMaterial;
    private final String name;

    public ItemEconomyProvider(Material material, String name) {
        this.currencyMaterial = material;
        this.name = name;
    }

    @Override
    public String getCurrencyName() {
        return name;
    }

    @Override
    public double getBalance(OfflinePlayer player) {
        if (!player.isOnline())
            return 0;
        Player p = (Player) player;
        int count = 0;
        for (ItemStack is : p.getInventory().getContents()) {
            if (is != null && is.getType() == currencyMaterial) {
                count += is.getAmount();
            }
        }
        return count;
    }

    @Override
    public String getId() {
        return "item_" + currencyMaterial.name().toLowerCase();
    }

    @Override
    public String format(double amount) {
        return (int) amount + " " + name;
    }

    @Override
    public boolean has(OfflinePlayer player, double amount) {
        if (!player.isOnline())
            return false;
        Player p = (Player) player;
        return p.getInventory().contains(currencyMaterial, (int) amount);
    }

    @Override
    public boolean withdraw(OfflinePlayer player, double amount) {
        if (!has(player, amount))
            return false;
        if (player.isOnline()) {
            Player p = (Player) player;
            // Basic remove. For exact NBT matching, we need strict checks, but for MVP:
            HashMap<Integer, ItemStack> leftover = p.getInventory()
                    .removeItem(new ItemStack(currencyMaterial, (int) amount));
            return leftover.isEmpty();
        }
        return false;
    }

    @Override
    public boolean deposit(OfflinePlayer player, double amount) {
        if (player.isOnline()) {
            Player p = (Player) player;
            HashMap<Integer, ItemStack> leftover = p.getInventory()
                    .addItem(new ItemStack(currencyMaterial, (int) amount));
            // If full, drop naturally
            if (!leftover.isEmpty()) {
                leftover.values().forEach(item -> p.getWorld().dropItem(p.getLocation(), item));
            }
            return true;
        }
        return false;
    }
}
