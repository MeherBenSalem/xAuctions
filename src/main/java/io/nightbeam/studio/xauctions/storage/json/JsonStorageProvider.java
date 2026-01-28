package io.nightbeam.studio.xauctions.storage.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import io.nightbeam.studio.xauctions.api.model.Auction;
import io.nightbeam.studio.xauctions.api.storage.StorageProvider;
import io.nightbeam.studio.xauctions.XAuctionsPlugin;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class JsonStorageProvider implements StorageProvider {

    private final XAuctionsPlugin plugin;
    private final Gson gson;
    private final File auctionsFile;
    private final File backupDir;

    // In-memory cache for fast access, synced to file periodically or on change
    private final Map<UUID, Auction> auctionCache = new ConcurrentHashMap<>();

    public JsonStorageProvider(XAuctionsPlugin plugin) {
        this.plugin = plugin;
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeHierarchyAdapter(org.bukkit.inventory.ItemStack.class,
                        new io.nightbeam.studio.xauctions.utils.ItemStackGsonAdapter())
                .create();
        this.auctionsFile = new File(plugin.getDataFolder(), "data/auctions.json");
        this.backupDir = new File(plugin.getDataFolder(), "backups");
    }

    @Override
    public void init() {
        if (!auctionsFile.getParentFile().exists()) {
            auctionsFile.getParentFile().mkdirs();
        }
        if (!backupDir.exists()) {
            backupDir.mkdirs();
        }

        // Initial load
        loadFromFile();

        // Start auto-backup task
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, this::createBackup, 6000L, 6000L); // Every
                                                                                                                // 5
                                                                                                                // mins
    }

    @Override
    public void shutdown() {
        saveToFile();
    }

    private void loadFromFile() {
        if (!auctionsFile.exists())
            return;

        try (Reader reader = new FileReader(auctionsFile)) {
            List<Auction> list = gson.fromJson(reader, new TypeToken<List<Auction>>() {
            }.getType());
            if (list != null) {
                list.forEach(a -> auctionCache.put(a.getAuctionId(), a));
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load auctions.json", e);
        }
    }

    private void saveToFile() {
        try (Writer writer = new FileWriter(auctionsFile)) {
            gson.toJson(new ArrayList<>(auctionCache.values()), writer);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save auctions.json", e);
        }
    }

    private void createBackup() {
        if (!auctionsFile.exists())
            return;
        try {
            String timestamp = String.valueOf(System.currentTimeMillis());
            File backup = new File(backupDir, "auctions_" + timestamp + ".json");
            Files.copy(auctionsFile.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);

            // Cleanup old backups (keep last 10)
            File[] files = backupDir.listFiles();
            if (files != null && files.length > 10) {
                Arrays.sort(files, Comparator.comparingLong(File::lastModified));
                for (int i = 0; i < files.length - 10; i++) {
                    files[i].delete();
                }
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to create JSON backup: " + e.getMessage());
        }
    }

    @Override
    public CompletableFuture<Void> saveAuction(Auction auction) {
        return CompletableFuture.runAsync(() -> {
            auctionCache.put(auction.getAuctionId(), auction);
            saveToFile(); // In production, maybe throttle this
        });
    }

    @Override
    public CompletableFuture<Void> updateAuction(Auction auction) {
        return saveAuction(auction);
    }

    @Override
    public CompletableFuture<Void> deleteAuction(UUID auctionId) {
        return CompletableFuture.runAsync(() -> {
            auctionCache.remove(auctionId);
            saveToFile();
        });
    }

    @Override
    public CompletableFuture<List<Auction>> loadActiveAuctions() {
        return CompletableFuture.supplyAsync(() -> auctionCache.values().stream()
                .filter(a -> !a.isSold() && !a.isDeleted() && !a.isExpired())
                .toList());
    }

    @Override
    public CompletableFuture<List<Auction>> loadPlayerAuctions(UUID sellerUuid) {
        return CompletableFuture.supplyAsync(() -> auctionCache.values().stream()
                .filter(a -> a.getSellerUuid().equals(sellerUuid))
                .toList());
    }

    @Override
    public void invalidateCache(UUID auctionId) {
        // For JSON (flat file), if another server updated the file (via NFS/shared
        // mount), we should reload.
        // But typically JSON is single-server or strictly synced via FS events.
        // If Redis says update, we might re-read from disk if we suspect disk changed.
        // Here, we'll just remove from cache so next fetch loads from file/source.
        auctionCache.remove(auctionId);
        loadFromFile(); // Reloading all might be heavy, but safe for JSON.
    }
}
