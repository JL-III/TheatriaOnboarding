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

The Essentials/Lands/Rankup/LuckPerms/TheatriaSessions hooks are **reflective**: no compile-time dependency,
bound at runtime if the plugin is present, and tolerant of API version
differences. The Rankup hook registers the event dynamically by class name with
a reflective executor. The TheatriaSessions hook resolves the plugin's public
`SessionsAPI` by class name and calls `hasEarnedDailyReward(uuid)`, re-fetching the
API instance per call (it is null while that plugin is disabled). If a hook can't
bind (plugin absent, or an API mismatch — logged once), that task falls back to
command/statistic detection so the book still works.
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

- `/starter` (alias `/guide`) — open the virtual book. Perm
  `theatria.onboarding.use` (default: true).
- `/starter reset [player]` — reset progress (admin). Perm
  `theatria.onboarding.admin` (default: op).
- **Auto-open on first join** (configurable): the first time a player ever
  joins, open the book automatically.

## Config (`config.yml`)

- `earn-target: 1000.0` — balance needed for EARN.
- `daily-minutes: 30` — playtime for DAILY.
- `rtp-min-distance: 100.0` — teleport distance (or world change) that completes RTP.
- `rankup-starting-groups: [default]` — LuckPerms groups that count as "not yet
  ranked up"; a primary group outside this set completes RANKUP on join. Empty
  disables the retroactive check.
- `auto-open-first-join: true`
- `commands:` — fallback command aliases for `sethome`, `claim`, `rankup`, and
  the `sell` EARN-fallback. `sethome`/`claim` are only used when their
  Essentials/Lands hook is unavailable; `rankup` always uses commands.

## Build target

Targeting **Paper `26.1.2`** (server-reported version) on **Java 21**. Both are
single, clearly-commented settings in `pom.xml` (`paper.version`, `java.version`)
plus `api-version` in `plugin.yml` — adjust them if the build can't resolve the
artifact or the server rejects the api-version. Everything else in the codebase
is version-independent.

> Note: `26.1.2` could not be verified against the PaperMC Maven repo from the
> dev sandbox (network-blocked), so confirm the artifact resolves when you build.
