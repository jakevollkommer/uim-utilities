package com.uimutilities.cox;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.api.Point;
import net.runelite.api.TileObject;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

/**
 * Labels the raid exit while the player still has items on the floor of the raid.
 */
public class CoxExitOverlay extends Overlay
{
	private static final Color WARNING_COLOR = new Color(255, 80, 80);
	private static final int LABEL_Z_OFFSET = 130;

	private final CoxGroundItems coxGroundItems;

	@Inject
	public CoxExitOverlay(CoxGroundItems coxGroundItems)
	{
		this.coxGroundItems = coxGroundItems;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!coxGroundItems.hasItemsLeftBehind() || coxGroundItems.getExitObjects().isEmpty())
		{
			return null;
		}

		graphics.setFont(FontManager.getRunescapeBoldFont());
		String label = label(coxGroundItems.getGroundItemCount());
		coxGroundItems.getExitObjects().forEach(exit -> renderLabel(graphics, exit, label));
		return null;
	}

	private static String label(int itemCount)
	{
		String items = itemCount == 1 ? "1 item" : itemCount + " items";
		return "You left " + items + " on the ground";
	}

	private static void renderLabel(Graphics2D graphics, TileObject exit, String label)
	{
		Point location = exit.getCanvasTextLocation(graphics, label, LABEL_Z_OFFSET);
		if (location != null)
		{
			OverlayUtil.renderTextLocation(graphics, location, label, WARNING_COLOR);
		}
	}
}
