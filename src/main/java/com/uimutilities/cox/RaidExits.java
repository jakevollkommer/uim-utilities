package com.uimutilities.cox;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.runelite.api.GameObject;
import net.runelite.api.MenuAction;
import net.runelite.api.Scene;
import net.runelite.api.Tile;
import net.runelite.api.TileObject;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.gameval.ObjectID;

/**
 * The ways out of the raid: the objects the exit label is drawn on, and the ids the deprioritize
 * matches. The exit steps out of the first room are confirmed as 49999; the rest are candidates from
 * the gameval constants and are still to be seen in a raid.
 */
class RaidExits
{
	// RAIDS_EXIT_STEPS_MULTI and RAIDS_EXIT_STEPS_RELOAD live in the package-private ObjectID1, so
	// they are written as raw ids
	private static final Set<Integer> EXIT_OBJECT_IDS = Set.of(
		49999, // RAIDS_EXIT_STEPS_MULTI, the steps out of the first room
		50000, // RAIDS_EXIT_STEPS_RELOAD
		ObjectID.RAIDS_BOSSEXIT, // 29996
		ObjectID.RAIDS_EXIT_STEPS // 29778
	);

	private final List<TileObject> exits = new ArrayList<>();

	// Objects spawn while the scene is still loading, before the in-dungeon varbit says the raid is
	// what loaded, so those spawns are missed and the scene is read back for exits instead
	private boolean scanned;

	// Every object seen in the raid, once each, so the exits that are still guesses can name
	// themselves in a dev client. Goes once the ids are confirmed.
	private final Set<String> loggedObjects = new HashSet<>();

	boolean isExit(int objectId)
	{
		return EXIT_OBJECT_IDS.contains(objectId);
	}

	List<TileObject> objects()
	{
		return exits;
	}

	/** The exit out of the Olm chamber only appears once the fight is over. */
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
		loggedObjects.clear();
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

	/** @return an object worth naming in the log, or null when it has been named already. */
	String describeOnce(MenuEntryAdded event)
	{
		String object = event.getIdentifier() + " " + event.getOption() + " " + event.getTarget();
		return loggedObjects.add(object) ? object : null;
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
