package com.uimutilities;

import java.util.Collection;
import java.util.List;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.ItemDespawned;
import net.runelite.api.events.ItemSpawned;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.ui.overlay.Overlay;

/**
 * One quality of life feature. The plugin owns the EventBus subscriptions and hands every event to
 * every feature, so a feature never registers for itself and adding one means adding a class to the
 * plugin's list rather than touching its event plumbing.
 */
public interface Feature
{
	/** Overlays to register while the plugin runs. */
	default Collection<Overlay> overlays()
	{
		return List.of();
	}

	default void startUp()
	{
	}

	default void shutDown()
	{
	}

	default void onGameTick()
	{
	}

	default void onGameStateChanged(GameStateChanged event)
	{
	}

	default void onVarbitChanged(VarbitChanged event)
	{
	}

	default void onItemSpawned(ItemSpawned event)
	{
	}

	default void onItemDespawned(ItemDespawned event)
	{
	}

	default void onItemContainerChanged(ItemContainerChanged event)
	{
	}

	default void onGameObjectSpawned(GameObjectSpawned event)
	{
	}

	default void onGameObjectDespawned(GameObjectDespawned event)
	{
	}

	default void onMenuEntryAdded(MenuEntryAdded event)
	{
	}

	default void onConfigChanged(ConfigChanged event)
	{
	}
}
