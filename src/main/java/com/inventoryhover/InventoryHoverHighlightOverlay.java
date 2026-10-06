package com.inventoryhover;

import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetType;
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
		if (slot == null && config.prayers())
		{
			slot = findHoveredClickable(InterfaceID.Prayerbook.CONTAINER, mouse);
		}
		if (slot == null && config.spells())
		{
			slot = findHoveredClickable(InterfaceID.MagicSpellbook.SPELLLAYER, mouse);
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

	/**
	 * Returns the bounds of the smallest visible clickable widget under the mouse
	 * inside the given root, or null. Bounds are read live each frame, so this
	 * follows the prayer/spellbook layout when it is filtered or resized.
	 */
	private Rectangle findHoveredClickable(int rootId, Point mouse)
	{
		Widget root = client.getWidget(rootId);
		if (root == null || root.isHidden())
		{
			return null;
		}
		return findHoveredClickable(root, mouse.getX(), mouse.getY(), null);
	}

	private static Rectangle findHoveredClickable(Widget widget, int x, int y, Rectangle best)
	{
		if (widget == null || widget.isSelfHidden())
		{
			return best;
		}

		Rectangle bounds = widget.getBounds();
		if (bounds == null || !bounds.contains(x, y))
		{
			// Children of layers can sit outside the parent bounds, so still descend
			if (widget.getType() != WidgetType.LAYER)
			{
				return best;
			}
		}
		else if (isClickable(widget) && (best == null || area(bounds) < area(best)))
		{
			best = bounds;
		}

		best = findHoveredClickable(widget.getStaticChildren(), x, y, best);
		best = findHoveredClickable(widget.getDynamicChildren(), x, y, best);
		best = findHoveredClickable(widget.getNestedChildren(), x, y, best);
		return best;
	}

	private static Rectangle findHoveredClickable(Widget[] widgets, int x, int y, Rectangle best)
	{
		if (widgets != null)
		{
			for (Widget child : widgets)
			{
				best = findHoveredClickable(child, x, y, best);
			}
		}
		return best;
	}

	private static boolean isClickable(Widget widget)
	{
		String verb = widget.getTargetVerb();
		if (verb != null && !verb.isEmpty())
		{
			return true;
		}

		String[] actions = widget.getActions();
		if (actions != null)
		{
			for (String action : actions)
			{
				if (action != null && !action.isEmpty())
				{
					return true;
				}
			}
		}
		return false;
	}

	private static long area(Rectangle r)
	{
		return (long) r.width * r.height;
	}

	private static boolean isEmpty(Widget slot)
	{
		int id = slot.getItemId();
		return id == -1 || id == EMPTY_SLOT_ITEM || slot.getItemQuantity() <= 0;
	}

	private void draw(Graphics2D graphics, Rectangle bounds)
	{
		InventoryHoverHighlightConfig.HighlightStyle style = config.style();
		int arc = config.cornerRadius() * 2;
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		if (style != InventoryHoverHighlightConfig.HighlightStyle.OUTLINE)
		{
			graphics.setColor(config.fillColor());
			graphics.fill(new RoundRectangle2D.Float(bounds.x, bounds.y, bounds.width, bounds.height, arc, arc));
		}

		if (style != InventoryHoverHighlightConfig.HighlightStyle.FILL)
		{
			int width = config.outlineWidth();
			graphics.setColor(config.outlineColor());
			graphics.setStroke(new BasicStroke(width));
			// Inset by half the stroke so the outline stays inside the clickable area
			float inset = width / 2f;
			graphics.draw(new RoundRectangle2D.Float(
				bounds.x + inset,
				bounds.y + inset,
				bounds.width - width,
				bounds.height - width,
				arc, arc));
		}
	}
}
