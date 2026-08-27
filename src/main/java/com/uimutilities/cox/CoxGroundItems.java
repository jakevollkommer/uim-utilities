package com.uimutilities.cox;

import com.uimutilities.UimUtilitiesConfig;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
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
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.gameval.VarbitID;

/**
 * Ultimate ironmen use the floor of the Chambers of Xeric as storage. The raid destroys
 * everything left on the ground once you leave, so this tracks the player's own ground
 * items inside the raid and, while any are left, deprioritizes the exit's left-click
 * option and labels the exit. The menu entry is only pushed down, never removed: the
 * plugin hub rejects conditional menu entry removal.
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

	private final Client client;
	private final UimUtilitiesConfig config;

	// Self-owned ground items in the current raid, as a stack count per item id per tile.
	// The key is the instance template point, which survives the scene reloads between
	// rooms; instance world coordinates do not, since the chunks are remapped every load.
	private final Map<WorldPoint, Map<Integer, Integer>> itemsByTile = new HashMap<>();
	private final List<TileObject> exitObjects = new ArrayList<>();

	// A scene load reports nothing as despawned and re-reports everything the new scene
	// covers, so the counts are rebuilt from the loaded scene and the rooms that dropped
	// out of it are merged back from here.
	private final Map<WorldPoint, Map<Integer, Integer>> itemsBeforeSceneLoad = new HashMap<>();
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
		itemsBeforeSceneLoad.clear();
		exitObjects.clear();
		sceneLoadPending = false;
	}

	/** Picks up what is already lying there when the plugin is enabled mid-raid. */
	public void rebuildFromScene()
	{
		reset();
		coxSceneLoaded = isCoxSceneLoaded();
		if (isInRaidDungeon())
		{
			scanLoadedScene();
		}
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

	/** True while the player is in the raid with items of their own still on the floor. */
	public boolean hasItemsLeftBehind()
	{
		return config.coxWarnGroundItems() && !itemsByTile.isEmpty() && isInRaidDungeon();
	}

	public void onItemSpawned(ItemSpawned event)
	{
		// Everything the new scene holds is read back from it once the load finishes
		if (sceneLoadPending || !isInRaidDungeon() || event.getItem().getOwnership() != TileItem.OWNERSHIP_SELF)
		{
			return;
		}

		remember(templatePointOf(event.getTile()), event.getItem().getId());
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
			exitObjects.clear();
			itemsBeforeSceneLoad.clear();
			itemsBeforeSceneLoad.putAll(itemsByTile);
			itemsByTile.clear();
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

		if (sceneLoadPending)
		{
			sceneLoadPending = false;
			rebuildAfterSceneLoad();
		}

		boolean holdsRaidState = !itemsByTile.isEmpty() || !exitObjects.isEmpty();
		if (holdsRaidState && !isInRaidDungeon())
		{
			reset();
		}
	}

	public void onVarbitChanged(VarbitChanged event)
	{
		boolean leftTheDungeon = event.getVarbitId() == VarbitID.RAIDS_CLIENT_INDUNGEON && event.getValue() == 0;
		if (leftTheDungeon)
		{
			reset();
		}
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

	/**
	 * Reads the counts back out of the freshly loaded scene, then merges back the rooms
	 * it does not cover, which are still holding their items. Rooms it does cover are
	 * left to what the scene says, so an item picked up in a room that has since
	 * unloaded cannot linger as a phantom warning.
	 */
	private void rebuildAfterSceneLoad()
	{
		if (!isInRaidDungeon())
		{
			reset();
			return;
		}

		scanLoadedScene();

		WorldView worldView = client.getTopLevelWorldView();
		itemsBeforeSceneLoad.forEach((tile, stacks) ->
		{
			boolean outsideTheNewScene = WorldPoint.toLocalInstance(worldView, tile).isEmpty();
			if (outsideTheNewScene)
			{
				itemsByTile.putIfAbsent(tile, stacks);
			}
		});
		itemsBeforeSceneLoad.clear();
	}

	private void scanLoadedScene()
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
						rememberItemsOn(tile);
						rememberExitObjectsOn(tile);
					}
				}
			}
		}
	}

	private void rememberItemsOn(Tile tile)
	{
		List<TileItem> groundItems = tile.getGroundItems();
		if (groundItems == null)
		{
			return;
		}

		WorldPoint point = templatePointOf(tile);
		groundItems.stream()
			.filter(item -> item.getOwnership() == TileItem.OWNERSHIP_SELF)
			.forEach(item -> remember(point, item.getId()));
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
}
