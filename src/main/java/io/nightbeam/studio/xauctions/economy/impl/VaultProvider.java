package io.nightbeam.studio.xauctions.economy.impl;

import io.nightbeam.studio.xauctions.api.economy.EconomyProvider;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

public class VaultProvider implements EconomyProvider {

    private final Economy economy;

    public VaultProvider() {
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            throw new IllegalStateException("Vault Economy not found!");
        }
        this.economy = rsp.getProvider();
    }

    @Override
    public String getId() {
        return "Vault";
    }

    @Override
    public String getCurrencyName() {
        return economy.currencyNamePlural();
    }

    @Override
    public boolean has(OfflinePlayer player, double amount) {
        return economy.has(player, amount);
    }

    @Override
    public boolean withdraw(OfflinePlayer player, double amount) {
        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    @Override
    public boolean deposit(OfflinePlayer player, double amount) {
        return economy.depositPlayer(player, amount).transactionSuccess();
    }

    @Override
    public double getBalance(OfflinePlayer player) {
        return economy.getBalance(player);
    }

    @Override
    public String format(double amount) {
        return economy.format(amount);
    }
}
