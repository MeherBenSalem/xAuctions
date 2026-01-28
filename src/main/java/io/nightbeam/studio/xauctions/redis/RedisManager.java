package io.nightbeam.studio.xauctions.redis;

import io.nightbeam.studio.xauctions.XAuctionsPlugin;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;

import java.util.concurrent.CompletableFuture;

public class RedisManager {

    private final XAuctionsPlugin plugin;
    private JedisPool jedisPool;
    private JedisPubSub pubSub;

    public RedisManager(XAuctionsPlugin plugin) {
        this.plugin = plugin;
    }

    public void init() {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(16);

        // TODO: Load from config
        jedisPool = new JedisPool(config, "localhost", 6379);

        // Start subscription thread
        startSubscription();
    }

    private java.util.function.BiConsumer<String, String> listener;

    public void setListener(java.util.function.BiConsumer<String, String> listener) {
        this.listener = listener;
    }

    private void startSubscription() {
        pubSub = new JedisPubSub() {
            @Override
            public void onMessage(String channel, String message) {
                if (listener != null) {
                    listener.accept(channel, message);
                }
            }
        };

        new Thread(() -> {
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.subscribe(pubSub, "xauctions:update");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void publishUpdate(String message) {
        CompletableFuture.runAsync(() -> {
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.publish("xauctions:update", message);
            }
        });
    }

    public void shutdown() {
        if (pubSub != null && pubSub.isSubscribed()) {
            pubSub.unsubscribe();
        }
        if (jedisPool != null) {
            jedisPool.close();
        }
    }
}
