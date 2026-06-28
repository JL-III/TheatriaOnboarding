package com.theatria.onboarding;

import com.theatria.onboarding.hook.EssentialsHook;
import com.theatria.onboarding.hook.LandsCreateListener;
import com.theatria.onboarding.hook.LandsHook;
import com.theatria.onboarding.hook.LuckPermsHook;
import com.theatria.onboarding.hook.RankupHook;
import com.theatria.onboarding.hook.SessionsHook;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * TheatriaOnboarding — a guided new-player experience built around a dynamic
 * virtual Tutorial Guide book ({@code /tutorial}) that re-renders from each
 * player's live progress, crossing out completed tasks while keeping them
 * visible for reference.
 */
public final class TheatriaOnboarding extends JavaPlugin {

    private ProgressManager progress;
    private BookRenderer renderer;
    private Economy economy; // null when Vault is absent
    private EssentialsHook essentialsHook;
    private LandsHook landsHook;
    private RankupHook rankupHook;
    private LuckPermsHook luckPermsHook;
    private SessionsHook sessionsHook;
    private boolean debug;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.debug = getConfig().getBoolean("debug", false);
        setupEconomy();

        this.progress = new ProgressManager(this);
        this.renderer = new BookRenderer(this);

        this.essentialsHook = new EssentialsHook(getLogger());
        this.landsHook = new LandsHook(this, getLogger());
        this.rankupHook = new RankupHook(this, getLogger(),
                player -> progress.complete(player, TaskId.RANKUP, "Rankup event"));
        this.luckPermsHook = new LuckPermsHook(this, getLogger());
        this.sessionsHook = new SessionsHook(this);

        TutorialCommand command = new TutorialCommand(this);
        PluginCommand tutorial = getCommand("tutorial");
        if (tutorial != null) {
            tutorial.setExecutor(command);
            tutorial.setTabCompleter(command);
        } else {
            getLogger().severe("Command 'tutorial' missing from plugin.yml — disabling.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getServer().getPluginManager().registerEvents(new OnboardingListeners(this), this);

        // Instant CLAIM completion via Lands' typed post-create event. Registered ONLY
        // when Lands is present, so the Lands API classes never resolve otherwise
        // (keeps Lands a soft dependency). LandsHook's poll stays as the backstop.
        if (getServer().getPluginManager().getPlugin("Lands") != null) {
            getServer().getPluginManager().registerEvents(new LandsCreateListener(this), this);
        }

        // Periodically re-check balance/playtime tasks for online players.
        long period = 20L * 30; // 30 seconds
        getServer().getScheduler().runTaskTimer(this, this::recheckOnline, period, period);

        // Handle players already online (e.g. after a /reload).
        getServer().getOnlinePlayers().forEach(p -> {
            progress.load(p.getUniqueId());
            progress.markSeen(p);
        });

        getLogger().info("TheatriaOnboarding enabled. "
                + "Vault: " + (economy != null)
                + ", Essentials hook: " + essentialsHook.isAvailable()
                + ", Lands hook: " + landsHook.isAvailable()
                + ", Rankup hook: " + rankupHook.isAvailable()
                + ", LuckPerms rank check: " + luckPermsHook.isAvailable()
                + ", Sessions hook: " + sessionsHook.isAvailable()
                + ", debug: " + debug + ".");
    }

    @Override
    public void onDisable() {
        if (progress != null) {
            progress.saveAll();
        }
    }

    private void setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp != null) {
            this.economy = rsp.getProvider();
        }
    }

    private void recheckOnline() {
        getServer().getOnlinePlayers().forEach(p -> progress.recheck(p));
    }

    public ProgressManager progress() {
        return progress;
    }

    public BookRenderer renderer() {
        return renderer;
    }

    public Economy economy() {
        return economy;
    }

    public EssentialsHook essentialsHook() {
        return essentialsHook;
    }

    public LandsHook landsHook() {
        return landsHook;
    }

    public RankupHook rankupHook() {
        return rankupHook;
    }

    public LuckPermsHook luckPermsHook() {
        return luckPermsHook;
    }

    public SessionsHook sessionsHook() {
        return sessionsHook;
    }

    /** Whether verbose debug logging is enabled (config.yml {@code debug}). */
    public boolean isDebug() {
        return debug;
    }

    /** Logs to the server console at INFO, but only when {@code debug} is enabled. */
    public void debug(String msg) {
        if (debug) {
            getLogger().info("[debug] " + msg);
        }
    }
}
