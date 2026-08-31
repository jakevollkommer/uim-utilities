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
import com.uimutilities.cox.GroundItemWarning;
import com.uimutilities.lootingbag.LootingBagProtection;
import com.uimutilities.shops.SellProtection;
import javax.inject.Inject;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.ItemDespawned;
import net.runelite.api.events.ItemSpawned;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.LinkBrowser;

@PluginDescriptor(
	name = "UIM Utilities",
	description = "Quality of life and safety warnings for ultimate ironman accounts",
	tags = {"jake", "uim", "ultimate ironman", "ironman", "utilities", "warning", "items", "item", "ground",
		"drop", "dropped", "deathpile", "floor", "storage", "cox", "chambers", "xeric", "olm", "raid", "raids", "exit", "leave",
		"looting bag", "loot", "bag", "destroy", "protect", "shop", "sell", "store", "general store"}
)
public class UimUtilitiesPlugin extends Plugin
{
	private static final String ISSUES_URL = "https://github.com/jakevollkommer/uim-utilities/issues";
	private static final String SUPPORT_URL = "https://ko-fi.com/jakevollkommer";

	@Inject
	private ClientThread clientThread;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private GroundItemWarning groundItemWarning;

	@Inject
	private LootingBagProtection lootingBagProtection;

	@Inject
	private SellProtection sellProtection;

	// A plain array, walked rather than streamed: menu entry events fire for every entry of every
	// menu, and an iterator or a capturing lambda per event is allocation for nothing
	private Feature[] features = new Feature[0];

	@Provides
	UimUtilitiesConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(UimUtilitiesConfig.class);
	}

	@Override
	protected void startUp()
	{
		features = new Feature[]
		{
			groundItemWarning,
			lootingBagProtection,
			sellProtection,
		};

		for (Feature feature : features)
		{
			feature.overlays().forEach(overlayManager::add);
		}

		clientThread.invokeLater(() ->
		{
			for (Feature feature : features)
			{
				feature.startUp();
			}
		});
	}

	@Override
	protected void shutDown()
	{
		for (Feature feature : features)
		{
			feature.overlays().forEach(overlayManager::remove);
			feature.shutDown();
		}
		features = new Feature[0];
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		for (Feature feature : features)
		{
			feature.onGameTick();
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		for (Feature feature : features)
		{
			feature.onGameStateChanged(event);
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		for (Feature feature : features)
		{
			feature.onVarbitChanged(event);
		}
	}

	@Subscribe
	public void onItemSpawned(ItemSpawned event)
	{
		for (Feature feature : features)
		{
			feature.onItemSpawned(event);
		}
	}

	@Subscribe
	public void onItemDespawned(ItemDespawned event)
	{
		for (Feature feature : features)
		{
			feature.onItemDespawned(event);
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		for (Feature feature : features)
		{
			feature.onItemContainerChanged(event);
		}
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		for (Feature feature : features)
		{
			feature.onGameObjectSpawned(event);
		}
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned event)
	{
		for (Feature feature : features)
		{
			feature.onGameObjectDespawned(event);
		}
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		for (Feature feature : features)
		{
			feature.onMenuEntryAdded(event);
		}
	}

	// The config panel cannot host real buttons, so the Feedback "buttons" are checkboxes that act as
	// buttons: any click of the box, tick or untick, opens the link.
	// This method MUST be named onConfigChanged — EventBus.register throws otherwise.
	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!UimUtilitiesConfig.GROUP.equals(event.getGroup()) || event.getNewValue() == null)
		{
			return;
		}

		for (Feature feature : features)
		{
			feature.onConfigChanged(event);
		}

		if (UimUtilitiesConfig.SUGGEST_BUTTON_KEY.equals(event.getKey()))
		{
			LinkBrowser.browse(ISSUES_URL);
			return;
		}

		if (UimUtilitiesConfig.SUPPORT_BUTTON_KEY.equals(event.getKey()))
		{
			LinkBrowser.browse(SUPPORT_URL);
		}
	}
}
