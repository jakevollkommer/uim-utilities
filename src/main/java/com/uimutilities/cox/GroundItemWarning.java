package com.uimutilities.cox;

import com.uimutilities.Feature;
import com.uimutilities.UimUtilitiesConfig;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.MenuEntry;
import net.runelite.api.Tile;
import net.runelite.api.TileItem;
import net.runelite.api.TileObject;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.ItemDespawned;
import net.runelite.api.events.ItemSpawned;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.ui.overlay.Overlay;

/**
 * Ultimate ironmen use the floor of the Chambers of Xeric as storage, and the raid destroys
 * everything left on it once they leave. While anything they carried in is still lying there, the
 * way out is labelled with the count and its left-click option is pushed down, so leaving takes a
 * deliberate right-click. The menu entry is never removed: the plugin hub rejects that.
 */
@Slf4j
@Singleton
public class GroundItemWarning implements Feature
{
	// The raid gate can read false for a tick as the scene changes around the entrance, and tearing
	// the count down on that blip would drop a live warning while the items are still on the floor
	private static final int TICKS_OUTSIDE_BEFORE_FORGETTING = 5;

	private final Client client;
	private final UimUtilitiesConfig config;
	private final RaidScope raidScope;
	private final CarriedItems carriedItems = new CarriedItems();
	private final InventoryLedger inventoryLedger = new InventoryLedger();
	private final DropWatcher dropWatcher = new DropWatcher();
	private final DroppedItems droppedItems = new DroppedItems();
	private final RaidExits raidExits = new RaidExits();
	private final ExitLabelOverlay overlay;

	// A scene load reports nothing as despawned and reports everything the new scene covers as
	// spawning again, so item events say nothing about drops until the load settles
	private boolean sceneLoading;
	private boolean wasInRaid;
	private int ticksOutsideTheRaid;

	@Inject
	public GroundItemWarning(Client client, UimUtilitiesConfig config)
	{
		this.client = client;
		this.config = config;
		this.raidScope = new RaidScope(client);
		this.overlay = new ExitLabelOverlay(this);
	}

	@Override
	public Collection<Overlay> overlays()
	{
		return List.of(overlay);
	}

	@Override
	public void shutDown()
	{
		forgetTheRaid();
	}

	/** True while the player is in the raid with items they dropped still on its floor. */
	public boolean hasItemsLeftBehind()
	{
		return config.coxWarnGroundItems() && !droppedItems.isEmpty() && raidScope.isInRaidDungeon();
	}

	public int itemsLeftBehind()
	{
		return droppedItems.count();
	}

	public List<TileObject> exits()
	{
		return raidExits.objects();
	}

	@Override
	public void onGameTick()
	{
		raidScope.refresh();
		sceneLoading = false;
		dropWatcher.expire(client.getTickCount());
		logRaidTransitions();

		if (raidScope.isInRaidDungeon())
		{
			ticksOutsideTheRaid = 0;
			captureWhatCanBeLeftBehind();
			findTheExits();
			return;
		}

		ticksOutsideTheRaid++;
		boolean holdsRaidState = !droppedItems.isEmpty() || !exits().isEmpty() || carriedItems.areKnown();
		if (holdsRaidState && ticksOutsideTheRaid >= TICKS_OUTSIDE_BEFORE_FORGETTING)
		{
			forgetTheRaid();
		}
	}

	@Override
	public void onGameStateChanged(GameStateChanged event)
	{
		GameState state = event.getGameState();
		if (state == GameState.LOADING)
		{
			sceneLoading = true;
			dropWatcher.clear();
			raidExits.sceneUnloaded();
			return;
		}

		boolean leftTheGame = state == GameState.LOGIN_SCREEN || state == GameState.HOPPING;
		if (leftTheGame)
		{
			forgetTheRaid();
		}
	}

	@Override
	public void onVarbitChanged(VarbitChanged event)
	{
		boolean leftTheDungeon = event.getVarbitId() == VarbitID.RAIDS_CLIENT_INDUNGEON && event.getValue() == 0;
		if (leftTheDungeon)
		{
			forgetTheRaid();
		}
	}

	@Override
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (event.getContainerId() != InventoryID.INV)
		{
			return;
		}

		List<Integer> itemsThatLeft = inventoryLedger.itemsThatLeft(event.getItemContainer());
		if (!isTracking())
		{
			return;
		}

		itemsThatLeft.forEach(this::noticeItemLeftInventory);
	}

	@Override
	public void onItemSpawned(ItemSpawned event)
	{
		if (!isTracking() || event.getItem().getOwnership() != TileItem.OWNERSHIP_SELF)
		{
			return;
		}

		Tile tile = event.getTile();
		int itemId = event.getItem().getId();
		dropWatcher
			.itemLanded(itemId, raidScope.templatePointOf(tile), raidScope.distanceFromPlayer(tile), client.getTickCount())
			.ifPresent(landedAt -> countDrop(itemId, landedAt));
	}

	@Override
	public void onItemDespawned(ItemDespawned event)
	{
		if (!isTracking() || event.getItem().getOwnership() != TileItem.OWNERSHIP_SELF)
		{
			return;
		}

		droppedItems.remove(raidScope.templatePointOf(event.getTile()), event.getItem().getId());
		logCount();
	}

	@Override
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		if (raidScope.isRaidSceneLoaded())
		{
			raidExits.objectSpawned(event.getGameObject());
		}
	}

	@Override
	public void onGameObjectDespawned(GameObjectDespawned event)
	{
		raidExits.objectDespawned(event.getGameObject());
	}

	@Override
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		MenuEntry entry = event.getMenuEntry();
		if (!RaidExits.isObjectAction(entry.getType()))
		{
			return;
		}

		logObject(event);

		boolean shouldMakeLeavingDeliberate = config.coxDeprioritizeExit()
			&& hasItemsLeftBehind()
			&& raidExits.isExit(event.getIdentifier());
		if (shouldMakeLeavingDeliberate)
		{
			entry.setDeprioritized(true);
		}
	}

	private boolean isTracking()
	{
		return !sceneLoading && raidScope.isInRaidDungeon();
	}

	private void noticeItemLeftInventory(int itemId)
	{
		if (!carriedItems.includes(itemId))
		{
			log.debug("CoX: item {} left the inventory but was not carried in", itemId);
			return;
		}

		dropWatcher.itemLeftInventory(itemId, client.getTickCount())
			.ifPresent(landedAt -> countDrop(itemId, landedAt));
	}

	private void countDrop(int itemId, WorldPoint tile)
	{
		log.debug("CoX: counted dropped item {} at {}", itemId, tile);
		droppedItems.add(tile, itemId);
		logCount();
	}

	private void captureWhatCanBeLeftBehind()
	{
		if (carriedItems.areKnown())
		{
			return;
		}

		if (!carriedItems.capture(client.getItemContainer(InventoryID.INV), client.getItemContainer(InventoryID.WORN)))
		{
			return;
		}

		// The ledger starts from the same moment, or the first drop of the raid has nothing to be
		// compared against
		inventoryLedger.reset(client.getItemContainer(InventoryID.INV));
		log.debug("CoX: carried in {} item ids", carriedItems.size());
	}

	private void findTheExits()
	{
		int found = raidExits.scanOnce(client.getTopLevelWorldView().getScene());
		boolean worthLogging = found >= 0 && log.isDebugEnabled();
		if (worthLogging)
		{
			// The ids say whether these are really ways out: an id that turns up in every room is
			// one of the guesses matching something else
			log.debug("CoX: {} exit objects in the scene {}", found, exitObjectIds());
		}
	}

	private List<Integer> exitObjectIds()
	{
		return exits().stream()
			.map(TileObject::getId)
			.collect(Collectors.toList());
	}

	private void forgetTheRaid()
	{
		droppedItems.clear();
		carriedItems.forget();
		inventoryLedger.forget();
		dropWatcher.clear();
		raidExits.sceneUnloaded();
		sceneLoading = false;
		ticksOutsideTheRaid = 0;
	}

	private void logRaidTransitions()
	{
		boolean inRaid = raidScope.isInRaidDungeon();
		if (inRaid != wasInRaid)
		{
			wasInRaid = inRaid;
			log.debug("CoX: {} the raid dungeon", inRaid ? "entered" : "left");
		}
	}

	private void logObject(MenuEntryAdded event)
	{
		if (!log.isDebugEnabled() || !raidScope.isInRaidDungeon())
		{
			return;
		}

		String object = raidExits.describeOnce(event);
		if (object != null)
		{
			log.debug("CoX: object {}", object);
		}
	}

	private void logCount()
	{
		if (log.isDebugEnabled())
		{
			log.debug("CoX: {} items left behind", droppedItems.count());
		}
	}
}
