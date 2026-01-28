package io.nightbeam.studio.xauctions.economy.impl;

import io.nightbeam.studio.xauctions.api.economy.EconomyProvider;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public class LevelEconomyProvider implements EconomyProvider {

    @Override
    public String getCurrencyName() {
        return "Levels";
    }

    @Override
    public double getBalance(OfflinePlayer player) {
        if (player.isOnline()) {
            return ((Player) player).getLevel();
        }
        return 0;
    }

    @Override
    public String getId() {
        return "level";
    }

    @Override
    public String format(double amount) {
        return (int) amount + " Lvl";
    }

    @Override
    public boolean has(OfflinePlayer player, double amount) {
        if (!player.isOnline())
            return false;
        return ((Player) player).getLevel() >= amount;
    }

    @Override
    public boolean withdraw(OfflinePlayer player, double amount) {
        if (!has(player, amount))
            return false;
        if (player.isOnline()) {
            Player p = (Player) player;
            p.setLevel(p.getLevel() - (int) amount);
            return true;
        }
        return false;
    }

    @Override
    public boolean deposit(OfflinePlayer player, double amount) {
        if (player.isOnline()) {
            Player p = (Player) player;
            p.setLevel(p.getLevel() + (int) amount);
            return true;
        }
        return false;
    }
}
