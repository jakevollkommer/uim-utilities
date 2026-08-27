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

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.LinkBrowser;

@Slf4j
@PluginDescriptor(
	name = "UIM Utilities",
	description = "Quality of life and safety warnings for ultimate ironman accounts",
	tags = {"jake", "uim,ultimate ironman,ironman,utilities,warning,items,ground,cox,raid"}
)
public class UimUtilitiesPlugin extends Plugin
{
	@Inject
	private UimUtilitiesConfig config;

	@Provides
	UimUtilitiesConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(UimUtilitiesConfig.class);
	}

	@Override
	protected void startUp()
	{
	}

	@Override
	protected void shutDown()
	{
	}

	// The config panel cannot host real buttons, so the Feedback "buttons" are checkboxes
	// that act as buttons: any click of the box, tick or untick, opens the link.
	// This method MUST be named onConfigChanged — EventBus.register throws otherwise.
	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!UimUtilitiesConfig.GROUP.equals(event.getGroup()) || event.getNewValue() == null)
		{
			return;
		}

		if (UimUtilitiesConfig.SUGGEST_BUTTON_KEY.equals(event.getKey()))
		{
			LinkBrowser.browse("https://github.com/jakevollkommer/uim-utilities/issues");
			return;
		}

		if (UimUtilitiesConfig.SUPPORT_BUTTON_KEY.equals(event.getKey()))
		{
			LinkBrowser.browse("https://ko-fi.com/jakevollkommer");
		}
	}
}
