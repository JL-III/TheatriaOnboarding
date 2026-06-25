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
import java.util.UUID;

/**
 * Handles {@code /starter} (open the guide), {@code /starter reset [player]}, and
 * {@code /starter debug [player]} (a runtime snapshot for fault-finding).
 */
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
        if (args.length >= 1 && args[0].equalsIgnoreCase("debug")) {
            return handleDebug(sender, args);
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

    /**
     * Dumps a live snapshot for a player: each task's completion state, which detection
     * hooks are available, and the Sessions side's view of the DAILY reward. This is the
     * fastest way to answer "why is (or isn't) DAILY completing for this player right now".
     */
    private boolean handleDebug(CommandSender sender, String[] args) {
        if (!sender.hasPermission("theatria.onboarding.admin")) {
            sender.sendMessage(Component.text("You don't have permission to do that.", NamedTextColor.RED));
            return true;
        }

        Player target;
        if (args.length >= 2) {
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage(Component.text("Player not found (must be online): " + args[1],
                        NamedTextColor.RED));
                return true;
            }
        } else if (sender instanceof Player self) {
            target = self;
        } else {
            sender.sendMessage(Component.text("Usage: /starter debug <player>", NamedTextColor.RED));
            return true;
        }

        UUID uuid = target.getUniqueId();
        plugin.progress().recheck(target); // refresh the snapshot before reporting

        sender.sendMessage(Component.text("=== Onboarding debug: " + target.getName() + " ===",
                NamedTextColor.AQUA));

        StringBuilder tasks = new StringBuilder();
        for (TaskId task : TaskId.values()) {
            tasks.append(task.name())
                    .append(plugin.progress().isComplete(uuid, task) ? " ✓  " : " ✗  ");
        }
        sender.sendMessage(Component.text("Tasks: " + tasks.toString().trim(), NamedTextColor.GRAY));

        sender.sendMessage(Component.text("Hooks: "
                + "Vault=" + (plugin.economy() != null)
                + " Essentials=" + plugin.essentialsHook().isAvailable()
                + " Lands=" + plugin.landsHook().isAvailable()
                + " Rankup=" + plugin.rankupHook().isAvailable()
                + " LuckPerms=" + plugin.luckPermsHook().isAvailable()
                + " Sessions=" + plugin.sessionsHook().isAvailable(), NamedTextColor.GRAY));

        sender.sendMessage(Component.text("DAILY/Sessions: " + plugin.sessionsHook().describe(target),
                NamedTextColor.GRAY));
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1 && sender.hasPermission("theatria.onboarding.admin")) {
            List<String> subs = new ArrayList<>();
            for (String sub : List.of("reset", "debug")) {
                if (sub.startsWith(args[0].toLowerCase())) {
                    subs.add(sub);
                }
            }
            return subs;
        }
        if (args.length == 2
                && (args[0].equalsIgnoreCase("reset") || args[0].equalsIgnoreCase("debug"))
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
