package com.uimutilities.lootingbag;

import com.uimutilities.Feature;
import com.uimutilities.UimUtilitiesConfig;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarbitID;

/**
 * Destroying a looting bag outside the Wilderness takes everything inside it with it, which for an
 * ultimate ironman is the whole point of carrying one. Inside the Wilderness the same click drops
 * the contents on the floor, which is the deliberate move when a deathbank rules out deathpiling, so
 * by default the option is removed everywhere except there.
 */
@Singleton
public class LootingBagProtection implements Feature
{
	private static final String DESTROY = "Destroy";

	private static final Set<Integer> LOOTING_BAG_IDS = Set.of(
		ItemID.LOOTING_BAG,
		ItemID.LOOTING_BAG_OPEN
	);

	private final Client client;
	private final UimUtilitiesConfig config;

	@Inject
	public LootingBagProtection(Client client, UimUtilitiesConfig config)
	{
		this.client = client;
		this.config = config;
	}

	@Override
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		// Most menu entries carry no item at all, so the id is the cheap way out of this
		boolean isLootingBagEntry = LOOTING_BAG_IDS.contains(event.getItemId());
		if (!isLootingBagEntry || !DESTROY.equals(event.getOption()) || !shouldRemoveDestroy())
		{
			return;
		}

		client.getMenu().removeMenuEntry(event.getMenuEntry());
	}

	private boolean shouldRemoveDestroy()
	{
		switch (config.lootingBagDestroy())
		{
			case REMOVE:
				return true;
			case ALLOW_IN_WILDERNESS:
				return !isInWilderness();
			default:
				return false;
		}
	}

	private boolean isInWilderness()
	{
		return client.getVarbitValue(VarbitID.INSIDE_WILDERNESS) == 1;
	}
}
