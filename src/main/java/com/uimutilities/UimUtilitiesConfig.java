/*
 * Copyright (c) 2026, Jake Vollkommer
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.uimutilities;

import com.uimutilities.shops.DefaultProtectedItems;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup(UimUtilitiesConfig.GROUP)
public interface UimUtilitiesConfig extends Config
{
	String GROUP = "uimutilities";
	String PROTECTED_ITEMS_KEY = "protectedItems";
	String SUGGEST_BUTTON_KEY = "suggestButton";
	String SUPPORT_BUTTON_KEY = "supportButton";

	@ConfigSection(
		name = "Chambers of Xeric",
		description = "Warnings for the Chambers of Xeric",
		position = 0
	)
	String coxSection = "coxSection";

	@ConfigItem(
		keyName = "coxWarnGroundItems",
		name = "Warn about ground items",
		description = "Warn at the raid exit while your own items are still on the floor",
		section = coxSection,
		position = 0
	)
	default boolean coxWarnGroundItems()
	{
		return true;
	}

	@ConfigItem(
		keyName = "coxDeprioritizeExit",
		name = "Deprioritize the exit",
		description = "Push the exit's left-click option down while items are left behind",
		section = coxSection,
		position = 1
	)
	default boolean coxDeprioritizeExit()
	{
		return true;
	}

	@ConfigSection(
		name = "Looting bag",
		description = "Protection for the looting bag and what is inside it",
		position = 1
	)
	String lootingBagSection = "lootingBagSection";

	@ConfigItem(
		keyName = "hideLootingBagDestroy",
		name = "Hide the Destroy option",
		description = "Take Destroy off the looting bag, which loses everything inside it",
		section = lootingBagSection,
		position = 0
	)
	default boolean hideLootingBagDestroy()
	{
		return true;
	}

	@ConfigSection(
		name = "Shops",
		description = "Protection while a shop is open",
		position = 2
	)
	String shopSection = "shopSection";

	@ConfigItem(
		keyName = "blockSelling",
		name = "Block selling protected items",
		description = "Take the Sell options off the items on the list below",
		section = shopSection,
		position = 0
	)
	default boolean blockSelling()
	{
		return true;
	}

	@ConfigItem(
		keyName = PROTECTED_ITEMS_KEY,
		name = "Protected items",
		description = "Item names, comma separated. * matches anything, and a name also covers its charged and degraded variants",
		section = shopSection,
		position = 1
	)
	default String protectedItems()
	{
		return DefaultProtectedItems.NAMES;
	}

	@ConfigSection(
		name = "Feedback",
		description = "Early release: not feature complete and warnings may be inaccurate. Bug reports and feature requests are very welcome",
		position = 99
	)
	String feedbackSection = "feedbackSection";

	@ConfigItem(
		keyName = SUGGEST_BUTTON_KEY,
		name = "Report a bug or idea",
		description = "Found a bug or have a feature request? Click the box to open the GitHub issues page",
		section = feedbackSection,
		position = 0
	)
	default boolean suggestButton()
	{
		return false;
	}

	@ConfigItem(
		keyName = SUPPORT_BUTTON_KEY,
		name = "Buy me a coffee ❤",
		description = "Enjoying the plugin? Click the box to open the Ko-fi page",
		section = feedbackSection,
		position = 1
	)
	default boolean supportButton()
	{
		return false;
	}
}
