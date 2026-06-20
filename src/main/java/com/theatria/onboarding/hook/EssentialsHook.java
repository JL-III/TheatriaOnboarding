package com.theatria.onboarding.hook;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Reflective hook into EssentialsX so we can confirm a player actually has a
 * home set (rather than merely having typed {@code /sethome}). Uses reflection
 * so the plugin needs no compile-time Essentials dependency and tolerates
 * version differences; if anything fails it reports unavailable and the caller
 * falls back to command detection.
 */
public final class EssentialsHook {

    private final Logger logger;
    private Plugin essentials;
    private boolean bound;
    private boolean warned;

    public EssentialsHook(Logger logger) {
        this.logger = logger;
        Plugin plugin = Bukkit.getPluginManager().getPlugin("Essentials");
        if (plugin != null && plugin.isEnabled()) {
            this.essentials = plugin;
            this.bound = true; // methods are resolved per call, defensively
        }
    }

    public boolean isAvailable() {
        return bound;
    }

    /** True if the player has at least one Essentials home. */
    public boolean hasHome(Player player) {
        if (!bound) {
            return false;
        }
        try {
            Object user = resolveUser(player);
            if (user == null) {
                return false;
            }
            Method getHomes = user.getClass().getMethod("getHomes");
            getHomes.setAccessible(true);
            Object homes = getHomes.invoke(user);
            return homes instanceof Collection<?> collection && !collection.isEmpty();
        } catch (Throwable t) {
            warnOnce(t);
            return false;
        }
    }

    private Object resolveUser(Player player) throws Exception {
        try {
            Method byPlayer = essentials.getClass().getMethod("getUser", Player.class);
            byPlayer.setAccessible(true);
            return byPlayer.invoke(essentials, player);
        } catch (NoSuchMethodException ignored) {
            Method byUuid = essentials.getClass().getMethod("getUser", UUID.class);
            byUuid.setAccessible(true);
            return byUuid.invoke(essentials, player.getUniqueId());
        }
    }

    private void warnOnce(Throwable t) {
        if (!warned) {
            warned = true;
            logger.warning("Essentials hook failed; using command detection for /sethome instead. " + t);
        }
    }
}
