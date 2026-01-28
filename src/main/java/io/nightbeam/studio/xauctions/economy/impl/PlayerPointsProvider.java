package io.nightbeam.studio.xauctions.economy.impl;

import io.nightbeam.studio.xauctions.api.economy.EconomyProvider;
import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;

public class PlayerPointsProvider implements EconomyProvider {

    private final PlayerPointsAPI ppAPI;

    public PlayerPointsProvider() {
        Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin("PlayerPoints");
        if (plugin == null || !plugin.isEnabled()) {
            throw new IllegalStateException("PlayerPoints not found!");
        }
        this.ppAPI = PlayerPoints.getInstance().getAPI();
    }

    @Override
    public String getId() {
        return "PlayerPoints";
    }

    @Override
    public String getCurrencyName() {
        return "Points";
    }

    @Override
    public boolean has(OfflinePlayer player, double amount) {
        return ppAPI.look(player.getUniqueId()) >= (int) amount;
    }

    @Override
    public boolean withdraw(OfflinePlayer player, double amount) {
        return ppAPI.take(player.getUniqueId(), (int) amount);
    }

    @Override
    public boolean deposit(OfflinePlayer player, double amount) {
        return ppAPI.give(player.getUniqueId(), (int) amount);
    }

    @Override
    public double getBalance(OfflinePlayer player) {
        return ppAPI.look(player.getUniqueId());
    }

    @Override
    public String format(double amount) {
        return (int) amount + " Points";
    }
}
