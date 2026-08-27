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

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup(UimUtilitiesConfig.GROUP)
public interface UimUtilitiesConfig extends Config
{
	String GROUP = "uimutilities";
	String SUGGEST_BUTTON_KEY = "suggestButton";
	String SUPPORT_BUTTON_KEY = "supportButton";

	@ConfigSection(
		name = "Feedback",
		description = "Suggestions, bug reports, and support",
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
