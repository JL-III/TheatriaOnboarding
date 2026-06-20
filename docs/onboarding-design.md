# Theatria Onboarding — Design

## Goal

Replace the "wall of oak signs at spawn" with a guided, task-based onboarding
that walks a brand-new player through Theatria's core gameplay loop and gets
them invested before they have a chance to bounce.

## Design principles

1. **One action per step.** Each task is a single command with a single,
   obvious outcome. No step should ask the player to do two new things at once.
2. **Quick wins early.** The first ~3 steps (`/rtp`, `/sethome`, a first sale)
   are instant and satisfying. We earn the right to ask for a grind later.
3. **Bounded goals, never open-ended.** "Go make money" is replaced with "earn
   $X by doing Y." A new player should always know *exactly* what done looks
   like.
4. **Don't rely on signs alone.** Most players skim signs. The Starter Guide
   book is the source of truth; signs and the portal just funnel players into
   it.

## The flow

### Stage 0 — First join (spawn)

- Player spawns into the **welcome area** (signs / hologram).
- The Starter Guide book is **auto-given on first join** *and* reopenable any
  time with `/starter`. (Belt and suspenders — signs point to `/starter` for
  anyone who closed it.)
- Signs do three things only: welcome, "type `/starter`", and "walk through the
  portal." Everything else lives in the book.

### Stage 1 — Explore: find your spot (`/rtp`)

- The **RTP portal** at spawn is the player's first teleport into the wild — it
  doubles as the tutorial for what `/rtp` does.
- The book's Task 1 reframes the portal: *"Didn't like where you landed? Type
  `/rtp` to reroll until you find a place you love."* This removes the
  redundancy between "walk through the portal" and "Task 1 is `/rtp`" — the
  portal is the first roll, `/rtp` is how you re-roll.

### Stage 2 — Settle: set your home (`/sethome`)

- Once they like a spot: `/sethome`, and learn `/home` to return.
- Instant, satisfying, and it anchors them to a place they now feel is "theirs."

### Stage 3 — Earn: your first money (`/kit starter` → `/worth` → `/sell hand`)

This is the make-or-break step (see Friction analysis below). Structured as:

1. **Get tools:** `/kit starter` (or however we hand out starter gear).
2. **First quick win:** mine/chop a few blocks, then `/sell hand` immediately so
   the money loop *clicks* in under a minute.
3. **Learn to value items:** `/worth` while holding something.
4. **Hit a concrete target:** *"Earn $5,000 — the cost of your first claim.
   Fastest safe method: [FILL IN, e.g. mine cobblestone / harvest crops]."*

### Stage 4 — Protect: claim your land (`/claim`)

- Claims run on the **Lands** plugin. The player stands on the spot and types
  `/claim`; it costs **$5,000**, pulled from their EssentialsX balance (Lands →
  Vault → Essentials economy).
- Because the claim costs money, Stage 3's earn target = **$5,000**. The two
  steps are one continuous goal: "earn $5,000, then `/claim`."
- **Verify:** in a default Lands setup a player usually creates a land first
  (`/lands create <name>` or via `/lands menu`) before `/claim` works on chunks.
  Confirm whether Theatria's `/claim` auto-handles land creation for a brand-new
  player, or whether the book needs a "create your land first" sub-step.

### Stage 5 — Progress: rank up (`/rank up`)

- Framed as "what's next," not a gated tutorial step.
- *"Keep selling to grow your balance, then `/rank up` for more perks."*

### Bonus — Daily reward (playtime)

- Surfaced **early** (a spawn sign + near the top of the book) as a retention
  hook: *"Play ~30 minutes to claim your daily reward — easy money to jump-start
  your wallet."*

---

## Friction analysis: "go make money to claim a chunk"

**You asked whether this step is too much friction. Short answer: the *concept*
is right and you should keep it — but *as worded* it's the single biggest
drop-off risk in the funnel, because it's open-ended.**

Steps 1–2 are instant. Then we suddenly ask the player to "go mine/farm/kill
*somewhere* for *some* amount of money." That open-endedness is what loses
people, not the idea of earning. A new player doesn't know **how much** they
need, **which method** is fastest, or **how long** it'll take — so it reads as
"go grind indefinitely," and that's where they log off.

The earning step is also the *most important* thing onboarding can teach,
because the economy is Theatria's core loop. So we don't cut it — we **convert
it from an open-ended grind into a bounded, guided quest:**

1. **Give a starter kit.** A fresh player with no pickaxe/sword can't earn.
   `/kit starter` (basic tools + food) removes the cold-start wall.
2. **One immediate sale.** Have them `/sell hand` on their very first gathered
   stack so they see money arrive *before* the grind. Momentum beats
   instruction.
3. **Name one method.** Don't offer mining *or* farming *or* combat — pick the
   fastest, safest beginner method and tell them exactly that. (Combat risks
   item loss and demoralizes newbies; steer the first goal toward mining or
   farming.)
4. **Set a concrete dollar target** equal to (or just above) the first claim
   cost, so "done" is unambiguous.
5. **Make $5,000 reachable in one short session.** The claim cost is fixed at
   $5,000 (Lands), so instead of discounting the claim, tune the *earn rate* to
   meet it: make sure the recommended method yields ~$5,000 in a reasonable
   sit-down, and lean on the **daily reward** to cover a meaningful chunk of it
   (e.g., a reward worth ~$1–2k turns the first claim from a slog into a
   near-finish). If $5,000 turns out to take too long for a fresh account,
   that's the number to revisit — or add a small first-join stipend.

**Verdict:** Keep the step. With a starter kit + one quick win + a single named
method + the concrete $5,000 target (plus the daily reward chipping in), it's
the *right* amount of friction — it teaches the economy without losing people.
Left open-ended, it's where your funnel leaks.

---

## Open questions / assumptions to confirm

These change the copy and the flow, so flagging them explicitly:

1. ~~**Claim mechanic.**~~ ✅ Confirmed: **Lands** plugin, `/claim`, **$5,000**
   pulled from EssentialsX balance via Vault.
2. **Lands land-creation sub-step.** Does `/claim` work standalone for a
   brand-new player, or do they need `/lands create` first? (See Stage 4.) If
   the latter, the book gets one extra line.
3. **Plugin stack.** Confirmed: Lands (`/claim`) + EssentialsX (`/sethome`,
   `/home`, `/worth`, `/sell hand`, `/kit`, economy). Still to confirm: the RTP
   plugin (`/rtp`) and the rank-ladder plugin (`/rank up`). Knowing the exact
   plugins lets me write real config (book NBT/JSON, sign data, kit defs).
4. **Values to fill in:** starter kit name + contents, the recommended
   money-making method (the one we name in the book), rank-up cost, and the
   daily-reward amount.
5. **Book delivery:** OK to auto-give the book on first join in addition to
   `/starter`? (Strongly recommended.)
6. **Deliverable format:** do you want (a) this design + copy, (b) actual
   implementable server files (book NBT/JSON, sign text, kit/permission config),
   or (c) both? That decides what I build next.
