package com.theatria.onboarding;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.Locale;

/**
 * Drives progress from gameplay:
 * <ul>
 *   <li>loads/saves data around join and quit, and opens the guide on first join;</li>
 *   <li>completes RTP when the player leaves spawn (the portal teleport, or any
 *       long-distance / cross-world teleport) — they may keep using /rtp after;</li>
 *   <li>completes command-driven tasks (RANKUP, and SETHOME/CLAIM only when their
 *       Essentials/Lands hook is unavailable) when the player runs them.</li>
 * </ul>
 * SETHOME and CLAIM are normally confirmed by {@link ProgressManager#recheck} via
 * the plugin hooks; a short delayed recheck after each command catches the new
 * state right after the command executes.
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

    /** RTP completes the first time the player is flung out into the wild. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        if (plugin.progress().isComplete(player.getUniqueId(), TaskId.RTP)) {
            return;
        }
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) {
            return;
        }
        double minDistance = plugin.getConfig().getDouble("rtp-min-distance", 100.0);
        boolean leftSpawn = from.getWorld() == null || to.getWorld() == null
                || !from.getWorld().equals(to.getWorld())
                || from.distanceSquared(to) >= minDistance * minDistance;
        if (leftSpawn) {
            plugin.progress().complete(player, TaskId.RTP, "teleport");
        }
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

        // RANKUP is normally confirmed via the Rankup event (live) and the
        // LuckPerms check (retroactive); only fall back to command detection when
        // neither is available.
        if (!plugin.rankupHook().isAvailable() && !plugin.luckPermsHook().isAvailable()) {
            check(player, message, "commands.rankup", TaskId.RANKUP);
        }

        // SETHOME / CLAIM are normally confirmed via the Essentials / Lands hooks
        // in recheck(); only fall back to command detection when a hook is absent.
        if (!plugin.essentialsHook().isAvailable()) {
            check(player, message, "commands.sethome", TaskId.SETHOME);
        }
        if (!plugin.landsHook().isAvailable()
                && plugin.progress().isComplete(player.getUniqueId(), TaskId.EARN)) {
            // Without the Lands hook, only credit /claim once they've hit the money
            // target — a bare /claim without funds would otherwise false-complete it.
            check(player, message, "commands.claim", TaskId.CLAIM);
        }

        // Sell only completes EARN when there's no economy hook to read balances.
        if (plugin.economy() == null) {
            check(player, message, "commands.sell", TaskId.EARN);
        }

        // Re-check shortly after so hook-based tasks (home/claim) see post-execution state.
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                plugin.progress().recheck(player);
            }
        }, 5L);
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
                        () -> plugin.progress().complete(player, task, "command"));
                return;
            }
        }
    }
}
