package io.nightbeam.studio.xauctions.core.managers;

import io.nightbeam.studio.xauctions.xAuctions;
import io.nightbeam.studio.xauctions.api.model.Auction;
import io.nightbeam.studio.xauctions.api.economy.EconomyProvider;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class AuctionManager {

    private final xAuctions plugin;

    public AuctionManager(xAuctions plugin) {
        this.plugin = plugin;
    }

    /**
     * Creates a new auction (BIN).
     */
    public void createAuction(Player player, ItemStack item, double price, long durationSeconds) {
        // 1. Validation (Limits check TODO)

        // Fee Calculation
        double feePercent = plugin.getConfig().getDouble("auctions.listing-fee-percent", 0.01);
        double fee = price * feePercent;

        EconomyProvider eco = plugin.getEconomyManager().getDefaultProvider();
        if (eco != null && fee > 0) {
            if (!eco.has(player, fee)) {
                plugin.getMessageManager().send(player, "not-enough-money");
                return;
            }
            eco.withdraw(player, fee);
            plugin.getMessageManager().send(player, "listing-fee", "%amount%", eco.format(fee));
        }

        // 2. Take item
        player.getInventory().removeItem(item);

        // 3. Create Auction Object
        long start = System.currentTimeMillis();
        long expire = start + (durationSeconds * 1000);

        Auction auction = Auction.builder()
                .auctionId(UUID.randomUUID())
                .sellerUuid(player.getUniqueId())
                .sellerName(player.getName())
                .itemStack(item)
                .price(price)
                .startTime(start)
                .expireTime(expire)
                .type(Auction.AuctionType.BIN)
                .currency("vault") // Default for now
                .sold(false)
                .collected(false)
                .deleted(false)
                .build();

        // 4. Save to Storage
        plugin.getStorageProvider().saveAuction(auction).thenAccept(v -> {
            plugin.getMessageManager().send(player, "auction-created",
                    "%item%", item.getType().toString(),
                    "%price%", eco != null ? eco.format(price) : String.valueOf(price));

            // Discord Log
            if (plugin.getDiscordWebhook() != null) {
                plugin.getDiscordWebhook().sendEmbed(
                        "New Auction",
                        "Seller: " + player.getName() + "nItem: " + item.getType() + "nPrice: " + price,
                        0x00FF00);
            }
        });
    }

    /**
     * Buy an auction (BIN).
     */
    public void buyAuction(Player buyer, Auction auction) {
        if (auction.isSold() || auction.isExpired()) {
            buyer.sendMessage("§cThis auction is no longer available.");
            return;
        }

        if (auction.getSellerUuid().equals(buyer.getUniqueId())) {
            buyer.sendMessage("§cYou cannot buy your own auction!");
            return;
        }

        // Economy Check
        EconomyProvider economy = plugin.getEconomyManager().getDefaultProvider();
        if (economy == null) {
            buyer.sendMessage("§cEconomy system not available.");
            return;
        }

        if (!economy.has(buyer, auction.getPrice())) {
            plugin.getMessageManager().send(buyer, "not-enough-money");
            return;
        }

        // Transaction
        economy.withdraw(buyer, auction.getPrice());

        // Mark as sold
        auction.setSold(true);

        // Give Item
        buyer.getInventory().addItem(auction.getItemStack());

        // Save & Notify
        plugin.getStorageProvider().updateAuction(auction).thenRun(() -> {
            plugin.getMessageManager().send(buyer, "auction-bought",
                    "%item%", auction.getItemStack().getType().toString(),
                    "%price%", economy.format(auction.getPrice()));

            Player seller = Bukkit.getPlayer(auction.getSellerUuid());
            if (seller != null) {
                plugin.getMessageManager().send(seller, "auction-sold",
                        "%item%", auction.getItemStack().getType().toString(),
                        "%price%", economy.format(auction.getPrice()));

                economy.deposit(seller, auction.getPrice());
                auction.setCollected(true);
                plugin.getStorageProvider().updateAuction(auction);
            }

            // Discord Log
            if (plugin.getDiscordWebhook() != null) {
                plugin.getDiscordWebhook().sendEmbed(
                        "Auction Sold",
                        "Buyer: " + buyer.getName() + "nSeller: " + auction.getSellerName() + "nItem: "
                                + auction.getItemStack().getType() + "nPrice: " + auction.getPrice(),
                        0xFFFF00);
            }
        });
    }

    /**
     * Collect earnings or expired items.
     */
    public void collectAuction(Player player, Auction auction) {
        if (!auction.getSellerUuid().equals(player.getUniqueId())) {
            plugin.getMessageManager().send(player, "no-permission"); // Reusing permission msg or add new
                                                                      // 'not-your-auction'
            return;
        }

        if (auction.isCollected()) {
            player.sendMessage("§cAlready collected.");
            return;
        }

        EconomyProvider economy = plugin.getEconomyManager().getDefaultProvider();

        if (auction.isSold()) {
            // Give Money
            economy.deposit(player, auction.getPrice());
            auction.setCollected(true);
            plugin.getStorageProvider().updateAuction(auction);
            plugin.getMessageManager().send(player, "collected-money", "%amount%", economy.format(auction.getPrice()));
        } else if (auction.isExpired() || auction.isDeleted()) {
            // Give Item
            if (player.getInventory().firstEmpty() == -1) {
                plugin.getMessageManager().send(player, "inventory-full");
                return;
            }
            player.getInventory().addItem(auction.getItemStack());
            auction.setCollected(true);
            plugin.getStorageProvider().updateAuction(auction);
            plugin.getMessageManager().send(player, "collected-item");
        } else {
            // Active
        }
    }

    /**
     * Cancel an active auction.
     */
    public void cancelAuction(Player player, Auction auction) {
        if (!auction.getSellerUuid().equals(player.getUniqueId())) {
            plugin.getMessageManager().send(player, "no-permission");
            return;
        }

        if (auction.isSold() || auction.isExpired()) {
            player.sendMessage("§cCannot cancel this auction.");
            return;
        }

        auction.setExpireTime(0L);

        plugin.getStorageProvider().updateAuction(auction).thenRun(() -> {
            plugin.getMessageManager().send(player, "auction-cancelled");
        });
    }
}
