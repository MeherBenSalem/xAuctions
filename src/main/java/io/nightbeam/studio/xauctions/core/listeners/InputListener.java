package io.nightbeam.studio.xauctions.core.listeners;

import io.nightbeam.studio.xauctions.xAuctions;
import io.nightbeam.studio.xauctions.gui.menus.AuctionsMenu;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class InputListener implements Listener {

    private final xAuctions plugin;
    private final Set<UUID> awaitingSearch = new HashSet<>();

    public InputListener(xAuctions plugin) {
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

            // Sync to main thread to open GUI
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                plugin.getGuiManager().openMenu(player, new AuctionsMenu(plugin, query));
            });
        }
    }
}
