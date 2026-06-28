package com.theatria.onboarding.hook;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.User;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.logging.Logger;

/**
 * Hook into EssentialsX so we can confirm a player actually has a home set (rather
 * than merely having typed {@code /sethome}).
 *
 * <p>Uses the published EssentialsX API directly (no reflection). It stays a
 * <em>soft</em> dependency: the cast to {@link Essentials} runs only after a
 * string-based plugin-presence check, so the Essentials classes never resolve when
 * Essentials is absent; on any failure it reports unavailable and the caller falls
 * back to command detection.
 */
public final class EssentialsHook {

    private final Logger logger;
    private Essentials essentials; // null until/unless bound
    private boolean bound;
    private boolean warned;

    public EssentialsHook(Logger logger) {
        this.logger = logger;
        // Note: check presence by name (no Essentials type) before casting, so the
        // Essentials class is only referenced once we know it's installed.
        Plugin plugin = Bukkit.getPluginManager().getPlugin("Essentials");
        if (plugin != null && plugin.isEnabled()) {
            this.essentials = (Essentials) plugin;
            this.bound = true;
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
            User user = essentials.getUser(player);
            return user != null && !user.getHomes().isEmpty();
        } catch (Throwable t) {
            warnOnce(t);
            return false;
        }
    }

    private void warnOnce(Throwable t) {
        if (!warned) {
            warned = true;
            logger.warning("Essentials hook failed; using command detection for /sethome instead. " + t);
        }
    }
}
