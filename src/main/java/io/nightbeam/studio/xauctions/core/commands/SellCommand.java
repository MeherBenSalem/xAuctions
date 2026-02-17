package io.nightbeam.studio.xauctions.core.commands;

import io.nightbeam.studio.xauctions.XAuctionsPlugin;
import io.nightbeam.studio.xauctions.api.model.AuctionRequest;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class SellCommand implements CommandExecutor {

    private final XAuctionsPlugin plugin;

    public SellCommand(XAuctionsPlugin plugin) {
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
            player.sendMessage("§cUsage: /ah sell <price> [amount]");
            return true;
        }

        double price;
        try {
            price = Double.parseDouble(args[0]);
        } catch (NumberFormatException e) {
            player.sendMessage("§cInvalid price.");
            return true;
        }

        double minPrice = plugin.getPluginConfig().getAuctionSettings().minPrice();
        double maxPrice = plugin.getPluginConfig().getAuctionSettings().maxPrice();

        if (price < minPrice) {
            player.sendMessage("§cPrice must be at least " + minPrice);
            return true;
        }
        if (price > maxPrice) {
            player.sendMessage("§cPrice cannot exceed " + maxPrice);
            return true;
        }

        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || hand.getType() == Material.AIR) {
            player.sendMessage("§cYou must hold an item to sell it.");
            return true;
        }

        int amount = hand.getAmount();
        if (args.length >= 2) {
            try {
                int requestedAmount = Integer.parseInt(args[1]);
                if (requestedAmount < 1) {
                    player.sendMessage("§cAmount must be at least 1.");
                    return true;
                }
                if (requestedAmount > amount) {
                    player.sendMessage("§cYou only have " + amount + " of this item.");
                    return true;
                }
                amount = requestedAmount;
            } catch (NumberFormatException e) {
                player.sendMessage("§cInvalid amount.");
                return true;
            }
        }

        ItemStack itemToSell = hand.clone();
        itemToSell.setAmount(amount);

        // Use the new AuctionService
        long duration = plugin.getPluginConfig().getAuctionSettings().defaultDurationSeconds();
        AuctionRequest request = new AuctionRequest(player, itemToSell, price, duration);

        player.sendMessage("§eCreating auction...");

        // We need to handle item removal manually if we are only selling partial stack
        // But AuctionService usually removes the item from inventory.
        // Let's check AuctionService implementation.
        // AuctionServiceImpl removes request.item() from inventory.
        // seller.getInventory().removeItem(request.item());
        // Since we are passing a clone with specific amount, removeItem might work if
        // it matches exactly.
        // However, standard removeItem removes ANY matching item.
        // For precise control (selling from hand), we should probably handle removal
        // here or ensure service handles it right.
        // Current Service: seller.getInventory().removeItem(request.item());
        // This is risky if player has multiple stacks. It might remove from wrong
        // stack.
        // But for "Hand", we usually want to reduce hand.

        // BETTER APPROACH:
        // We reduce the hand amount locally here, and pass a generic "item" to service?
        // OR we let service handle it.
        // Service implementation: seller.getInventory().removeItem(request.item());
        // If we sell 5 diamonds and have 64 in hand.
        // request.item() is 5 diamonds.
        // removeItem(5 diamonds) will remove 5 diamonds from inventory (likely starting
        // from hotbar).
        // This is generally acceptable behavior for /ah sell.

        plugin.getAuctionService().createAuction(request)
                .thenAccept(result -> plugin.getPlatformAdapter().runEntityTask(player, () -> {
                    if (result.isSuccess()) {
                        player.sendMessage("§aAuction created successfully!");
                    } else {
                        player.sendMessage("§cFailed to create auction: " + result.getError());
                    }
                }))
                .exceptionally(ex -> {
                    plugin.getPlatformAdapter().runEntityTask(player,
                            () -> player.sendMessage("§cAn error occurred: " + ex.getMessage()));
                    return null;
                });

        return true;
    }
}
