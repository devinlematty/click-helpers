package com.inventoryhover;

import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

class InventoryHoverHighlightOverlay extends Overlay
{
	// Placeholder item the game puts in empty slots of some item containers
	private static final int EMPTY_SLOT_ITEM = 6512;

	private final Client client;
	private final InventoryHoverHighlightConfig config;

	@Inject
	InventoryHoverHighlightOverlay(Client client, InventoryHoverHighlightConfig config)
	{
		this.client = client;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (config.hideWhenMenuOpen() && client.isMenuOpen())
		{
			return null;
		}

		Point mouse = client.getMouseCanvasPosition();
		if (mouse == null || mouse.getX() < 0 || mouse.getY() < 0)
		{
			return null;
		}

		Rectangle slot = findHoveredSlot(InterfaceID.Inventory.ITEMS, mouse);
		if (slot == null && config.bankInventory())
		{
			slot = findHoveredSlot(InterfaceID.Bankside.ITEMS, mouse);
		}

		if (slot != null)
		{
			draw(graphics, slot);
		}
		return null;
	}

	/**
	 * Returns the clickable bounds of the inventory slot under the mouse, or null.
	 * Each dynamic child of the items container is one slot, and its bounds are
	 * the exact area the client accepts clicks in for that slot.
	 */
	private Rectangle findHoveredSlot(int containerId, Point mouse)
	{
		Widget container = client.getWidget(containerId);
		if (container == null || container.isHidden())
		{
			return null;
		}

		Widget[] slots = container.getDynamicChildren();
		if (slots == null)
		{
			return null;
		}

		for (Widget slot : slots)
		{
			if (slot == null || slot.isHidden())
			{
				continue;
			}
			if (config.onlyItems() && isEmpty(slot))
			{
				continue;
			}

			Rectangle bounds = slot.getBounds();
			if (bounds != null && bounds.contains(mouse.getX(), mouse.getY()))
			{
				return bounds;
			}
		}
		return null;
	}

	private static boolean isEmpty(Widget slot)
	{
		int id = slot.getItemId();
		return id == -1 || id == EMPTY_SLOT_ITEM || slot.getItemQuantity() <= 0;
	}

	private void draw(Graphics2D graphics, Rectangle bounds)
	{
		InventoryHoverHighlightConfig.HighlightStyle style = config.style();

		if (style != InventoryHoverHighlightConfig.HighlightStyle.OUTLINE)
		{
			graphics.setColor(config.fillColor());
			graphics.fill(bounds);
		}

		if (style != InventoryHoverHighlightConfig.HighlightStyle.FILL)
		{
			int width = config.outlineWidth();
			graphics.setColor(config.outlineColor());
			graphics.setStroke(new BasicStroke(width));
			// Inset by half the stroke so the outline stays inside the clickable area
			int inset = width / 2;
			graphics.drawRect(
				bounds.x + inset,
				bounds.y + inset,
				bounds.width - 1 - inset * 2,
				bounds.height - 1 - inset * 2);
		}
	}
}
