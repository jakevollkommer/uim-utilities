package com.uimutilities.cox;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import net.runelite.api.Point;
import net.runelite.api.TileObject;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

/**
 * Labels the way out of the raid while the player still has items on the floor of it.
 */
class ExitLabelOverlay extends Overlay
{
	private static final Color WARNING_COLOR = new Color(255, 80, 80);
	private static final int LABEL_HEIGHT_ABOVE_OBJECT = 130;

	private final GroundItemWarning groundItemWarning;

	ExitLabelOverlay(GroundItemWarning groundItemWarning)
	{
		this.groundItemWarning = groundItemWarning;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!groundItemWarning.hasItemsLeftBehind())
		{
			return null;
		}

		graphics.setFont(FontManager.getRunescapeBoldFont());
		String label = labelFor(groundItemWarning.itemsLeftBehind());
		groundItemWarning.exits().forEach(exit -> drawLabel(graphics, exit, label));
		return null;
	}

	private static String labelFor(int itemCount)
	{
		String items = itemCount == 1 ? "1 item" : itemCount + " items";
		return "You left " + items + " on the ground";
	}

	private static void drawLabel(Graphics2D graphics, TileObject exit, String label)
	{
		Point location = exit.getCanvasTextLocation(graphics, label, LABEL_HEIGHT_ABOVE_OBJECT);
		if (location != null)
		{
			OverlayUtil.renderTextLocation(graphics, location, label, WARNING_COLOR);
		}
	}
}
