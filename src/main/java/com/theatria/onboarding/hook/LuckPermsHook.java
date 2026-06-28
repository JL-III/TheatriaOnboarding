package com.theatria.onboarding.hook;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.logging.Logger;

/**
 * Hook into LuckPerms used to retroactively detect rank-ups: if a player's primary
 * group is something other than a configured starting rank, they have already
 * ranked up at least once. This complements {@link RankupHook} (which only catches
 * live rank-up events) by catching ranks gained while offline or before this plugin
 * was installed — checked on join via {@code ProgressManager#recheck}.
 *
 * <p>Uses the published LuckPerms API directly (Maven Central, no reflection). It
 * stays a <em>soft</em> dependency: the LuckPerms types are only touched once the
 * plugin-presence check passes, so they never resolve when LuckPerms is absent.
 * Disabled unless {@code rankup-starting-groups} is configured, so it can't misfire
 * on a server whose starting rank isn't "default".
 */
public final class LuckPermsHook {

    private final Plugin plugin;
    private final Logger logger;
    private UserManager userManager; // null until/unless bound
    private boolean bound;
    private boolean warned;

    public LuckPermsHook(Plugin plugin, Logger logger) {
        this.plugin = plugin;
        this.logger = logger;
        if (Bukkit.getPluginManager().getPlugin("LuckPerms") == null) {
            return; // LuckPerms types below are never reached when it's absent
        }
        try {
            LuckPerms api = LuckPermsProvider.get();
            this.userManager = api.getUserManager();
            this.bound = true;
        } catch (Throwable t) {
            logger.warning("LuckPerms present but API hook failed to bind; "
                    + "skipping the join-time rank check. " + t);
        }
    }

    /** Available only when LuckPerms is bound AND starting groups are configured. */
    public boolean isAvailable() {
        return bound && !startingGroups().isEmpty();
    }

    /**
     * True if the player's primary group is none of the configured starting
     * ranks — i.e. they have already ranked up at least once.
     */
    public boolean hasRankedUp(Player player) {
        if (!bound) {
            return false;
        }
        List<String> starting = startingGroups();
        if (starting.isEmpty()) {
            return false;
        }
        try {
            User user = userManager.getUser(player.getUniqueId());
            if (user == null) {
                return false;
            }
            String name = user.getPrimaryGroup();
            for (String start : starting) {
                if (start.equalsIgnoreCase(name)) {
                    return false;
                }
            }
            return true;
        } catch (Throwable t) {
            warnOnce(t);
            return false;
        }
    }

    private List<String> startingGroups() {
        return plugin.getConfig().getStringList("rankup-starting-groups");
    }

    private void warnOnce(Throwable t) {
        if (!warned) {
            warned = true;
            logger.warning("LuckPerms rank check failed; relying on the live rank-up event. " + t);
        }
    }
}
