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
import io.nightbeam.studio.xauctions.utils.PlatformAdapter;
import io.nightbeam.studio.xauctions.utils.Log;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implementation of the AuctionService.
 */
public class AuctionServiceImpl implements AuctionService {

    private final XAuctionsPlugin plugin;
    private final PlatformAdapter platformAdapter;
    private final AtomicInteger activeCount = new AtomicInteger(0);
    private boolean countInitialized = false;

    public AuctionServiceImpl(XAuctionsPlugin plugin) {
        this.plugin = plugin;
        this.platformAdapter = plugin.getPlatformAdapter();
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

        // Fire event and modify inventory on the main thread
        CompletableFuture<Auction> mainThread = new CompletableFuture<>();
        // Region-safe: seller inventory and event callbacks must run on seller's owning thread.
        platformAdapter.runEntityTask(seller, () -> {
            try {
                AuctionCreateEvent event = new AuctionCreateEvent(auction, seller);
                Bukkit.getPluginManager().callEvent(event);
                if (event.isCancelled()) {
                    mainThread.complete(null);
                    return;
                }

                // Remove item from inventory on main thread
                seller.getInventory().removeItem(request.item());
                mainThread.complete(auction);
            } catch (Throwable t) {
                mainThread.completeExceptionally(t);
            }
        });

        // After main-thread actions complete, save the auction asynchronously
        return mainThread.thenCompose(a -> {
            if (a == null) {
                return CompletableFuture.completedFuture(Result.error("Cancelled by plugin"));
            }
            return plugin.getStorageProvider().saveAuction(a).thenApply(v -> {
                Log.info("Auction created: " + auction.getAuctionId());
                activeCount.incrementAndGet();
                return Result.success(auction);
            }).exceptionally(ex -> Result.error("Storage error: " + ex.getMessage()));
        });
    }

    @Override
    public CompletableFuture<Result<Void>> buyAuction(Player buyer, Auction auction) {
        CompletableFuture<Result<Void>> precheck = new CompletableFuture<>();
        // Region-safe: buyer validations and buy event run on buyer's owning thread.
        platformAdapter.runEntityTask(buyer, () -> {
            if (plugin.getPlayerBlacklistManager().isBlacklisted(buyer)) {
                precheck.complete(Result.error("You are blacklisted from auctions"));
                return;
            }

            if (auction.isSold() || auction.isExpired()) {
                precheck.complete(Result.error("Auction not available"));
                return;
            }

            if (auction.getSellerUuid().equals(buyer.getUniqueId())) {
                precheck.complete(Result.error("You cannot buy your own auction"));
                return;
            }

            EconomyProvider eco = plugin.getEconomyManager().getProvider(auction.getCurrency());
            if (eco == null) {
                eco = plugin.getEconomyManager().getDefaultProvider();
            }
            if (eco == null) {
                precheck.complete(Result.error("Economy not available"));
                return;
            }

            if (!eco.has(buyer, auction.getPrice())) {
                precheck.complete(Result.error("Not enough " + eco.getCurrencyName()));
                return;
            }

            AuctionBuyEvent event = new AuctionBuyEvent(auction, buyer);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                precheck.complete(Result.error("Cancelled by plugin"));
                return;
            }

            precheck.complete(Result.success());
        });

        return precheck.thenCompose(precheckResult -> {
            if (!precheckResult.isSuccess()) {
                return CompletableFuture.completedFuture(Result.error(precheckResult.getError()));
            }

            return plugin.getStorageProvider().attemptBuy(auction.getAuctionId(), buyer.getUniqueId())
                    .thenCompose(success -> {
                        if (!success) {
                            return CompletableFuture.completedFuture(Result.error("Auction already sold or unavailable"));
                        }

                        CompletableFuture<Void> buyerFinalizeFuture = new CompletableFuture<>();
                        // Region-safe: buyer inventory, economy withdraw, and world drops are buyer-thread work.
                        platformAdapter.runEntityTask(buyer, () -> {
                            try {
                                EconomyProvider buyerEco = plugin.getEconomyManager().getProvider(auction.getCurrency());
                                if (buyerEco == null) {
                                    buyerEco = plugin.getEconomyManager().getDefaultProvider();
                                }

                                if (buyerEco == null || !buyerEco.withdraw(buyer, auction.getPrice())) {
                                    buyerFinalizeFuture.completeExceptionally(new IllegalStateException("Failed to withdraw buyer funds"));
                                    return;
                                }

                                auction.setSold(true);
                                auction.setBuyerUuid(buyer.getUniqueId());
                                activeCount.decrementAndGet();

                                java.util.HashMap<Integer, org.bukkit.inventory.ItemStack> leftover = buyer.getInventory()
                                        .addItem(auction.getItemStack());
                                if (!leftover.isEmpty()) {
                                    Location dropLocation = buyer.getLocation();
                                    for (org.bukkit.inventory.ItemStack drop : leftover.values()) {
                                        // Region-safe: item drops are explicitly scheduled for the location's region.
                                        platformAdapter.runLocationTask(dropLocation, () ->
                                                dropLocation.getWorld().dropItem(dropLocation, drop)
                                        );
                                    }
                                    buyer.sendMessage("§eInventory full, item dropped on ground!");
                                }

                                buyerFinalizeFuture.complete(null);
                            } catch (Throwable throwable) {
                                buyerFinalizeFuture.completeExceptionally(throwable);
                            }
                        });

                        return buyerFinalizeFuture.thenCompose(v -> {
                            boolean autoClaim = plugin.getPluginConfig().getAuctionSettings().autoClaimOnline();
                            Player seller = Bukkit.getPlayer(auction.getSellerUuid());

                            CompletableFuture<Void> sellerFuture = CompletableFuture.completedFuture(null);
                            if (seller != null && seller.isOnline()) {
                                if (autoClaim) {
                                    sellerFuture = collectSellerPaymentOnline(seller, buyer, auction);
                                } else if (plugin.getPluginConfig().getAuctionSettings().broadcastSale()) {
                                    sellerFuture = notifySellerForPendingCollection(seller, buyer, auction);
                                }
                            }

                            return sellerFuture.thenCompose(v2 -> plugin.getStorageProvider().updateAuction(auction)
                                    .thenApply(v3 -> Result.<Void>success())
                                    .exceptionally(ex -> Result.error("Storage error: " + ex.getMessage())));
                        }).exceptionally(ex -> Result.error("Buy flow error: " + ex.getMessage()));
                    });
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
                    return Result.<Void>success();
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

            EconomyProvider sellerEco = plugin.getEconomyManager().getProvider(auction.getCurrency());
            if (sellerEco == null) {
                sellerEco = plugin.getEconomyManager().getDefaultProvider();
            }
            if (sellerEco == null) {
                return CompletableFuture.completedFuture(Result.error("Economy not available"));
            }

            EconomyProvider finalSellerEco = sellerEco;
            CompletableFuture<Void> collectFlow = new CompletableFuture<>();
            // Region-safe: collection modifies seller inventory/wallet and sends player messages.
            platformAdapter.runEntityTask(player, () -> {
                try {
                    if (!finalSellerEco.deposit(player, finalAmount)) {
                        collectFlow.completeExceptionally(new IllegalStateException("Failed to deposit funds"));
                        return;
                    }

                    auction.setCollected(true);
                    player.sendMessage("§aCollected " + finalSellerEco.format(finalAmount) + " (Tax: "
                            + finalSellerEco.format(tax) + ")");
                    collectFlow.complete(null);
                } catch (Throwable throwable) {
                    collectFlow.completeExceptionally(throwable);
                }
            });

            return collectFlow.thenCompose(v -> plugin.getStorageProvider().updateAuction(auction)
                    .thenApply(v2 -> Result.<Void>success())
                    .exceptionally(ex -> Result.error("Storage error: " + ex.getMessage())))
                    .exceptionally(ex -> Result.error("Collection error: " + ex.getMessage()));
        } else {
            // Return Item (Expired/Cancelled/Deleted)
            CompletableFuture<Result<Void>> reclaimFlow = new CompletableFuture<>();
            // Region-safe: reclaiming modifies seller inventory and possibly the world in seller region.
            platformAdapter.runEntityTask(player, () -> {
                if (player.getInventory().firstEmpty() == -1) {
                    reclaimFlow.complete(Result.error("Inventory full"));
                    return;
                }

                player.getInventory().addItem(auction.getItemStack());
                auction.setCollected(true);
                plugin.getStorageProvider().updateAuction(auction)
                        .thenApply(v -> Result.<Void>success())
                        .exceptionally(ex -> Result.error("Storage error: " + ex.getMessage()))
                        .thenAccept(reclaimFlow::complete);
            });
            return reclaimFlow;
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

    private CompletableFuture<Void> collectSellerPaymentOnline(Player seller, Player buyer, Auction auction) {
        CompletableFuture<Void> sellerFuture = new CompletableFuture<>();
        platformAdapter.runEntityTask(seller, () -> {
            try {
                EconomyProvider sellerEco = plugin.getEconomyManager().getProvider(auction.getCurrency());
                if (sellerEco == null) {
                    sellerEco = plugin.getEconomyManager().getDefaultProvider();
                }

                if (sellerEco != null) {
                    double tax = plugin.getTaxManager().calculateTax(auction.getPrice(), auction.getItemStack());
                    double finalAmount = auction.getPrice() - tax;

                    sellerEco.deposit(seller, finalAmount);
                    auction.setCollected(true);
                    seller.sendMessage("§aItem sold to " + buyer.getName() + " for " + sellerEco.format(finalAmount));
                    if (tax > 0) {
                        seller.sendMessage("§7(Tax paid: " + sellerEco.format(tax) + ")");
                    }
                }

                sellerFuture.complete(null);
            } catch (Throwable throwable) {
                sellerFuture.completeExceptionally(throwable);
            }
        });
        return sellerFuture;
    }

    private CompletableFuture<Void> notifySellerForPendingCollection(Player seller, Player buyer, Auction auction) {
        CompletableFuture<Void> sellerFuture = new CompletableFuture<>();
        platformAdapter.runEntityTask(seller, () -> {
            try {
                EconomyProvider sellerEco = plugin.getEconomyManager().getProvider(auction.getCurrency());
                if (sellerEco == null) {
                    sellerEco = plugin.getEconomyManager().getDefaultProvider();
                }

                String formattedPrice = sellerEco != null
                        ? sellerEco.format(auction.getPrice())
                        : String.valueOf(auction.getPrice());

                seller.sendMessage("§aYour item was sold to " + buyer.getName() + " for " + formattedPrice);
                seller.sendMessage("§7Type /ah to collect your earnings.");
                sellerFuture.complete(null);
            } catch (Throwable throwable) {
                sellerFuture.completeExceptionally(throwable);
            }
        });
        return sellerFuture;
    }
}
