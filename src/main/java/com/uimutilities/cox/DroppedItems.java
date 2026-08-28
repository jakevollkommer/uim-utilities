package com.uimutilities.cox;

import java.util.HashMap;
import java.util.Map;
import net.runelite.api.coords.WorldPoint;

/**
 * What the player has dropped in this raid and not picked back up, as a stack count per item id per
 * tile. Tiles are instance template points, so an entry survives the scene reloads between rooms and
 * a room the player has walked away from keeps what it is holding.
 */
class DroppedItems
{
	private final Map<WorldPoint, Map<Integer, Integer>> stacksByTile = new HashMap<>();

	void add(WorldPoint tile, int itemId)
	{
		stacksByTile
			.computeIfAbsent(tile, point -> new HashMap<>())
			.merge(itemId, 1, Integer::sum);
	}

	void remove(WorldPoint tile, int itemId)
	{
		Map<Integer, Integer> stacks = stacksByTile.get(tile);
		int held = stacks == null ? 0 : stacks.getOrDefault(itemId, 0);
		if (held == 0)
		{
			return;
		}

		if (held > 1)
		{
			stacks.put(itemId, held - 1);
			return;
		}

		stacks.remove(itemId);
		if (stacks.isEmpty())
		{
			stacksByTile.remove(tile);
		}
	}

	int count()
	{
		return stacksByTile.values().stream()
			.flatMap(stacks -> stacks.values().stream())
			.mapToInt(Integer::intValue)
			.sum();
	}

	boolean isEmpty()
	{
		return stacksByTile.isEmpty();
	}

	void clear()
	{
		stacksByTile.clear();
	}
}
