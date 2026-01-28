package io.nightbeam.studio.xauctions.api.model;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class Auction {

    private UUID auctionId;
    private UUID sellerUuid;
    private String sellerName;
    private ItemStack itemStack;
    private double price;
    private long requestTime;
    private long startTime;
    private long expireTime;
    private AuctionType type;
    private String currency;
    private boolean deleted;
    private boolean sold;
    private boolean collected;

    public Auction(UUID auctionId, UUID sellerUuid, String sellerName, ItemStack itemStack, double price,
            long requestTime, long startTime, long expireTime, AuctionType type, String currency, boolean deleted,
            boolean sold, boolean collected) {
        this.auctionId = auctionId;
        this.sellerUuid = sellerUuid;
        this.sellerName = sellerName;
        this.itemStack = itemStack;
        this.price = price;
        this.requestTime = requestTime;
        this.startTime = startTime;
        this.expireTime = expireTime;
        this.type = type;
        this.currency = currency;
        this.deleted = deleted;
        this.sold = sold;
        this.collected = collected;
    }

    public static AuctionBuilder builder() {
        return new AuctionBuilder();
    }

    public UUID getAuctionId() {
        return auctionId;
    }

    public UUID getSellerUuid() {
        return sellerUuid;
    }

    public String getSellerName() {
        return sellerName;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public double getPrice() {
        return price;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(long expireTime) {
        this.expireTime = expireTime;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public boolean isSold() {
        return sold;
    }

    public void setSold(boolean sold) {
        this.sold = sold;
    }

    public boolean isCollected() {
        return collected;
    }

    public void setCollected(boolean collected) {
        this.collected = collected;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expireTime;
    }

    public static class AuctionBuilder {
        private UUID auctionId;
        private UUID sellerUuid;
        private String sellerName;
        private ItemStack itemStack;
        private double price;
        private long requestTime;
        private long startTime;
        private long expireTime;
        private AuctionType type;
        private String currency;
        private boolean deleted;
        private boolean sold;
        private boolean collected;

        public AuctionBuilder auctionId(UUID auctionId) {
            this.auctionId = auctionId;
            return this;
        }

        public AuctionBuilder sellerUuid(UUID sellerUuid) {
            this.sellerUuid = sellerUuid;
            return this;
        }

        public AuctionBuilder sellerName(String sellerName) {
            this.sellerName = sellerName;
            return this;
        }

        public AuctionBuilder itemStack(ItemStack itemStack) {
            this.itemStack = itemStack;
            return this;
        }

        public AuctionBuilder price(double price) {
            this.price = price;
            return this;
        }

        public AuctionBuilder startTime(long startTime) {
            this.startTime = startTime;
            return this;
        }

        public AuctionBuilder expireTime(long expireTime) {
            this.expireTime = expireTime;
            return this;
        }

        public AuctionBuilder type(AuctionType type) {
            this.type = type;
            return this;
        }

        public AuctionBuilder currency(String currency) {
            this.currency = currency;
            return this;
        }

        public AuctionBuilder deleted(boolean deleted) {
            this.deleted = deleted;
            return this;
        }

        public AuctionBuilder sold(boolean sold) {
            this.sold = sold;
            return this;
        }

        public AuctionBuilder collected(boolean collected) {
            this.collected = collected;
            return this;
        }

        public Auction build() {
            return new Auction(auctionId, sellerUuid, sellerName, itemStack, price, requestTime, startTime, expireTime,
                    type, currency, deleted, sold, collected);
        }
    }

    public enum AuctionType {
        BIN,
        BID
    }
}
