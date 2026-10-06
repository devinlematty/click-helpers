package com.inventoryhover;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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

	// Fraction of a tick the pulse cue stays visible
	private static final double PULSE_LENGTH = 0.3;

	private static final int MARKER_SIZE = 6;

	/** What is under the mouse: the clickable target (may be null) inside an area. */
	private static class Hover
	{
		final Rectangle target;
		final Rectangle area;

		Hover(Rectangle target, Rectangle area)
		{
			this.target = target;
			this.area = area;
		}
	}

	private final Client client;
	private final InventoryHoverHighlightPlugin plugin;
	private final InventoryHoverHighlightConfig config;

	@Inject
	InventoryHoverHighlightOverlay(Client client, InventoryHoverHighlightPlugin plugin, InventoryHoverHighlightConfig config)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		long now = System.nanoTime();

		Hover hover = findHover();
		Rectangle target = hover == null ? null : hover.target;

		drawAutocast(graphics, target);
		drawFlashes(graphics, now);

		if (hover != null)
		{
			if (target != null)
			{
				if (config.crosshair())
				{
					drawCrosshair(graphics, target, hover.area);
				}
				drawHighlight(graphics, target, tickPhase(now));
			}
			else if (config.misclickGuard())
			{
				drawMisclick(graphics);
			}
		}

		drawQueuedClicks(graphics);
		return null;
	}

	// Hover detection

	private Hover findHover()
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

		Hover hover = findHoveredSlot(InterfaceID.Inventory.ITEMS, mouse);
		if (hover == null && config.bankInventory())
		{
			hover = findHoveredSlot(InterfaceID.Bankside.ITEMS, mouse);
		}
		if (hover == null && config.prayers())
		{
			hover = findHoveredClickable(InterfaceID.Prayerbook.CONTAINER, mouse);
		}
		if (hover == null && config.spells())
		{
			hover = findHoveredClickable(InterfaceID.MagicSpellbook.SPELLLAYER, mouse);
		}
		return hover;
	}

	/**
	 * Finds the inventory slot under the mouse. Each dynamic child of the items
	 * container is one slot, and its bounds are the exact area the client
	 * accepts clicks in. Returns a Hover with a null target when the mouse is in
	 * the container but on a skipped slot or in a gap, or null when outside it.
	 */
	private Hover findHoveredSlot(int containerId, Point mouse)
	{
		Widget container = client.getWidget(containerId);
		if (container == null || container.isHidden())
		{
			return null;
		}

		Rectangle area = container.getBounds();
		if (area == null || !area.contains(mouse.getX(), mouse.getY()))
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
			if (slot == null)
			{
				continue;
			}

			Rectangle bounds = slot.getBounds();
			if (bounds == null || !bounds.contains(mouse.getX(), mouse.getY()))
			{
				continue;
			}

			// The game hides the widget of an empty slot, so the hidden flag
			// only matters when empty slots are excluded
			if (config.onlyItems() && (slot.isSelfHidden() || isEmpty(slot)))
			{
				// On a slot, just not a highlighted one: not a misclick gap
				return null;
			}
			return new Hover(bounds, area);
		}
		return new Hover(null, area);
	}

	/**
	 * Finds the smallest visible clickable widget under the mouse inside the
	 * given root. Bounds are read live each frame, so this follows the
	 * prayer/spellbook layout when it is filtered or resized.
	 */
	private Hover findHoveredClickable(int rootId, Point mouse)
	{
		Widget root = client.getWidget(rootId);
		if (root == null || root.isHidden())
		{
			return null;
		}

		Rectangle area = root.getBounds();
		if (area == null || !area.contains(mouse.getX(), mouse.getY()))
		{
			return null;
		}
		return new Hover(findHoveredClickable(root, mouse.getX(), mouse.getY(), null), area);
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

	// Drawing

	/** Progress through the current game tick, 0 at the start and 1 at the end. */
	private double tickPhase(long now)
	{
		long elapsed = now - plugin.lastTickNanos + config.tickLatency() * 1_000_000L;
		return Math.max(0, Math.min(1, (double) elapsed / InventoryHoverHighlightPlugin.TICK_NANOS));
	}

	private void drawHighlight(Graphics2D graphics, Rectangle bounds, double phase)
	{
		InventoryHoverHighlightConfig.HighlightStyle style = config.style();
		InventoryHoverHighlightConfig.TickCue cue = config.tickCue();

		if (style != InventoryHoverHighlightConfig.HighlightStyle.OUTLINE)
		{
			graphics.setColor(config.fillColor());
			graphics.fill(fillShape(bounds));
		}

		if (cue == InventoryHoverHighlightConfig.TickCue.PULSE && phase < PULSE_LENGTH)
		{
			Color outline = config.outlineColor();
			double strength = 1 - phase / PULSE_LENGTH;
			graphics.setColor(withAlpha(outline, (int) (outline.getAlpha() * 0.6 * strength)));
			graphics.fill(fillShape(bounds));
		}

		if (style == InventoryHoverHighlightConfig.HighlightStyle.FILL)
		{
			return;
		}

		Color outline = config.outlineColor();
		if (cue == InventoryHoverHighlightConfig.TickCue.COLOR)
		{
			// Hue from green (1/3) at the start of the tick to red (0) at the end
			Color hue = Color.getHSBColor((float) ((1 - phase) / 3), 1f, 1f);
			outline = withAlpha(hue, outline.getAlpha());
		}

		int width = config.outlineWidth();
		graphics.setStroke(new BasicStroke(width));
		Shape shape = outlineShape(bounds, width);

		if (cue == InventoryHoverHighlightConfig.TickCue.DRAIN)
		{
			// Faint full outline, with a bright part that shrinks as the tick runs out
			graphics.setColor(withAlpha(outline, outline.getAlpha() / 3));
			graphics.draw(shape);
			graphics.setColor(outline);
			drawPartial(graphics, shape, 1 - phase);
		}
		else
		{
			graphics.setColor(outline);
			graphics.draw(shape);
		}
	}

	private void drawFlashes(Graphics2D graphics, long now)
	{
		if (!config.clickFlash())
		{
			plugin.flashes.clear();
			return;
		}

		long duration = config.clickFlashDuration() * 1_000_000L;
		Color color = config.clickFlashColor();
		for (Iterator<InventoryHoverHighlightPlugin.Click> it = plugin.flashes.iterator(); it.hasNext(); )
		{
			InventoryHoverHighlightPlugin.Click click = it.next();
			long age = now - click.time;
			if (age >= duration)
			{
				it.remove();
				continue;
			}

			double strength = 1 - (double) age / duration;
			graphics.setColor(withAlpha(color, (int) (color.getAlpha() * strength)));
			graphics.fill(fillShape(click.bounds));
		}
	}

	private void drawQueuedClicks(Graphics2D graphics)
	{
		if (!config.clickMarker())
		{
			return;
		}

		for (InventoryHoverHighlightPlugin.Click click : plugin.queuedClicks.values())
		{
			Rectangle b = click.bounds;
			graphics.setColor(click.count > 1 ? config.doubleClickColor() : config.clickMarkerColor());
			graphics.fill(new Ellipse2D.Float(
				b.x + b.width - MARKER_SIZE - 2,
				b.y + 2,
				MARKER_SIZE,
				MARKER_SIZE));
		}
	}

	private void drawCrosshair(Graphics2D graphics, Rectangle target, Rectangle area)
	{
		graphics.setColor(config.crosshairColor());
		graphics.setStroke(new BasicStroke(1));
		int cx = (int) target.getCenterX();
		int cy = (int) target.getCenterY();
		graphics.drawLine(area.x, cy, area.x + area.width - 1, cy);
		graphics.drawLine(cx, area.y, cx, area.y + area.height - 1);
	}

	private void drawMisclick(Graphics2D graphics)
	{
		Point mouse = client.getMouseCanvasPosition();
		int r = 4;
		graphics.setColor(config.misclickColor());
		graphics.setStroke(new BasicStroke(2));
		graphics.drawLine(mouse.getX() - r, mouse.getY() - r, mouse.getX() + r, mouse.getY() + r);
		graphics.drawLine(mouse.getX() - r, mouse.getY() + r, mouse.getX() + r, mouse.getY() - r);
	}

	private void drawAutocast(Graphics2D graphics, Rectangle hovered)
	{
		if (!config.autocastMarker() || !config.spells() || plugin.autocastComponent == -1)
		{
			return;
		}

		Widget spell = client.getWidget(plugin.autocastComponent);
		if (spell == null || spell.isHidden())
		{
			return;
		}

		Rectangle bounds = spell.getBounds();
		// Hovering the spell shows the hover highlight instead
		if (bounds == null || bounds.equals(hovered))
		{
			return;
		}

		int width = config.outlineWidth();
		graphics.setColor(config.autocastColor());
		graphics.setStroke(new BasicStroke(width));
		graphics.draw(outlineShape(bounds, width));
	}

	private Shape fillShape(Rectangle b)
	{
		int arc = config.cornerRadius() * 2;
		return new RoundRectangle2D.Float(b.x, b.y, b.width, b.height, arc, arc);
	}

	private Shape outlineShape(Rectangle b, int width)
	{
		// Inset by half the stroke so the outline stays inside the clickable area
		int arc = config.cornerRadius() * 2;
		float inset = width / 2f;
		return new RoundRectangle2D.Float(b.x + inset, b.y + inset, b.width - width, b.height - width, arc, arc);
	}

	/** Draws the first {@code fraction} of the shape's outline, measured by length. */
	private static void drawPartial(Graphics2D graphics, Shape shape, double fraction)
	{
		if (fraction <= 0)
		{
			return;
		}
		if (fraction >= 1)
		{
			graphics.draw(shape);
			return;
		}

		List<Line2D> segments = new ArrayList<>();
		double total = 0;
		float[] c = new float[6];
		float startX = 0, startY = 0, x = 0, y = 0;
		for (PathIterator it = shape.getPathIterator(null, 0.5); !it.isDone(); it.next())
		{
			switch (it.currentSegment(c))
			{
				case PathIterator.SEG_MOVETO:
					startX = x = c[0];
					startY = y = c[1];
					break;
				case PathIterator.SEG_LINETO:
					segments.add(new Line2D.Float(x, y, c[0], c[1]));
					x = c[0];
					y = c[1];
					break;
				case PathIterator.SEG_CLOSE:
					segments.add(new Line2D.Float(x, y, startX, startY));
					x = startX;
					y = startY;
					break;
			}
		}
		for (Line2D s : segments)
		{
			total += s.getP1().distance(s.getP2());
		}

		double remaining = total * fraction;
		Path2D path = new Path2D.Float();
		boolean started = false;
		for (Line2D s : segments)
		{
			double length = s.getP1().distance(s.getP2());
			if (length == 0)
			{
				continue;
			}
			if (!started)
			{
				path.moveTo(s.getX1(), s.getY1());
				started = true;
			}
			if (remaining >= length)
			{
				path.lineTo(s.getX2(), s.getY2());
				remaining -= length;
			}
			else
			{
				double t = remaining / length;
				path.lineTo(s.getX1() + (s.getX2() - s.getX1()) * t, s.getY1() + (s.getY2() - s.getY1()) * t);
				break;
			}
		}
		graphics.draw(path);
	}

	private static Color withAlpha(Color c, int alpha)
	{
		return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, alpha)));
	}
}
