# UIM Utilities

Quality of life and safety warnings for ultimate ironman accounts

## Features

### Chambers of Xeric: ground items left behind

The raid destroys everything left on the floor once you leave. While any of your own
items are still on the ground inside the raid, the exit is labelled with how many you
left and its left-click option is deprioritized, so leaving takes a deliberate
right-click. The menu entry is never removed.

### Looting bag: no Destroy option

Destroying a looting bag outside the Wilderness loses everything inside it. The Destroy
option is taken off the bag while the setting is on, so there is nothing to misclick.

### Shops: no selling protected items

A general store will buy a twisted bow, and the item is gone the moment the shop closes.
The Sell options are taken off every item on the protected list, which starts as the gear
from the [ultimate ironman equipment guide](https://oldschool.runescape.wiki/w/Ultimate_Ironman_Guide/Equipment).
Entries are item names with `*` wildcards, and a plain name also covers that item's
charged, ornamented and degraded variants.

## Development

```
./gradlew runPlugin   # launch RuneLite in developer mode with the plugin loaded
./gradlew jar         # build the sideloadable jar
./gradlew deploy      # build and copy the jar to ~/.runelite/sideloaded-plugins
```
