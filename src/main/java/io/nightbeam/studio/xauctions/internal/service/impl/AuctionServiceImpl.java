package io.nightbeam.studio.xauctions.internal.service.impl;

import io.nightbeam.studio.xauctions.XAuctionsPlugin;
import io.nightbeam.studio.xauctions.api.economy.EconomyProvider;
import io.nightbeam.studio.xauctions.api.model.Auction;
import io.nightbeam.studio.xauctions.api.model.AuctionRequest;
import io.nightbeam.studio.xauctions.api.model.Result;
import io.nightbeam.studio.xauctions.api.service.AuctionService;
import io.nightbeam.studio.xauctions.api.event.AuctionBuyEvent;
import io.nightbeam.studio.xauctions.api.event.AuctionCreateEvent;
import io.nightbeam.studio.xauctions.config.PluginConfig;
import io.nightbeam.studio.xauctions.utils.Log;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implementation of the AuctionService.
 */
public class AuctionServiceImpl implements AuctionService {

    private final XAuctionsPlugin plugin;
    private final AtomicInteger activeCount = new AtomicInteger(0);
    private boolean countInitialized = false;

    public AuctionServiceImpl(XAuctionsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public CompletableFuture<Result<Auction>> createAuction(AuctionRequest request) {
        Player seller = request.seller();
        PluginConfig config = plugin.getPluginConfig();

        // 0. Blacklist Check (Player & Item)
        if (plugin.getPlayerBlacklistManager().isBlacklisted(seller)) {
            return CompletableFuture.completedFuture(Result.error("You are blacklisted from auctions"));
        }
        if (plugin.getItemBlacklistManager().isBlacklisted(request.item())) {
            return CompletableFuture.completedFuture(Result.error("Item is blacklisted"));
        }

        // 1. Check Limits (Async)
        return plugin.getStorageProvider().loadPlayerAuctions(seller.getUniqueId()).thenCompose(playerAuctions -> {
            long currentActive = playerAuctions.stream().filter(a -> !a.isSold() && !a.isExpired()).count();
            if (!plugin.getListingLimitManager().canCreateListing(seller, (int) currentActive)) {
                return CompletableFuture.completedFuture(Result.error("Listing limit reached (" + currentActive + "/"
                        + plugin.getListingLimitManager().getMaxListings(seller) + ")"));
            }

            // 2. Fees
            if (config.getAuctionSettings().listingFee() > 0) {
                EconomyProvider eco = plugin.getEconomyManager().getDefaultProvider();
                if (eco == null)
                    return CompletableFuture.completedFuture(Result.error("Economy not available"));

                if (!eco.has(seller, config.getAuctionSettings().listingFee())) {
                    return CompletableFuture.completedFuture(Result.error("Not enough money for listing fee"));
                }
                eco.withdraw(seller, config.getAuctionSettings().listingFee());
            }

            // 3. Create
            return doCreateAuction(seller, request, config);
        });
    }

    private CompletableFuture<Result<Auction>> doCreateAuction(Player seller, AuctionRequest request,
            PluginConfig config) {
        long start = System.currentTimeMillis();
        long expire = start + (request.durationSeconds() * 1000);

        Auction auction = Auction.builder()
                .auctionId(UUID.randomUUID())
                .sellerUuid(seller.getUniqueId())
                .sellerName(seller.getName())
                .itemStack(request.item().clone())
                .price(request.price())
                .startTime(start)
                .expireTime(expire)
                .type(request.type())
                .currency(request.currency())
                .sold(false)
                .collected(false)
                .deleted(false)
                .build();

        // Fire Event
        AuctionCreateEvent event = new AuctionCreateEvent(auction, seller);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return CompletableFuture.completedFuture(Result.error("Cancelled by plugin"));
        }

        // Remove item from inventory
        seller.getInventory().removeItem(request.item());

        // Save
        return plugin.getStorageProvider().saveAuction(auction).thenApply(v -> {
            Log.info("Auction created: " + auction.getAuctionId());
            activeCount.incrementAndGet();
            return Result.success(auction);
        }).exceptionally(ex -> Result.error("Storage error: " + ex.getMessage()));
    }

    @Override
    public CompletableFuture<Result<Void>> buyAuction(Player buyer, Auction auction) {
        if (plugin.getPlayerBlacklistManager().isBlacklisted(buyer)) {
            return CompletableFuture.completedFuture(Result.error("You are blacklisted from auctions"));
        }

        if (auction.isSold() || auction.isExpired()) {
            return CompletableFuture.completedFuture(Result.error("Auction not available"));
        }

        EconomyProvider eco = plugin.getEconomyManager().getProvider(auction.getCurrency());
        if (eco == null)
            eco = plugin.getEconomyManager().getDefaultProvider();
        if (eco == null)
            return CompletableFuture.completedFuture(Result.error("Economy not available"));

        if (!eco.has(buyer, auction.getPrice())) {
            return CompletableFuture.completedFuture(Result.error("Not enough " + eco.getCurrencyName()));
        }

        // Fire Event
        AuctionBuyEvent event = new AuctionBuyEvent(auction, buyer);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return CompletableFuture.completedFuture(Result.error("Cancelled by plugin"));
        }

        // Atomic DB Update
        EconomyProvider finalEco = eco;
        return plugin.getStorageProvider().attemptBuy(auction.getAuctionId(), buyer.getUniqueId())
                .thenCompose(success -> {
                    if (!success) {
                        return CompletableFuture.completedFuture(Result.error("Auction already sold or unavailable"));
                    }

                    // Success: Deduct money from buyer
                    finalEco.withdraw(buyer, auction.getPrice());

                    // Update local object
                    auction.setSold(true);
                    auction.setBuyerUuid(buyer.getUniqueId());
                    activeCount.decrementAndGet();

                    // Instant Payout Check
                    boolean autoClaim = plugin.getPluginConfig().getAuctionSettings().autoClaimOnline();
                    Player seller = Bukkit.getPlayer(auction.getSellerUuid());

                    if (autoClaim && seller != null && seller.isOnline()) {
                        // Calculate Tax
                        double tax = plugin.getTaxManager().calculateTax(auction.getPrice(), auction.getItemStack());
                        double finalAmount = auction.getPrice() - tax;

                        // Deposit to seller
                        EconomyProvider sellerEco = plugin.getEconomyManager().getProvider(auction.getCurrency());
                        if (sellerEco == null)
                            sellerEco = plugin.getEconomyManager().getDefaultProvider();

                        sellerEco.deposit(seller, finalAmount);
                        auction.setCollected(true);

                        seller.sendMessage(
                                "§aItem sold to " + buyer.getName() + " for " + sellerEco.format(finalAmount));
                        if (tax > 0) {
                            seller.sendMessage("§7(Tax paid: " + sellerEco.format(tax) + ")");
                        }
                    } else {
                        // Notify seller if online but auto-claim disabled, or just let them check /ah
                        if (plugin.getPluginConfig().getAuctionSettings().broadcastSale()) {
                            // Global broadcast is handled separately or we can do it here?
                            // Config says broadcast-sale. Usually global.
                            // But we should notify seller personally if online.
                            if (seller != null && seller.isOnline()) {
                                seller.sendMessage("§aYour item was sold to " + buyer.getName() + " for "
                                        + finalEco.format(auction.getPrice()));
                                seller.sendMessage("§7Type /ah to collect your earnings.");
                            }
                        }
                    }

                    // Give item to buyer
                    java.util.HashMap<Integer, org.bukkit.inventory.ItemStack> leftover = buyer.getInventory()
                            .addItem(auction.getItemStack());
                    if (!leftover.isEmpty()) {
                        for (org.bukkit.inventory.ItemStack drop : leftover.values()) {
                            buyer.getWorld().dropItem(buyer.getLocation(), drop);
                        }
                        buyer.sendMessage("§eInventory full, item dropped on ground!");
                    }

                    // Update DB with collected status if changed
                    return plugin.getStorageProvider().updateAuction(auction).thenApply(v -> Result.success());
                });
    }

    @Override
    public CompletableFuture<Result<Void>> cancelAuction(Player player, Auction auction) {
        if (!auction.getSellerUuid().equals(player.getUniqueId())) {
            return CompletableFuture.completedFuture(Result.error("Not your auction"));
        }

        auction.setExpireTime(0L); // Expire it
        return plugin.getStorageProvider().updateAuction(auction)
                .thenApply(v -> {
                    activeCount.decrementAndGet();
                    return Result.success();
                });
    }

    @Override
    public CompletableFuture<Result<Void>> collectAuction(Player player, Auction auction) {
        if (!auction.isSold() && !auction.isExpired() && !auction.isDeleted()) {
            return CompletableFuture.completedFuture(Result.error("Auction is active"));
        }
        if (auction.isCollected()) {
            return CompletableFuture.completedFuture(Result.error("Already collected"));
        }

        EconomyProvider eco = plugin.getEconomyManager().getProvider(auction.getCurrency());
        if (eco == null)
            eco = plugin.getEconomyManager().getDefaultProvider();

        if (auction.isSold()) {
            // Calculate Tax
            double tax = plugin.getTaxManager().calculateTax(auction.getPrice(), auction.getItemStack());
            double finalAmount = auction.getPrice() - tax;

            eco.deposit(player, finalAmount);

            auction.setCollected(true);
            final EconomyProvider finalEco = eco;
            return plugin.getStorageProvider().updateAuction(auction).thenApply(v -> {
                player.sendMessage(
                        "§aCollected " + finalEco.format(finalAmount) + " (Tax: " + finalEco.format(tax) + ")");
                return Result.success();
            });
        } else {
            // Return Item (Expired/Cancelled/Deleted)
            if (player.getInventory().firstEmpty() == -1) {
                return CompletableFuture.completedFuture(Result.error("Inventory full"));
            }
            player.getInventory().addItem(auction.getItemStack());
            auction.setCollected(true);
            return plugin.getStorageProvider().updateAuction(auction).thenApply(v -> Result.success());
        }
    }

    @Override
    public int getActiveCount() {
        if (!countInitialized) {
            countInitialized = true;
            plugin.getStorageProvider().loadActiveAuctions().thenAccept(list -> activeCount.set(list.size()));
        }
        return activeCount.get();
    }
}
