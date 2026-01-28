package io.nightbeam.studio.xauctions.core.managers;

import io.nightbeam.studio.xauctions.XAuctionsPlugin;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class PlayerBlacklistManager {

    private final XAuctionsPlugin plugin;
    private final Set<UUID> blacklistedUuids = new HashSet<>();

    public PlayerBlacklistManager(XAuctionsPlugin plugin) {
        this.plugin = plugin;
    }

    public void init() {
        loadConfig();
    }

    public void loadConfig() {
        blacklistedUuids.clear();
        List<String> list = plugin.getConfig().getStringList("blacklist.players");
        for (String s : list) {
            try {
                blacklistedUuids.add(UUID.fromString(s));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public boolean isBlacklisted(OfflinePlayer player) {
        return blacklistedUuids.contains(player.getUniqueId());
    }

    public void blacklist(OfflinePlayer player) {
        blacklistedUuids.add(player.getUniqueId());
        save();
    }

    public void unblacklist(OfflinePlayer player) {
        blacklistedUuids.remove(player.getUniqueId());
        save();
    }

    private void save() {
        // Update config
        List<String> list = blacklistedUuids.stream().map(UUID::toString).toList();
        plugin.getConfig().set("blacklist.players", list);
        plugin.saveConfig();
    }
}
