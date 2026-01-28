package io.nightbeam.studio.xauctions.storage.sql;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.nightbeam.studio.xauctions.api.model.Auction;
import io.nightbeam.studio.xauctions.api.storage.StorageProvider;
import io.nightbeam.studio.xauctions.XAuctionsPlugin;

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
        // TODO: Load these from config.yml
        config.setJdbcUrl("jdbc:mysql://localhost:3306/minecraft");
        config.setUsername("root");
        config.setPassword("password");
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        dataSource = new HikariDataSource(config);

        createTable();
    }

    private void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS xauctions_data (" +
                "auction_id CHAR(36) PRIMARY KEY, " +
                "seller_uuid CHAR(36), " +
                "seller_name VARCHAR(16), " +
                "item_base64 TEXT, " + // Will need a serializer
                "price DOUBLE, " +
                "start_time BIGINT, " +
                "expire_time BIGINT, " +
                "is_sold BOOLEAN, " +
                "is_collected BOOLEAN" +
                ");";
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void shutdown() {
        if (dataSource != null) {
            dataSource.close();
        }
    }

    @Override
    public CompletableFuture<Void> saveAuction(Auction auction) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT INTO xauctions_data VALUES (?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE is_sold=?, is_collected=?";
            try (Connection conn = dataSource.getConnection();
                    PreparedStatement ps = conn.prepareStatement(sql)) {
                // Mapping logic
                ps.setString(1, auction.getAuctionId().toString());
                ps.setString(2, auction.getSellerUuid().toString());
                ps.setString(3, auction.getSellerName());
                ps.setString(4, io.nightbeam.studio.xauctions.utils.ItemSerializer.toBase64(auction.getItemStack()));
                ps.setDouble(5, auction.getPrice());
                ps.setLong(6, auction.getStartTime());
                ps.setLong(7, auction.getExpireTime());
                ps.setBoolean(8, auction.isSold());
                ps.setBoolean(9, auction.isCollected());
                ps.setBoolean(10, auction.isSold());
                ps.setBoolean(11, auction.isCollected());
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
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
                e.printStackTrace();
            }
            return auctions;
        });
    }

    private Auction mapResultSetToAuction(ResultSet rs) throws SQLException {
        return Auction.builder()
                .auctionId(UUID.fromString(rs.getString("auction_id")))
                .sellerUuid(UUID.fromString(rs.getString("seller_uuid")))
                .sellerName(rs.getString("seller_name"))
                .itemStack(io.nightbeam.studio.xauctions.utils.ItemSerializer.fromBase64(rs.getString("item_base64")))
                .price(rs.getDouble("price"))
                .startTime(rs.getLong("start_time"))
                .expireTime(rs.getLong("expire_time"))
                .sold(rs.getBoolean("is_sold"))
                .collected(rs.getBoolean("is_collected"))
                .build();
    }

    @Override
    public CompletableFuture<List<Auction>> loadPlayerAuctions(UUID sellerUuid) {
        return CompletableFuture.completedFuture(new ArrayList<>());
    }

    @Override
    public void invalidateCache(UUID auctionId) {
        // SQL is stateless
    }
}
