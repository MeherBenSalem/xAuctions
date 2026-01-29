package io.nightbeam.studio.xauctions.economy.impl;

import io.nightbeam.studio.xauctions.api.economy.EconomyProvider;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import java.util.logging.Logger;

/**
 * Safe Vault provider wrapper. Never throws on missing Vault or provider.
 * Consumers should check {@link #isAvailable()} before relying on Vault-specific behavior.
 */
public class VaultProvider implements EconomyProvider {

    private final Economy economy;
    private final boolean available;

    public VaultProvider() {
        Logger logger = Bukkit.getLogger();
        RegisteredServiceProvider<Economy> rsp =
                Bukkit.getServer().getServicesManager().getRegistration(Economy.class);

        if (rsp == null || rsp.getProvider() == null) {
            this.economy = null;
            this.available = false;
            if (rsp == null) {
                logger.fine("Vault RegisteredServiceProvider<Economy> not found.");
            } else {
                logger.fine("Vault RegisteredServiceProvider returned null provider.");
            }
        } else {
            this.economy = rsp.getProvider();
            this.available = true;
        }
    }

    /**
     * True if Vault + an Economy provider are available and hooked.
     */
    public boolean isAvailable() {
        return available;
    }

    @Override
    public String getId() {
        return "Vault";
    }

    @Override
    public String getCurrencyName() {
        if (!available) return "Vault";
        try {
            return economy.currencyNamePlural();
        } catch (Throwable t) {
            return "Vault";
        }
    }

    @Override
    public boolean has(OfflinePlayer player, double amount) {
        if (!available) return false;
        try {
            return economy.has(player, amount);
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public boolean withdraw(OfflinePlayer player, double amount) {
        if (!available) return false;
        try {
            return economy.withdrawPlayer(player, amount).transactionSuccess();
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public boolean deposit(OfflinePlayer player, double amount) {
        if (!available) return false;
        try {
            return economy.depositPlayer(player, amount).transactionSuccess();
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public double getBalance(OfflinePlayer player) {
        if (!available) return 0.0;
        try {
            return economy.getBalance(player);
        } catch (Throwable t) {
            return 0.0;
        }
    }

    @Override
    public String format(double amount) {
        if (!available) return String.valueOf(amount);
        try {
            return economy.format(amount);
        } catch (Throwable t) {
            return String.valueOf(amount);
        }
    }
}
