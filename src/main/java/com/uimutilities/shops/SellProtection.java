package com.uimutilities.shops;

import com.uimutilities.Feature;
import com.uimutilities.UimUtilitiesConfig;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.util.Text;

/**
 * A general store will happily buy a twisted bow, and the item is gone the moment the shop closes.
 * Sell options are taken off the items the mode says a shop may not buy, so selling one of them
 * means editing a list first.
 */
@Singleton
public class SellProtection implements Feature
{
	private static final String SELL = "Sell";

	private final Client client;
	private final UimUtilitiesConfig config;
	private final ItemNameList blockList = new ItemNameList();
	private final ItemNameList allowList = new ItemNameList();

	@Inject
	public SellProtection(Client client, UimUtilitiesConfig config)
	{
		this.client = client;
		this.config = config;
	}

	@Override
	public void startUp()
	{
		rebuildLists();
	}

	@Override
	public void onConfigChanged(ConfigChanged event)
	{
		boolean listChanged = UimUtilitiesConfig.PROTECTED_ITEMS_KEY.equals(event.getKey())
			|| UimUtilitiesConfig.SELLABLE_ITEMS_KEY.equals(event.getKey());
		if (listChanged)
		{
			rebuildLists();
		}
	}

	@Override
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		// Most menu entries carry no item at all, so the id is the cheap way out of this
		boolean isSellEntry = event.getItemId() > 0 && event.getOption().startsWith(SELL);
		if (!isSellEntry)
		{
			return;
		}

		if (isProtected(Text.removeTags(event.getTarget())))
		{
			client.getMenu().removeMenuEntry(event.getMenuEntry());
		}
	}

	private boolean isProtected(String itemName)
	{
		switch (config.sellProtection())
		{
			case BLOCK_LISTED:
				return blockList.covers(itemName);
			case ALLOW_LISTED_ONLY:
				return !allowList.covers(itemName);
			default:
				return false;
		}
	}

	private void rebuildLists()
	{
		blockList.replaceWith(config.protectedItems());
		allowList.replaceWith(config.sellableItems());
	}
}
