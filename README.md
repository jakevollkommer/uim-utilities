# UIM Utilities

Quality of life and safety warnings for ultimate ironman accounts

## Features

### Chambers of Xeric: ground items left behind

The raid destroys everything left on the floor once you leave. While anything you carried
in and dropped is still on the ground, the exit is labelled with how many items you left
and its left-click option is deprioritized, so leaving takes a deliberate right-click. The
menu entry is never removed. Loot picked up inside the raid is not counted, only what you
brought in and dropped.

### Looting bag: no Destroy option

Destroying a looting bag outside the Wilderness loses everything inside it. The Destroy
option is taken off the bag while the setting is on, so there is nothing to misclick.

### Shops: no selling protected items

A general store will buy a twisted bow, and the item is gone the moment the shop closes.
The Sell options are taken off every item on the protected list, which starts as the gear
from the [ultimate ironman equipment guide](https://oldschool.runescape.wiki/w/Ultimate_Ironman_Guide/Equipment).
Entries are item names with `*` wildcards, and a plain name also covers that item's
charged, ornamented and degraded variants.

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
