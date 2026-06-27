# Spawn signs / portal area

Replacement copy for the spawn signs. A Minecraft sign is **4 lines, ~15
characters/line**, so signs only do the bare minimum: welcome the player, send
them to `/starter`, and funnel them through the portal. Everything else lives in
the Starter Guide book.

> If a hologram plugin is available, a single hologram can replace signs 1–2 and
> hold more text comfortably. Signs are the fallback.

---

### Sign 1 — Welcome
```
  » WELCOME «
  to Theatria
   New here?
  Read below v
```

### Sign 2 — Start the guide (place most prominently)
```
    STEP 1
    Type:
   /starter
 opens your guide
```

### Sign 3 — At the RTP portal
```
    STEP 2
  Walk through
  the PORTAL  >
   to the wild
```

### Sign 4 — Daily reward teaser
```
     TIP
  Play daily for
  a free reward!
   See /starter
```
> Signs are static, so don't hardcode a minute count here (it would drift from the
> Sessions threshold). Point at `/starter`, whose book shows the live requirement.
> If you do want a number on the sign, set it to TheatriaSessions' real threshold
> (active, non-AFK minutes) and keep it in sync by hand.

### Sign 5 — Help (optional)
```
    STUCK?
   Ask in chat
  or reread the
 guide: /starter
```
