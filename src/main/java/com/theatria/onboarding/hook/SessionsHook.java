package com.theatria.onboarding.hook;

import com.playtheatria.sessions.api.SessionsAPI;
import com.theatria.onboarding.TheatriaOnboarding;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Hook into TheatriaSessions, the authoritative source for the daily playtime
 * reward. Completing the DAILY task through this hook reflects whether the player
 * has actually earned today's reward (active, non-AFK playtime past the configured
 * threshold, reset daily) instead of the vanilla {@code Statistic.PLAY_ONE_MINUTE},
 * which counts lifetime and AFK time and never resets.
 *
 * <p>Unlike the third-party hooks (Essentials/Lands/Rankup/LuckPerms), which use
 * reflection because we don't control those plugins, TheatriaSessions is
 * first-party: we depend on its published {@code SessionsAPI} directly (compile-time)
 * and call it without reflection. It stays a <em>soft</em> dependency: a
 * plugin-presence check gates access, and every call is wrapped so that if
 * TheatriaSessions is absent OR an incompatible version is installed (so the
 * {@code SessionsAPI} class is missing and resolving it throws
 * {@link NoClassDefFoundError}), the hook reports not-earned/unavailable and the
 * caller falls back to the playtime statistic — rather than letting the linkage
 * error abort {@code recheck} and take SETHOME/CLAIM/RANKUP down with it.
 */
public final class SessionsHook {

    private final TheatriaOnboarding plugin;
    private final boolean present;
    private boolean warned;

    public SessionsHook(TheatriaOnboarding plugin) {
        this.plugin = plugin;
        this.present = Bukkit.getPluginManager().getPlugin("TheatriaSessions") != null;
        plugin.debug("SessionsHook: TheatriaSessions " + (present ? "present" : "absent")
                + " — DAILY uses " + (present ? "the SessionsAPI" : "the playtime statistic"));
    }

    /** True when TheatriaSessions is installed. Gate all SessionsAPI use on this. */
    public boolean isAvailable() {
        return present;
    }

    /**
     * True if TheatriaSessions reports the player has earned today's daily reward.
     * Returns false (warning once) if the API is unavailable or a call fails — e.g.
     * the plugin is mid-(re)load, or an incompatible version lacks {@code SessionsAPI}.
     */
    public boolean hasEarnedReward(Player player) {
        if (!present) {
            return false;
        }
        try {
            SessionsAPI api = SessionsAPI.get();
            if (api == null) {
                warnOnce("SessionsAPI.get() returned null (TheatriaSessions disabled or mid-reload)");
                return false;
            }
            boolean earned = api.hasEarnedDailyReward(player.getUniqueId());
            if (plugin.isDebug()) {
                UUID id = player.getUniqueId();
                plugin.debug("SessionsHook: " + player.getName() + " earned=" + earned
                        + " [" + api.getSessionSeconds(id) + "/" + api.getThresholdSeconds()
                        + "s active, metThreshold=" + api.hasMetThreshold(id)
                        + ", hasSession=" + api.hasSession(id) + "]");
            }
            return earned;
        } catch (Throwable t) {
            warnOnce("SessionsAPI call failed — is TheatriaSessions a compatible version? " + t);
            return false;
        }
    }

    /**
     * The daily-reward active-playtime threshold in whole minutes, or {@code -1} when
     * TheatriaSessions is unavailable (so callers fall back to their own configured
     * value). Lets the book show the live requirement instead of a hardcoded number.
     */
    public int thresholdMinutes() {
        if (!present) {
            return -1;
        }
        try {
            SessionsAPI api = SessionsAPI.get();
            if (api == null) {
                return -1;
            }
            return Math.round(api.getThresholdSeconds() / 60.0f);
        } catch (Throwable t) {
            warnOnce("SessionsAPI call failed — is TheatriaSessions a compatible version? " + t);
            return -1;
        }
    }

    /**
     * A one-line, human-readable snapshot of the Sessions side for the {@code /tutorial
     * debug} dump. Safe to call even when TheatriaSessions is absent or incompatible.
     */
    public String describe(Player player) {
        if (!present) {
            return "TheatriaSessions absent — DAILY uses the playtime statistic";
        }
        try {
            SessionsAPI api = SessionsAPI.get();
            if (api == null) {
                return "TheatriaSessions present but SessionsAPI unavailable (disabled/mid-reload?)";
            }
            UUID id = player.getUniqueId();
            return String.format(
                    "session=%d/%ds active, metThreshold=%b, earnedReward=%b, hasSession=%b",
                    api.getSessionSeconds(id), api.getThresholdSeconds(),
                    api.hasMetThreshold(id), api.hasEarnedDailyReward(id), api.hasSession(id));
        } catch (Throwable t) {
            return "SessionsAPI call failed (incompatible TheatriaSessions version?): " + t;
        }
    }

    private void warnOnce(String reason) {
        if (!warned) {
            warned = true;
            plugin.getLogger().warning("TheatriaSessions hook: " + reason
                    + ". DAILY falls through as not-earned until resolved.");
        }
    }
}
