package com.uimutilities.lootingbag;

import com.uimutilities.UimUtilitiesConfig;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.gameval.ItemID;

/**
 * Destroying a looting bag outside the Wilderness takes everything inside with it, which
 * for an ultimate ironman is the whole point of carrying one. The Destroy option is taken
 * off the bag entirely while the setting is on, so there is nothing to misclick; the bag
 * can still be destroyed by turning the setting off.
 */
@Singleton
public class LootingBagProtection
{
	private static final String DESTROY = "Destroy";

	private static final Set<Integer> LOOTING_BAG_IDS = Set.of(
		ItemID.LOOTING_BAG, // 11941
		ItemID.LOOTING_BAG_OPEN // 22586
	);

	private final Client client;
	private final UimUtilitiesConfig config;

	@Inject
	public LootingBagProtection(Client client, UimUtilitiesConfig config)
	{
		this.client = client;
		this.config = config;
	}

	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		// Most entries carry no item at all, so the id is the cheap way out of this
		boolean isLootingBagEntry = LOOTING_BAG_IDS.contains(event.getItemId());
		if (!isLootingBagEntry || !DESTROY.equals(event.getOption()) || !config.hideLootingBagDestroy())
		{
			return;
		}

		client.getMenu().removeMenuEntry(event.getMenuEntry());
	}
}
