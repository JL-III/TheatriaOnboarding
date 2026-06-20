package com.theatria.onboarding.hook;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Reflective hook into the Lands plugin so we can confirm a player has actually
 * claimed land (rather than merely having typed {@code /claim}). Reflection
 * keeps the plugin free of a compile-time Lands dependency and tolerant of API
 * version changes; on any failure it reports unavailable and the caller falls
 * back to command detection.
 */
public final class LandsHook {

    private final Logger logger;
    private Object integration;
    private Method getLandPlayer;
    private boolean bound;
    private boolean warned;

    public LandsHook(Plugin plugin, Logger logger) {
        this.logger = logger;
        Plugin lands = Bukkit.getPluginManager().getPlugin("Lands");
        if (lands == null || !lands.isEnabled()) {
            return;
        }
        try {
            Class<?> apiClass = Class.forName("me.angeschossen.lands.api.LandsIntegration");
            // Modern API exposes a static of(Plugin); older versions use a constructor.
            try {
                Method of = apiClass.getMethod("of", Plugin.class);
                this.integration = of.invoke(null, plugin);
            } catch (NoSuchMethodException noStaticFactory) {
                this.integration = apiClass.getConstructor(Plugin.class).newInstance(plugin);
            }
            this.getLandPlayer = apiClass.getMethod("getLandPlayer", UUID.class);
            this.getLandPlayer.setAccessible(true);
            this.bound = true;
        } catch (Throwable t) {
            logger.warning("Lands present but API hook failed to bind; using command detection for /claim. " + t);
        }
    }

    public boolean isAvailable() {
        return bound;
    }

    /**
     * True if the player belongs to at least one Lands claim. During onboarding
     * the only land a new player is part of is one they created via /claim, so a
     * non-empty result reliably means "they have claimed".
     */
    public boolean hasClaim(Player player) {
        if (!bound) {
            return false;
        }
        try {
            Object landPlayer = getLandPlayer.invoke(integration, player.getUniqueId());
            if (landPlayer == null) {
                return false;
            }
            Method getLands = landPlayer.getClass().getMethod("getLands");
            getLands.setAccessible(true);
            Object lands = getLands.invoke(landPlayer);
            return lands instanceof Collection<?> collection && !collection.isEmpty();
        } catch (Throwable t) {
            warnOnce(t);
            return false;
        }
    }

    private void warnOnce(Throwable t) {
        if (!warned) {
            warned = true;
            logger.warning("Lands hook call failed; using command detection for /claim instead. " + t);
        }
    }
}
