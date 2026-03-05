package io.nightbeam.studio.xauctions.core.listeners;

import io.nightbeam.studio.xauctions.XAuctionsPlugin;
import io.nightbeam.studio.xauctions.gui.menus.AuctionsMenu;
import io.nightbeam.studio.xauctions.utils.SchedulerUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

public class InputListener implements Listener {

    private final XAuctionsPlugin plugin;
    // HashSet is not thread-safe; chat events come from async threads so use a concurrent set
    private final Set<UUID> awaitingSearch = Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());

    public InputListener(XAuctionsPlugin plugin) {
        this.plugin = plugin;
    }

    public void awaitSearch(Player player) {
        awaitingSearch.add(player.getUniqueId());
        player.sendMessage("§aEnter your search query in chat (or type 'cancel'):");
        player.closeInventory();
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (awaitingSearch.contains(player.getUniqueId())) {
            event.setCancelled(true);
            awaitingSearch.remove(player.getUniqueId());

            String query = event.getMessage();
            if (query.equalsIgnoreCase("cancel")) {
                player.sendMessage("§cSearch cancelled.");
                return; // Optionally open main menu
            }

            // Sync to main thread to open GUI (use scheduler util to be Folia-safe)
            SchedulerUtils.run(plugin, player, () -> {
                plugin.getGuiManager().openMenu(player, new AuctionsMenu(plugin, query));
            });
        }
    }
}
