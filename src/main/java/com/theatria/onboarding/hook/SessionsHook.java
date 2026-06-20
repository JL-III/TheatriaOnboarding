package com.theatria.onboarding.hook;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Reflective hook into TheatriaSessions, the authoritative source for the daily
 * playtime reward. Completing the DAILY task through this hook reflects whether the
 * player has actually earned today's reward (active, non-AFK playtime past the
 * configured threshold, reset daily) instead of the vanilla
 * {@code Statistic.PLAY_ONE_MINUTE}, which counts lifetime and AFK time and never
 * resets.
 *
 * <p>Binds reflectively to the plugin's public {@code SessionsAPI} so this plugin
 * needs no compile-time dependency and tolerates version differences. If
 * TheatriaSessions is absent or the API can't be bound, it reports unavailable and
 * the caller falls back to the playtime-statistic check.
 */
public final class SessionsHook {

    private static final String API_CLASS = "com.playtheatria.sessions.api.SessionsAPI";

    private final Logger logger;
    private Method getApi; // static SessionsAPI.get()
    private Method hasEarnedDailyReward; // SessionsAPI#hasEarnedDailyReward(UUID)
    private boolean bound;
    private boolean warned;

    public SessionsHook(Logger logger) {
        this.logger = logger;
        Plugin sessions = Bukkit.getPluginManager().getPlugin("TheatriaSessions");
        if (sessions == null || !sessions.isEnabled()) {
            return;
        }
        try {
            Class<?> apiClass = Class.forName(API_CLASS);
            this.getApi = apiClass.getMethod("get");
            this.getApi.setAccessible(true);
            this.hasEarnedDailyReward = apiClass.getMethod("hasEarnedDailyReward", UUID.class);
            this.hasEarnedDailyReward.setAccessible(true);
            this.bound = true;
        } catch (Throwable t) {
            logger.warning("TheatriaSessions present but API hook failed to bind; using the "
                    + "playtime statistic for the daily reward instead. " + t);
        }
    }

    public boolean isAvailable() {
        return bound;
    }

    /**
     * True if TheatriaSessions reports the player has earned today's daily reward.
     * The API instance is resolved per call because {@code SessionsAPI.get()} returns
     * null while that plugin is disabled and is replaced on each enable.
     */
    public boolean hasEarnedReward(Player player) {
        if (!bound) {
            return false;
        }
        try {
            Object api = getApi.invoke(null);
            if (api == null) {
                return false; // TheatriaSessions disabled / reloading
            }
            Object result = hasEarnedDailyReward.invoke(api, player.getUniqueId());
            return result instanceof Boolean earned && earned;
        } catch (Throwable t) {
            warnOnce(t);
            return false;
        }
    }

    private void warnOnce(Throwable t) {
        if (!warned) {
            warned = true;
            logger.warning("TheatriaSessions hook call failed; using the playtime statistic for "
                    + "the daily reward instead. " + t);
        }
    }
}
