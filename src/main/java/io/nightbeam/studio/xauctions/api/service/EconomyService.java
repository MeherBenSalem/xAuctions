package io.nightbeam.studio.xauctions.api.service;

import org.bukkit.OfflinePlayer;

public interface EconomyService {

    /**
     * withdrawal money from a player.
     *
     * @param player The player to withdraw from.
     * @param amount The amount to withdraw.
     * @return true if successful, false otherwise (e.g. not enough funds).
     */
    boolean withdraw(OfflinePlayer player, double amount);

    /**
     * Deposit money to a player.
     *
     * @param player The player to deposit to.
     * @param amount The amount to deposit.
     * @return true if successful.
     */
    boolean deposit(OfflinePlayer player, double amount);

    /**
     * Check if a player has enough money.
     *
     * @param player The player.
     * @param amount The amount to check for.
     * @return true if they have enough.
     */
    boolean has(OfflinePlayer player, double amount);

    /**
     * Format a currency amount.
     *
     * @param amount The amount.
     * @return The formatted string.
     */
    String format(double amount);
}
