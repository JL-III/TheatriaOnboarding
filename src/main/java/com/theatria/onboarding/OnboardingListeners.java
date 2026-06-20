package com.theatria.onboarding;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;
import java.util.Locale;

/**
 * Drives progress from gameplay: loads/saves data around join and quit, opens
 * the guide on first join, and completes command-driven tasks (RTP, SETHOME,
 * CLAIM, RANKUP, and the SELL fallback for EARN) when the player runs them.
 */
public class OnboardingListeners implements Listener {

    private final TheatriaOnboarding plugin;

    public OnboardingListeners(TheatriaOnboarding plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        boolean first = plugin.progress().isFirstJoin(player.getUniqueId());

        plugin.progress().load(player.getUniqueId());
        plugin.progress().markSeen(player);
        plugin.progress().recheck(player);

        if (first && plugin.getConfig().getBoolean("auto-open-first-join", true)) {
            // Slight delay so the client is ready to receive the book.
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    player.openBook(plugin.renderer().build(plugin.progress().get(player.getUniqueId())));
                }
            }, 40L);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.progress().unload(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();
        if (message.startsWith("/")) {
            message = message.substring(1);
        }
        message = message.trim().toLowerCase(Locale.ROOT);

        // Make sure balance-driven progress (EARN) is current before we use it.
        plugin.progress().recheck(player);

        check(player, message, "commands.rtp", TaskId.RTP);
        check(player, message, "commands.sethome", TaskId.SETHOME);
        // Only credit CLAIM once they've actually reached the money target — a
        // bare /claim without funds would otherwise mark it done prematurely.
        if (plugin.progress().isComplete(player.getUniqueId(), TaskId.EARN)) {
            check(player, message, "commands.claim", TaskId.CLAIM);
        }
        check(player, message, "commands.rankup", TaskId.RANKUP);
        // Sell only completes EARN when there's no economy hook to read balances.
        if (plugin.economy() == null) {
            check(player, message, "commands.sell", TaskId.EARN);
        }
    }

    private void check(Player player, String message, String configPath, TaskId task) {
        if (plugin.progress().isComplete(player.getUniqueId(), task)) {
            return;
        }
        for (String alias : plugin.getConfig().getStringList(configPath)) {
            alias = alias.toLowerCase(Locale.ROOT);
            if (message.equals(alias) || message.startsWith(alias + " ")) {
                // Run next tick so the underlying command resolves first.
                plugin.getServer().getScheduler().runTask(plugin,
                        () -> plugin.progress().complete(player, task));
                return;
            }
        }
    }
}
