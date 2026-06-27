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
| 1 | `RTP` | Leave spawn via the portal | first long-distance / cross-world teleport |
| 2 | `SETHOME` | `/sethome` | **Essentials API: player has ≥1 home** |
| 3 | `EARN` | Reach $1,000 | balance ≥ 1000 (Vault) |
| 4 | `CLAIM` | First claim (`/claim`) | **Lands API: player is in ≥1 claim** |
| 5 | `RANKUP` | `/rankup` | **Rankup `PlayerRankupEvent`** (live) + **LuckPerms** past-starting-rank check (on join) |
| ★ | `DAILY` | Play for the daily reward | **TheatriaSessions API: player has earned today's reward** (fallback: 30 min playtime) |

## Progress detection

The plugin prefers **real state checks** over "a command was typed", and only
falls back to command matching when a plugin/hook isn't available:

- `SETHOME` → **Essentials hook** (`getUser(player).getHomes()` non-empty). The
  player must actually have a home — typing `/sethome` alone isn't enough.
- `CLAIM` → **Lands hook** (`LandsIntegration.getLandPlayer(uuid).getLands()`
  non-empty). The player must actually be in a claim. During onboarding the only
  land a new player belongs to is the one their first `/claim` created.
- `EARN` → Vault `Economy#getBalance ≥ target`.
- `DAILY` → **TheatriaSessions hook** (`SessionsAPI#hasEarnedDailyReward(uuid)`): the
  player has actually earned today's reward — active (non-AFK) playtime past
  TheatriaSessions' configured threshold, which resets daily. When TheatriaSessions
  is unavailable, falls back to `Statistic.PLAY_ONE_MINUTE` crossing the configured
  minutes (lifetime, AFK-inclusive — only an approximation).
- `RTP` → `PlayerTeleportEvent`: completes on the first teleport that changes
  world or covers ≥ `rtp-min-distance` blocks — i.e. when the spawn portal flings
  them into the wild. They may keep using `/rtp` to reroll afterwards.
- `RANKUP` → **Rankup hook** (live) listens for
  `sh.okx.rankup.events.PlayerRankupEvent`, which only fires on a successful
  rank-up — so a `/rankup` without enough money won't count. A **LuckPerms hook**
  also runs on join (via `recheck`) and completes RANKUP retroactively if the
  player's primary group is past the configured `rankup-starting-groups` (catches
  ranks gained while offline). Disabled unless starting groups are configured.

The Essentials/Lands/Rankup/LuckPerms hooks are **reflective**: no compile-time
dependency, bound at runtime if the plugin is present, and tolerant of API version
differences. The Rankup hook registers the event dynamically by class name with a
reflective executor. If a hook can't bind (plugin absent, or an API mismatch —
logged once), that task falls back to command detection so the book still works.

TheatriaSessions is **first-party**, so DAILY instead depends on its published
`SessionsAPI` directly — a `provided` Maven dependency
(`com.playtheatria:theatriasessions` from GitHub Packages) — and calls
`SessionsAPI.get().hasEarnedDailyReward(uuid)` with no reflection. It stays a soft
dependency: the call is gated by a plugin-presence check, so when TheatriaSessions
is absent its classes are never touched and DAILY falls back to the playtime
statistic.
`CLAIM`'s command fallback additionally requires `EARN` to be done, so a broke
`/claim` can't false-complete it. No hard dependencies block startup.

Completion is re-evaluated on join, on relevant events, on a short delayed
re-check after each command (so post-execution state like a new home is caught),
and on a 30-second timer — then persisted, so the book is correct whenever opened.

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

- `/starter` (aliases `/guide`, `/tutorial`) — open the virtual book. Perm
  `theatria.onboarding.use` (default: true).
- `/starter reset [player]` — reset progress (admin). Perm
  `theatria.onboarding.admin` (default: op).
- `/starter debug [player]` — print a runtime snapshot for fault-finding (admin):
  each task's completion state, which detection hooks are available, and the
  Sessions side's view of DAILY (active seconds vs threshold, met-threshold,
  earned). Defaults to the sender when no player is given.
- **Auto-open on first join** (configurable): the first time a player ever
  joins, open the book automatically.
- **Join reminder** (`join-reminder`, default true): a clickable "open your
  Starter Guide" line sent on each join until onboarding is complete; gated on
  `theatria.onboarding.use` (so alts get none).

## Observability

For diagnosing why a task does or doesn't complete at runtime:

- **Always on:** the enable line logs which hooks bound; each task completion logs
  `[onboarding] <player> completed <TASK> (via <source>)` at INFO, so the path is
  in the record (e.g. `DAILY ... (via SessionsAPI)` vs `(via playtime statistic)`).
- **`debug: true`** (config) adds per-recheck DAILY decisions — the SessionsAPI's
  answer plus active-seconds/threshold. Chatty (recheck runs every 30s per online
  player), so turn it on to investigate, then off.
- **On demand:** `/starter debug [player]` collapses all of the above into one
  snapshot without touching the log level.
- On the Sessions side, `SessionsAPI` logs each `hasEarnedDailyReward` query under
  TheatriaSessions' own `debug: true`.

## Config (`config.yml`)

- `debug: false` — verbose per-recheck DAILY decision logging to the console.
- `earn-target: 1000.0` — balance needed for EARN.
- `daily-minutes: 30` — playtime for DAILY.
- `rtp-min-distance: 100.0` — teleport distance (or world change) that completes RTP.
- `rankup-starting-groups: [default]` — LuckPerms groups that count as "not yet
  ranked up"; a primary group outside this set completes RANKUP on join. Empty
  disables the retroactive check.
- `auto-open-first-join: true`
- `join-reminder: true` — clickable "open /starter" nudge on join until done.
- `commands:` — fallback command aliases for `sethome`, `claim`, `rankup`, and
  the `sell` EARN-fallback. `sethome`/`claim` are only used when their
  Essentials/Lands hook is unavailable; `rankup` always uses commands.

## Build target

Targeting **Paper 26.1.2** (Mojang's 2026 year-based scheme) on **Java 25**.

- `paper.version` = `26.1.2.build.69-stable`. Paper changed its Maven version
  format for 26.1+ to `{MCVERSION}.build.{N}-stable` (the old
  `{VERSION}-R0.1-SNAPSHOT` format is gone). Bump `build.N` to the latest stable
  build for 26.1.2 if that one is pruned.
- `java.version` = `25` (Minecraft 26.1.2 requires JDK 25, so the build needs
  JDK 25 too).
- `api-version` in `plugin.yml` = `26.1.2` (fall back to `26` / `26.1` if the
  server rejects it).

All three live in `pom.xml` / `plugin.yml`; everything else in the codebase is
version-independent.
