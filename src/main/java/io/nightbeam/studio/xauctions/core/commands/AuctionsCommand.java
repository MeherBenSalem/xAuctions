package io.nightbeam.studio.xauctions.core.commands;

import io.nightbeam.studio.xauctions.XAuctionsPlugin;
import io.nightbeam.studio.xauctions.gui.menus.MainMenu;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import java.util.Arrays;

public class AuctionsCommand implements CommandExecutor {

    private final XAuctionsPlugin plugin;

    public AuctionsCommand(XAuctionsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command!");
            return true;
        }

        // Support subcommand: /ah sell <price> [amount]
        if (args.length > 0 && args[0].equalsIgnoreCase("sell")) {
            // delegate to SellCommand with args shifted (skip "sell")
            var sellCmd = new SellCommand(plugin);
            String[] subArgs = (args.length > 1) ? Arrays.copyOfRange(args, 1, args.length) : new String[0];
            return sellCmd.onCommand(sender, command, label, subArgs);
        }

        plugin.getGuiManager().openMenu(player, new MainMenu(plugin));
        return true;
    }
}
