package com.uimutilities.cox;

import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.Tile;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.VarbitID;

/**
 * Where and when the raid counts.
 *
 * Object and item ids repeat across unrelated content and must never be matched globally, so
 * everything in this package is gated on RAIDS_CLIENT_INDUNGEON. The varbit belongs to the Chambers
 * of Xeric alone, which scopes the ids more tightly than a list of the raid's regions would: the
 * raid is laid out across more regions than any hardcoded list is likely to hold, and a room in a
 * region missing from it would turn the whole feature off without a word.
 */
class RaidScope
{
	private final Client client;

	RaidScope(Client client)
	{
		this.client = client;
	}

	boolean isInRaidDungeon()
	{
		return client.getVarbitValue(VarbitID.RAIDS_CLIENT_INDUNGEON) == 1;
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
}
