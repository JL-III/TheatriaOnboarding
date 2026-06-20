# Theatria Onboarding

A redesign of the new-player onboarding experience for the Theatria Minecraft
server. Today onboarding is just oak signs at spawn; this project replaces that
with a guided, task-based flow built around a **virtual Starter Guide book** —
opened with `/starter`, re-rendered from each player's live progress, with
completed tasks struck through but still shown for reference. Delivered as a
small custom **Paper plugin**.

## The core loop we teach

New players learn Theatria's actual gameplay loop, one step at a time:

> **Explore → Settle → Earn → Protect → Progress**

| Step | Player goal | Command(s) |
|------|-------------|-----------|
| 1. Explore | Find a spot you like | `/rtp` |
| 2. Settle | Make it your home | `/sethome`, `/home` |
| 3. Earn | Gather & sell to build a balance | `/kit starter`, `/worth`, `/sell hand` |
| 4. Protect | Claim your land | `/claim` |
| 5. Progress | Rank up for perks | `/rank up` |
| Bonus | Daily reward for playtime | play ~30 min |

## What's here

- **The plugin** — `pom.xml` + `src/main/…`: a Paper plugin that serves the
  dynamic `/starter` book and tracks per-player progress.
- [`docs/onboarding-design.md`](docs/onboarding-design.md) — the full flow,
  design principles, the friction analysis, and open questions.
- [`docs/plugin-architecture.md`](docs/plugin-architecture.md) — how the dynamic
  virtual book works: task model, progress detection, persistence, rendering.
- [`content/starter-book.md`](content/starter-book.md) — page-by-page text for
  the `/starter` virtual book.
- [`content/spawn-signs.md`](content/spawn-signs.md) — replacement text for the
  spawn signs / portal area.

## Building

```bash
mvn clean package
```

The jar lands in `target/TheatriaOnboarding-1.0.0.jar`; drop it in `plugins/`.

> The build pulls `paper-api` and `VaultAPI` from the PaperMC and JitPack Maven
> repos, so it needs network access to those. **Two settings in `pom.xml` are
> marked for adjustment** — `paper.version` (the paper-api artifact, versioned by
> Minecraft version: defaults to `1.21-R0.1-SNAPSHOT`, matching `api-version` in
> `plugin.yml`) and `java.version` (defaulted to 21). If Maven can't resolve
> `paper-api`, run `/version` on the server and use that Minecraft version's
> `-R0.1-SNAPSHOT` artifact. Likewise, set `api-version` in `plugin.yml` to a value
> the server accepts.

## In-game

- `/starter` (alias `/guide`) — open the virtual Starter Guide.
- `/starter reset [player]` — reset progress (needs `theatria.onboarding.admin`).
- The book auto-opens on a player's first ever join (toggle in `config.yml`).

## Status

**v1 plugin drafted.** The dynamic virtual book, progress tracking, persistence,
and commands are implemented. Detection uses real state/events where it matters:
**SETHOME via the Essentials API** (player actually has a home), **CLAIM via the
Lands API** (player actually owns a claim), and **RANKUP via the Rankup
`PlayerRankupEvent`** (a live rank-up) plus a **LuckPerms** join-time check for
ranks gained while offline, and **DAILY via the TheatriaSessions API** (the player
has actually earned today's reward), with `RTP` completing when the spawn portal
sends them into the wild. Confirmed mechanics are wired in: Lands `/claim` (first
claim auto-creates the land, $1,000 target from EssentialsX via Vault), and the
mine-&-sell cobblestone/coal/copper method. The
Essentials/Lands/Rankup/LuckPerms/TheatriaSessions hooks are reflective, so the
build needs no extra dependencies and degrades to command/statistic detection if a
hook can't bind.

**Not yet verified:** the build couldn't be compiled in the dev sandbox (the
Maven repos are firewalled there). Pending values stay in `config.yml` /
`content/` — starter kit contents, rank-up cost, daily-reward amount, and the
exact RTP/rank plugins. See the open questions in the design doc.
