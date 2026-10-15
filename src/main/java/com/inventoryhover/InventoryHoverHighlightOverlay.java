package com.inventoryhover;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Composite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
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
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.Prayer;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetType;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.FontManager;
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

	// How long a clicked practice target keeps glowing
	private static final long PRACTICE_HIT_NANOS = 400_000_000L;

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
		/** Prefix of the tab settings this panel uses. */
		final String tab;
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
		/** Other clickable areas, like filter buttons, that are neither dead nor hover targets. */
		final List<Rectangle> others = new ArrayList<>();
		/** Widget of each target, for drawing its icon again. */
		final Map<Rectangle, Widget> widgets = new HashMap<>();

		Panel(PanelKind kind, Rectangle area)
		{
			this.kind = kind;
			this.area = area;
			this.tab = kind == PanelKind.PRAYERS ? TabSettings.PRAYERS
				: kind == PanelKind.SPELLS ? TabSettings.SPELLS
				: TabSettings.INVENTORY;
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

	/** Panels of the frame being drawn. */
	private List<Panel> framePanels = new ArrayList<>();

	/** Practice targets clicked recently, glowing in the hit color. */
	private final List<Mark> practiceHits = new ArrayList<>();

	private Rectangle lastTarget;
	private boolean lastTickCue;
	private PanelKind lastKind;

	/** Area under the mouse that is waiting out the hover delay. */
	private Rectangle hoverCandidate;
	private long hoverSince;

	private final Random random = new Random();
	private Color discoColor = Color.WHITE;
	private long discoSince;

	private final Client client;
	private final InventoryHoverHighlightPlugin plugin;
	private final InventoryHoverHighlightConfig config;
	private final ItemManager itemManager;
	private final SpriteManager spriteManager;
	private final TabSettings tabs;

	@Inject
	InventoryHoverHighlightOverlay(Client client, InventoryHoverHighlightPlugin plugin,
		InventoryHoverHighlightConfig config, ItemManager itemManager, SpriteManager spriteManager, TabSettings tabs)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		this.itemManager = itemManager;
		this.spriteManager = spriteManager;
		this.tabs = tabs;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		long now = System.nanoTime();

		List<Panel> panels = collectPanels();
		framePanels = panels;

		Point mouse = client.getMouseCanvasPosition();
		boolean hoverEnabled = mouse != null && mouse.getX() >= 0 && mouse.getY() >= 0;

		Panel panel = null;
		Rectangle candidate = null;
		if (hoverEnabled)
		{
			panel = panelAt(panels, mouse);
			if (panel != null && !(tabOf(panel.kind).hideWhenMenuOpen && client.isMenuOpen()))
			{
				candidate = smallestAt(panel.targets, mouse);
			}
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

		Rectangle target = applyHoverDelay(candidate, panel == null ? 0 : tabOf(panel.kind).hoverDelay, now);
		updateDisco(target, panel, now);

		drawDeadAreas(graphics, panels, panel);
		drawSlotBackgrounds(graphics, panels);
		drawSpotlights(graphics, panels, panel, mouse);
		drawHiddenItems(graphics, panels);
		drawCooldowns(graphics, panels, now);
		drawGrid(graphics, panels, panel, target);
		drawActivePrayers(graphics);
		drawSelectedSpell(graphics);
		drawAutocast(graphics, target);
		drawFlashes(graphics, now);

		double phase = tickPhase(now);
		drawPrayerBookCue(graphics, panels, target, phase);
		updateTrails(target, panel, now);
		drawTrails(graphics, panels, phase, now);

		if (target != null)
		{
			drawHighlight(graphics, target, phase, panel.tickCue, false, panel);
		}

		drawQueuedClicks(graphics);
		updatePractice(panels, now);
		processClicks(panels, popups);
		drawPractice(graphics, panels, now);
		drawMissDots(graphics, panels, now);
		drawMissOutlines(graphics, panels, now);
		drawRings(graphics, deadClicks, config.misclickColor(), now);
		drawRings(graphics, plugin.rejectedClicks, config.rejectedClickColor(), now);
		graphics.setClip(originalClip);
		return null;
	}

	/** Picks a new disco color on a new hovered area, or when the disco interval runs out. */
	private void updateDisco(Rectangle target, Panel panel, long now)
	{
		TabSettings.Tab tab = tabOf(panel == null ? null : panel.kind);
		if (!tab.discoMode || target == null)
		{
			return;
		}

		boolean newArea = !target.equals(lastTarget);
		boolean expired = tab.discoInterval > 0 && now - discoSince >= tab.discoInterval * 1_000_000L;
		if (newArea || expired)
		{
			discoColor = Color.getHSBColor(random.nextFloat(), 0.85f, 1f);
			discoSince = now;
		}
	}

	/** The hovered area once the mouse has rested on it for the hover delay, or null. */
	private Rectangle applyHoverDelay(Rectangle candidate, int delayMillis, long now)
	{
		if (!Objects.equals(candidate, hoverCandidate))
		{
			hoverCandidate = candidate;
			hoverSince = now;
		}
		if (candidate == null || now - hoverSince < delayMillis * 1_000_000L)
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
		addSlotPanel(panels, PanelKind.INVENTORY, InterfaceID.Inventory.ITEMS, -1);
		if (config.bankInventory())
		{
			// The bank inventory's item layer hugs its slots, so its frame is what holds the gaps
			addSlotPanel(panels, PanelKind.BANK, InterfaceID.Bankside.ITEMS, InterfaceID.Bankside.ITEMS_CONTAINER);
		}
		boolean outer = config.deadOuterAreas();
		if (config.prayers())
		{
			addClickablePanel(panels, PanelKind.PRAYERS, InterfaceID.Prayerbook.CONTAINER,
				outer ? InterfaceID.Prayerbook.UNIVERSE : -1);
		}
		if (config.spells())
		{
			addClickablePanel(panels, PanelKind.SPELLS, InterfaceID.MagicSpellbook.SPELLLAYER,
				outer ? InterfaceID.MagicSpellbook.UNIVERSE : -1);
		}
		return panels;
	}

	/**
	 * Widens a panel to an outer widget, like the whole tab. The outer widget's
	 * other clickable parts, like filter buttons, are kept apart so they are
	 * neither shaded nor counted as misses.
	 */
	private static Rectangle outerArea(Rectangle area, Widget outer)
	{
		if (outer == null || outer.isHidden() || outer.getBounds() == null)
		{
			return area;
		}
		return area.union(outer.getBounds());
	}

	private static void addOthers(Panel panel, Widget outer)
	{
		if (outer == null || outer.isHidden())
		{
			return;
		}

		Map<Rectangle, Widget> found = new LinkedHashMap<>();
		collectClickable(outer, found);
		for (Rectangle r : found.keySet())
		{
			if (!panel.slots.contains(r) && !panel.targets.contains(r))
			{
				panel.others.add(r);
			}
		}
	}

	/**
	 * Adds an item container panel. Each dynamic child of the container is one
	 * slot, and its bounds are the exact area the client accepts clicks in.
	 */
	private void addSlotPanel(List<Panel> panels, PanelKind kind, int containerId, int outerId)
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

		Widget outer = outerId == -1 ? null : client.getWidget(outerId);
		Panel panel = new Panel(kind, outerArea(container.getBounds(), outer));
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
				panel.widgets.put(bounds, slot);
			}
			// The game hides the widget of an empty slot, so the hidden flag
			// only matters when empty slots are excluded
			if (!config.onlyItems() || !empty)
			{
				panel.targets.add(bounds);
			}
		}
		addOthers(panel, outer);
		panels.add(panel);
	}

	/**
	 * Adds a panel of every visible clickable widget under the root. Bounds are
	 * read live each frame, so this follows the prayer/spellbook layout when it
	 * is filtered or resized.
	 */
	private void addClickablePanel(List<Panel> panels, PanelKind kind, int rootId, int outerId)
	{
		Widget root = client.getWidget(rootId);
		if (root == null || root.isHidden() || root.getBounds() == null)
		{
			return;
		}

		Map<Rectangle, Widget> found = new LinkedHashMap<>();
		collectClickable(root, found);

		Widget outer = outerId == -1 ? null : client.getWidget(outerId);
		Panel panel = new Panel(kind, outerArea(root.getBounds(), outer));
		panel.tickCue = kind == PanelKind.PRAYERS;
		// A clickable widget can contain a smaller clickable child; keep only the innermost
		for (Map.Entry<Rectangle, Widget> entry : found.entrySet())
		{
			Rectangle r = entry.getKey();
			boolean containsOther = false;
			for (Rectangle other : found.keySet())
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
				panel.widgets.put(r, entry.getValue());
			}
		}
		panel.slots.addAll(panel.targets);
		addOthers(panel, outer);
		panels.add(panel);
	}

	private static void collectClickable(Widget widget, Map<Rectangle, Widget> found)
	{
		if (widget == null || widget.isSelfHidden())
		{
			return;
		}

		Rectangle bounds = widget.getBounds();
		if (bounds != null && bounds.width > 0 && bounds.height > 0 && isClickable(widget))
		{
			found.putIfAbsent(bounds, widget);
		}

		collectClickable(widget.getStaticChildren(), found);
		collectClickable(widget.getDynamicChildren(), found);
		collectClickable(widget.getNestedChildren(), found);
	}

	private static void collectClickable(Widget[] widgets, Map<Rectangle, Widget> found)
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
	private void drawHighlight(Graphics2D graphics, Rectangle bounds, double phase, boolean useTickCue, boolean cueOnly,
		Panel panel)
	{
		// Every cue mode restarts at the cue position: 0 at the cue point, 1 just before the next
		double cycle = phase - config.pulsePosition() / 100.0;
		cycle -= Math.floor(cycle);

		TabSettings.Tab tab = tabOf(panel == null ? null : panel.kind);
		InventoryHoverHighlightConfig.HighlightStyle style = tab.style;
		InventoryHoverHighlightConfig.TickCue cue = useTickCue
			? config.tickCue()
			: InventoryHoverHighlightConfig.TickCue.OFF;

		if (!cueOnly && style != InventoryHoverHighlightConfig.HighlightStyle.OUTLINE)
		{
			graphics.setColor(hoverColor(tab.fillColor, tab));
			graphics.fill(fillShape(bounds, tab.cornerRadius));
		}

		if (cue == InventoryHoverHighlightConfig.TickCue.PULSE)
		{
			if (cycle < PULSE_LENGTH)
			{
				Color pulse = config.pulseColor();
				double strength = 1 - cycle / PULSE_LENGTH;
				graphics.setColor(withAlpha(pulse, (int) (pulse.getAlpha() * strength)));
				graphics.fill(fillShape(bounds, tab.cornerRadius));
			}
		}

		// Keep the icon's own colors by drawing it again over the fill
		if (!cueOnly && panel != null && tab.keepIconColors)
		{
			drawIcon(graphics, panel, bounds);
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

		Color outline = hoverColor(tab.outlineColor, tab);
		if (cue == InventoryHoverHighlightConfig.TickCue.COLOR)
		{
			// Hue from green (1/3) at the start of the tick to red (0) at the end
			Color hue = Color.getHSBColor((float) ((1 - cycle) / 3), 1f, 1f);
			outline = withAlpha(hue, outline.getAlpha());
		}

		int width = tab.outlineWidth;
		graphics.setStroke(new BasicStroke(width));
		Shape shape = outlineShape(bounds, width, tab.cornerRadius);

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
	private Color hoverColor(Color color, TabSettings.Tab tab)
	{
		return tab.discoMode ? withAlpha(discoColor, color.getAlpha()) : color;
	}

	/** Tick cue on the listed prayers that are not hovered. */
	private void drawPrayerBookCue(Graphics2D graphics, List<Panel> panels, Rectangle target, double phase)
	{
		if (!config.tickCueAllPrayers() || config.tickCue() == InventoryHoverHighlightConfig.TickCue.OFF
			|| plugin.cuePrayers.isEmpty() || panels.stream().noneMatch(p -> p.kind == PanelKind.PRAYERS))
		{
			return;
		}

		Panel prayers = panelOf(panels, PanelKind.PRAYERS);
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
				drawHighlight(graphics, bounds, phase, true, true, prayers);
			}
		}
	}

	/** Starts a fade-out for the previously hovered area when the hover moves. */
	private void updateTrails(Rectangle target, Panel panel, long now)
	{
		if (lastTarget != null && !lastTarget.equals(target))
		{
			Rectangle left = lastTarget;
			trails.removeIf(t -> t.bounds.equals(left));
			if (tabOf(lastKind).hoverFade > 0)
			{
				trails.add(new Mark(left, lastTickCue, now, lastKind));
			}
		}
		if (target != null)
		{
			// The live hover replaces any fade on the same area
			trails.removeIf(t -> t.bounds.equals(target));
		}
		lastTarget = target;
		lastTickCue = panel != null && panel.tickCue;
		lastKind = panel == null ? null : panel.kind;
	}

	/** Draws recently hovered areas, including their tick cue, fading out. */
	private void drawTrails(Graphics2D graphics, List<Panel> panels, double phase, long now)
	{
		Composite original = graphics.getComposite();
		for (Iterator<Mark> it = trails.iterator(); it.hasNext(); )
		{
			Mark trail = it.next();
			long duration = tabOf(trail.kind).hoverFade * 1_000_000L;
			long age = now - trail.time;
			if (age >= duration)
			{
				it.remove();
				continue;
			}

			float strength = 1f - (float) age / duration;
			graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, strength));
			drawHighlight(graphics, trail.bounds, phase, trail.tickCue, false,
				trail.kind == null ? null : panelOf(panels, trail.kind));
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
			if (panel == null)
			{
				continue;
			}

			SwitchPractice.Result practice = practiceClick(panel, at, click[2]);
			if (practice == SwitchPractice.Result.HIT)
			{
				continue;
			}
			if (practice == SwitchPractice.Result.IGNORED)
			{
				boolean onEmpty = smallestAt(panel.empties, at) != null;
				if (!onEmpty && (smallestAt(panel.targets, at) != null || smallestAt(panel.others, at) != null))
				{
					continue;
				}
				plugin.stats.add(ClickStats.Kind.MISSED);
			}

			if (config.deadClickMarker())
			{
				deadClicks.add(click);
			}
			if (tabs.get(panel.tab).missDots)
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
		for (Iterator<MissDot> it = missDots.iterator(); it.hasNext(); )
		{
			MissDot dot = it.next();
			TabSettings.Tab tab = tabOf(dot.kind);
			long age = now - dot.time;
			if (!tab.missDots || age >= tab.missDotDuration * 1_000_000_000L)
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
			long duration = tab.missDotDuration * 1_000_000_000L;

			double strength = 1 - (double) age / duration;
			Color color = tab.missDotColor;
			graphics.setColor(withAlpha(color, (int) (color.getAlpha() * strength)));
			drawDot(graphics, tab.missDotShape, panel.area.x + dot.dx, panel.area.y + dot.dy, tab.missDotSize,
				tab.missDotHollow);
		}
	}

	private static void drawDot(Graphics2D graphics, InventoryHoverHighlightConfig.MissDotShape shape,
		double x, double y, int size, boolean hollow)
	{
		double h = size / 2.0;
		graphics.setStroke(new BasicStroke(1.5f));
		Path2D path = new Path2D.Double();
		Shape dot;
		switch (shape)
		{
			case RING:
				graphics.draw(new Ellipse2D.Double(x - h, y - h, size, size));
				return;
			case CROSS:
				graphics.draw(new Line2D.Double(x - h, y - h, x + h, y + h));
				graphics.draw(new Line2D.Double(x - h, y + h, x + h, y - h));
				return;
			case PLUS:
				graphics.draw(new Line2D.Double(x - h, y, x + h, y));
				graphics.draw(new Line2D.Double(x, y - h, x, y + h));
				return;
			case SQUARE:
				dot = new Rectangle2D.Double(x - h, y - h, size, size);
				break;
			case DIAMOND:
				path.moveTo(x, y - h);
				path.lineTo(x + h, y);
				path.lineTo(x, y + h);
				path.lineTo(x - h, y);
				path.closePath();
				dot = path;
				break;
			case TRIANGLE:
				path.moveTo(x, y - h);
				path.lineTo(x + h, y + h);
				path.lineTo(x - h, y + h);
				path.closePath();
				dot = path;
				break;
			case HEART:
				// Two lobes meeting at a dip on top, curving down to a point at the bottom
				path.moveTo(x, y + h);
				path.curveTo(x - h * 1.2, y + h * 0.2, x - h * 0.6, y - h * 1.0, x, y - h * 0.4);
				path.curveTo(x + h * 0.6, y - h * 1.0, x + h * 1.2, y + h * 0.2, x, y + h);
				path.closePath();
				dot = path;
				break;
			default:
				dot = new Ellipse2D.Double(x - h, y - h, size, size);
				break;
		}

		if (hollow)
		{
			graphics.draw(dot);
		}
		else
		{
			graphics.fill(dot);
		}
	}

	/** Tab settings of a panel kind; the general settings for none. */
	private TabSettings.Tab tabOf(PanelKind kind)
	{
		if (kind == null)
		{
			return tabs.get(TabSettings.INVENTORY);
		}
		switch (kind)
		{
			case PRAYERS:
				return tabs.get(TabSettings.PRAYERS);
			case SPELLS:
				return tabs.get(TabSettings.SPELLS);
			default:
				return tabs.get(TabSettings.INVENTORY);
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
			int width = tabOf(mark.kind).outlineWidth + 1;
			graphics.setStroke(new BasicStroke(width));
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

			graphics.setColor(color);
			graphics.fill(deadArea(panel));
		}
	}

	/** The parts of a panel where a click does nothing. */
	private Area deadArea(Panel panel)
	{
		Area dead = new Area(panel.area);
		for (Rectangle slot : panel.slots)
		{
			dead.subtract(new Area(config.roundedAreas() ? fillShape(slot) : slot));
		}
		for (Rectangle other : panel.others)
		{
			dead.subtract(new Area(other));
		}
		return dead;
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

	/** Glow at the mouse that dims the hovered panel, or every panel, outward. */
	private void drawSpotlights(Graphics2D graphics, List<Panel> panels, Panel hovered, Point mouse)
	{
		if (mouse == null || mouse.getX() < 0 || mouse.getY() < 0)
		{
			return;
		}

		Paint original = graphics.getPaint();
		for (Panel panel : panels)
		{
			TabSettings.Tab tab = tabs.get(panel.tab);
			if (!tab.spotlight || (panel != hovered && !tab.spotlightAlways))
			{
				continue;
			}

			graphics.setPaint(spotlightPaint(tab, mouse));
			graphics.fill(tab.spotlightDeadOnly ? deadArea(panel) : panel.area);
		}
		graphics.setPaint(original);
	}

	/**
	 * Glow at the mouse that turns clear at the glow size, then darkens to the
	 * dim color at the radius. With falloff, the dim holds until the falloff
	 * start, then the same gradient runs in reverse out to the falloff end,
	 * ending in the falloff color.
	 */
	private RadialGradientPaint spotlightPaint(TabSettings.Tab tab, Point mouse)
	{
		float radius = tab.spotlightRadius;
		float glowSize = tab.spotlightGlowSize / 100f;
		Point2D center = new Point2D.Float(mouse.getX(), mouse.getY());
		if (!tab.spotlightFalloff)
		{
			Color dim = tab.spotlightDim;
			return gradient(center, radius,
				new float[]{0f, glowSize, 1f},
				new Color[]{glowSize > 0 ? tab.spotlightGlow : withAlpha(dim, 0), withAlpha(dim, 0), dim});
		}

		// Past the falloff start the same gradient runs in reverse, scaled to the falloff length:
		// the dim fades to clear over the share it took to build up, then the falloff color fades
		// in over the glow's share, mirroring the glow at the mouse
		Color dim = tab.spotlightDim;
		Color falloff = tab.spotlightFalloffColor;
		float start = Math.max(radius, tab.spotlightFalloffDistance);
		float end = Math.max(start + 1, tab.spotlightFalloffEnd);
		float clear = start + (end - start) * (1 - glowSize);
		return gradient(center, end,
			new float[]{0f, glowSize * radius / end, radius / end, start / end, clear / end, clear / end, 1f},
			new Color[]{glowSize > 0 ? tab.spotlightGlow : withAlpha(dim, 0), withAlpha(dim, 0), dim, dim,
				withAlpha(dim, 0), withAlpha(falloff, 0), falloff});
	}

	/** A radial gradient whose stops are nudged apart, since they must keep increasing. */
	private static RadialGradientPaint gradient(Point2D center, float radius, float[] stops, Color[] colors)
	{
		float[] fixed = stops.clone();
		for (int i = 1; i < fixed.length; i++)
		{
			fixed[i] = Math.max(fixed[i], fixed[i - 1] + 0.001f);
		}
		// Squeeze back under 1 from the end if nudging pushed past it
		fixed[fixed.length - 1] = 1f;
		for (int i = fixed.length - 2; i >= 0; i--)
		{
			fixed[i] = Math.min(fixed[i], fixed[i + 1] - 0.001f);
		}
		fixed[0] = 0f;
		return new RadialGradientPaint(center, radius, fixed, colors);
	}

	/** Colors the background of every slot with the tab's slot background, icons kept on top. */
	private void drawSlotBackgrounds(Graphics2D graphics, List<Panel> panels)
	{
		for (Panel panel : panels)
		{
			Color color = tabs.get(panel.tab).slotBackground;
			if (color == null)
			{
				continue;
			}

			graphics.setColor(color);
			for (Rectangle slot : panel.slots)
			{
				graphics.fill(fillShape(slot));
			}
			for (Rectangle slot : panel.slots)
			{
				drawIcon(graphics, panel, slot);
			}
		}
	}

	/** Draws the icon of the item, prayer or spell in an area again, over anything drawn on it. */
	private void drawIcon(Graphics2D graphics, Panel panel, Rectangle bounds)
	{
		Widget widget = panel.widgets.get(bounds);
		if (widget == null)
		{
			return;
		}

		if (panel.kind == PanelKind.INVENTORY || panel.kind == PanelKind.BANK)
		{
			int id = widget.getItemId();
			int quantity = widget.getItemQuantity();
			if (plugin.isFaded(widget) || id <= 0 || id == EMPTY_SLOT_ITEM || quantity <= 0)
			{
				return;
			}
			boolean stackable = quantity > 1 || itemManager.getItemComposition(id).isStackable();
			BufferedImage image = itemManager.getImage(id, quantity, stackable);
			if (image != null)
			{
				graphics.drawImage(image, bounds.x, bounds.y, null);
			}
			return;
		}
		drawSprites(graphics, widget);
	}

	/** Draws the sprites of a widget and its visible children at their places. */
	private void drawSprites(Graphics2D graphics, Widget widget)
	{
		if (widget == null || widget.isSelfHidden())
		{
			return;
		}

		Rectangle bounds = widget.getBounds();
		if (widget.getSpriteId() >= 0 && bounds != null && bounds.width > 0 && bounds.height > 0)
		{
			BufferedImage sprite = spriteManager.getSprite(widget.getSpriteId(), 0);
			if (sprite != null)
			{
				graphics.drawImage(sprite, bounds.x, bounds.y, bounds.width, bounds.height, null);
			}
		}

		for (Widget[] children : new Widget[][]{
			widget.getStaticChildren(), widget.getDynamicChildren(), widget.getNestedChildren()})
		{
			if (children != null)
			{
				for (Widget child : children)
				{
					drawSprites(graphics, child);
				}
			}
		}
	}

	// Switch practice

	/** Starts practice rounds on the inventory's empty slots. */
	private void updatePractice(List<Panel> panels, long now)
	{
		SwitchPractice practice = plugin.practice;
		Panel inventory = panelOf(panels, PanelKind.INVENTORY);
		if (!practice.isActive() || inventory == null)
		{
			return;
		}

		List<Integer> empty = new ArrayList<>();
		for (int i = 0; i < inventory.slots.size(); i++)
		{
			if (inventory.empties.contains(inventory.slots.get(i)))
			{
				empty.add(i);
			}
		}
		practice.roundDelay = config.practiceRoundDelay() * 1_000_000L;
		practice.extraDelay = config.practiceRandomDelay() ? practice.roundDelay : 0;
		practice.timeLimit = (long) (config.practiceTimeLimitSeconds() * 1_000_000_000L);
		practice.ordered = config.practiceOrdered();
		practice.roundsPerSession = config.practiceRounds();

		int rounds = practice.roundCount();
		practice.update(empty, config.practiceTargets(), now, client.getTickCount());
		if (practice.roundCount() != rounds)
		{
			plugin.practiceRoundDone();
		}
		if (practice.isComplete())
		{
			plugin.practiceSessionDone();
		}
	}

	/** Scores a click for switch practice; IGNORED when it is not a practice click. */
	private SwitchPractice.Result practiceClick(Panel panel, Point at, long time)
	{
		SwitchPractice practice = plugin.practice;
		if (panel.kind != PanelKind.INVENTORY || !practice.isActive())
		{
			return SwitchPractice.Result.IGNORED;
		}

		int slot = -1;
		for (int i = 0; i < panel.slots.size(); i++)
		{
			if (panel.slots.get(i).contains(at.getX(), at.getY()))
			{
				slot = i;
				break;
			}
		}

		int rounds = practice.roundCount();
		SwitchPractice.Result result = practice.click(slot, time, client.getTickCount());
		if (result == SwitchPractice.Result.HIT)
		{
			practiceHits.add(new Mark(panel.slots.get(slot), false, time, PanelKind.INVENTORY));
		}
		if (practice.roundCount() != rounds)
		{
			plugin.practiceRoundDone();
			if (practice.isComplete())
			{
				plugin.practiceSessionDone();
			}
		}
		return result;
	}

	/** Practice targets, glowing hits and the status line above the inventory. */
	private void drawPractice(Graphics2D graphics, List<Panel> panels, long now)
	{
		SwitchPractice practice = plugin.practice;
		Panel inventory = panelOf(panels, PanelKind.INVENTORY);
		if (!practice.isActive())
		{
			practiceHits.clear();
			return;
		}
		if (inventory == null)
		{
			return;
		}

		int width = tabs.get(TabSettings.INVENTORY).outlineWidth;
		boolean numbers = config.practiceNumbers() || config.practiceOrdered();
		int next = practice.nextTarget();
		int number = 0;
		graphics.setFont(FontManager.getRunescapeBoldFont());
		for (Map.Entry<Integer, Boolean> target : practice.targets().entrySet())
		{
			number++;
			if (target.getKey() >= inventory.slots.size())
			{
				continue;
			}
			Rectangle bounds = inventory.slots.get(target.getKey());
			Color color = target.getValue() ? config.practiceHitColor() : config.practiceTargetColor();
			graphics.setColor(color);
			graphics.fill(fillShape(bounds));
			// In order mode the next target gets a thicker outline
			int stroke = config.practiceOrdered() && target.getKey() == next ? width + 1 : width;
			graphics.setStroke(new BasicStroke(stroke));
			graphics.setColor(withAlpha(color, 255));
			graphics.draw(outlineShape(bounds, stroke));

			if (numbers)
			{
				String label = String.valueOf(number);
				int textWidth = graphics.getFontMetrics().stringWidth(label);
				int x = (int) bounds.getCenterX() - textWidth / 2;
				int y = (int) bounds.getCenterY() + graphics.getFontMetrics().getAscent() / 2 - 1;
				graphics.setColor(Color.BLACK);
				graphics.drawString(label, x + 1, y + 1);
				graphics.setColor(Color.WHITE);
				graphics.drawString(label, x, y);
			}
		}

		if (config.practiceTimerBar() && practice.timeLimit > 0 && !practice.targets().isEmpty())
		{
			double left = practice.timeLeft(now);
			Rectangle area = inventory.area;
			graphics.setColor(new Color(0, 0, 0, 120));
			graphics.fillRect(area.x, area.y, area.width, 4);
			// Green when the round starts, red as time runs out
			graphics.setColor(Color.getHSBColor((float) (left / 3), 0.9f, 1f));
			graphics.fillRect(area.x, area.y, (int) Math.round(area.width * left), 4);
		}

		Color hit = config.practiceHitColor();
		for (Iterator<Mark> it = practiceHits.iterator(); it.hasNext(); )
		{
			Mark mark = it.next();
			long age = now - mark.time;
			if (age >= PRACTICE_HIT_NANOS)
			{
				it.remove();
				continue;
			}
			double strength = 1 - (double) age / PRACTICE_HIT_NANOS;
			graphics.setColor(withAlpha(hit, (int) (hit.getAlpha() * strength)));
			graphics.fill(fillShape(mark.bounds));
		}

		InventoryHoverHighlightConfig.PracticeStats mode = config.practiceStatsMode();
		if (mode != InventoryHoverHighlightConfig.PracticeStats.OFF)
		{
			drawPracticeStatus(graphics, inventory, practice, mode);
		}
	}

	/** Stats lines stacked upward from the top of the inventory. */
	private static void drawPracticeStatus(Graphics2D graphics, Panel inventory, SwitchPractice practice,
		InventoryHoverHighlightConfig.PracticeStats mode)
	{
		List<String> lines = new ArrayList<>();
		boolean showLast = mode != InventoryHoverHighlightConfig.PracticeStats.SESSION;
		boolean showSession = mode != InventoryHoverHighlightConfig.PracticeStats.LAST_ROUND;
		SwitchPractice.Summary session = practice.summary();

		if (inventory.empties.isEmpty())
		{
			lines.add("Practice: empty some inventory slots");
		}
		else if (practice.lastRound == null)
		{
			lines.add("Practice: click the lit slots");
		}
		else
		{
			if (showSession && session != null)
			{
				String rounds = practice.roundsPerSession > 0
					? session.rounds + "/" + practice.roundsPerSession
					: String.valueOf(session.rounds);
				lines.add(String.format("Rounds %s, 1-tick %.0f%%, accuracy %.0f%%, timeouts %d",
					rounds, session.oneTickRate, session.accuracy, session.timeouts));
				lines.add(String.format("Avg %.0f ms (best %d), %.1f ticks, react %.0f ms",
					session.switchMillis, session.bestSwitchMillis, session.ticks, session.reactMillis));
			}
			if (showLast)
			{
				SwitchPractice.Round last = practice.lastRound;
				lines.add(last.timedOut
					? String.format("Last: timed out, %d hit, %d miss%s", last.hits, last.misses,
						last.misses == 1 ? "" : "es")
					: String.format("Last: %d ms, %d tick%s, %d miss%s", last.switchMillis, last.ticks,
						last.ticks == 1 ? "" : "s", last.misses, last.misses == 1 ? "" : "es"));
			}
		}

		Font font = FontManager.getRunescapeSmallFont();
		graphics.setFont(font);
		int lineHeight = graphics.getFontMetrics().getHeight();
		int x = inventory.area.x + 2;
		int y = inventory.area.y - 3 - lineHeight * (lines.size() - 1);
		for (String line : lines)
		{
			graphics.setColor(Color.BLACK);
			graphics.drawString(line, x + 1, y + 1);
			graphics.setColor(Color.WHITE);
			graphics.drawString(line, x, y);
			y += lineHeight;
		}
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

				long total = (long) plugin.consumeLength(kind) * InventoryHoverHighlightPlugin.TICK_NANOS;
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

		int width = tabs.get(TabSettings.PRAYERS).outlineWidth;
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
		int width = tabs.get(TabSettings.SPELLS).outlineWidth;
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

		int width = tabs.get(TabSettings.SPELLS).outlineWidth;
		graphics.setColor(config.autocastColor());
		graphics.setStroke(new BasicStroke(width));
		graphics.draw(outlineShape(bounds, width));
	}

	private Shape fillShape(Rectangle b)
	{
		return fillShape(b, shapeTab(b).cornerRadius);
	}

	/** Settings of the tab whose panel holds an area, for its corner radius; the inventory's when none. */
	private TabSettings.Tab shapeTab(Rectangle b)
	{
		for (Panel panel : framePanels)
		{
			if (panel.area.contains(b.getCenterX(), b.getCenterY()))
			{
				return tabs.get(panel.tab);
			}
		}
		return tabs.get(TabSettings.INVENTORY);
	}

	private static Shape fillShape(Rectangle b, int radius)
	{
		int arc = radius * 2;
		return new RoundRectangle2D.Float(b.x, b.y, b.width, b.height, arc, arc);
	}

	private Shape outlineShape(Rectangle b, int width)
	{
		return outlineShape(b, width, shapeTab(b).cornerRadius);
	}

	private static Shape outlineShape(Rectangle b, int width, int radius)
	{
		// Inset by half the stroke so the outline stays inside the clickable area
		int arc = radius * 2;
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
