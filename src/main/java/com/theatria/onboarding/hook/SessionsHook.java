package com.theatria.onboarding.hook;

import com.playtheatria.sessions.api.SessionsAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

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

    private final boolean present;

    public SessionsHook() {
        this.present = Bukkit.getPluginManager().getPlugin("TheatriaSessions") != null;
    }

    /** True when TheatriaSessions is installed. Gate all {@link #hasEarnedReward} calls on this. */
    public boolean isAvailable() {
        return present;
    }

    /**
     * True if TheatriaSessions reports the player has earned today's daily reward.
     * Only call when {@link #isAvailable()}. {@code SessionsAPI.get()} is null while
     * that plugin is mid-(re)load, in which case this returns false.
     */
    public boolean hasEarnedReward(Player player) {
        if (!present) {
            return false;
        }
        SessionsAPI api = SessionsAPI.get();
        return api != null && api.hasEarnedDailyReward(player.getUniqueId());
    }
}
