package com.theatria.onboarding.hook;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Hook into the Rankup plugin (sh.okx.rankup, "Rankup3"). Instead of guessing
 * from the {@code /rankup} command, it listens for the plugin's real
 * {@code PlayerRankupEvent}, which only fires on a successful rank-up — so a
 * {@code /rankup} attempt without enough money won't complete the task.
 *
 * <p>The event is registered dynamically by class name with a reflective
 * executor, so the plugin needs no compile-time Rankup dependency. If the plugin
 * is absent or the event can't be bound, this reports unavailable and the caller
 * falls back to command detection.
 */
public final class RankupHook implements Listener {

    private final Logger logger;
    private boolean bound;
    private boolean warned;

    public RankupHook(Plugin plugin, Logger logger, Consumer<Player> onRankup) {
        this.logger = logger;
        Plugin rankup = Bukkit.getPluginManager().getPlugin("Rankup");
        if (rankup == null || !rankup.isEnabled()) {
            return;
        }
        try {
            @SuppressWarnings("unchecked")
            Class<? extends Event> eventClass =
                    (Class<? extends Event>) Class.forName("sh.okx.rankup.events.PlayerRankupEvent");
            Bukkit.getPluginManager().registerEvent(
                    eventClass, this, EventPriority.MONITOR,
                    (listener, event) -> {
                        try {
                            Object player = event.getClass().getMethod("getPlayer").invoke(event);
                            if (player instanceof Player p) {
                                onRankup.accept(p);
                            }
                        } catch (Throwable t) {
                            warnOnce(t);
                        }
                    },
                    plugin);
            this.bound = true;
        } catch (Throwable t) {
            logger.warning("Rankup present but event hook failed to bind; using command detection for /rankup. " + t);
        }
    }

    public boolean isAvailable() {
        return bound;
    }

    private void warnOnce(Throwable t) {
        if (!warned) {
            warned = true;
            logger.warning("Rankup event hook failed; using command detection for /rankup instead. " + t);
        }
    }
}
