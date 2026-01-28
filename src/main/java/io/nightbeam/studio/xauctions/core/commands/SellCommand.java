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

        if (price < 0) {
            player.sendMessage("§cPrice cannot be negative.");
            return true;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) {
            player.sendMessage("§cYou must hold an item to sell it.");
            return true;
        }

        // Use the new AuctionService
        long duration = plugin.getPluginConfig().getAuctionSettings().defaultDurationSeconds();
        AuctionRequest request = new AuctionRequest(player, item.clone(), price, duration);

        player.sendMessage("§eCreating auction...");

        plugin.getAuctionService().createAuction(request)
                .thenAccept(result -> {
                    if (result.isSuccess()) {
                        // Success message is handled by service or we can send it here
                        // plugin.getMessageManager().send(player, "auction-created", ...);
                        // For now, service logs it, we can just confirm to user if needed,
                        // but the service might have already sent a message or fired an event.
                        // The legacy AuctionManager sent messages directly.
                        // Implementation of AuctionServiceImpl currently only logs to console.
                        // Let's send a basic message here for now, or TODO: Move message logic to
                        // service or event listener.
                        player.sendMessage("§aAuction created successfully!");
                    } else {
                        player.sendMessage("§cFailed to create auction: " + result.getError());
                    }
                })
                .exceptionally(ex -> {
                    player.sendMessage("§cAn error occurred: " + ex.getMessage());
                    return null;
                });

        return true;
    }
}
