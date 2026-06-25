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
 * first-party: we depend on its published {@code SessionsAPI} directly (compile-time,
 * {@code provided} scope) and call it with no reflection. It stays a <em>soft</em>
 * dependency — every use of {@link SessionsAPI} is gated by {@link #isAvailable()}
 * (a plugin-presence check that touches no Sessions classes), so when the plugin is
 * absent its classes are never loaded and the caller falls back to the playtime
 * statistic.
 */
public final class SessionsHook {

    private final TheatriaOnboarding plugin;
    private final boolean present;
    private boolean warnedNullApi;

    public SessionsHook(TheatriaOnboarding plugin) {
        this.plugin = plugin;
        this.present = Bukkit.getPluginManager().getPlugin("TheatriaSessions") != null;
        plugin.debug("SessionsHook: TheatriaSessions " + (present ? "present" : "absent")
                + " — DAILY uses " + (present ? "the SessionsAPI" : "the playtime statistic"));
    }

    /** True when TheatriaSessions is installed. Gate all {@link #hasEarnedReward} calls on this. */
    public boolean isAvailable() {
        return present;
    }

    /**
     * True if TheatriaSessions reports the player has earned today's daily reward.
     * Only call when {@link #isAvailable()}. {@code SessionsAPI.get()} is null while
     * that plugin is mid-(re)load, in which case this returns false (and warns once).
     */
    public boolean hasEarnedReward(Player player) {
        if (!present) {
            return false;
        }
        SessionsAPI api = SessionsAPI.get();
        if (api == null) {
            if (!warnedNullApi) {
                warnedNullApi = true;
                plugin.getLogger().warning("TheatriaSessions is installed but SessionsAPI.get() "
                        + "returned null (plugin disabled or mid-reload?); DAILY can't be confirmed "
                        + "until it is back. Falling through as not-earned for now.");
            }
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
    }

    /**
     * A one-line, human-readable snapshot of the Sessions side for the {@code /starter
     * debug} dump. Keeps all {@link SessionsAPI} access behind the presence guard so it
     * is safe to call even when TheatriaSessions is absent.
     */
    public String describe(Player player) {
        if (!present) {
            return "TheatriaSessions absent — DAILY uses the playtime statistic";
        }
        SessionsAPI api = SessionsAPI.get();
        if (api == null) {
            return "TheatriaSessions present but SessionsAPI unavailable (disabled/mid-reload?)";
        }
        UUID id = player.getUniqueId();
        return String.format(
                "session=%d/%ds active, metThreshold=%b, earnedReward=%b, hasSession=%b",
                api.getSessionSeconds(id), api.getThresholdSeconds(),
                api.hasMetThreshold(id), api.hasEarnedDailyReward(id), api.hasSession(id));
    }
}
