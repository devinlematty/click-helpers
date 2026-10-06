package com.inventoryhover;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Composite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.Prayer;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

class InventoryHoverHighlightOverlay extends Overlay
{
	// Placeholder item the game puts in empty slots of some item containers
	private static final int EMPTY_SLOT_ITEM = 6512;

	// Fraction of a tick the pulse cue stays visible
	private static final double PULSE_LENGTH = 0.25;

	private static final int MARKER_SIZE = 6;

	/** One panel (inventory, prayers, spells) and the clickable areas inside it. */
	private static class Panel
	{
		final Rectangle area;
		/** Whether the tick cue applies; only the prayer tab, where flicking happens. */
		boolean tickCue;
		/** Areas the hover highlight can fill. */
		final List<Rectangle> targets = new ArrayList<>();
		/** Every slot, including skipped ones, so the misclick guard ignores them. */
		final List<Rectangle> slots = new ArrayList<>();
		/** Slots with no item, where a click does nothing. */
		final List<Rectangle> empties = new ArrayList<>();

		Panel(Rectangle area)
		{
			this.area = area;
		}
	}

	/** A recently hovered area that is fading out. */
	private static class Trail
	{
		final Rectangle bounds;
		final boolean tickCue;
		final long leftAt;

		Trail(Rectangle bounds, boolean tickCue, long leftAt)
		{
			this.bounds = bounds;
			this.tickCue = tickCue;
			this.leftAt = leftAt;
		}
	}

	private final List<Trail> trails = new ArrayList<>();

	/** Clicks that did nothing, as {x, y, nanoTime}. */
	private final List<long[]> deadClicks = new ArrayList<>();

	private static final int DEAD_CLICK_NANOS = 500_000_000;
	private Rectangle lastTarget;
	private boolean lastTickCue;

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

		List<Panel> panels = collectPanels();

		Point mouse = client.getMouseCanvasPosition();
		boolean hoverEnabled = mouse != null && mouse.getX() >= 0 && mouse.getY() >= 0
			&& !(config.hideWhenMenuOpen() && client.isMenuOpen());

		Panel panel = null;
		Rectangle target = null;
		if (hoverEnabled)
		{
			panel = panelAt(panels, mouse);
			target = panel == null ? null : smallestAt(panel.targets, mouse);
		}

		drawDeadAreas(graphics, panels, panel);
		drawGrid(graphics, panels, panel, target);
		drawActivePrayers(graphics);
		drawSelectedSpell(graphics);
		drawAutocast(graphics, target);
		drawFlashes(graphics, now);

		double phase = tickPhase(now);
		updateTrails(target, panel != null && panel.tickCue, now);
		drawTrails(graphics, phase, now);

		if (target != null)
		{
			drawHighlight(graphics, target, phase, panel.tickCue);
		}

		drawQueuedClicks(graphics);
		drawDeadClicks(graphics, panels, now);
		return null;
	}

	// Clickable area detection

	private List<Panel> collectPanels()
	{
		List<Panel> panels = new ArrayList<>();
		addSlotPanel(panels, InterfaceID.Inventory.ITEMS);
		if (config.bankInventory())
		{
			addSlotPanel(panels, InterfaceID.Bankside.ITEMS);
		}
		if (config.prayers())
		{
			addClickablePanel(panels, InterfaceID.Prayerbook.CONTAINER);
		}
		if (config.spells())
		{
			addClickablePanel(panels, InterfaceID.MagicSpellbook.SPELLLAYER);
		}
		return panels;
	}

	/**
	 * Adds an item container panel. Each dynamic child of the container is one
	 * slot, and its bounds are the exact area the client accepts clicks in.
	 */
	private void addSlotPanel(List<Panel> panels, int containerId)
	{
		Widget container = client.getWidget(containerId);
		if (container == null || container.isHidden() || container.getBounds() == null)
		{
			return;
		}

		Widget[] slots = container.getDynamicChildren();
		if (slots == null)
		{
			return;
		}

		Point origin = container.getCanvasLocation();
		if (origin == null)
		{
			return;
		}

		Panel panel = new Panel(container.getBounds());
		for (Widget slot : slots)
		{
			if (slot == null || slot.getWidth() <= 0 || slot.getHeight() <= 0)
			{
				continue;
			}

			// Hidden (empty) slots are not rendered, so getBounds() is stale for
			// them. Build every slot from the container position plus the slot's
			// layout offset, which the game keeps current even while hidden.
			Rectangle bounds = new Rectangle(
				origin.getX() + slot.getRelativeX(),
				origin.getY() + slot.getRelativeY(),
				slot.getWidth(),
				slot.getHeight());

			panel.slots.add(bounds);
			if (slot.isSelfHidden() || isEmpty(slot))
			{
				panel.empties.add(bounds);
			}
			// The game hides the widget of an empty slot, so the hidden flag
			// only matters when empty slots are excluded
			if (!config.onlyItems() || (!slot.isSelfHidden() && !isEmpty(slot)))
			{
				panel.targets.add(bounds);
			}
		}
		panels.add(panel);
	}

	/**
	 * Adds a panel of every visible clickable widget under the root. Bounds are
	 * read live each frame, so this follows the prayer/spellbook layout when it
	 * is filtered or resized.
	 */
	private void addClickablePanel(List<Panel> panels, int rootId)
	{
		Widget root = client.getWidget(rootId);
		if (root == null || root.isHidden() || root.getBounds() == null)
		{
			return;
		}

		Set<Rectangle> found = new LinkedHashSet<>();
		collectClickable(root, found);

		Panel panel = new Panel(root.getBounds());
		panel.tickCue = rootId == InterfaceID.Prayerbook.CONTAINER;
		// A clickable widget can contain a smaller clickable child; keep only the innermost
		for (Rectangle r : found)
		{
			boolean containsOther = false;
			for (Rectangle other : found)
			{
				if (other != r && r.contains(other))
				{
					containsOther = true;
					break;
				}
			}
			if (!containsOther)
			{
				panel.targets.add(r);
			}
		}
		panel.slots.addAll(panel.targets);
		panels.add(panel);
	}

	private static void collectClickable(Widget widget, Set<Rectangle> found)
	{
		if (widget == null || widget.isSelfHidden())
		{
			return;
		}

		Rectangle bounds = widget.getBounds();
		if (bounds != null && bounds.width > 0 && bounds.height > 0 && isClickable(widget))
		{
			found.add(bounds);
		}

		collectClickable(widget.getStaticChildren(), found);
		collectClickable(widget.getDynamicChildren(), found);
		collectClickable(widget.getNestedChildren(), found);
	}

	private static void collectClickable(Widget[] widgets, Set<Rectangle> found)
	{
		if (widgets != null)
		{
			for (Widget child : widgets)
			{
				collectClickable(child, found);
			}
		}
	}

	private static Panel panelAt(List<Panel> panels, Point mouse)
	{
		for (Panel panel : panels)
		{
			if (panel.area.contains(mouse.getX(), mouse.getY()))
			{
				return panel;
			}
		}
		return null;
	}

	private static Rectangle smallestAt(List<Rectangle> rects, Point mouse)
	{
		Rectangle best = null;
		for (Rectangle r : rects)
		{
			if (r.contains(mouse.getX(), mouse.getY()) && (best == null || area(r) < area(best)))
			{
				best = r;
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

	/**
	 * Progress through the current click window, 0 when it opens and 1 when it
	 * closes. A click reaches the server about one round trip after the client
	 * sees a tick, so the window boundary is one round trip (the latency
	 * setting) before each tick the client sees, not the tick itself.
	 */
	private double tickPhase(long now)
	{
		long elapsed = now - plugin.lastTickNanos + config.tickLatency() * 1_000_000L;
		double phase = (double) elapsed / InventoryHoverHighlightPlugin.TICK_NANOS;
		// Wrap past the boundary, since the next tick arrives after the window closes
		return phase - Math.floor(phase);
	}

	private void drawHighlight(Graphics2D graphics, Rectangle bounds, double phase, boolean useTickCue)
	{
		// Every cue mode restarts at the cue position: 0 at the cue point, 1 just before the next
		double cycle = phase - config.pulsePosition() / 100.0;
		cycle -= Math.floor(cycle);

		InventoryHoverHighlightConfig.HighlightStyle style = config.style();
		InventoryHoverHighlightConfig.TickCue cue = useTickCue
			? config.tickCue()
			: InventoryHoverHighlightConfig.TickCue.OFF;

		if (style != InventoryHoverHighlightConfig.HighlightStyle.OUTLINE)
		{
			graphics.setColor(config.fillColor());
			graphics.fill(fillShape(bounds));
		}

		if (cue == InventoryHoverHighlightConfig.TickCue.PULSE)
		{
			if (cycle < PULSE_LENGTH)
			{
				Color pulse = config.pulseColor();
				double strength = 1 - cycle / PULSE_LENGTH;
				graphics.setColor(withAlpha(pulse, (int) (pulse.getAlpha() * strength)));
				graphics.fill(fillShape(bounds));
			}
		}

		// Drain and Color are drawn on the outline, so they force it on even in fill-only style
		boolean outlineCue = cue == InventoryHoverHighlightConfig.TickCue.DRAIN
			|| cue == InventoryHoverHighlightConfig.TickCue.COLOR;
		if (style == InventoryHoverHighlightConfig.HighlightStyle.FILL && !outlineCue)
		{
			return;
		}

		Color outline = config.outlineColor();
		if (cue == InventoryHoverHighlightConfig.TickCue.COLOR)
		{
			// Hue from green (1/3) at the start of the tick to red (0) at the end
			Color hue = Color.getHSBColor((float) ((1 - cycle) / 3), 1f, 1f);
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
			drawPartial(graphics, shape, 1 - cycle);
		}
		else
		{
			graphics.setColor(outline);
			graphics.draw(shape);
		}
	}

	/** Starts a fade-out for the previously hovered area when the hover moves. */
	private void updateTrails(Rectangle target, boolean tickCue, long now)
	{
		if (lastTarget != null && !lastTarget.equals(target))
		{
			Rectangle left = lastTarget;
			trails.removeIf(t -> t.bounds.equals(left));
			if (config.hoverFade() > 0)
			{
				trails.add(new Trail(left, lastTickCue, now));
			}
		}
		if (target != null)
		{
			// The live hover replaces any fade on the same area
			trails.removeIf(t -> t.bounds.equals(target));
		}
		lastTarget = target;
		lastTickCue = tickCue;
	}

	/** Draws recently hovered areas, including their tick cue, fading out. */
	private void drawTrails(Graphics2D graphics, double phase, long now)
	{
		long duration = config.hoverFade() * 1_000_000L;
		Composite original = graphics.getComposite();
		for (Iterator<Trail> it = trails.iterator(); it.hasNext(); )
		{
			Trail trail = it.next();
			long age = now - trail.leftAt;
			if (age >= duration)
			{
				it.remove();
				continue;
			}

			float strength = 1f - (float) age / duration;
			graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, strength));
			drawHighlight(graphics, trail.bounds, phase, trail.tickCue);
		}
		graphics.setComposite(original);
	}

	/** Red ring where a click landed on an empty slot or a gap with no interaction. */
	private void drawDeadClicks(Graphics2D graphics, List<Panel> panels, long now)
	{
		for (long[] click; (click = plugin.mouseClicks.poll()) != null; )
		{
			if (!config.deadClickMarker())
			{
				continue;
			}

			Point at = new Point((int) click[0], (int) click[1]);
			Panel panel = panelAt(panels, at);
			if (panel != null && (smallestAt(panel.targets, at) == null || smallestAt(panel.empties, at) != null))
			{
				deadClicks.add(click);
			}
		}

		Color color = config.misclickColor();
		graphics.setStroke(new BasicStroke(2));
		for (Iterator<long[]> it = deadClicks.iterator(); it.hasNext(); )
		{
			long[] click = it.next();
			long age = now - click[2];
			if (age >= DEAD_CLICK_NANOS)
			{
				it.remove();
				continue;
			}

			double progress = (double) age / DEAD_CLICK_NANOS;
			// Ring grows slightly while it fades
			double radius = 5 + 4 * progress;
			graphics.setColor(withAlpha(color, (int) (color.getAlpha() * (1 - progress))));
			graphics.draw(new Ellipse2D.Double(click[0] - radius, click[1] - radius, radius * 2, radius * 2));
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

	/** Shades the parts of a panel where a click does nothing. */
	private void drawDeadAreas(Graphics2D graphics, List<Panel> panels, Panel hovered)
	{
		InventoryHoverHighlightConfig.GridMode mode = config.deadAreas();
		if (mode == InventoryHoverHighlightConfig.GridMode.OFF)
		{
			return;
		}

		graphics.setColor(config.deadAreaColor());
		for (Panel panel : panels)
		{
			if (mode == InventoryHoverHighlightConfig.GridMode.IN_PANEL && panel != hovered)
			{
				continue;
			}

			Area dead = new Area(panel.area);
			for (Rectangle slot : panel.slots)
			{
				dead.subtract(new Area(slot));
			}
			graphics.fill(dead);
		}
	}

	/** Outlines every area the hover highlight can fill. */
	private void drawGrid(Graphics2D graphics, List<Panel> panels, Panel hovered, Rectangle target)
	{
		InventoryHoverHighlightConfig.GridMode mode = config.grid();
		if (mode == InventoryHoverHighlightConfig.GridMode.OFF)
		{
			return;
		}

		graphics.setColor(config.gridColor());
		graphics.setStroke(new BasicStroke(1));
		for (Panel panel : panels)
		{
			if (mode == InventoryHoverHighlightConfig.GridMode.IN_PANEL && panel != hovered)
			{
				continue;
			}
			for (Rectangle r : panel.targets)
			{
				// The hover highlight draws over the hovered area
				if (!r.equals(target))
				{
					graphics.draw(outlineShape(r, 1));
				}
			}
		}
	}

	/** Highlights active prayers using the server-confirmed state. */
	private void drawActivePrayers(Graphics2D graphics)
	{
		if (!config.activePrayers())
		{
			return;
		}

		Widget container = client.getWidget(InterfaceID.Prayerbook.CONTAINER);
		if (container == null || container.isHidden())
		{
			return;
		}

		int width = config.outlineWidth();
		for (int i = 0; i < InventoryHoverHighlightPlugin.PRAYER_COUNT; i++)
		{
			Widget widget = client.getWidget(InterfaceID.Prayerbook.PRAYER1 + i);
			if (widget == null || widget.isHidden() || widget.getBounds() == null)
			{
				continue;
			}

			Prayer prayer = plugin.prayerFor(widget);
			if (prayer == null)
			{
				continue;
			}

			boolean confirmed = plugin.isPrayerActive(prayer);
			boolean predicted = config.pendingPrayers() ? plugin.predictedActive(prayer) : confirmed;
			if (!confirmed && !predicted)
			{
				continue;
			}

			Rectangle bounds = widget.getBounds();
			Shape outline = outlineShape(bounds, width);
			if (confirmed == predicted)
			{
				graphics.setColor(config.activePrayerFill());
				graphics.fill(fillShape(bounds));
				graphics.setStroke(new BasicStroke(width));
				graphics.setColor(config.activePrayerColor());
				graphics.draw(outline);
				continue;
			}

			// Pending: the click is sent but the server has not confirmed it yet.
			// Turning on: dashed outline only. Turning off: half fill, dashed outline.
			if (confirmed)
			{
				Color fill = config.activePrayerFill();
				graphics.setColor(withAlpha(fill, fill.getAlpha() / 2));
				graphics.fill(fillShape(bounds));
			}
			graphics.setStroke(new BasicStroke(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
				10f, new float[]{3f, 3f}, 0f));
			graphics.setColor(config.activePrayerColor());
			graphics.draw(outline);
		}
	}

	/** Gold highlight on a spell selected for manual casting, like an active prayer. */
	private void drawSelectedSpell(Graphics2D graphics)
	{
		if (!config.selectedSpell() || !client.isWidgetSelected())
		{
			return;
		}

		Widget spell = client.getSelectedWidget();
		if (spell == null || spell.isHidden() || spell.getBounds() == null
			|| spell.getId() >>> 16 != InterfaceID.MagicSpellbook.SPELLLAYER >>> 16)
		{
			return;
		}

		Rectangle bounds = spell.getBounds();
		int width = config.outlineWidth();
		graphics.setColor(config.activePrayerFill());
		graphics.fill(fillShape(bounds));
		graphics.setStroke(new BasicStroke(width));
		graphics.setColor(config.activePrayerColor());
		graphics.draw(outlineShape(bounds, width));
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
