package com.uimutilities.lootingbag;

/**
 * What to do with the looting bag's Destroy option. Destroying the bag in the Wilderness drops its
 * contents on the floor, which is how an ultimate ironman gets them back when a deathbank stops them
 * deathpiling. Anywhere else it deletes them.
 */
public enum LootingBagDestroy
{
	ALLOW("Allow"),
	REMOVE("Remove"),
	ALLOW_IN_WILDERNESS("Allow in the Wilderness");

	private final String label;

	LootingBagDestroy(String label)
	{
		this.label = label;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
