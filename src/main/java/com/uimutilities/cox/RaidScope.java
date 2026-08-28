package com.uimutilities.cox;

import java.util.Set;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.Tile;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.VarbitID;

/**
 * Where and when the raid counts. Object and item ids repeat across unrelated content, so every
 * decision in this package is gated on both the in-dungeon varbit and one of the raid's own regions
 * being the loaded scene.
 */
class RaidScope
{
	private static final Set<Integer> RAID_REGIONS = Set.of(
		12889, 13136, 13137, 13138, 13139, 13140, 13141, 13145,
		13393, 13394, 13395, 13396, 13397, 13401
	);

	private final Client client;

	// Refreshed once a tick rather than read live: item spawns fire all over the game, and each
	// one paying for a region scan is work for nothing
	private boolean raidSceneLoaded;

	RaidScope(Client client)
	{
		this.client = client;
	}

	void refresh()
	{
		raidSceneLoaded = scanForRaidRegion();
	}

	boolean isInRaidDungeon()
	{
		return raidSceneLoaded && client.getVarbitValue(VarbitID.RAIDS_CLIENT_INDUNGEON) == 1;
	}

	boolean isRaidSceneLoaded()
	{
		return raidSceneLoaded;
	}

	/**
	 * The instance template point of a tile, which survives the scene reloads between rooms.
	 * Instance world coordinates do not: the chunks are remapped on every load.
	 */
	WorldPoint templatePointOf(Tile tile)
	{
		return WorldPoint.fromLocalInstance(client, tile.getLocalLocation(), tile.getPlane());
	}

	/** @return tiles between the player and this tile, or {@link Integer#MAX_VALUE} with no player. */
	int distanceFromPlayer(Tile tile)
	{
		Player player = client.getLocalPlayer();
		WorldPoint playerAt = player == null ? null : player.getWorldLocation();
		return playerAt == null ? Integer.MAX_VALUE : playerAt.distanceTo(tile.getWorldLocation());
	}

	private boolean scanForRaidRegion()
	{
		WorldView worldView = client.getTopLevelWorldView();
		int[] regions = worldView == null ? null : worldView.getMapRegions();
		if (regions == null)
		{
			return false;
		}

		for (int region : regions)
		{
			if (RAID_REGIONS.contains(region))
			{
				return true;
			}
		}
		return false;
	}
}
