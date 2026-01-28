package io.nightbeam.studio.xauctions.core.commands;

import io.nightbeam.studio.xauctions.xAuctions;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class SellCommand implements CommandExecutor {

    private final xAuctions plugin;

    public SellCommand(xAuctions plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command!");
            return true;
        }

        if (args.length < 1) {
            player.sendMessage("§cUsage: /ah sell <price>");
            return true;
        }

        double price;
        try {
            price = Double.parseDouble(args[0]);
        } catch (NumberFormatException e) {
            player.sendMessage("§cInvalid price.");
            return true;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) {
            player.sendMessage("§cYou must hold an item to sell it.");
            return true;
        }

        // Create the auction (Default 24h duration for now)
        plugin.getAuctionManager().createAuction(player, item.clone(), price, 86400); // 24 hours

        return true;
    }
}
