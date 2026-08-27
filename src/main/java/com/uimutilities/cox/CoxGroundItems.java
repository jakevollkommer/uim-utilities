package com.uimutilities.cox;

import com.uimutilities.UimUtilitiesConfig;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.Player;
import net.runelite.api.Tile;
import net.runelite.api.TileItem;
import net.runelite.api.TileObject;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ItemDespawned;
import net.runelite.api.events.ItemSpawned;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.gameval.VarbitID;

/**
 * Ultimate ironmen use the floor of the Chambers of Xeric as storage. The raid destroys
 * everything left on the ground once you leave, so this tracks what the player has
 * dropped inside the raid and, while any of it is left, deprioritizes the exit's
 * left-click option and labels the exit. The menu entry is only pushed down, never
 * removed: the plugin hub rejects conditional menu entry removal.
 *
 * Only items the player brought into the raid and then dropped are counted. A raid is
 * full of the player's own ground items that were never carried in, mainly loot from
 * everything killed along the way, and warning about those is noise.
 */
@Singleton
public class CoxGroundItems
{
	// The exit objects inside the raid. RAIDS_EXIT_STEPS_MULTI and RAIDS_EXIT_STEPS_RELOAD
	// live in the package-private ObjectID1, so they are written as raw ids.
	private static final Set<Integer> EXIT_OBJECT_IDS = Set.of(
		ObjectID.RAIDS_BOSSEXIT, // 29996
		ObjectID.RAIDS_EXIT_STEPS, // 29778
		49999, // RAIDS_EXIT_STEPS_MULTI
		50000 // RAIDS_EXIT_STEPS_RELOAD
	);

	// Object ids repeat across unrelated content, so every match is gated on the raid
	// being the loaded scene as well as on the in-dungeon varbit.
	private static final Set<Integer> COX_REGIONS = Set.of(
		12889, 13136, 13137, 13138, 13139, 13140, 13141, 13145,
		13393, 13394, 13395, 13396, 13397, 13401
	);

	private static final String DROP = "Drop";

	// A drop reaches the floor a tick or two after the click, and up to two tiles away
	// if the player is running as it lands
	private static final int DROP_TIMEOUT_TICKS = 3;
	private static final int DROP_RANGE = 2;

	private final Client client;
	private final UimUtilitiesConfig config;

	// What the player dropped in this raid, as a stack count per item id per tile. The key
	// is the instance template point, which survives the scene reloads between rooms;
	// instance world coordinates do not, since the chunks are remapped every load.
	private final Map<WorldPoint, Map<Integer, Integer>> itemsByTile = new HashMap<>();
	private final List<TileObject> exitObjects = new ArrayList<>();

	// Drop clicks waiting for the item to appear on the floor
	private final List<PendingDrop> pendingDrops = new ArrayList<>();

	// Item ids carried into the raid. Empty means it was never captured, in which case
	// every drop counts rather than none: a missed warning is what loses the items.
	private final Set<Integer> carriedIn = new HashSet<>();

	private boolean sceneLoadPending;

	// Whether the raid is the loaded scene, refreshed once a tick so that item spawns
	// anywhere in the game do not each pay for a region scan
	private boolean coxSceneLoaded;

	@Inject
	public CoxGroundItems(Client client, UimUtilitiesConfig config)
	{
		this.client = client;
		this.config = config;
	}

	public void reset()
	{
		itemsByTile.clear();
		exitObjects.clear();
		pendingDrops.clear();
		carriedIn.clear();
		sceneLoadPending = false;
	}

	/** Starts tracking from here when the plugin is enabled mid-raid. */
	public void rebuildFromScene()
	{
		reset();
		coxSceneLoaded = isCoxSceneLoaded();
		if (!isInRaidDungeon())
		{
			return;
		}

		// Items already on the floor cannot be told apart from raid loot, so the count
		// starts empty and follows what is dropped from here on
		captureCarriedItems();
		scanLoadedSceneForExits();
	}

	public List<TileObject> getExitObjects()
	{
		return exitObjects;
	}

	public int getGroundItemCount()
	{
		return itemsByTile.values().stream()
			.flatMap(stacks -> stacks.values().stream())
			.mapToInt(Integer::intValue)
			.sum();
	}

	/** True while the player is in the raid with items they dropped still on the floor. */
	public boolean hasItemsLeftBehind()
	{
		return config.coxWarnGroundItems() && !itemsByTile.isEmpty() && isInRaidDungeon();
	}

	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		boolean isDropOfACarriedItem = DROP.equals(event.getMenuOption())
			&& event.getItemId() > 0
			&& wasCarriedIn(event.getItemId())
			&& isInRaidDungeon();
		if (isDropOfACarriedItem)
		{
			pendingDrops.add(new PendingDrop(event.getItemId(), client.getTickCount()));
		}
	}

	public void onItemSpawned(ItemSpawned event)
	{
		// A scene load re-reports everything it covers, and none of that is a fresh drop
		if (sceneLoadPending || !isInRaidDungeon() || event.getItem().getOwnership() != TileItem.OWNERSHIP_SELF)
		{
			return;
		}

		Tile tile = event.getTile();
		if (claimPendingDrop(event.getItem().getId(), tile))
		{
			remember(templatePointOf(tile), event.getItem().getId());
		}
	}

	public void onItemDespawned(ItemDespawned event)
	{
		if (sceneLoadPending || !isInRaidDungeon() || event.getItem().getOwnership() != TileItem.OWNERSHIP_SELF)
		{
			return;
		}

		forget(templatePointOf(event.getTile()), event.getItem().getId());
	}

	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		// The exit out of the Olm chamber only appears once the fight is over
		if (coxSceneLoaded)
		{
			rememberIfExit(event.getGameObject());
		}
	}

	public void onGameObjectDespawned(GameObjectDespawned event)
	{
		exitObjects.remove(event.getGameObject());
	}

	public void onGameStateChanged(GameStateChanged event)
	{
		GameState state = event.getGameState();
		if (state == GameState.LOADING)
		{
			// Objects are not reported as despawned when the scene is rebuilt; the items
			// stay counted, since the rooms they are in still hold them
			exitObjects.clear();
			pendingDrops.clear();
			sceneLoadPending = true;
			return;
		}

		boolean leftTheGame = state == GameState.LOGIN_SCREEN || state == GameState.HOPPING;
		if (leftTheGame)
		{
			reset();
		}
	}

	public void onGameTick()
	{
		coxSceneLoaded = isCoxSceneLoaded();
		sceneLoadPending = false;
		expirePendingDrops();

		boolean holdsRaidState = !itemsByTile.isEmpty() || !exitObjects.isEmpty();
		if (holdsRaidState && !isInRaidDungeon())
		{
			reset();
		}
	}

	public void onVarbitChanged(VarbitChanged event)
	{
		if (event.getVarbitId() != VarbitID.RAIDS_CLIENT_INDUNGEON)
		{
			return;
		}

		if (event.getValue() == 0)
		{
			reset();
			return;
		}

		captureCarriedItems();
	}

	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		if (!config.coxDeprioritizeExit() || !hasItemsLeftBehind())
		{
			return;
		}

		MenuEntry entry = event.getMenuEntry();
		if (isObjectAction(entry.getType()) && EXIT_OBJECT_IDS.contains(event.getIdentifier()))
		{
			entry.setDeprioritized(true);
		}
	}

	/** Everything in the inventory and worn on entering the raid: what can be left behind. */
	private void captureCarriedItems()
	{
		carriedIn.clear();
		addContainerItems(InventoryID.INV);
		addContainerItems(InventoryID.WORN);
	}

	private void addContainerItems(int inventoryId)
	{
		ItemContainer container = client.getItemContainer(inventoryId);
		if (container == null)
		{
			return;
		}

		for (Item item : container.getItems())
		{
			if (item.getId() > 0)
			{
				carriedIn.add(item.getId());
			}
		}
	}

	private boolean wasCarriedIn(int itemId)
	{
		return carriedIn.isEmpty() || carriedIn.contains(itemId);
	}

	/** @return true when this spawn is the item a recent drop click was waiting for. */
	private boolean claimPendingDrop(int itemId, Tile tile)
	{
		Player player = client.getLocalPlayer();
		WorldPoint droppedAt = player == null ? null : player.getWorldLocation();
		if (droppedAt == null || droppedAt.distanceTo(tile.getWorldLocation()) > DROP_RANGE)
		{
			return false;
		}

		int oldestAllowed = client.getTickCount() - DROP_TIMEOUT_TICKS;
		for (int i = 0; i < pendingDrops.size(); i++)
		{
			PendingDrop drop = pendingDrops.get(i);
			if (drop.itemId == itemId && drop.tick >= oldestAllowed)
			{
				pendingDrops.remove(i);
				return true;
			}
		}
		return false;
	}

	private void expirePendingDrops()
	{
		int oldestAllowed = client.getTickCount() - DROP_TIMEOUT_TICKS;
		pendingDrops.removeIf(drop -> drop.tick < oldestAllowed);
	}

	private void scanLoadedSceneForExits()
	{
		Tile[][][] tiles = client.getTopLevelWorldView().getScene().getTiles();
		for (Tile[][] plane : tiles)
		{
			for (Tile[] column : plane)
			{
				for (Tile tile : column)
				{
					if (tile != null)
					{
						rememberExitObjectsOn(tile);
					}
				}
			}
		}
	}

	private void rememberExitObjectsOn(Tile tile)
	{
		GameObject[] objects = tile.getGameObjects();
		if (objects == null)
		{
			return;
		}

		for (GameObject object : objects)
		{
			rememberIfExit(object);
		}
	}

	private void rememberIfExit(GameObject object)
	{
		// A multi-tile object is listed on every tile it covers, and the scene scan runs
		// over objects the spawn events have already reported
		boolean isNewExit = object != null
			&& EXIT_OBJECT_IDS.contains(object.getId())
			&& !exitObjects.contains(object);
		if (isNewExit)
		{
			exitObjects.add(object);
		}
	}

	private void remember(WorldPoint tile, int itemId)
	{
		itemsByTile
			.computeIfAbsent(tile, point -> new HashMap<>())
			.merge(itemId, 1, Integer::sum);
	}

	private void forget(WorldPoint tile, int itemId)
	{
		Map<Integer, Integer> stacks = itemsByTile.get(tile);
		if (stacks == null)
		{
			return;
		}

		int remaining = stacks.getOrDefault(itemId, 0) - 1;
		if (remaining > 0)
		{
			stacks.put(itemId, remaining);
			return;
		}

		stacks.remove(itemId);
		if (stacks.isEmpty())
		{
			itemsByTile.remove(tile);
		}
	}

	private WorldPoint templatePointOf(Tile tile)
	{
		return WorldPoint.fromLocalInstance(client, tile.getLocalLocation(), tile.getPlane());
	}

	private boolean isInRaidDungeon()
	{
		return coxSceneLoaded && client.getVarbitValue(VarbitID.RAIDS_CLIENT_INDUNGEON) == 1;
	}

	private boolean isCoxSceneLoaded()
	{
		WorldView worldView = client.getTopLevelWorldView();
		int[] regions = worldView == null ? null : worldView.getMapRegions();
		if (regions == null)
		{
			return false;
		}

		for (int region : regions)
		{
			if (COX_REGIONS.contains(region))
			{
				return true;
			}
		}
		return false;
	}

	private static boolean isObjectAction(MenuAction action)
	{
		switch (action)
		{
			case GAME_OBJECT_FIRST_OPTION:
			case GAME_OBJECT_SECOND_OPTION:
			case GAME_OBJECT_THIRD_OPTION:
			case GAME_OBJECT_FOURTH_OPTION:
			case GAME_OBJECT_FIFTH_OPTION:
				return true;
			default:
				return false;
		}
	}

	private static class PendingDrop
	{
		private final int itemId;
		private final int tick;

		private PendingDrop(int itemId, int tick)
		{
			this.itemId = itemId;
			this.tick = tick;
		}
	}
}
