package com.uimutilities.cox;

import java.util.HashSet;
import java.util.Set;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;

/**
 * The item ids the player brought into the raid, which is what can be left behind. A raid also fills
 * the floor with the player's own items that were never carried in, mostly loot from what was killed
 * along the way, and warning about those is noise.
 *
 * Nothing is filtered out until the capture succeeds: a missed warning is what loses items.
 */
class CarriedItems
{
	private final Set<Integer> itemIds = new HashSet<>();
	private boolean known;

	/**
	 * @return true once both containers have been read. A container can exist before the server has
	 * filled it, and capturing it empty would make every later drop look like raid loot, so an empty
	 * result is treated as not yet captured.
	 */
	boolean capture(ItemContainer inventory, ItemContainer worn)
	{
		if (inventory == null || worn == null)
		{
			return false;
		}

		itemIds.clear();
		addItemIds(inventory);
		addItemIds(worn);
		known = !itemIds.isEmpty();
		return known;
	}

	boolean areKnown()
	{
		return known;
	}

	int size()
	{
		return itemIds.size();
	}

	boolean includes(int itemId)
	{
		return !known || itemIds.contains(itemId);
	}

	void forget()
	{
		itemIds.clear();
		known = false;
	}

	private void addItemIds(ItemContainer container)
	{
		for (Item item : container.getItems())
		{
			if (item.getId() > 0)
			{
				itemIds.add(item.getId());
			}
		}
	}
}
