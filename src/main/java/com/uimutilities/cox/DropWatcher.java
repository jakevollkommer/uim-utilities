package com.uimutilities.cox;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import net.runelite.api.coords.WorldPoint;

/**
 * Pairs the two halves of a drop: the item leaving the inventory, and the item landing on the floor
 * beside the player. They belong to the same server tick but arrive in either order, so whichever is
 * seen first waits here a few ticks for the other. Nothing counts until both halves are in, which is
 * what keeps loot that lands next to the player out of the count.
 */
class DropWatcher
{
	private static final int TIMEOUT_TICKS = 3;
	private static final int LANDING_RANGE = 2;

	private final List<WaitingDeparture> departures = new ArrayList<>();
	private final List<WaitingLanding> landings = new ArrayList<>();

	/** @return where the drop landed, once the inventory half is in too. */
	Optional<WorldPoint> itemLanded(int itemId, WorldPoint tile, int distanceFromPlayer, int tick)
	{
		if (distanceFromPlayer > LANDING_RANGE)
		{
			return Optional.empty();
		}

		boolean inventoryHalfWasWaiting = claimFirst(departures, departure -> departure.matches(itemId, tick)).isPresent();
		if (inventoryHalfWasWaiting)
		{
			return Optional.of(tile);
		}

		landings.add(new WaitingLanding(itemId, tile, tick));
		return Optional.empty();
	}

	/** @return where the drop landed, once the floor half is in too. */
	Optional<WorldPoint> itemLeftInventory(int itemId, int tick)
	{
		Optional<WorldPoint> landed = claimFirst(landings, landing -> landing.matches(itemId, tick))
			.map(landing -> landing.tile);
		if (landed.isEmpty())
		{
			departures.add(new WaitingDeparture(itemId, tick));
		}
		return landed;
	}

	void expire(int tick)
	{
		int oldestAllowed = tick - TIMEOUT_TICKS;
		departures.removeIf(departure -> departure.tick < oldestAllowed);
		landings.removeIf(landing -> landing.tick < oldestAllowed);
	}

	void clear()
	{
		departures.clear();
		landings.clear();
	}

	/**
	 * Takes the first half still waiting for its partner, one at a time: two of the same item dropped
	 * in quick succession are two drops, and consuming both halves at once would count only one.
	 */
	private static <T> Optional<T> claimFirst(List<T> waiting, Predicate<T> matches)
	{
		Iterator<T> candidates = waiting.iterator();
		while (candidates.hasNext())
		{
			T candidate = candidates.next();
			if (matches.test(candidate))
			{
				candidates.remove();
				return Optional.of(candidate);
			}
		}
		return Optional.empty();
	}

	private static final class WaitingDeparture
	{
		private final int itemId;
		private final int tick;

		private WaitingDeparture(int itemId, int tick)
		{
			this.itemId = itemId;
			this.tick = tick;
		}

		private boolean matches(int itemId, int tick)
		{
			return this.itemId == itemId && this.tick >= tick - TIMEOUT_TICKS;
		}
	}

	private static final class WaitingLanding
	{
		private final int itemId;
		private final WorldPoint tile;
		private final int tick;

		private WaitingLanding(int itemId, WorldPoint tile, int tick)
		{
			this.itemId = itemId;
			this.tile = tile;
			this.tick = tick;
		}

		private boolean matches(int itemId, int tick)
		{
			return this.itemId == itemId && this.tick >= tick - TIMEOUT_TICKS;
		}
	}
}
