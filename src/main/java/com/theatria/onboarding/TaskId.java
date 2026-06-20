package com.theatria.onboarding;

/**
 * The ordered onboarding tasks. Each carries the text shown in the Starter Guide
 * book: a title and the body lines. When a task is complete the same text is
 * still rendered, but struck through (see {@link BookRenderer}), so the player
 * can always reference the command they used.
 */
public enum TaskId {

    RTP("Set out to find a spot", new String[]{
            "Walk through the",
            "spawn portal to",
            "reach the wild.",
            "",
            "Not satisfied? Use",
            "/rtp until you find",
            "a spot you like,",
            "then continue your",
            "/starter tasks."
    }),

    SETHOME("Set your home", new String[]{
            "Found your spot?",
            "Lock it in:",
            "",
            "/sethome",
            "",
            "Then /home brings",
            "you back anytime."
    }),

    EARN("Make your first money", new String[]{
            "Grab gear:",
            "/kit starter",
            "",
            "Mine & sell cobble,",
            "coal, copper.",
            "Value: /worth",
            "Sell: /sell hand",
            "",
            "GOAL: $1,000"
    }),

    CLAIM("Claim your land", new String[]{
            "Stand where you",
            "want to build:",
            "",
            "/claim",
            "",
            "Costs $1,000 and",
            "creates your land."
    }),

    RANKUP("Rank up", new String[]{
            "Keep selling, then:",
            "",
            "/rank up",
            "",
            "Higher rank =",
            "more perks."
    }),

    DAILY("Daily reward", new String[]{
            "Play ~30 min to",
            "grab your DAILY",
            "REWARD - easy",
            "money to start."
    });

    private final String title;
    private final String[] body;

    TaskId(String title, String[] body) {
        this.title = title;
        this.body = body;
    }

    public String title() {
        return title;
    }

    public String[] body() {
        return body;
    }

    /** Total number of tasks, used for the "X / N" progress header. */
    public static int count() {
        return values().length;
    }
}
