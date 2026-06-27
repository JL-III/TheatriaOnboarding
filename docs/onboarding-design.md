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
4. **Don't rely on signs alone.** Most players skim signs. The Tutorial Guide
   book is the source of truth; signs and the portal just funnel players into
   it.

## The flow

### Stage 0 — First join (spawn)

- Player spawns into the **welcome area** (signs / hologram).
- The Tutorial Guide book is **auto-given on first join** *and* reopenable any
  time with `/tutorial`. (Belt and suspenders — signs point to `/tutorial` for
  anyone who closed it.)
- Signs do three things only: welcome, "type `/tutorial`", and "walk through the
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

### Stage 3 — Earn: your first money (`/kit welcome` → `/worth` → `/sell hand`)

This is the make-or-break step (see Friction analysis below). Structured as:

1. **Get tools:** `/kit welcome` (or however we hand out welcome gear).
2. **First quick win:** mine/chop a few blocks, then `/sell hand` immediately so
   the money loop *clicks* in under a minute.
3. **Learn to value items:** `/worth` while holding something.
4. **Hit a concrete target:** *"Earn $1,000 — the cost of your first claim.
   Mine and sell cobblestone, coal, and copper."*

### Stage 4 — Protect: claim your land (`/claim`)

- Claims run on the **Lands** plugin. A brand-new player just types `/claim` —
  the first `/claim` auto-creates their land and claims the chunk they're
  standing in. `/lands create <name>` exists but is optional; we deliberately
  teach only `/claim` to keep it simple.
- **First-claim cost = land-creation price.** Recommended: set Lands'
  land-creation price to **$1,000** (pulled from EssentialsX via Vault). That
  makes the first claim a $1,000 goal — the single biggest friction reducer
  available — which is why Stage 3's earn target is $1,000.
- Note: additional chunk claims after the first can cost more (e.g. the $5,000
  per-claim figure). That's fine — onboarding only needs to get them their first
  claim, so the book stays focused on the $1,000 first claim.

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

1. **Give a welcome kit.** A fresh player with no pickaxe/sword can't earn.
   `/kit welcome` (basic tools + food) removes the cold-start wall.
2. **One immediate sale.** Have them `/sell hand` on their very first gathered
   stack so they see money arrive *before* the grind. Momentum beats
   instruction.
3. **Name one method.** Don't offer mining *or* farming *or* combat — pick the
   fastest, safest beginner method and tell them exactly that. (Combat risks
   item loss and demoralizes newbies; steer the first goal toward mining or
   farming.)
4. **Set a concrete dollar target** equal to (or just above) the first claim
   cost, so "done" is unambiguous.
5. **Keep the first claim cheap.** Set the Lands land-creation price to
   **$1,000** so the first `/claim` is a $1,000 goal, not a grind. Mining and
   selling cobblestone/coal/copper should clear that in one short session, and
   the **daily reward** can cover a big chunk of it — turning the first claim
   into a near-finish rather than a wall. (Later chunk claims can cost more;
   onboarding only needs the first one.)

**Verdict:** Keep the step. With a welcome kit + one quick win + a single named
method (mine & sell cobblestone/coal/copper) + a low $1,000 first-claim target
backstopped by the daily reward, it's the *right* amount of friction — it
teaches the economy without losing people. Left open-ended, it's where your
funnel leaks.

---

## Open questions / assumptions to confirm

These change the copy and the flow, so flagging them explicitly:

1. ~~**Claim mechanic.**~~ ✅ Confirmed: **Lands** `/claim`. First `/claim`
   auto-creates the land (no `/lands create` needed); recommended first-claim
   cost **$1,000** (land-creation price, from EssentialsX via Vault).
2. ~~**Money method.**~~ ✅ Confirmed: mine & sell cobblestone, coal, copper.
3. **Set the price.** Confirm the Lands land-creation price is actually set to
   $1,000 in config, so the book's "$1,000" matches reality.
4. **Plugin stack.** Confirmed: Lands (`/claim`) + EssentialsX (`/sethome`,
   `/home`, `/worth`, `/sell hand`, `/kit`, economy). Still to confirm: the RTP
   plugin (`/rtp`) and the rank-ladder plugin (`/rank up`). Knowing the exact
   plugins lets me write real config (book NBT/JSON, sign data, kit defs).
5. **Values to fill in:** welcome kit name + contents, rank-up cost, and the
   daily-reward amount.
6. **Book delivery:** OK to auto-give the book on first join in addition to
   `/tutorial`? (Strongly recommended.)
7. **Deliverable format:** do you want (a) this design + copy, (b) actual
   implementable server files (book NBT/JSON, sign text, kit/permission config),
   or (c) both? That decides what I build next.
