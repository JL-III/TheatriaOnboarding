package com.theatria.onboarding;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Sound;
import org.bukkit.Statistic;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Loads, caches, persists and re-evaluates onboarding progress. Progress is
 * stored per player in {@code playerdata/<uuid>.yml} inside the plugin folder.
 */
public class ProgressManager {

    private final TheatriaOnboarding plugin;
    private final File dir;
    private final Map<UUID, PlayerProgress> cache = new HashMap<>();

    public ProgressManager(TheatriaOnboarding plugin) {
        this.plugin = plugin;
        this.dir = new File(plugin.getDataFolder(), "playerdata");
        if (!dir.exists() && !dir.mkdirs()) {
            plugin.getLogger().warning("Could not create playerdata folder: " + dir);
        }
    }

    private File fileFor(UUID uuid) {
        return new File(dir, uuid + ".yml");
    }

    /** True if this player has never been seen before (no saved data yet). */
    public boolean isFirstJoin(UUID uuid) {
        return !cache.containsKey(uuid) && !fileFor(uuid).exists();
    }

    public PlayerProgress get(UUID uuid) {
        return cache.computeIfAbsent(uuid, this::loadFromDisk);
    }

    public void load(UUID uuid) {
        get(uuid);
    }

    public void unload(UUID uuid) {
        save(uuid);
        cache.remove(uuid);
    }

    public boolean isComplete(UUID uuid, TaskId task) {
        return get(uuid).isComplete(task);
    }

    private PlayerProgress loadFromDisk(UUID uuid) {
        PlayerProgress progress = new PlayerProgress();
        File file = fileFor(uuid);
        if (!file.exists()) {
            return progress;
        }
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        progress.setSeen(yml.getBoolean("seen", true));
        for (TaskId task : TaskId.values()) {
            if (yml.contains("tasks." + task.name())) {
                progress.complete(task, yml.getLong("tasks." + task.name()));
            }
        }
        return progress;
    }

    public void save(UUID uuid) {
        PlayerProgress progress = cache.get(uuid);
        if (progress == null) {
            return;
        }
        YamlConfiguration yml = new YamlConfiguration();
        yml.set("seen", progress.seen());
        for (TaskId task : TaskId.values()) {
            if (progress.isComplete(task)) {
                yml.set("tasks." + task.name(), progress.timestamp(task));
            }
        }
        try {
            yml.save(fileFor(uuid));
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save progress for " + uuid, e);
        }
    }

    public void saveAll() {
        for (UUID uuid : cache.keySet()) {
            save(uuid);
        }
    }

    public void reset(UUID uuid) {
        cache.remove(uuid);
        File file = fileFor(uuid);
        if (file.exists() && !file.delete()) {
            plugin.getLogger().warning("Could not delete progress file for " + uuid);
        }
    }

    /** Marks the player as having seen the guide; persists on first transition. */
    public void markSeen(Player player) {
        PlayerProgress progress = get(player.getUniqueId());
        if (!progress.seen()) {
            progress.setSeen(true);
            save(player.getUniqueId());
        }
    }

    /** Completes a task (idempotent) and notifies the player when newly done. */
    public void complete(Player player, TaskId task) {
        PlayerProgress progress = get(player.getUniqueId());
        if (!progress.complete(task, System.currentTimeMillis())) {
            return;
        }
        save(player.getUniqueId());
        notifyComplete(player, task, progress);
    }

    private void notifyComplete(Player player, TaskId task, PlayerProgress progress) {
        player.sendMessage(Component.text("✔ Task complete: ", NamedTextColor.GREEN)
                .append(Component.text(task.title(), NamedTextColor.WHITE)));
        if (progress.allComplete()) {
            player.sendMessage(Component.text("You've finished onboarding — welcome to Theatria!",
                    NamedTextColor.GOLD));
        } else {
            player.sendMessage(Component.text("Open /starter to see what's next.", NamedTextColor.GRAY));
        }
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.4f);
    }

    /**
     * Re-evaluates the state-checked tasks for an online player: balance (EARN),
     * playtime (DAILY), home set (SETHOME, via Essentials) and land claimed
     * (CLAIM, via Lands). Tasks whose plugin hook is unavailable are left to the
     * command-listener fallback instead.
     */
    public void recheck(Player player) {
        UUID uuid = player.getUniqueId();

        if (!isComplete(uuid, TaskId.EARN) && plugin.economy() != null) {
            double target = plugin.getConfig().getDouble("earn-target", 1000.0);
            if (plugin.economy().getBalance(player) >= target) {
                complete(player, TaskId.EARN);
            }
        }

        if (!isComplete(uuid, TaskId.DAILY)) {
            int minutes = player.getStatistic(Statistic.PLAY_ONE_MINUTE) / (20 * 60);
            if (minutes >= plugin.getConfig().getInt("daily-minutes", 30)) {
                complete(player, TaskId.DAILY);
            }
        }

        if (!isComplete(uuid, TaskId.SETHOME)
                && plugin.essentialsHook().isAvailable()
                && plugin.essentialsHook().hasHome(player)) {
            complete(player, TaskId.SETHOME);
        }

        if (!isComplete(uuid, TaskId.CLAIM)
                && plugin.landsHook().isAvailable()
                && plugin.landsHook().hasClaim(player)) {
            complete(player, TaskId.CLAIM);
        }
    }
}
