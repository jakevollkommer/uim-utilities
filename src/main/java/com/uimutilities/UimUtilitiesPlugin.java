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
import com.uimutilities.cox.CoxExitOverlay;
import com.uimutilities.cox.CoxGroundItems;
import com.uimutilities.lootingbag.LootingBagProtection;
import com.uimutilities.shops.SellProtection;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemDespawned;
import net.runelite.api.events.ItemSpawned;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.LinkBrowser;

@Slf4j
@PluginDescriptor(
	name = "UIM Utilities",
	description = "Quality of life and safety warnings for ultimate ironman accounts",
	tags = {"jake", "uim", "ultimate ironman", "ironman", "utilities", "warning", "items", "item", "ground",
		"drop", "dropped", "deathpile", "floor", "storage", "cox", "chambers", "xeric", "olm", "raid", "raids", "exit", "leave", "looting bag", "loot", "bag", "destroy", "protect", "shop", "sell", "store", "general store"}
)
public class UimUtilitiesPlugin extends Plugin
{
	@Inject
	private ClientThread clientThread;

	@Inject
	private OverlayManager overlayManager;

	// Each feature owns its own state and rules; the plugin only routes events to them
	@Inject
	private CoxGroundItems coxGroundItems;

	@Inject
	private CoxExitOverlay coxExitOverlay;

	@Inject
	private LootingBagProtection lootingBagProtection;

	@Inject
	private SellProtection sellProtection;

	@Provides
	UimUtilitiesConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(UimUtilitiesConfig.class);
	}

	@Override
	protected void startUp()
	{
		overlayManager.add(coxExitOverlay);
		sellProtection.rebuildFromConfig();
		// Items already lying on the floor only fire spawn events on the next scene load
		clientThread.invokeLater(coxGroundItems::rebuildFromScene);
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(coxExitOverlay);
		coxGroundItems.reset();
	}

	@Subscribe
	public void onItemSpawned(ItemSpawned event)
	{
		coxGroundItems.onItemSpawned(event);
	}

	@Subscribe
	public void onItemDespawned(ItemDespawned event)
	{
		coxGroundItems.onItemDespawned(event);
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		coxGroundItems.onGameObjectSpawned(event);
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned event)
	{
		coxGroundItems.onGameObjectDespawned(event);
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		coxGroundItems.onGameStateChanged(event);
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		coxGroundItems.onGameTick();
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		coxGroundItems.onVarbitChanged(event);
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		coxGroundItems.onMenuOptionClicked(event);
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		coxGroundItems.onMenuEntryAdded(event);
		lootingBagProtection.onMenuEntryAdded(event);
		sellProtection.onMenuEntryAdded(event);
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

		if (UimUtilitiesConfig.PROTECTED_ITEMS_KEY.equals(event.getKey()))
		{
			sellProtection.rebuildFromConfig();
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
