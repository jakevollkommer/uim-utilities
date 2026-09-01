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

**This collides with the looting bag feature already in the plugin**, which removes Destroy from the
bag. Rebagging is the one time destroying it is deliberate. Options, in preference order:

1. Remove Destroy only outside the Wilderness, where it deletes items, and leave it in the
   Wilderness, where it is the intended move. The rule matches the game's own behaviour.
2. Keep the removal unconditional and let the player toggle the setting off to rebag.

Option 1 is better protection and better ergonomics, but it is conditional removal, which the hub's
rejected-features page is wary of. Worth deciding before either feature is submitted again.

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

## Where a category can come from

The interesting question, because a hardcoded list of every item in the game is not maintainable and
the hub rejects raw ids as config input. Three sources, in precedence order:

1. **What the player said.** A name list per category, comma separated, `*` wildcards, exactly like
   the shop feature's protected list. Always wins, and covers anything the other two get wrong.
2. **What the game says.** This is the part that avoids the maintenance burden:
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
- What an inventory-full pile does: only stackables already held can be taken, so the sort should
  account for that rather than putting an untakeable item on left-click.

## Open questions for Jake

1. The category set. Magic, ranged, melee, potions, food, runes, herbs, seeds, tools, other? Or
   fewer, bigger buckets?
2. Should the sort always be on, on a hotkey, or only when a pile is large enough to look like a
   rebag?
3. Within a category, what order? Value, quantity, name, or leave the game's order alone?
4. The Destroy conflict above: conditional removal, or leave it to the toggle?
