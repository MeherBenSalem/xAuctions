package io.nightbeam.studio.xauctions.storage.sql;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.nightbeam.studio.xauctions.api.model.Auction;
import io.nightbeam.studio.xauctions.api.storage.StorageProvider;
import io.nightbeam.studio.xauctions.XAuctionsPlugin;
import io.nightbeam.studio.xauctions.utils.ItemSerializer;
import org.bukkit.Bukkit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class SqlStorageProvider implements StorageProvider {

    private final XAuctionsPlugin plugin;
    private HikariDataSource dataSource;

    public SqlStorageProvider(XAuctionsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void init() {
        HikariConfig config = new HikariConfig();
        // TODO: Load these from config.yml properly
        // For now, we assume defaults or placeholder values, user to configure.
        config.setJdbcUrl("jdbc:mysql://localhost:3306/minecraft");
        config.setUsername("root");
        config.setPassword("password");

        // Optimizations
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("useLocalSessionState", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        config.addDataSourceProperty("cacheResultSetMetadata", "true");
        config.addDataSourceProperty("maintainTimeStats", "false"); // Improve performance

        dataSource = new HikariDataSource(config);

        createTable();
    }

    private void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS xauctions_data (" +
                "auction_id CHAR(36) PRIMARY KEY, " +
                "seller_uuid CHAR(36) NOT NULL, " +
                "seller_name VARCHAR(16) NOT NULL, " +
                "buyer_uuid CHAR(36), " + // New: Track who bought it
                "item_base64 TEXT NOT NULL, " +
                "price DOUBLE NOT NULL, " +
                "currency_type VARCHAR(32) DEFAULT 'vault', " + // New: Support multicurrency
                "start_time BIGINT NOT NULL, " +
                "expire_time BIGINT NOT NULL, " +
                "is_sold BOOLEAN DEFAULT FALSE, " +
                "is_collected BOOLEAN DEFAULT FALSE, " +
                "INDEX (is_sold), " +
                "INDEX (expire_time), " +
                "INDEX (seller_uuid)" +
                ");";

        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.execute();

            // Migration for older tables if needed (simplistic check)
            try {
                conn.prepareStatement("ALTER TABLE xauctions_data ADD COLUMN currency_type VARCHAR(32) DEFAULT 'vault'")
                        .execute();
            } catch (SQLException ignored) {
                // Col exists
            }
            try {
                conn.prepareStatement("ALTER TABLE xauctions_data ADD COLUMN buyer_uuid CHAR(36)").execute();
            } catch (SQLException ignored) {
                // Col exists
            }

        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to create/update xAuctions SQL table: " + e.getMessage());
        }
    }

    @Override
    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    @Override
    public CompletableFuture<Void> saveAuction(Auction auction) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT INTO xauctions_data (auction_id, seller_uuid, seller_name, item_base64, price, currency_type, start_time, expire_time, is_sold, is_collected, buyer_uuid) "
                    +
                    "VALUES (?,?,?,?,?,?,?,?,?,?,?) " +
                    "ON DUPLICATE KEY UPDATE is_sold=?, is_collected=?, buyer_uuid=?";

            try (Connection conn = dataSource.getConnection();
                    PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, auction.getAuctionId().toString());
                ps.setString(2, auction.getSellerUuid().toString());
                ps.setString(3, auction.getSellerName());
                ps.setString(4, ItemSerializer.toBase64(auction.getItemStack()));
                ps.setDouble(5, auction.getPrice());
                ps.setString(6, auction.getCurrency());
                ps.setLong(7, auction.getStartTime());
                ps.setLong(8, auction.getExpireTime());
                ps.setBoolean(9, auction.isSold());
                ps.setBoolean(10, auction.isCollected());
                ps.setString(11, auction.getBuyerUuid() != null ? auction.getBuyerUuid().toString() : null);

                // Update part
                ps.setBoolean(12, auction.isSold());
                ps.setBoolean(13, auction.isCollected());
                ps.setString(14, auction.getBuyerUuid() != null ? auction.getBuyerUuid().toString() : null);

                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Error saving auction " + auction.getAuctionId() + ": " + e.getMessage());
            }
        });
    }

    /**
     * ATOMIC BUY OPERATION
     * Returns true if the purchase was successful (row updated), false if already
     * sold/gone.
     */
    @Override
    public CompletableFuture<Boolean> attemptBuy(UUID auctionId, UUID buyerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "UPDATE xauctions_data SET is_sold=true, buyer_uuid=? WHERE auction_id=? AND is_sold=false";
            try (Connection conn = dataSource.getConnection();
                    PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, buyerUuid.toString());
                ps.setString(2, auctionId.toString());

                int rows = ps.executeUpdate();
                return rows > 0; // If 1, we secured the item. If 0, someone else got it.

            } catch (SQLException e) {
                plugin.getLogger().severe("Error executing atomic buy for " + auctionId + ": " + e.getMessage());
                return false;
            }
        });
    }

    @Override
    public CompletableFuture<Void> updateAuction(Auction auction) {
        return saveAuction(auction);
    }

    @Override
    public CompletableFuture<Void> deleteAuction(UUID auctionId) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                    PreparedStatement ps = conn.prepareStatement("DELETE FROM xauctions_data WHERE auction_id=?")) {
                ps.setString(1, auctionId.toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public CompletableFuture<List<Auction>> loadActiveAuctions() {
        return CompletableFuture.supplyAsync(() -> {
            List<Auction> auctions = new ArrayList<>();
            // Select NOT sold and NOT expired
            String sql = "SELECT * FROM xauctions_data WHERE is_sold=false AND expire_time > ?";
            try (Connection conn = dataSource.getConnection();
                    PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, System.currentTimeMillis());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        auctions.add(mapResultSetToAuction(rs));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Error loading active auctions: " + e.getMessage());
            }
            return auctions;
        });
    }

    @Override
    public CompletableFuture<List<Auction>> loadPlayerAuctions(UUID sellerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            List<Auction> auctions = new ArrayList<>();
            String sql = "SELECT * FROM xauctions_data WHERE seller_uuid=?";
            try (Connection conn = dataSource.getConnection();
                    PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, sellerUuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        auctions.add(mapResultSetToAuction(rs));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Error loading player auctions: " + e.getMessage());
            }
            return auctions;
        });
    }

    private Auction mapResultSetToAuction(ResultSet rs) throws SQLException {
        String buyerIdStr = rs.getString("buyer_uuid");
        UUID buyerUuid = (buyerIdStr != null && !buyerIdStr.isEmpty()) ? UUID.fromString(buyerIdStr) : null;

        // Handle currency default for bad data
        String currency = rs.getString("currency_type");
        if (currency == null)
            currency = "vault";

        return Auction.builder()
                .auctionId(UUID.fromString(rs.getString("auction_id")))
                .sellerUuid(UUID.fromString(rs.getString("seller_uuid")))
                .sellerName(rs.getString("seller_name"))
                .buyerUuid(buyerUuid)
                .itemStack(ItemSerializer.fromBase64(rs.getString("item_base64")))
                .price(rs.getDouble("price"))
                .currency(currency)
                .startTime(rs.getLong("start_time"))
                .expireTime(rs.getLong("expire_time"))
                .sold(rs.getBoolean("is_sold"))
                .collected(rs.getBoolean("is_collected"))
                .type(Auction.AuctionType.BIN) // Default to BIN for now
                .build();
    }

    @Override
    public void invalidateCache(UUID auctionId) {
        // No-op for SQL, we query fresh or use short-lived local cache in GUI
    }
}
