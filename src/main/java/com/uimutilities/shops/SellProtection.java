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
 * Sell options are taken off the items on the protected list, so selling one means editing the list
 * first.
 */
@Singleton
public class SellProtection implements Feature
{
	private static final String SELL = "Sell";

	private final Client client;
	private final UimUtilitiesConfig config;
	private final ProtectedItemList protectedItems = new ProtectedItemList();

	@Inject
	public SellProtection(Client client, UimUtilitiesConfig config)
	{
		this.client = client;
		this.config = config;
	}

	@Override
	public void startUp()
	{
		protectedItems.replaceWith(config.protectedItems());
	}

	@Override
	public void onConfigChanged(ConfigChanged event)
	{
		if (UimUtilitiesConfig.PROTECTED_ITEMS_KEY.equals(event.getKey()))
		{
			protectedItems.replaceWith(config.protectedItems());
		}
	}

	@Override
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		// Most menu entries carry no item at all, so the id is the cheap way out of this
		boolean isSellEntry = event.getItemId() > 0 && event.getOption().startsWith(SELL);
		if (!isSellEntry || !config.blockSelling())
		{
			return;
		}

		if (protectedItems.covers(Text.removeTags(event.getTarget())))
		{
			client.getMenu().removeMenuEntry(event.getMenuEntry());
		}
	}
}
