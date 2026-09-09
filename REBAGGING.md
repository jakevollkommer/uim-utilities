# Rebagging: design notes

Not built. This is the plan and the research behind it.

## The problem

An ultimate ironman stores items in a looting bag and destroys it in the Wilderness to get them
back, which drops the whole bag on the floor. The pile comes back in no useful order, so taking the
items you actually want means reading a menu of twenty-odd `Take` lines and right-clicking through
it, one item at a time, remembering as you go. The ask is to left-click the pile repeatedly and have
the items arrive in an order you chose: potions first, then magic gear, then melee, and so on.

## How rebagging actually works

- Destroying the bag **in the Wilderness** drops the contents on the floor. Destroying it anywhere
  else, Ferox Enclave included, destroys the contents permanently.
- The pile lasts **60 minutes**, counted only while logged in, and in the Wilderness it is visible
  to everyone immediately, so the sorting has to be fast rather than merely convenient.
- Deathpiling is the alternative, and produces the same problem: a pile in arbitrary order.

**Settled 2026-09-09.** The looting bag setting is now Allow, Remove, or Allow in the Wilderness,
defaulting to the last, which is the shape lootbag-utilities already ships on the hub. Rebagging is
the one deliberate use of Destroy, and it only happens in the Wilderness.

Context worth keeping: an ultimate ironman deathpiles to empty the bag almost every time. Destroying
it is for when a deathbank is active and deathpiling is not possible, so they accept a few seconds
of Wilderness risk. A deathpile produces the same unordered heap, so the sort should not be tied to
bag destruction alone.

## How the menu can be reordered

The mechanism is settled, and there is precedent in both core RuneLite and a hub plugin.

- `client.getMenuEntries()` is an array where **index 0 is the bottom of the menu and the last
  element is the top**, which is the left-click action. Reordering the array reorders the menu.
- Ground item entries are the types from `WIDGET_TARGET_ON_GROUND_ITEM` (17) through
  `GROUND_ITEM_FIFTH_OPTION` (22). `MenuEntry.getIdentifier()` is the item id.
- **Core precedent**: `GroundItemsPlugin.onClientTick` rewrites the whole entry array to collapse
  duplicate ground items and append ` x N` to their targets.
- **Hub precedent**: geheur's more-menu-entry-swaps sorts ground item entries by price. It finds
  each contiguous run of ground item entries, sorts that run by a computed value, writes it back in
  place, and leaves every other entry untouched. That is exactly the shape this feature needs, with
  a category rank in place of a price.

Sorting is a swap, not a removal, so it sits on the accepted side of the hub's menu rules.

## Where a category comes from

**Decided 2026-09-09.** The categories are magic, ranged, melee, potions, food, runes, herbs, seeds,
tools and other. A curated set of names per category is the primary source, and the derived signals
below are the fallback for anything the sets do not name, rather than the other way round.

1. **What the player said.** A name list per category, comma separated, `*` wildcards, exactly like
   the shop feature's lists. Always wins.
2. **The curated sets.** Hardcoded names per category, shipped with the plugin.
3. **What the game says**, for anything still unnamed:
   - `ItemComposition.getInventoryActions()` contains `Eat` for food and `Drink` for potions. Local,
     instant, reliable.
   - `ItemManager.getItemStats(id).getEquipment()` gives the equipment slot and the attack bonuses
     (`amagic`, `arange`, `astab`/`aslash`/`acrush`, `str`, `rstr`). Whichever bonus dominates says
     whether a piece is magic, ranged or melee gear, so a Bandos chestplate classifies itself.
     **Caveat**: these stats are fetched over the network at startup, so they can be missing early or
     absent entirely offline. Classification must degrade to the name rules rather than misfile.
   - `isStackable`, `getNote`, and name patterns cover runes, herbs, seeds and logs.
3. **Fallback.** Anything unclassified lands in a single bucket that the player can position, rather
   than being dropped to the bottom silently.

## When the sort is active

**Decided 2026-09-09:** only for a rebag pile, not every ground item menu in the game.

Detecting one, in order of how much I trust it:

- **The bag leaving the inventory.** A looting bag disappearing from the inventory on the same tick
  as a burst of items landing on one tile is unambiguous, and the plugin already watches inventory
  departures for the raid feature.
- **Instantly public items.** `TileItem.getVisibleTime()` is when an item becomes visible to other
  players. An item the player drops is private for a minute first; the contents of a destroyed bag
  in the Wilderness are visible immediately. A self-owned item that is already public at spawn is
  therefore a strong tell.
- **The burst itself.** Many items appearing on one tile within a tick or two, which also catches
  deathpiles, where the same problem exists.

Despawn time alone does not discriminate: `getDespawnTime()` is an hour out for an ordinary drop as
well, so it confirms a pile is fresh rather than that it came from a bag.

## Loadouts

Asked for 2026-09-09: named presets, with a few shipped ready to use. Wildy clues wants spade, clue
and construction cape at the top; a raid trip wants brews and gear. One fixed category order cannot
serve both, and editing the order every time defeats the point.

A loadout is two things: the category order, and a short list of item names pinned above every
category. The pins are what make the wildy clue case work, since a spade belongs to no category
worth promoting on its own.

Shape, from cheapest to best:

- **A dropdown of shipped loadouts plus one custom slot.** An enum in the config, with the custom
  order and pins in two text items. Cheap, and the shipped ones are curated data in their own file.
- **Loadouts stored as JSON in a config string**, the way deathbank-utility persists its state
  through Gson. Any number of them, still no new UI, but editing raw JSON in a config box is grim.
- **A panel with drag to reorder and a loadout picker.** What this wants to be eventually.

Switching wants to be fast, since it happens at the pile: a config dropdown is one click away
through the sidebar, a hotkey is faster and is the same shape as geheur's hotkeyable swaps.

## Ordering interface

The request is drag and drop. That means a side panel, which is a bigger build and a bigger review
surface. Staging it:

- **First**: an ordered, comma separated category list in the config, so the order is the text.
  Editable, hub-friendly, no new UI.
- **Later**: a panel with drag to reorder, once the category model has proven itself in a real
  rebag. Deathbank-utility already ships a panel, so there is a pattern to copy.

## The coexistence problem

Jake's client runs geheur's more-menu-entry-swaps, which sorts the same ground item block on the
same event. Two plugins sorting the same array means last writer wins, and the order will look
random depending on subscriber order. This has to be decided, not discovered:

- Run at an explicit `@Subscribe(priority = ...)` so this plugin sorts last, or
- Detect that plugin's ground item sort and stand down, or
- Document that its ground item price sort should be off.

Core's Ground Items collapse feature also rewrites entries, on `ClientTick` rather than
`PostMenuSort`, and appends ` x N` to targets. Any name matching must tolerate that suffix.

## To verify before building

- That the last array element is the left-click entry, in a real pile rather than by inference.
- Whether the client caps how many entries a menu can hold, since a rebag pile is large.
- Whether ground item entries are always one contiguous run, or several, as geheur's code assumes.
- Whether `getItemStats` is populated by the time a rebag happens, and what fraction of a typical
  bag it can classify without help.
- What an inventory-full pile does. Only stackables already held can be taken, so the sort could put
  an untakeable item on left-click. Deprioritised: it matters little for this use case.

**Order within a category:** leave the game's own order alone.

## Open questions for Jake

1. Whether the curated sets ship as one file per category or one file with a category per block.
2. Whether an unrecognised item sits above or below the categories that were recognised.
3. Whether a deathpile should trigger the sort too. It produces the same heap, and the burst
   detection would catch it for free.
