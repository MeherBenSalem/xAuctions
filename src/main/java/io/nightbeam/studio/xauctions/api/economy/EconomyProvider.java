package io.nightbeam.studio.xauctions.api.economy;

import org.bukkit.OfflinePlayer;

public interface EconomyProvider {

    /**
     * The unique identifier for this economy provider (e.g., "VAULT",
     * "PLAYERPOINTS").
     */
    String getId();

    /**
     * Display name for the currency (e.g., "Coins", "Points").
     */
    String getCurrencyName();

    boolean has(OfflinePlayer player, double amount);

    boolean withdraw(OfflinePlayer player, double amount);

    boolean deposit(OfflinePlayer player, double amount);

    double getBalance(OfflinePlayer player);

    /**
     * Format the amount into a readable string (e.g. "$1,000").
     */
    String format(double amount);
}
