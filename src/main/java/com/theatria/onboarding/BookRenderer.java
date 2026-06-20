package com.theatria.onboarding;

import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the virtual Starter Guide book from a player's live progress. The book
 * is never an inventory item — it is opened transiently via {@code openBook}.
 * Completed tasks are rendered struck through but still fully visible, so the
 * book doubles as a permanent reference.
 */
public class BookRenderer {

    public Book build(PlayerProgress progress) {
        List<Component> pages = new ArrayList<>();
        pages.add(coverPage(progress));

        int number = 1;
        for (TaskId task : TaskId.values()) {
            pages.add(taskPage(number++, task, progress.isComplete(task)));
        }

        pages.add(helpPage());

        return Book.book(
                Component.text("Starter Guide"),
                Component.text("Theatria"),
                pages
        );
    }

    private Component coverPage(PlayerProgress progress) {
        return Component.text()
                .append(Component.text("Welcome to", NamedTextColor.DARK_GRAY))
                .append(Component.newline())
                .append(Component.text("Theatria", NamedTextColor.DARK_GREEN, TextDecoration.BOLD))
                .append(Component.newline()).append(Component.newline())
                .append(Component.text("This book is your", NamedTextColor.BLACK))
                .append(Component.newline())
                .append(Component.text("guide. Do the tasks", NamedTextColor.BLACK))
                .append(Component.newline())
                .append(Component.text("in order.", NamedTextColor.BLACK))
                .append(Component.newline()).append(Component.newline())
                .append(Component.text("Reopen anytime:", NamedTextColor.BLACK))
                .append(Component.newline())
                .append(Component.text("/starter", NamedTextColor.DARK_AQUA))
                .append(Component.newline()).append(Component.newline())
                .append(Component.text("Progress: ", NamedTextColor.DARK_GRAY))
                .append(Component.text(progress.completedCount() + " / " + TaskId.count(),
                        NamedTextColor.DARK_GREEN, TextDecoration.BOLD))
                .build();
    }

    private Component taskPage(int number, TaskId task, boolean done) {
        var builder = Component.text()
                .append(Component.text("TASK " + number, done ? NamedTextColor.GRAY : NamedTextColor.DARK_GRAY))
                .append(Component.newline());

        if (done) {
            builder.append(Component.text("✔ ", NamedTextColor.GREEN)
                    .append(line(task.title(), true).decorate(TextDecoration.BOLD)));
        } else {
            builder.append(Component.text(task.title(), NamedTextColor.DARK_BLUE, TextDecoration.BOLD));
        }
        builder.append(Component.newline()).append(Component.newline());

        for (String text : task.body()) {
            builder.append(line(text, done)).append(Component.newline());
        }
        return builder.build();
    }

    /** A body line: normal when pending, gray + struck through when complete. */
    private Component line(String text, boolean done) {
        if (done) {
            return Component.text(text, NamedTextColor.GRAY).decorate(TextDecoration.STRIKETHROUGH);
        }
        return Component.text(text, NamedTextColor.BLACK);
    }

    private Component helpPage() {
        return Component.text()
                .append(Component.text("All done?", NamedTextColor.DARK_GREEN, TextDecoration.BOLD))
                .append(Component.newline()).append(Component.newline())
                .append(Component.text("Completed tasks", NamedTextColor.BLACK))
                .append(Component.newline())
                .append(Component.text("stay crossed out", NamedTextColor.BLACK))
                .append(Component.newline())
                .append(Component.text("here for reference.", NamedTextColor.BLACK))
                .append(Component.newline()).append(Component.newline())
                .append(Component.text("Stuck? Just ask", NamedTextColor.BLACK))
                .append(Component.newline())
                .append(Component.text("in chat.", NamedTextColor.BLACK))
                .append(Component.newline()).append(Component.newline())
                .append(Component.text("Have fun!", NamedTextColor.DARK_GREEN))
                .build();
    }
}
