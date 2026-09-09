# UIM Utilities

Quality of life and safety warnings for ultimate ironman accounts

## Features

### Chambers of Xeric: ground items left behind

The raid destroys everything left on the floor once you leave. While anything you carried
in and dropped is still on the ground, the exit is labelled with how many items you left
and its left-click option is deprioritized, so leaving takes a deliberate right-click. The
menu entry is never removed. Loot picked up inside the raid is not counted, only what you
brought in and dropped.

### Looting bag: the Destroy option

Destroying a looting bag outside the Wilderness loses everything inside it. Inside the Wilderness
the same click drops the contents on the floor, which is the deliberate move when a deathbank rules
out deathpiling. By default the option is removed everywhere except the Wilderness, and it can be
set to Allow or Remove outright.

### Shops: no selling protected items

A general store will buy a twisted bow, and the item is gone the moment the shop closes.
Two modes. **Block listed items** takes the Sell options off everything on the block list, which
starts as the gear from the
[ultimate ironman equipment guide](https://oldschool.runescape.wiki/w/Ultimate_Ironman_Guide/Equipment).
**Only allow listed items** does the opposite: nothing can be sold except what is on the sellable
list, so an item you never thought to list is protected by default.

Both lists are item names with `*` wildcards, comma separated, and a plain name also covers that
item's charged, ornamented and degraded variants. They live in their own collapsed config sections,
since the block list is long.

## Early release

This plugin is an early release and not feature complete. Its warnings can be wrong in both
directions, so treat them as a second pair of eyes rather than a guarantee, especially on an
ultimate ironman where the mistakes are permanent. Bug reports and feature requests are very
welcome on the
[issues page](https://github.com/jakevollkommer/uim-utilities/issues), also reachable from the
plugin config's Feedback section.

## Development

```
./gradlew runPlugin   # launch RuneLite in developer mode with the plugin loaded
./gradlew jar         # build the sideloadable jar
./gradlew deploy      # build and copy the jar to ~/.runelite/sideloaded-plugins
```
