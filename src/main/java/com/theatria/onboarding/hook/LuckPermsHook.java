package com.theatria.onboarding.hook;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Reflective hook into LuckPerms used to retroactively detect rank-ups: if a
 * player's primary group is something other than a configured starting rank,
 * they have already ranked up at least once. This complements {@link RankupHook}
 * (which only catches live rank-up events) by catching ranks gained while
 * offline or before this plugin was installed — checked on join via
 * {@code ProgressManager#recheck}.
 *
 * <p>Disabled unless {@code rankup-starting-groups} is configured, so it can't
 * misfire on a server whose starting rank isn't "default".
 */
public final class LuckPermsHook {

    private final Plugin plugin;
    private final Logger logger;
    private Object userManager;
    private Method getUser;
    private boolean bound;
    private boolean warned;

    public LuckPermsHook(Plugin plugin, Logger logger) {
        this.plugin = plugin;
        this.logger = logger;
        if (Bukkit.getPluginManager().getPlugin("LuckPerms") == null) {
            return;
        }
        try {
            Object api = Class.forName("net.luckperms.api.LuckPermsProvider")
                    .getMethod("get").invoke(null);
            Method getUserManager = api.getClass().getMethod("getUserManager");
            getUserManager.setAccessible(true);
            this.userManager = getUserManager.invoke(api);
            this.getUser = userManager.getClass().getMethod("getUser", UUID.class);
            this.getUser.setAccessible(true);
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
            Object user = getUser.invoke(userManager, player.getUniqueId());
            if (user == null) {
                return false;
            }
            Method getPrimaryGroup = user.getClass().getMethod("getPrimaryGroup");
            getPrimaryGroup.setAccessible(true);
            Object group = getPrimaryGroup.invoke(user);
            if (!(group instanceof String name)) {
                return false;
            }
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
