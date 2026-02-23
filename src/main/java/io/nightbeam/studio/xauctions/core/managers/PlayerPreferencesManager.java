package io.nightbeam.studio.xauctions.core.managers;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages per-player auction house GUI preferences (filter, sort order).
 *
 * <p>All preferences are stored in-memory only. They persist for the lifetime
 * of the player's session and are evicted on {@link org.bukkit.event.player.PlayerQuitEvent}.</p>
 */
public class PlayerPreferencesManager {

    private final ConcurrentHashMap<UUID, PlayerPreferences> preferences = new ConcurrentHashMap<>();

    /**
     * Returns the preferences for the given player, creating defaults if none exist.
     */
    public PlayerPreferences getOrCreate(UUID playerId) {
        return preferences.computeIfAbsent(playerId, id -> new PlayerPreferences());
    }

    /**
     * Removes stored preferences for the given player.
     * Call on player quit to prevent unbounded memory growth.
     */
    public void evict(UUID playerId) {
        preferences.remove(playerId);
    }

    /**
     * Returns the number of players currently tracked in the cache.
     */
    public int getCacheSize() {
        return preferences.size();
    }
}
