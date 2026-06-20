package com.theatria.onboarding;

import java.util.EnumMap;
import java.util.Map;

/**
 * In-memory onboarding progress for a single player: which tasks are done (with
 * the completion timestamp) and whether they have ever seen the guide.
 */
public class PlayerProgress {

    private boolean seen;
    private final Map<TaskId, Long> completed = new EnumMap<>(TaskId.class);

    public boolean seen() {
        return seen;
    }

    public void setSeen(boolean seen) {
        this.seen = seen;
    }

    public boolean isComplete(TaskId task) {
        return completed.containsKey(task);
    }

    /** Marks a task complete; returns false if it was already complete. */
    public boolean complete(TaskId task, long timestamp) {
        return completed.putIfAbsent(task, timestamp) == null;
    }

    public Long timestamp(TaskId task) {
        return completed.get(task);
    }

    public int completedCount() {
        return completed.size();
    }

    public boolean allComplete() {
        return completed.size() >= TaskId.count();
    }

    public void clear() {
        completed.clear();
        seen = false;
    }
}
