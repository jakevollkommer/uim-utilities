package com.uimutilities.cox;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.runelite.api.GameObject;
import net.runelite.api.MenuAction;
import net.runelite.api.Scene;
import net.runelite.api.Tile;
import net.runelite.api.TileObject;
import net.runelite.api.gameval.ObjectID;

/**
 * The ways out of the raid: the objects the exit label is drawn on, and the ids the deprioritize
 * matches. Only RAIDS_EXIT_STEPS_MULTI has been seen in a raid, as the steps out of the first room;
 * the rest are candidates from the gameval constants and are still to be confirmed.
 */
class RaidExits
{
	// gameval's ObjectID1 is package-private, so the two ids it holds are named here instead
	private static final int RAIDS_EXIT_STEPS_MULTI = 49999;
	private static final int RAIDS_EXIT_STEPS_RELOAD = 50000;

	private static final Set<Integer> EXIT_OBJECT_IDS = Set.of(
		RAIDS_EXIT_STEPS_MULTI,
		RAIDS_EXIT_STEPS_RELOAD,
		ObjectID.RAIDS_BOSSEXIT,
		ObjectID.RAIDS_EXIT_STEPS
	);

	private final List<TileObject> exits = new ArrayList<>();

	// Objects spawn while the scene is still loading, before the in-dungeon varbit says the raid is
	// what loaded, so those spawns are missed and the scene is read back for exits instead
	private boolean scanned;

	boolean isExit(int objectId)
	{
		return EXIT_OBJECT_IDS.contains(objectId);
	}

	List<TileObject> objects()
	{
		return exits;
	}

	/** Catches an exit that appears mid-raid, such as the one out of the Olm chamber. */
	void objectSpawned(GameObject object)
	{
		remember(object);
	}

	void objectDespawned(GameObject object)
	{
		exits.remove(object);
	}

	void sceneUnloaded()
	{
		exits.clear();
		scanned = false;
	}

	/** @return the number of exits found, or -1 when the scene has already been read. */
	int scanOnce(Scene scene)
	{
		if (scanned)
		{
			return -1;
		}

		scanned = true;
		for (Tile[][] plane : scene.getTiles())
		{
			for (Tile[] column : plane)
			{
				for (Tile tile : column)
				{
					rememberExitsOn(tile);
				}
			}
		}
		return exits.size();
	}

	static boolean isObjectAction(MenuAction action)
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

	private void rememberExitsOn(Tile tile)
	{
		GameObject[] objects = tile == null ? null : tile.getGameObjects();
		if (objects == null)
		{
			return;
		}

		for (GameObject object : objects)
		{
			remember(object);
		}
	}

	private void remember(GameObject object)
	{
		// A multi-tile object is listed on every tile it covers, and the scan runs over objects the
		// spawn events have already reported
		boolean isNewExit = object != null && isExit(object.getId()) && !exits.contains(object);
		if (isNewExit)
		{
			exits.add(object);
		}
	}
}
