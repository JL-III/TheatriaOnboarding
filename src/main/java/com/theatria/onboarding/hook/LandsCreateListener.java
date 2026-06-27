package com.theatria.onboarding.hook;

import com.theatria.onboarding.TaskId;
import com.theatria.onboarding.TheatriaOnboarding;
import me.angeschossen.lands.api.events.land.create.LandPostCreateEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.UUID;

/**
 * Typed listener for Lands' post-create event — completes the CLAIM task the moment
 * a player creates their land, instead of waiting up to 30s for the polling recheck
 * (which lags because {@code /lands create} finishes via a GUI, not synchronously at
 * the command).
 *
 * <p>Uses the published Lands API directly (no reflection). It stays a <em>soft</em>
 * dependency: this listener is registered only when Lands is installed (see
 * {@link TheatriaOnboarding#onEnable}), so the {@link LandPostCreateEvent} class is
 * never resolved when Lands is absent. {@code LandsHook}'s poll remains the backstop
 * for anything this misses (e.g. a land created while the plugin is mid-reload).
 */
public final class LandsCreateListener implements Listener {

    private final TheatriaOnboarding plugin;

    public LandsCreateListener(TheatriaOnboarding plugin) {
        this.plugin = plugin;
    }

    /** Fires after a land is actually created; not cancellable, so MONITOR is fine. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onLandCreate(LandPostCreateEvent event) {
        UUID uuid = event.getPlayerUUID(); // null when created without a player (console/admin)
        if (uuid == null) {
            return;
        }
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            // The land really exists and they paid for it, so credit CLAIM directly
            // (no EARN gate needed — the command fallback's gate guards bare /claim).
            plugin.progress().complete(player, TaskId.CLAIM, "Lands event");
        }
    }
}
