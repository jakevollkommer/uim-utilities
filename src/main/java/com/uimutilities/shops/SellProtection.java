package com.uimutilities.shops;

import com.uimutilities.UimUtilitiesConfig;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.client.util.Text;
import net.runelite.client.util.WildcardMatcher;

/**
 * A general store will happily buy a twisted bow, and the item is gone the moment the
 * shop closes. Sell options are taken off the items on the protected list, so selling
 * one means editing the list first.
 *
 * Entries are item names, optionally with * wildcards, in the style of the Ground Items
 * list. A plain name also covers that item's charged, ornamented and degraded variants,
 * which carry the base name plus a suffix (Trident of the seas (full), Ahrim's robetop 75).
 */
@Singleton
public class SellProtection
{
	private static final String SELL = "Sell";

	private final Client client;
	private final UimUtilitiesConfig config;

	// Names are matched far more often than the list changes, so the two kinds of entry
	// are split: exact names answer from a set, wildcards are the only ones walked
	private final Set<String> protectedNames = new HashSet<>();
	private final List<String> protectedPatterns = new ArrayList<>();

	@Inject
	public SellProtection(Client client, UimUtilitiesConfig config)
	{
		this.client = client;
		this.config = config;
	}

	public void rebuildFromConfig()
	{
		protectedNames.clear();
		protectedPatterns.clear();

		for (String entry : Text.fromCSV(config.protectedItems()))
		{
			if (entry.contains("*"))
			{
				protectedPatterns.add(entry);
				continue;
			}

			protectedNames.add(entry.toLowerCase());
		}
	}

	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		boolean isSellEntry = event.getItemId() > 0 && event.getOption().startsWith(SELL);
		if (!isSellEntry || !config.blockSelling())
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
		String name = itemName.toLowerCase();
		if (protectedNames.contains(name) || protectedNames.contains(withoutVariantSuffix(name)))
		{
			return true;
		}

		return protectedPatterns.stream().anyMatch(pattern -> WildcardMatcher.matches(pattern, itemName));
	}

	/**
	 * Strips the suffix that marks a variant of an item off its name, so that the one
	 * entry covers all of them: a trailing bracket for charges and ornament kits
	 * (Trident of the seas (full), Dragon dagger(p++)) and a trailing number for the
	 * degraded barrows pieces (Ahrim's robetop 75).
	 */
	private static String withoutVariantSuffix(String name)
	{
		if (name.endsWith(")"))
		{
			int bracket = name.lastIndexOf('(');
			return bracket <= 0 ? name : name.substring(0, bracket).trim();
		}

		int lastSpace = name.lastIndexOf(' ');
		boolean endsWithNumber = lastSpace > 0 && lastSpace < name.length() - 1
			&& name.substring(lastSpace + 1).chars().allMatch(Character::isDigit);
		return endsWithNumber ? name.substring(0, lastSpace) : name;
	}
}
