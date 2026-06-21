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

Requires **JDK 25** (Minecraft 26.1.2 needs Java 25) and Maven.

```bash
make build      # or: mvn clean package
```

The jar lands in `target/TheatriaOnboarding-1.0.0.jar`; drop it in `plugins/`.
Run `make help` to list targets (`build`, `clean`, `help`).

> Targets Paper **26.1.2** via `paper-api` `26.1.2.build.69-stable` (Paper's new
> 2026 build-tagged Maven format). Bump `paper.version` in `pom.xml` to the
> latest stable build for 26.1.2 if that one has been pruned.

> The build pulls `paper-api` and `VaultAPI` from the PaperMC and JitPack Maven
> repos, so it needs network access to those.

> It also pulls `com.playtheatria:theatriasessions` (the SessionsAPI, for the DAILY
> task) from **GitHub Packages**, which requires authentication even to read. Add a
> `github-theatriasessions` server (your GitHub username + a `read:packages` token)
> to `~/.m2/settings.xml` — see the comment in `pom.xml` — and bump
> `theatriasessions.version` to the latest TheatriaSessions has published.

## In-game

- `/starter` (alias `/guide`) — open the virtual Starter Guide.
- `/starter reset [player]` — reset progress (needs `theatria.onboarding.admin`).
- The book auto-opens on a player's first ever join (toggle in `config.yml`).
- Task-completion chat messages are clickable (hover: "Click to view your starter
  tasks") and run `/starter` when clicked.

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
mine-&-sell cobblestone/coal/copper method. The Essentials/Lands/Rankup/LuckPerms
hooks are reflective (no extra build deps); DAILY depends on TheatriaSessions'
published `SessionsAPI` directly (a `provided` GitHub Packages dependency). All
degrade to command/statistic detection if the plugin is absent.

**Build:** targets Paper 26.1.2 (`paper-api` `26.1.2.build.69-stable`) on JDK 25.
The dev sandbox can't compile it (PaperMC repo is firewalled there), so build on
a machine with repo access and JDK 25. **Pending values** to set when convenient:
starter kit contents, rank-up cost, daily-reward amount, and
`rankup-starting-groups` (your first rank group). See the design doc.
