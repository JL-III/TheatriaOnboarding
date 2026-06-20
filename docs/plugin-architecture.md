# Starter Guide — plugin architecture

The Starter Guide is a **virtual book** that re-renders from each player's live
progress every time they open it with `/starter`. Completed tasks are **struck
through but still shown**, so the book doubles as a permanent reference. This
requires a small custom **Paper plugin** (`TheatriaOnboarding`).

## Why a custom plugin (not a quests/GUI plugin)

The exact behavior — *a virtual book, opened on command, rebuilt from live
progress, completed lines struck through yet still visible* — is specific enough
that a bespoke plugin matches it precisely and avoids fighting a generic quests
or menu plugin. It's also small: one command, a progress store, a few listeners,
and a book renderer.

## Virtual book (never an item)

The book is **not** given to the inventory. On `/starter` the plugin builds a
`BookMeta` from the player's current progress and calls `player.openBook(book)`
(Paper API) to display it transiently. Closing it leaves nothing behind; the
next `/starter` rebuilds it fresh with any newly-completed tasks crossed out.

## Task model

An ordered list of tasks; each has an id, the instructional text shown while
incomplete, a short "done" label shown when complete (struck through), and a
completion trigger.

| # | id | Player goal | Completion trigger (see below) |
|---|------|-------------|-------------------------------|
| 1 | `RTP` | Find a spot (`/rtp`) | ran `/rtp` at least once |
| 2 | `SETHOME` | `/sethome` | player has ≥1 home |
| 3 | `EARN` | Reach $1,000 | balance ≥ 1000 (Vault) |
| 4 | `CLAIM` | First claim (`/claim`) | owns ≥1 Lands claim |
| 5 | `RANKUP` | `/rank up` | rank above starting group |
| ★ | `DAILY` | Play ~30 min for daily reward | 30 min playtime |

## Progress detection

Two strategies; the plugin uses a pragmatic mix and each is configurable:

- **State checks (preferred where cheap/reliable).** Don't trust "command was
  typed" — check the resulting state:
  - `EARN` → Vault `Economy#getBalance ≥ target`.
  - `SETHOME` → Essentials API: player has ≥1 home.
  - `CLAIM` → Lands API: player owns ≥1 claim (and/or listen to Lands'
    claim-created event).
  - `RANKUP` → permissions/rank plugin: group is past the starting rank.
- **Command listener (fallback / where there's no persistent state).**
  `PlayerCommandPreprocessEvent` matched against configurable aliases — used for
  `RTP` (no lasting state to check) and as a fallback for `RANKUP`.
- **Playtime** for `DAILY` → `Statistic.PLAY_ONE_MINUTE` (ticks played) crossing
  the configured minutes, re-checked on a light repeating task.

Soft-depends (`Vault`, `Essentials`, `Lands`, the rank plugin) are optional: if
a plugin is absent, that task falls back to its command-listener trigger so the
book still works. No hard dependencies block startup.

Completion is re-evaluated on join, on the relevant events, and on a light timer,
then persisted — so the book is correct whenever it's opened.

## Persistence

Per-player progress in `playerdata/<uuid>.yml` (simple, inspectable): for each
task a `completed` boolean + timestamp. Loaded on join, saved on change.

## Rendering (strikethrough, still visible)

When building the book, each task line is rendered via Adventure components:

- **Completed:** `✔ <done label>` with `TextDecoration.STRIKETHROUGH` (gray) —
  crossed out but still on the page.
- **Incomplete:** full instructions (the command + a one-line how-to), normal
  styling; the current/next task can be highlighted.
- A header shows overall progress, e.g. `Progress: 3 / 6`.

Pages follow the copy in [`../content/starter-book.md`]; the renderer just swaps
each task between its incomplete and struck-through completed form.

## Commands & permissions

- `/starter` (alias `/guide`) — open the virtual book. Perm
  `theatria.onboarding.use` (default: true).
- `/starter reset [player]` — reset progress (admin). Perm
  `theatria.onboarding.admin` (default: op).
- **Auto-open on first join** (configurable): the first time a player ever
  joins, open the book automatically.

## Config (`config.yml`)

- `earn-target: 1000`
- `daily-minutes: 30`
- `auto-open-first-join: true`
- per-task command aliases to watch (e.g. `rtp: [rtp, wild]`)
- per-task hook toggles (use Lands/Essentials/rank API if present)

## Build target — NEEDS CONFIRMATION

The plugin compiles against the Paper API and must match the server's Minecraft
version (which dictates the Java version too). Pending your answer:

- Paper **1.21.x** → Java **21**
- Paper **1.20.x** → Java **17**
- (Spigot works too; Adventure usage adjusts slightly.)

Everything above is version-independent — only the build target (pom + Java
level) depends on this.
