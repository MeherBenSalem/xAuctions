package io.nightbeam.studio.xauctions.core.managers;

import io.nightbeam.studio.xauctions.XAuctionsPlugin;
import io.nightbeam.studio.xauctions.api.storage.StorageProvider;
import io.nightbeam.studio.xauctions.redis.RedisManager;

import java.util.UUID;

public class NetworkSyncManager {

    private final XAuctionsPlugin plugin;
    private final RedisManager redisManager;
    private StorageProvider storageProvider;

    public NetworkSyncManager(XAuctionsPlugin plugin) {
        this.plugin = plugin;
        this.redisManager = new RedisManager(plugin);
    }

    public void init(StorageProvider storageProvider) {
        this.storageProvider = storageProvider;
        this.redisManager.setListener(this::handleMessage);
        this.redisManager.init();
    }

    public void handleMessage(String channel, String message) {
        if (!channel.equals("xauctions:update"))
            return;

        try {
            // Message format: "UPDATE:uuid"
            if (message.startsWith("UPDATE:")) {
                String uuidStr = message.split(":")[1];
                UUID uuid = UUID.fromString(uuidStr);
                plugin.getLogger().info("Network sync: invalidating auction " + uuid);
                if (storageProvider != null) {
                    storageProvider.invalidateCache(uuid);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void broadcastUpdate(UUID auctionId) {
        redisManager.publishUpdate("UPDATE:" + auctionId.toString());
    }

    public void shutdown() {
        redisManager.shutdown();
    }
}
