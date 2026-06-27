package com.theatria.onboarding.hook;

import me.angeschossen.lands.api.LandsIntegration;
import me.angeschossen.lands.api.player.LandPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.logging.Logger;

/**
 * Hook into the Lands plugin so we can confirm a player has actually claimed land
 * (rather than merely having typed {@code /lands create}). This is the polling
 * backstop; {@link LandsCreateListener} completes CLAIM instantly via the live
 * post-create event.
 *
 * <p>Uses the published Lands API directly (no reflection). It stays a <em>soft</em>
 * dependency: the Lands types are only touched after a by-name plugin-presence
 * check, so they never resolve when Lands is absent; on any failure it reports
 * unavailable and the caller falls back to command detection.
 */
public final class LandsHook {

    private final Logger logger;
    private LandsIntegration integration; // null until/unless bound
    private boolean bound;
    private boolean warned;

    public LandsHook(Plugin plugin, Logger logger) {
        this.logger = logger;
        Plugin lands = Bukkit.getPluginManager().getPlugin("Lands");
        if (lands == null || !lands.isEnabled()) {
            return; // Lands types below never reached when it's absent
        }
        try {
            this.integration = LandsIntegration.of(plugin);
            this.bound = true;
        } catch (Throwable t) {
            logger.warning("Lands present but API hook failed to bind; using command detection for /lands create. " + t);
        }
    }

    public boolean isAvailable() {
        return bound;
    }

    /**
     * True if the player belongs to at least one Lands claim. During onboarding the
     * only land a new player is part of is one they created via /lands create, so a
     * non-empty result reliably means "they have claimed".
     */
    public boolean hasClaim(Player player) {
        if (!bound) {
            return false;
        }
        try {
            LandPlayer landPlayer = integration.getLandPlayer(player.getUniqueId());
            return landPlayer != null && !landPlayer.getLands().isEmpty();
        } catch (Throwable t) {
            warnOnce(t);
            return false;
        }
    }

    private void warnOnce(Throwable t) {
        if (!warned) {
            warned = true;
            logger.warning("Lands hook call failed; using command detection for /lands create instead. " + t);
        }
    }
}
