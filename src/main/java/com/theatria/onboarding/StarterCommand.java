package com.theatria.onboarding;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Handles {@code /starter} (open the guide) and {@code /starter reset [player]}. */
public class StarterCommand implements TabExecutor {

    private final TheatriaOnboarding plugin;

    public StarterCommand(TheatriaOnboarding plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length >= 1 && args[0].equalsIgnoreCase("reset")) {
            return handleReset(sender, args);
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can open the guide.", NamedTextColor.RED));
            return true;
        }
        if (!player.hasPermission("theatria.onboarding.use")) {
            player.sendMessage(Component.text("You don't have permission to do that.", NamedTextColor.RED));
            return true;
        }

        plugin.progress().recheck(player);
        player.openBook(plugin.renderer().build(plugin.progress().get(player.getUniqueId())));
        return true;
    }

    private boolean handleReset(CommandSender sender, String[] args) {
        if (!sender.hasPermission("theatria.onboarding.admin")) {
            sender.sendMessage(Component.text("You don't have permission to do that.", NamedTextColor.RED));
            return true;
        }

        Player target;
        if (args.length >= 2) {
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage(Component.text("Player not found: " + args[1], NamedTextColor.RED));
                return true;
            }
        } else if (sender instanceof Player self) {
            target = self;
        } else {
            sender.sendMessage(Component.text("Usage: /starter reset <player>", NamedTextColor.RED));
            return true;
        }

        plugin.progress().reset(target.getUniqueId());
        sender.sendMessage(Component.text("Reset onboarding progress for " + target.getName() + ".",
                NamedTextColor.GREEN));
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1 && sender.hasPermission("theatria.onboarding.admin")) {
            if ("reset".startsWith(args[0].toLowerCase())) {
                return Collections.singletonList("reset");
            }
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("reset")
                && sender.hasPermission("theatria.onboarding.admin")) {
            List<String> names = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                    names.add(p.getName());
                }
            }
            return names;
        }
        return Collections.emptyList();
    }
}
