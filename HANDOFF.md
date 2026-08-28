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

`com.uimutilities.cox` owns the feature. `GroundItemWarning` is the coordinator and the only
public type in it; `RaidScope` answers where and when the raid counts, `CarriedItems` holds what
was brought in, `InventoryLedger` names what left the inventory, `DropWatcher` pairs that with what
landed on the floor, `DroppedItems` keeps the count per tile, `RaidExits` owns the exit ids and the
scene scan, and `ExitLabelOverlay` draws the label. Config lives in a "Chambers of Xeric" section:
`coxWarnGroundItems` (master) and `coxDeprioritizeExit`.

- Only items the player carried in and then dropped are counted. A drop click arms a
  pending drop (item id plus tick), and the next self-owned `ItemSpawned` of that id
  within 3 ticks and 2 tiles of the player claims it. Everything else on the floor is
  ignored, which is the fix for the first in-game run: a raid is full of the player's own
  ground items that were never carried in, mostly loot from what was killed on the way,
  and the count read 16 in a raid where nothing had been dropped.
- What counts as carried in is the inventory plus worn items, snapshotted when the
  in-dungeon varbit goes to 1. An empty snapshot means it was never captured, and then
  every drop counts rather than none: a missed warning is what loses the items.
- Everything is gated on `VarbitID.RAIDS_CLIENT_INDUNGEON == 1` plus a loaded CoX region.
- Tiles are keyed by their **instance template point**
  (`WorldPoint.fromLocalInstance`), not by instance world coordinates: the instance
  chunk mapping is rebuilt on every scene load, so raw world points would not match
  the same physical tile between rooms.
- Drops are remembered across the scene reloads between rooms. Spawns and despawns during
  a load are ignored, since a load re-reports everything the new scene covers and reports
  nothing for the rooms that dropped out of it; a reload cannot double count and cannot
  wipe the rooms left behind.
- Enabling the plugin mid-raid starts the count at zero. Items already on the floor cannot
  be told apart from raid loot, so tracking follows what is dropped from then on.
- The exit's left-click option is deprioritized (never removed) while items remain,
  matching `nex-leech-utility`'s `maybeDeprioritizeDoorEntry`.

**Remove before submitting to the hub:** `RaidExits.describeOnce` names every interactable object
in the raid at debug level. It exists only to identify the exits that are still guesses, and should
go once they are confirmed, along with the ids that never appear.

### Verified in-game 2026-08-27

The raid gate (in-dungeon varbit plus region), the carried-in snapshot, the drop pairing, the
pickup clearing the count, the exit scan and the label all work. Dropping a rune pickaxe logged
`counted dropped item 1275` with the floor spawn arriving first and the inventory change joining
it, picking it up took the count back to 0, and the label drew on the steps.

The deprioritize could not be judged: the steps are menu swapped in Jake's client, and a swap sets
the left-click after the deprioritize is applied. Worth knowing generally, since it means the
overlay is the real protection for anyone who swaps their exit.

Two things cost a session each before that. The plugin was disabled in the profile
(`runelite.uimutilitiesplugin=false`), which looks identical to a broken feature from the outside;
check that first. And the dev client sideloads the deployed jar on top of the copy `runPlugin`
builds, so the plugin appears twice and both copies share one config key, which is the likely way
it got disabled.

**Open question: the lobby counts as the dungeon.** The bank chest at Xeric's lookout (object
47420) was logged as an in-raid object, so the in-dungeon varbit plus region test is true outside
the raid proper. Items dropped in the lobby are not destroyed, so they should not count. Check
whether region 12889 is the lobby and whether `RAIDS_CLIENT_INDUNGEON` is really 1 there, then
tighten the gate.

### Still to verify in-game (a raid is needed, nothing here is confirmed)

1. **Which object is each exit.** 49999 (`RAIDS_EXIT_STEPS_MULTI`) is confirmed: it is the Climb
   steps out of the first room, and it also carries a Reload option. Still unconfirmed are
   `RAIDS_BOSSEXIT` 29996, `RAIDS_EXIT_STEPS` 29778 and 50000 (`RAIDS_EXIT_STEPS_RELOAD`); the
   post-Olm exit in particular has not been seen. Trim the set to what appears. The cache dumper in
   `~/projects/osrs/runelite` could not vet these: its `:cache` build no longer parses the live
   cache (`ObjectLoader.processOp` throws on an unknown opcode).
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

## Feature 2 — take Destroy off the looting bag

**Status: implemented, not yet verified in-game.** Branch `looting-bag-destroy`, stacked
on `cox-ground-item-warning`.

Destroying a looting bag outside the Wilderness loses everything inside it. While
`hideLootingBagDestroy` is on (default), the Destroy entry is removed from the bag's
menu, in `com.uimutilities.lootingbag.LootingBagProtection`. Matching is
`event.getItemId()` in `{LOOTING_BAG 11941, LOOTING_BAG_OPEN 22586}` plus the option
text `Destroy`, and removal is `client.getMenu().removeMenuEntry(...)`.

**Hub risk, decide before submitting.** The rejected-features wiki lists "conditional
menu entry removing" as overpowered, with the example of hiding attack options based on
game state. This removal is unconditional while the setting is on and only touches a
destructive inventory option, so it is a different thing, but it is still entry removal
and a reviewer may read it the other way. The fallback that is certainly accepted is
deprioritizing Destroy instead, the same treatment feature 1 gives the raid exit.

**Still to verify in-game.** That the entry is actually gone from both the closed and
open bag, in the inventory and anywhere else the bag shows a Destroy option, and that
turning the setting off restores it without a relog. The bank placeholder ids (18274,
22587) are not matched: confirm placeholders show Release rather than Destroy.

## Feature 3 — no selling protected items to shops

**Status: implemented, not yet verified in-game.** Branch `shop-sell-protection`, stacked
on `looting-bag-destroy`.

A general store will buy a twisted bow, and it is gone the moment the shop closes. While
`blockSelling` is on (default), every `Sell` entry is removed from items matching the
`protectedItems` list, in `com.uimutilities.shops.SellProtection`. Matching runs on the
menu entry's target text, so no item composition lookups on the menu path.

- Entries are item names, comma separated, `*` wildcards allowed, the same shape as the
  Ground Items lists. Parsing is `Text.fromCSV`, wildcards are `WildcardMatcher`, exact
  names answer from a lowercased set so only wildcard entries are walked.
- A plain name also covers that item's variants: a trailing bracket for charges and
  ornaments (`Trident of the seas (full)`, `Dragon dagger(p++)`) and a trailing number for
  degraded barrows pieces (`Ahrim's robetop 75`). Verified offline against both.
- The default list is the 233 items linked from the ultimate ironman guide's equipment
  page, in `ProtectedItems.DEFAULT`. It was generated from the page's wikitext through the
  wiki API: every `{{plink}}` target, resolved through redirects, with the aggregate pages
  (the blessed dragonhide slots, god blessings, god capes) expanded to their real item
  names, and `Damaged book (Ancient)` corrected to the in-game `Damaged book`.

**Hub risk, same as feature 2.** This is menu entry removal driven by a user list, not a
game state, but a reviewer may still read it against the rejected-features wiki's
"conditional menu entry removing" line. Deprioritizing the Sell entries is the fallback.

**Still to verify in-game.** That the shop inventory's Sell options carry the item name in
the target text and an item id (that is what the matching reads), that `Value` and
`Examine` survive, that the left-click sell is gone as well as the right-click ones, and
that editing the list applies without a relog. Also worth a pass on how many of the 233
names actually match in-game names: they come from wiki infobox names, and any that are
wrong simply fail to protect, silently.

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
