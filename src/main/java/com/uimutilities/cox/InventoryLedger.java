package com.uimutilities.cox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;

/**
 * What the inventory held last time it changed, so that the items which have since left it can be
 * named. Leaving the inventory is half of a drop; the other half is the item landing on the floor.
 */
class InventoryLedger
{
	private final Map<Integer, Integer> quantitiesByItemId = new HashMap<>();

	void reset(ItemContainer inventory)
	{
		quantitiesByItemId.clear();
		quantitiesByItemId.putAll(quantitiesOf(inventory));
	}

	/** @return the ids that have less in the inventory than they did, the ledger moving on to now. */
	List<Integer> itemsThatLeft(ItemContainer inventory)
	{
		Map<Integer, Integer> current = quantitiesOf(inventory);
		List<Integer> departed = new ArrayList<>();
		quantitiesByItemId.forEach((itemId, quantity) ->
		{
			if (current.getOrDefault(itemId, 0) < quantity)
			{
				departed.add(itemId);
			}
		});

		quantitiesByItemId.clear();
		quantitiesByItemId.putAll(current);
		return departed;
	}

	void forget()
	{
		quantitiesByItemId.clear();
	}

	private static Map<Integer, Integer> quantitiesOf(ItemContainer container)
	{
		Map<Integer, Integer> quantities = new HashMap<>();
		if (container == null)
		{
			return quantities;
		}

		for (Item item : container.getItems())
		{
			if (item.getId() > 0)
			{
				quantities.merge(item.getId(), item.getQuantity(), Integer::sum);
			}
		}
		return quantities;
	}
}
