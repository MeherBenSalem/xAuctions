package io.nightbeam.studio.xauctions.core.managers;

import io.nightbeam.studio.xauctions.XAuctionsPlugin;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;

public class MessageManager {

    private final XAuctionsPlugin plugin;
    private FileConfiguration messages;
    private File messagesFile;

    public MessageManager(XAuctionsPlugin plugin) {
        this.plugin = plugin;
        init();
    }

    private void init() {
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messages = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public void reload() {
        messages = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public String get(String key) {
        String prefix = messages.getString("prefix", "&8[&6xAuctions&8] ");
        String msg = messages.getString(key, "&cMissing message: " + key);
        return color(prefix + msg);
    }

    public String getRaw(String key) {
        return color(messages.getString(key, "&cMissing message: " + key));
    }

    private String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    public void send(Player player, String key) {
        player.sendMessage(get(key));
    }

    public void send(Player player, String key, String... placeholders) {
        String msg = get(key);
        for (int i = 0; i < placeholders.length; i += 2) {
            String target = placeholders[i];
            String replacement = (i + 1 < placeholders.length) ? placeholders[i + 1] : "";
            msg = msg.replace(target, replacement);
        }
        player.sendMessage(msg);
    }
}
