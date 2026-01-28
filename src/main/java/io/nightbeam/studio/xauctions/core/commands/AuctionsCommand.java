package io.nightbeam.studio.xauctions.core.commands;

import io.nightbeam.studio.xauctions.xAuctions;
import io.nightbeam.studio.xauctions.gui.menus.MainMenu;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class AuctionsCommand implements CommandExecutor {

    private final xAuctions plugin;

    public AuctionsCommand(xAuctions plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command!");
            return true;
        }

        plugin.getGuiManager().openMenu(player, new MainMenu(plugin));
        return true;
    }
}
