# UIM Utilities — handoff

Scaffolded from `~/projects/osrs/runelite-plugin-template` on 2026-08-27. Builds
clean (`./gradlew jar`) and the plugin loads. No GitHub remote yet, so feature branches
have nowhere to open a PR into main until one exists.

The plugin is a home for many ultimate-ironman quality-of-life features. Feature 1
is below; keep each feature in its own package/class so the plugin does not become
one god class (see `~/projects/osrs/minimalist` for how content areas are split).

## Feature 1 — warn before leaving items behind in Chambers of Xeric

**Status: implemented, not yet verified in-game.** Branch `cox-ground-item-warning`.

**The problem.** UIMs use the floor as storage inside CoX, commonly dropping items
before the Olm final phase. The raid instance is destroyed on exit, so anything
still on the ground is gone forever. People do leave without picking it up.

### What was built

`com.uimutilities.cox.CoxGroundItems` owns the whole feature; the plugin class only
routes events to it. `CoxExitOverlay` draws the label. Config lives in a "Chambers of
Xeric" section: `coxWarnGroundItems` (master) and `coxDeprioritizeExit`.

- Ground items are counted from `ItemSpawned`/`ItemDespawned` where
  `TileItem.getOwnership() == OWNERSHIP_SELF`, gated on
  `VarbitID.RAIDS_CLIENT_INDUNGEON == 1` plus a loaded CoX region.
- Tiles are keyed by their **instance template point**
  (`WorldPoint.fromLocalInstance`), not by instance world coordinates: the instance
  chunk mapping is rebuilt on every scene load, so raw world points would not match
  the same physical tile between rooms.
- Items are remembered across the scene reloads between rooms. On the first tick
  after a load, remembered tiles that the new scene *does* cover but did not report
  an item on are dropped, so a picked-up item cannot linger as a phantom warning.
  Tiles outside the new scene are kept.
- Enabling the plugin mid-raid scans the loaded scene once (`rebuildFromScene`),
  because items already on the floor do not fire spawn events.
- The exit's left-click option is deprioritized (never removed) while items remain,
  matching `nex-leech-utility`'s `maybeDeprioritizeDoorEntry`.

### Still to verify in-game (a raid is needed, nothing here is confirmed)

1. **Which object is the exit.** All four candidates are deprioritized:
   `RAIDS_BOSSEXIT` 29996, `RAIDS_EXIT_STEPS` 29778, and the raw ids 49999
   (`RAIDS_EXIT_STEPS_MULTI`) / 50000 (`RAIDS_EXIT_STEPS_RELOAD`) from the
   package-private `ObjectID1`. Confirm which one actually appears, and trim the set
   to the exits that destroy items. The cache dumper in `~/projects/osrs/runelite`
   could not vet these: its `:cache` build no longer parses the live cache
   (`ObjectLoader.processOp` throws on an unknown opcode).
2. **That the exit is a game object at all**, not a widget button. If it is a widget,
   the deprioritize half of the feature does not apply and the design becomes a
   confirm overlay.
3. **That the exit is a `GameObject`** specifically. The overlay anchors on
   `GameObjectSpawned`; if the exit turns out to be a ground/decorative/wall object,
   add that spawn event too.
4. **Ownership inside the raid.** Confirm dropped items report `OWNERSHIP_SELF` in
   an instance, and that teammates' drops and raid loot do not.
5. **Scene reload behavior.** Confirm the raid reloads the scene between rooms at
   all, and that the remembered-tile reconcile survives a walk out and back.

### Scope guard

Everything is gated on the in-dungeon varbit plus the CoX region list
(`12889, 13136-13141, 13145, 13393-13397, 13401`), because object ids are reused
across unrelated content.

## Later feature candidates (not designed yet)

- Deathpile timer/location awareness (note: Adam's `Death Indicator` hub plugin
  already covers where-you-died + despawn timer for non-instanced deaths — do not
  duplicate it; check it first).
- Looting-bag / lost-item warnings in other instanced content that destroys the
  instance on exit (Gauntlet, ToA, Nightmare).
- Deathbank tracking is already its own plugin (`~/projects/osrs/deathbank-utility`) —
  keep it there, do not fold it in.

## Conventions reminders

- Every `@Subscribe` method must be named `on` + event simple name or the plugin
  dies silently at enable time.
- `build=standard` in `runelite-plugin.properties` is mandatory for the hub.
- LICENSE must stay the hub's verbatim BSD-2 text with only the attribution line
  changed.
- Feature work goes on a branch and ends with a PR into main.
- Full conventions: `~/projects/osrs/CLAUDE.md`. Hub review rules learned the hard
  way are in that file's plugin-hub section plus PRs #15124/#15167/#15265.
