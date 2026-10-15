package com.inventoryhover;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Composite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.Prayer;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetType;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.util.Text;
import net.runelite.client.util.WildcardMatcher;

class InventoryHoverHighlightOverlay extends Overlay
{
	// Placeholder item the game puts in empty slots of some item containers
	private static final int EMPTY_SLOT_ITEM = 6512;

	// Fraction of a tick the pulse cue stays visible
	private static final double PULSE_LENGTH = 0.25;

	private static final int MARKER_SIZE = 6;

	private static final int MISS_DOT_SIZE = 4;

	// Game popups inside tracked panels that must stay readable: spell and prayer
	// descriptions and filter menus
	private static final int[] POPUPS = {
		InterfaceID.MagicSpellbook.TOOLTIP,
		InterfaceID.MagicSpellbook.FILTERMENU_CONTAINER,
		InterfaceID.Prayerbook.TOOLTIP,
		InterfaceID.Prayerbook.FILTERMENU_CONTAINER,
	};

	private enum PanelKind
	{
		INVENTORY,
		BANK,
		PRAYERS,
		SPELLS
	}

	/** One panel (inventory, prayers, spells) and the clickable areas inside it. */
	private static class Panel
	{
		final PanelKind kind;
		final Rectangle area;
		/** Whether the tick cue applies; only the prayer tab, where flicking happens. */
		boolean tickCue;
		/** Areas the hover highlight can fill. */
		final List<Rectangle> targets = new ArrayList<>();
		/** Every slot, including skipped ones, so dead area shading skips them. */
		final List<Rectangle> slots = new ArrayList<>();
		/** Slots with no item, where a click does nothing. */
		final List<Rectangle> empties = new ArrayList<>();
		/** Slot widgets holding an item, with their bounds. */
		final List<ItemSlot> items = new ArrayList<>();

		Panel(PanelKind kind, Rectangle area)
		{
			this.kind = kind;
			this.area = area;
		}
	}

	private static class ItemSlot
	{
		final Widget widget;
		final Rectangle bounds;

		ItemSlot(Widget widget, Rectangle bounds)
		{
			this.widget = widget;
			this.bounds = bounds;
		}
	}

	/** An area marked at a moment, like a hover fading out or a miss outline. */
	private static class Mark
	{
		final Rectangle bounds;
		final boolean tickCue;
		final long time;
		/** Panel the mark belongs to, or null when it shows in any panel. */
		final PanelKind kind;

		Mark(Rectangle bounds, boolean tickCue, long time)
		{
			this(bounds, tickCue, time, null);
		}

		Mark(Rectangle bounds, boolean tickCue, long time, PanelKind kind)
		{
			this.bounds = bounds;
			this.tickCue = tickCue;
			this.time = time;
			this.kind = kind;
		}
	}

	/** Recently hovered areas that are fading out. */
	private final List<Mark> trails = new ArrayList<>();

	/** Slots nearest a recent dead click. */
	private final List<Mark> missOutlines = new ArrayList<>();

	/** Clicks that did nothing, as {x, y, nanoTime}. */
	private final List<long[]> deadClicks = new ArrayList<>();

	/**
	 * A dead click kept as a small dot, stored relative to the panel it landed
	 * in. Dots only show while their own panel is open, so switching between the
	 * inventory, prayer and spellbook tabs keeps each tab's misses apart.
	 */
	private static class MissDot
	{
		final PanelKind kind;
		final int dx;
		final int dy;
		final long time;

		MissDot(PanelKind kind, int dx, int dy, long time)
		{
			this.kind = kind;
			this.dx = dx;
			this.dy = dy;
			this.time = time;
		}
	}

	private final List<MissDot> missDots = new ArrayList<>();

	private Rectangle lastTarget;
	private boolean lastTickCue;

	/** Area under the mouse that is waiting out the hover delay. */
	private Rectangle hoverCandidate;
	private long hoverSince;

	private final Random random = new Random();
	private Color discoColor = Color.WHITE;

	private final Client client;
	private final InventoryHoverHighlightPlugin plugin;
	private final InventoryHoverHighlightConfig config;
	private final ItemManager itemManager;

	@Inject
	InventoryHoverHighlightOverlay(Client client, InventoryHoverHighlightPlugin plugin,
		InventoryHoverHighlightConfig config, ItemManager itemManager)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		this.itemManager = itemManager;
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
		Rectangle candidate = null;
		if (hoverEnabled)
		{
			panel = panelAt(panels, mouse);
			candidate = panel == null ? null : smallestAt(panel.targets, mouse);
		}

		// Never draw over the game's own popups
		Area popups = collectPopups();
		Shape originalClip = graphics.getClip();
		if (!popups.isEmpty())
		{
			Area clip = new Area(originalClip != null ? originalClip
				: new Rectangle(0, 0, client.getCanvasWidth(), client.getCanvasHeight()));
			clip.subtract(popups);
			graphics.setClip(clip);
			if (mouse != null && popups.contains(mouse.getX(), mouse.getY()))
			{
				panel = null;
				candidate = null;
			}
		}

		Rectangle target = applyHoverDelay(candidate, now);
		if (config.discoMode() && target != null && !target.equals(lastTarget))
		{
			discoColor = Color.getHSBColor(random.nextFloat(), 0.85f, 1f);
		}

		drawDeadAreas(graphics, panels, panel);
		if (panel != null)
		{
			drawSpotlight(graphics, panel, mouse);
		}
		drawHiddenItems(graphics, panels);
		drawCooldowns(graphics, panels, now);
		drawGrid(graphics, panels, panel, target);
		drawActivePrayers(graphics);
		drawSelectedSpell(graphics);
		drawAutocast(graphics, target);
		drawFlashes(graphics, now);

		double phase = tickPhase(now);
		drawPrayerBookCue(graphics, panels, target, phase);
		updateTrails(target, panel != null && panel.tickCue, now);
		drawTrails(graphics, phase, now);

		if (target != null)
		{
			drawHighlight(graphics, target, phase, panel.tickCue, false);
		}

		drawQueuedClicks(graphics);
		processClicks(panels, popups);
		drawMissDots(graphics, panels, now);
		drawMissOutlines(graphics, panels, now);
		drawRings(graphics, deadClicks, config.misclickColor(), now);
		drawRings(graphics, plugin.rejectedClicks, config.rejectedClickColor(), now);
		graphics.setClip(originalClip);
		return null;
	}

	/** The hovered area once the mouse has rested on it for the hover delay, or null. */
	private Rectangle applyHoverDelay(Rectangle candidate, long now)
	{
		if (!Objects.equals(candidate, hoverCandidate))
		{
			hoverCandidate = candidate;
			hoverSince = now;
		}
		if (candidate == null || now - hoverSince < config.hoverDelay() * 1_000_000L)
		{
			return null;
		}
		return candidate;
	}

	/** Visible bounds of the game popups, from their drawn leaf widgets. */
	private Area collectPopups()
	{
		Area area = new Area();
		for (int id : POPUPS)
		{
			Widget widget = client.getWidget(id);
			if (widget != null && !widget.isHidden())
			{
				addLeaves(widget, area);
			}
		}
		return area;
	}

	/**
	 * Adds the bounds of visible widgets with no visible children. Popup roots are
	 * often layers the size of the whole panel, so only the drawn parts count.
	 */
	private static boolean addLeaves(Widget widget, Area area)
	{
		if (widget == null || widget.isSelfHidden())
		{
			return false;
		}

		boolean childAdded = false;
		for (Widget[] children : new Widget[][]{
			widget.getStaticChildren(), widget.getDynamicChildren(), widget.getNestedChildren()})
		{
			if (children != null)
			{
				for (Widget child : children)
				{
					childAdded |= addLeaves(child, area);
				}
			}
		}
		if (childAdded)
		{
			return true;
		}

		// An empty layer draws nothing
		Rectangle bounds = widget.getBounds();
		if (widget.getType() == WidgetType.LAYER || bounds == null || bounds.width <= 0 || bounds.height <= 0)
		{
			return false;
		}
		area.add(new Area(bounds));
		return true;
	}

	// Clickable area detection

	private List<Panel> collectPanels()
	{
		List<Panel> panels = new ArrayList<>();
		addSlotPanel(panels, PanelKind.INVENTORY, InterfaceID.Inventory.ITEMS);
		if (config.bankInventory())
		{
			addSlotPanel(panels, PanelKind.BANK, InterfaceID.Bankside.ITEMS);
		}
		if (config.prayers())
		{
			addClickablePanel(panels, PanelKind.PRAYERS, InterfaceID.Prayerbook.CONTAINER);
		}
		if (config.spells())
		{
			addClickablePanel(panels, PanelKind.SPELLS, InterfaceID.MagicSpellbook.SPELLLAYER);
		}
		return panels;
	}

	/**
	 * Adds an item container panel. Each dynamic child of the container is one
	 * slot, and its bounds are the exact area the client accepts clicks in.
	 */
	private void addSlotPanel(List<Panel> panels, PanelKind kind, int containerId)
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

		Panel panel = new Panel(kind, container.getBounds());
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
			boolean empty = slot.isSelfHidden() || isEmpty(slot);
			if (empty)
			{
				panel.empties.add(bounds);
			}
			else
			{
				panel.items.add(new ItemSlot(slot, bounds));
			}
			// The game hides the widget of an empty slot, so the hidden flag
			// only matters when empty slots are excluded
			if (!config.onlyItems() || !empty)
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
	private void addClickablePanel(List<Panel> panels, PanelKind kind, int rootId)
	{
		Widget root = client.getWidget(rootId);
		if (root == null || root.isHidden() || root.getBounds() == null)
		{
			return;
		}

		Set<Rectangle> found = new LinkedHashSet<>();
		collectClickable(root, found);

		Panel panel = new Panel(kind, root.getBounds());
		panel.tickCue = kind == PanelKind.PRAYERS;
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

	/** The target with an item nearest a point, the one a miss most likely meant, or null. */
	private static Rectangle nearestItem(Panel panel, Point at)
	{
		Rectangle best = null;
		long bestDistance = Long.MAX_VALUE;
		for (Rectangle r : panel.targets)
		{
			if (panel.empties.contains(r))
			{
				continue;
			}
			long dx = Math.max(0, Math.max(r.x - at.getX(), at.getX() - (r.x + r.width)));
			long dy = Math.max(0, Math.max(r.y - at.getY(), at.getY() - (r.y + r.height)));
			long distance = dx * dx + dy * dy;
			if (distance < bestDistance)
			{
				best = r;
				bestDistance = distance;
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

	/**
	 * Draws the hover highlight. With {@code cueOnly}, draws only the tick cue,
	 * for prayers that are not hovered.
	 */
	private void drawHighlight(Graphics2D graphics, Rectangle bounds, double phase, boolean useTickCue, boolean cueOnly)
	{
		// Every cue mode restarts at the cue position: 0 at the cue point, 1 just before the next
		double cycle = phase - config.pulsePosition() / 100.0;
		cycle -= Math.floor(cycle);

		InventoryHoverHighlightConfig.HighlightStyle style = config.style();
		InventoryHoverHighlightConfig.TickCue cue = useTickCue
			? config.tickCue()
			: InventoryHoverHighlightConfig.TickCue.OFF;

		if (!cueOnly && style != InventoryHoverHighlightConfig.HighlightStyle.OUTLINE)
		{
			graphics.setColor(hoverColor(config.fillColor()));
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
		if ((cueOnly || style == InventoryHoverHighlightConfig.HighlightStyle.FILL) && !outlineCue)
		{
			return;
		}
		// A miss outline replaces the normal outline until it fades
		if (!outlineCue && isMissOutlined(bounds))
		{
			return;
		}

		Color outline = hoverColor(config.outlineColor());
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

	/** A hover color, swapped for the disco color in disco mode with the same alpha. */
	private Color hoverColor(Color color)
	{
		return config.discoMode() ? withAlpha(discoColor, color.getAlpha()) : color;
	}

	/** Tick cue on the listed prayers that are not hovered. */
	private void drawPrayerBookCue(Graphics2D graphics, List<Panel> panels, Rectangle target, double phase)
	{
		if (!config.tickCueAllPrayers() || config.tickCue() == InventoryHoverHighlightConfig.TickCue.OFF
			|| plugin.cuePrayers.isEmpty() || panels.stream().noneMatch(p -> p.kind == PanelKind.PRAYERS))
		{
			return;
		}

		for (int i = 0; i < InventoryHoverHighlightPlugin.PRAYER_COUNT; i++)
		{
			Widget widget = client.getWidget(InterfaceID.Prayerbook.PRAYER1 + i);
			if (widget == null || widget.isHidden() || widget.getBounds() == null || widget.getName() == null)
			{
				continue;
			}

			Rectangle bounds = widget.getBounds();
			String name = Text.removeTags(widget.getName());
			if (!bounds.equals(target) && plugin.cuePrayers.stream().anyMatch(p -> WildcardMatcher.matches(p, name)))
			{
				drawHighlight(graphics, bounds, phase, true, true);
			}
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
				trails.add(new Mark(left, lastTickCue, now));
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
		for (Iterator<Mark> it = trails.iterator(); it.hasNext(); )
		{
			Mark trail = it.next();
			long age = now - trail.time;
			if (age >= duration)
			{
				it.remove();
				continue;
			}

			float strength = 1f - (float) age / duration;
			graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, strength));
			drawHighlight(graphics, trail.bounds, phase, trail.tickCue, false);
		}
		graphics.setComposite(original);
	}

	/**
	 * Sorts the mouse clicks queued by the plugin. A click on an empty slot or a
	 * gap with no interaction counts as a miss and feeds the miss markers.
	 */
	private void processClicks(List<Panel> panels, Area popups)
	{
		for (long[] click; (click = plugin.mouseClicks.poll()) != null; )
		{
			Point at = new Point((int) click[0], (int) click[1]);
			if (popups.contains(at.getX(), at.getY()))
			{
				continue;
			}

			Panel panel = panelAt(panels, at);
			if (panel == null || (smallestAt(panel.targets, at) != null && smallestAt(panel.empties, at) == null))
			{
				continue;
			}

			plugin.stats.add(ClickStats.Kind.MISSED);
			if (config.deadClickMarker())
			{
				deadClicks.add(click);
			}
			if (config.missDots())
			{
				missDots.add(new MissDot(panel.kind, at.getX() - panel.area.x, at.getY() - panel.area.y, click[2]));
			}
			if (config.missOutline())
			{
				Rectangle nearest = nearestItem(panel, at);
				if (nearest != null)
				{
					missOutlines.removeIf(m -> m.bounds.equals(nearest));
					missOutlines.add(new Mark(nearest, false, click[2], panel.kind));
				}
			}
		}
	}

	/** Small dots that stay at dead clicks, fading over the miss dot duration. */
	private void drawMissDots(Graphics2D graphics, List<Panel> panels, long now)
	{
		if (!config.missDots())
		{
			missDots.clear();
			return;
		}

		long duration = config.missDotDuration() * 1_000_000_000L;
		Color color = config.missDotColor();
		for (Iterator<MissDot> it = missDots.iterator(); it.hasNext(); )
		{
			MissDot dot = it.next();
			long age = now - dot.time;
			if (age >= duration)
			{
				it.remove();
				continue;
			}

			// Keep the dot while its panel is closed, but only draw it in its own panel
			Panel panel = panelOf(panels, dot.kind);
			if (panel == null)
			{
				continue;
			}

			double strength = 1 - (double) age / duration;
			graphics.setColor(withAlpha(color, (int) (color.getAlpha() * strength)));
			graphics.fill(new Ellipse2D.Double(panel.area.x + dot.dx - MISS_DOT_SIZE / 2.0,
				panel.area.y + dot.dy - MISS_DOT_SIZE / 2.0, MISS_DOT_SIZE, MISS_DOT_SIZE));
		}
	}

	private static Panel panelOf(List<Panel> panels, PanelKind kind)
	{
		for (Panel panel : panels)
		{
			if (panel.kind == kind)
			{
				return panel;
			}
		}
		return null;
	}

	/** Outline on the slot nearest a dead click, fading over the miss outline duration. */
	private void drawMissOutlines(Graphics2D graphics, List<Panel> panels, long now)
	{
		if (!config.missOutline())
		{
			missOutlines.clear();
			return;
		}

		long duration = config.missOutlineDuration() * 1_000_000L;
		Color color = config.missOutlineColor();
		int width = config.outlineWidth() + 1;
		graphics.setStroke(new BasicStroke(width));
		for (Iterator<Mark> it = missOutlines.iterator(); it.hasNext(); )
		{
			Mark mark = it.next();
			long age = now - mark.time;
			if (age >= duration)
			{
				it.remove();
				continue;
			}
			// Only draw the outline while the panel it belongs to is open
			if (mark.kind != null && panelOf(panels, mark.kind) == null)
			{
				continue;
			}

			double strength = 1 - (double) age / duration;
			graphics.setColor(withAlpha(color, (int) (color.getAlpha() * strength)));
			graphics.draw(outlineShape(mark.bounds, width));
		}
	}

	private boolean isMissOutlined(Rectangle bounds)
	{
		for (Mark mark : missOutlines)
		{
			if (mark.bounds.equals(bounds))
			{
				return true;
			}
		}
		return false;
	}

	/** Fading click markers for clicks given as {x, y, nanoTime}; expired ones are removed. */
	private void drawRings(Graphics2D graphics, List<long[]> clicks, Color color, long now)
	{
		long duration = config.ringDuration() * 1_000_000L;
		InventoryHoverHighlightConfig.RingAnimation animation = config.ringAnimation();
		graphics.setStroke(new BasicStroke(2));
		for (Iterator<long[]> it = clicks.iterator(); it.hasNext(); )
		{
			long[] click = it.next();
			long age = now - click[2];
			if (age >= duration)
			{
				it.remove();
				continue;
			}

			double progress = (double) age / duration;
			graphics.setColor(withAlpha(color, (int) (color.getAlpha() * (1 - progress))));

			double radius;
			switch (animation)
			{
				case SHRINK:
					radius = 10 - 6 * progress;
					break;
				case PULSE:
					radius = 6 + 2 * Math.sin(progress * Math.PI * 4);
					break;
				case CROSS:
					radius = 4 + 2 * progress;
					graphics.draw(new Line2D.Double(click[0] - radius, click[1] - radius, click[0] + radius, click[1] + radius));
					graphics.draw(new Line2D.Double(click[0] - radius, click[1] + radius, click[0] + radius, click[1] - radius));
					continue;
				default:
					radius = 5 + 4 * progress;
					break;
			}
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
			// Yellow for a click the game will ignore, like eating during the eat delay
			graphics.setColor(click.blocked ? config.rejectedClickColor()
				: click.count > 1 ? config.doubleClickColor() : config.clickMarkerColor());
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

		for (Panel panel : panels)
		{
			if (mode == InventoryHoverHighlightConfig.GridMode.IN_PANEL && panel != hovered)
			{
				continue;
			}

			Color color = deadAreaColor(panel.kind);
			if (color == null)
			{
				continue;
			}

			Area dead = new Area(panel.area);
			for (Rectangle slot : panel.slots)
			{
				dead.subtract(new Area(config.roundedAreas() ? fillShape(slot) : slot));
			}
			graphics.setColor(color);
			graphics.fill(dead);
		}
	}

	/** Dead area color of a panel, or null when its shading is off. */
	private Color deadAreaColor(PanelKind kind)
	{
		switch (kind)
		{
			case INVENTORY:
				return config.deadInventory() ? config.deadInventoryColor() : null;
			case BANK:
				return config.deadBank() ? config.deadBankColor() : null;
			case PRAYERS:
				return config.deadPrayers() ? config.deadPrayersColor() : null;
			default:
				return config.deadSpells() ? config.deadSpellsColor() : null;
		}
	}

	/** Glow at the mouse that dims the hovered panel outward. */
	private void drawSpotlight(Graphics2D graphics, Panel panel, Point mouse)
	{
		if (!config.spotlight())
		{
			return;
		}

		Color glow = config.spotlightGlow();
		Color dim = config.spotlightDim();
		Paint original = graphics.getPaint();
		graphics.setPaint(new RadialGradientPaint(
			new Point2D.Float(mouse.getX(), mouse.getY()),
			config.spotlightRadius(),
			new float[]{0f, 0.3f, 1f},
			new Color[]{glow, withAlpha(dim, 0), dim}));
		graphics.fill(panel.area);
		graphics.setPaint(original);
	}

	/** Backfill and icon outline for items the plugin made transparent. */
	private void drawHiddenItems(Graphics2D graphics, List<Panel> panels)
	{
		if (!config.hideItems() || (!config.hiddenItemShowOutline() && !config.hiddenItemShowFill()))
		{
			return;
		}

		Color fill = config.hiddenItemFill();
		Color outline = config.hiddenItemOutline();
		for (Panel panel : panels)
		{
			for (ItemSlot slot : panel.items)
			{
				if (!plugin.isFaded(slot.widget))
				{
					continue;
				}

				if (config.hiddenItemShowFill())
				{
					graphics.setColor(fill);
					graphics.fill(fillShape(slot.bounds));
				}
				if (config.hiddenItemShowOutline() && outline.getAlpha() > 0)
				{
					BufferedImage image = itemManager.getItemOutline(
						slot.widget.getItemId(), slot.widget.getItemQuantity(), outline);
					if (image != null)
					{
						graphics.drawImage(image, slot.bounds.x, slot.bounds.y, null);
					}
				}
			}
		}
	}

	/** Shades food and potions while eating or drinking another one would be ignored. */
	private void drawCooldowns(Graphics2D graphics, List<Panel> panels, long now)
	{
		if (!config.cooldowns())
		{
			return;
		}

		Color color = config.cooldownColor();
		long total = (long) InventoryHoverHighlightPlugin.CONSUME_DELAY_TICKS * InventoryHoverHighlightPlugin.TICK_NANOS;
		for (Panel panel : panels)
		{
			if (panel.kind != PanelKind.INVENTORY)
			{
				continue;
			}

			for (ItemSlot slot : panel.items)
			{
				InventoryHoverHighlightPlugin.Consumable kind = plugin.consumableFor(slot.widget.getItemId());
				int ticks = kind == null ? 0 : plugin.consumeRemaining(kind);
				if (ticks <= 0)
				{
					continue;
				}

				long remaining = ticks * (long) InventoryHoverHighlightPlugin.TICK_NANOS - (now - plugin.lastTickNanos);
				double fraction = Math.max(0, Math.min(1, (double) remaining / total));
				Rectangle b = slot.bounds;
				switch (config.cooldownStyle())
				{
					case DRAIN:
					{
						int height = (int) Math.round(b.height * fraction);
						graphics.setColor(color);
						graphics.fill(new Rectangle(b.x, b.y + b.height - height, b.width, height));
						break;
					}
					case DIM:
						graphics.setColor(withAlpha(color, (int) (color.getAlpha() * fraction)));
						graphics.fill(fillShape(b));
						break;
					default:
						graphics.setColor(color);
						graphics.fill(fillShape(b));
						break;
				}
			}
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
			for (Rectangle r : config.gridShowEmpty() ? panel.slots : panel.targets)
			{
				// The hover highlight draws over the hovered area
				if (!r.equals(target))
				{
					graphics.draw(config.roundedAreas() ? outlineShape(r, 1)
						: new Rectangle(r.x, r.y, r.width - 1, r.height - 1));
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
