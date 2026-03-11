package io.nightbeam.studio.xauctions.economy.impl;

import io.nightbeam.studio.xauctions.api.economy.EconomyProvider;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.OfflinePlayer;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Adapter around DonutCore's {@code io.nightbeam.donutcore.modules.economy.EconomyService}.
 *
 * <p>The dependency on DonutCore is intentionally soft: we look up the service via the
 * Bukkit {@link org.bukkit.plugin.ServicesManager} and use reflection to invoke methods.
 * If DonutCore is not present the provider is never registered, so this class may safely
 * exist on its own classpath.
 */
public class DonutCoreProvider implements EconomyProvider {
    private final Object service;
    private final Method hasMethod;
    private final Method withdrawMethod;
    private final Method depositMethod;
    private final Method getBalanceMethod;
    private final Method formatMethod;
    private final Method currencyPluralMethod;
    private final Method currencySingularMethod;

    public DonutCoreProvider(Object service) throws ReflectiveOperationException {
        this.service = service;
        Class<?> cls = service.getClass();
        // DonutCore's EconomyService does not expose a `has(UUID,double)` method;
        // we'll implement `has` by checking balance ourselves via getBalance().
        hasMethod = null;
        withdrawMethod = cls.getMethod("withdraw", UUID.class, double.class);
        depositMethod = cls.getMethod("deposit", UUID.class, double.class);
        getBalanceMethod = cls.getMethod("getBalance", UUID.class);
        // formatting helpers
        formatMethod = cls.getMethod("formatWithCurrency", double.class);
        currencyPluralMethod = cls.getMethod("getCurrencyNamePlural");
        currencySingularMethod = cls.getMethod("getCurrencyNameSingular");
    }

    private UUID uuidOf(OfflinePlayer player) {
        return player == null ? null : player.getUniqueId();
    }

    @Override
    public String getId() {
        return "DonutCore";
    }

    @Override
    public String getCurrencyName() {
        try {
            return (String) currencyPluralMethod.invoke(service);
        } catch (Throwable t) {
            return "Donut";
        }
    }

    @Override
    public boolean has(OfflinePlayer player, double amount) {
        UUID u = uuidOf(player);
        if (u == null) return false;
        try {
            double bal = (double) getBalanceMethod.invoke(service, u);
            return bal >= amount;
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public boolean withdraw(OfflinePlayer player, double amount) {
        UUID u = uuidOf(player);
        if (u == null) return false;
        try {
            EconomyResponse resp = (EconomyResponse) withdrawMethod.invoke(service, u, amount);
            return resp.transactionSuccess();
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public boolean deposit(OfflinePlayer player, double amount) {
        UUID u = uuidOf(player);
        if (u == null) return false;
        try {
            EconomyResponse resp = (EconomyResponse) depositMethod.invoke(service, u, amount);
            return resp.transactionSuccess();
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public double getBalance(OfflinePlayer player) {
        UUID u = uuidOf(player);
        if (u == null) return 0.0;
        try {
            return (double) getBalanceMethod.invoke(service, u);
        } catch (Throwable t) {
            return 0.0;
        }
    }

    @Override
    public String format(double amount) {
        try {
            return (String) formatMethod.invoke(service, amount);
        } catch (Throwable t) {
            return String.valueOf(amount);
        }
    }
}
