package com.uimutilities.shops;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.runelite.client.util.Text;
import net.runelite.client.util.WildcardMatcher;

/**
 * The item names the player has asked to protect, in the shape of the Ground Items lists: comma
 * separated, with * wildcards.
 *
 * A plain name also covers that item's variants, which carry the base name plus a suffix, so one
 * entry protects a trident of the seas full or empty and a barrows piece at any degradation.
 */
class ProtectedItemList
{
	// Names are matched far more often than the list changes, so exact entries answer from a set and
	// only wildcards are walked
	private final Set<String> names = new HashSet<>();
	private final List<String> patterns = new ArrayList<>();

	void replaceWith(String commaSeparatedNames)
	{
		names.clear();
		patterns.clear();

		for (String entry : Text.fromCSV(commaSeparatedNames))
		{
			if (entry.contains("*"))
			{
				patterns.add(entry);
				continue;
			}

			names.add(entry.toLowerCase());
		}
	}

	boolean covers(String itemName)
	{
		String name = itemName.toLowerCase();
		if (names.contains(name) || names.contains(withoutVariantSuffix(name)))
		{
			return true;
		}

		return patterns.stream().anyMatch(pattern -> WildcardMatcher.matches(pattern, itemName));
	}

	/**
	 * Strips what marks a name as a variant of an item: a trailing bracket for charges and ornament
	 * kits (Trident of the seas (full), Dragon dagger(p++)) and a trailing number for the degraded
	 * barrows pieces (Ahrim's robetop 75).
	 */
	private static String withoutVariantSuffix(String name)
	{
		if (name.endsWith(")"))
		{
			int bracket = name.lastIndexOf('(');
			return bracket <= 0 ? name : name.substring(0, bracket).trim();
		}

		int lastSpace = name.lastIndexOf(' ');
		boolean endsWithNumber = lastSpace > 0
			&& lastSpace < name.length() - 1
			&& name.substring(lastSpace + 1).chars().allMatch(Character::isDigit);
		return endsWithNumber ? name.substring(0, lastSpace) : name;
	}
}
