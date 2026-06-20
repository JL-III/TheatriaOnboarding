# Theatria Onboarding

A redesign of the new-player onboarding experience for the Theatria Minecraft
server. Today onboarding is just oak signs at spawn; this project replaces that
with a guided, task-based flow built around an in-game **Starter Guide** book.

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

- [`docs/onboarding-design.md`](docs/onboarding-design.md) — the full flow,
  design principles, the friction analysis, and open questions.
- [`content/starter-book.md`](content/starter-book.md) — page-by-page text for
  the `/starter` virtual book.
- [`content/spawn-signs.md`](content/spawn-signs.md) — replacement text for the
  spawn signs / portal area.

## Status

**v0 draft.** Flow and copy are drafted. Claim mechanic confirmed: **Lands**
`/claim` costs **$5,000** from the player's EssentialsX balance, so the earn
target is $5,000. Remaining `[FILL IN]` values (starter kit, recommended
money method, rank-up cost, daily-reward amount) are pending confirmation —
see the open questions in the design doc.
