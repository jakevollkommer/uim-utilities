# UIM Utilities — handoff

Scaffolded from `~/projects/osrs/runelite-plugin-template` on 2026-08-27. Builds
clean (`./gradlew jar`), plugin loads, nothing implemented yet beyond the Feedback
section. No GitHub remote yet.

The plugin is a home for many ultimate-ironman quality-of-life features. Feature 1
is below; keep each feature in its own package/class so the plugin does not become
one god class (see `~/projects/osrs/minimalist` for how content areas are split).

## Feature 1 — warn before leaving items behind in Chambers of Xeric

**The problem.** UIMs use the floor as storage inside CoX, commonly dropping items
before the Olm final phase. The raid instance is destroyed on exit, so anything
still on the ground is gone forever. People do leave without picking it up.

**The behavior to build.**
1. Track ground items inside the raid that belong to the player.
2. While any remain, **deprioritize** the raid exit's left-click option so a
   careless click cannot leave, forcing a deliberate right-click.
3. Draw an overlay on/near the exit reading something like
   `You left items on the ground` with the count.

**Do not remove or hide the menu entry.** The hub explicitly rejects
"conditional menu entry removing" as overpowered
(https://github.com/runelite/runelite/wiki/Rejected-or-Rolled-Back-Features).
Deprioritizing is accepted and is already shipping in Jake's `nex-leech-utility`
(hub PR #15167, merged), so copy that pattern exactly.

### The deprioritize pattern (proven, hub-approved)

`~/projects/osrs/nex-leech-utility/src/main/java/com/nexleechutility/NexLeechUtilityPlugin.java`,
`maybeDeprioritizeDoorEntry` around line 820:

```java
@Subscribe
public void onMenuEntryAdded(MenuEntryAdded event)
{
    MenuEntry entry = event.getMenuEntry();
    if (isObjectAction(entry.getType()) && EXIT_OBJECT_IDS.contains(event.getIdentifier()))
    {
        entry.setDeprioritized(true);
    }
}
```

`isObjectAction` switches over `GAME_OBJECT_FIRST_OPTION` … `FIFTH_OPTION`
(same file, ~line 834). Note it matches on `event.getIdentifier()` for the
object id, and gates on a config toggle first.

### Identifying the player's own ground items — use the client API, not a heuristic

`net.runelite.api.TileItem` exposes ownership directly:

```java
int OWNERSHIP_NONE = 0;
int OWNERSHIP_SELF = 1;   // <- what we want
int OWNERSHIP_OTHER = 2;
int OWNERSHIP_GROUP = 3;
int getOwnership();
```

So subscribe to `ItemSpawned` / `ItemDespawned` and keep a count of tiles holding
items where `getOwnership() == TileItem.OWNERSHIP_SELF`, scoped to the raid.
RuneLite's own Ground Items plugin uses this (`GroundItemsPlugin.java:275`) — read
it for the idiomatic handling. This avoids the guesswork of trying to infer
ownership from drop events, and correctly ignores teammates' items and raid drops.

Open question worth deciding early: whether to count *only* deliberately dropped
items or any self-owned ground item in the instance. Start with all self-owned
(simplest, honest) and refine if it produces noise.

### Exit object candidates (VERIFY IN-GAME)

From `runelite-api` gameval `ObjectID`:

| Constant | ID | Note |
|---|---|---|
| `RAIDS_BOSSEXIT` | 29996 | most likely the post-Olm exit |
| `RAIDS_EXIT_STEPS` | 29778 | |
| `RAIDS_EXIT_STEPS_MULTI` | 49999 | in package-private `ObjectID1`, use the raw int |
| `RAIDS_EXIT_STEPS_RELOAD` | 50000 | in package-private `ObjectID1`, use the raw int |

`ObjectID1` is package-private, so constants from it must be written as raw ints
with a comment naming them (deathbank-utility does this for its chest ids).

**First task: confirm which object is actually used** — dev client, CoX, kill Olm
(or use a scouted/practice raid), and inspect the exit with the object inspector.
Also check whether the exit is a game object at all vs a widget button; the user
described it as "the leave raid button or whatever", so both are possible. If it
is a widget, the deprioritize approach does not apply and the design changes to a
confirm overlay — settle this before writing code.

Also confirm: does the raid have more than one exit (Olm room vs lobby vs
"Leave party")? Only the item-destroying one should be gated.

### Scope guard

Warn only inside CoX for now. Region-gate everything so the menu entry
manipulation cannot leak elsewhere — `~/projects/osrs/CLAUDE.md` documents that
object/NPC ids are reused across unrelated content, and deathbank-utility's
`SAFE_DEATH_REGIONS`/CoX region list is a ready source of the CoX region ids
(`12889, 13136, 13137, 13138, 13139, 13140, 13141, 13145, 13393, 13394, 13395,
13396, 13397, 13401`).

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
