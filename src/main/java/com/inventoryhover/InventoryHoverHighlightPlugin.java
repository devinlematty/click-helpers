package com.inventoryhover;

import com.google.inject.Provides;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.awt.event.MouseEvent;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.api.EnumComposition;
import net.runelite.api.GameState;
import net.runelite.api.ParamID;
import net.runelite.api.Prayer;
import net.runelite.api.events.BeforeRender;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.input.MouseAdapter;
import net.runelite.client.input.MouseManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Text;

@PluginDescriptor(
	name = "Inventory Hover Highlight",
	description = "Highlights the clickable area of the hovered inventory slot",
	tags = {"inventory", "hover", "highlight", "click"}
)
public class InventoryHoverHighlightPlugin extends Plugin
{
	// Cache enum mapping the autocast varbit value to the spell's object
	private static final int AUTOCAST_SPELLS_ENUM = 1986;

	static final int TICK_NANOS = 600_000_000;

	static final int PRAYER_COUNT = 30;

	// Sprite the game draws behind an active prayer
	private static final int ACTIVE_PRAYER_GLOW_SPRITE = 4892;

	private static final Map<String, Prayer> PRAYERS_BY_NAME = new HashMap<>();

	static
	{
		for (Prayer prayer : Prayer.values())
		{
			PRAYERS_BY_NAME.put(prayer.name(), prayer);
		}
	}

	private static final int INVENTORY_GROUP = InterfaceID.Inventory.ITEMS >>> 16;
	private static final int BANKSIDE_GROUP = InterfaceID.Bankside.ITEMS >>> 16;
	private static final int PRAYER_GROUP = InterfaceID.Prayerbook.CONTAINER >>> 16;
	private static final int SPELLBOOK_GROUP = InterfaceID.MagicSpellbook.SPELLLAYER >>> 16;

	static class Click
	{
		final Rectangle bounds;
		final long time;
		int count;

		Click(Rectangle bounds, long time, int count)
		{
			this.bounds = bounds;
			this.time = time;
			this.count = count;
		}
	}

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private InventoryHoverHighlightConfig config;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private MouseManager mouseManager;

	/**
	 * Left clicks as {x, y, nanoTime}, queued from the input thread. The overlay
	 * decides on the client thread whether each one landed on a dead spot.
	 */
	final Queue<long[]> mouseClicks = new ConcurrentLinkedQueue<>();

	private final MouseAdapter mouseListener = new MouseAdapter()
	{
		@Override
		public MouseEvent mousePressed(MouseEvent e)
		{
			net.runelite.api.Point mouse = client.getMouseCanvasPosition();
			if (SwingUtilities.isLeftMouseButton(e) && !client.isMenuOpen() && mouse != null)
			{
				mouseClicks.add(new long[]{mouse.getX(), mouse.getY(), System.nanoTime()});
			}
			return e;
		}
	};

	@Inject
	private InventoryHoverHighlightOverlay overlay;

	/** Clicks to flash, oldest first. */
	final List<Click> flashes = new ArrayList<>();

	/** Clicks sent since the last game tick, keyed by widget. */
	final Map<Long, Click> queuedClicks = new LinkedHashMap<>();

	long lastTickNanos;

	/** Prayer toggles clicked but not yet confirmed by the server. */
	private final Map<Prayer, Pending> pendingPrayers = new EnumMap<>(Prayer.class);

	private static class Pending
	{
		final boolean expected;
		int ticks;

		Pending(boolean expected)
		{
			this.expected = expected;
		}
	}

	/** Component id of the spell set to autocast, or -1. */
	int autocastComponent = -1;

	@Provides
	InventoryHoverHighlightConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(InventoryHoverHighlightConfig.class);
	}

	@Override
	protected void startUp()
	{
		overlayManager.add(overlay);
		mouseManager.registerMouseListener(mouseListener);
		lastTickNanos = System.nanoTime();
		clientThread.invoke(this::updateAutocast);
	}

	@Override
	protected void shutDown()
	{
		clientThread.invoke(this::restorePrayerGlows);
		overlayManager.remove(overlay);
		mouseManager.unregisterMouseListener(mouseListener);
		mouseClicks.clear();
		flashes.clear();
		queuedClicks.clear();
		pendingPrayers.clear();
		autocastComponent = -1;
	}

	@Subscribe
	public void onBeforeRender(BeforeRender event)
	{
		if (!config.activePrayers())
		{
			return;
		}

		// The game re-shows the glow whenever its prayer script runs, so hide it every frame
		for (int i = 0; i < PRAYER_COUNT; i++)
		{
			Widget glow = findPrayerGlow(client.getWidget(InterfaceID.Prayerbook.PRAYER1 + i));
			if (glow != null && !glow.isSelfHidden())
			{
				glow.setHidden(true);
			}
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (event.getGroup().equals(InventoryHoverHighlightConfig.GROUP)
			&& event.getKey().equals("activePrayers")
			&& !config.activePrayers())
		{
			clientThread.invoke(this::restorePrayerGlows);
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		lastTickNanos = System.nanoTime();
		queuedClicks.clear();

		// Drop pending toggles once the server agrees, or after a couple of ticks
		pendingPrayers.entrySet().removeIf(e ->
			isPrayerActive(e.getKey()) == e.getValue().expected || ++e.getValue().ticks > 2);
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			updateAutocast();
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		if (event.getVarbitId() == VarbitID.AUTOCAST_SPELL)
		{
			updateAutocast();
		}
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		Widget widget = event.getWidget();
		if (widget == null || !isTracked(widget))
		{
			return;
		}

		Rectangle bounds = widget.getBounds();
		if (bounds == null)
		{
			return;
		}

		long now = System.nanoTime();
		flashes.add(new Click(bounds, now, 1));

		if (widget.getId() >>> 16 == PRAYER_GROUP)
		{
			Prayer prayer = prayerFor(widget);
			if (prayer != null)
			{
				pendingPrayers.put(prayer, new Pending(!predictedActive(prayer)));
			}
		}

		long key = ((long) widget.getId() << 16) | (widget.getIndex() & 0xffff);
		Click queued = queuedClicks.get(key);
		if (queued != null)
		{
			queued.count++;
		}
		else
		{
			queuedClicks.put(key, new Click(bounds, now, 1));
		}
	}

	private boolean isTracked(Widget widget)
	{
		int group = widget.getId() >>> 16;
		return group == INVENTORY_GROUP
			|| (group == BANKSIDE_GROUP && config.bankInventory())
			|| (group == PRAYER_GROUP && config.prayers())
			|| (group == SPELLBOOK_GROUP && config.spells());
	}

	/** Shows the game's glow again on the prayers that are active. */
	private void restorePrayerGlows()
	{
		for (int i = 0; i < PRAYER_COUNT; i++)
		{
			Widget widget = client.getWidget(InterfaceID.Prayerbook.PRAYER1 + i);
			Widget glow = findPrayerGlow(widget);
			if (glow != null)
			{
				Prayer prayer = prayerFor(widget);
				glow.setHidden(prayer == null || !isPrayerActive(prayer));
			}
		}
	}

	/** The child of a prayer widget that draws the game's active glow, or null. */
	private static Widget findPrayerGlow(Widget widget)
	{
		if (widget == null)
		{
			return null;
		}
		if (widget.getSpriteId() == ACTIVE_PRAYER_GLOW_SPRITE)
		{
			return widget;
		}

		for (Widget[] children : new Widget[][]{
			widget.getStaticChildren(), widget.getDynamicChildren(), widget.getNestedChildren()})
		{
			if (children == null)
			{
				continue;
			}
			for (Widget child : children)
			{
				Widget glow = findPrayerGlow(child);
				if (glow != null)
				{
					return glow;
				}
			}
		}
		return null;
	}

	/** The prayer a prayer tab widget belongs to, matched by its name, or null. */
	Prayer prayerFor(Widget widget)
	{
		if (widget == null || widget.getName() == null)
		{
			return null;
		}
		String key = Text.removeTags(widget.getName()).toUpperCase().replaceAll("[^A-Z0-9]+", "_");
		return PRAYERS_BY_NAME.get(key);
	}

	/** Prayer state including clicks the server has not confirmed yet. */
	boolean predictedActive(Prayer prayer)
	{
		Pending pending = pendingPrayers.get(prayer);
		return pending != null ? pending.expected : isPrayerActive(prayer);
	}

	/** Server-confirmed prayer state, unaffected by the client's predicted toggle. */
	boolean isPrayerActive(Prayer prayer)
	{
		return client.getServerVarbitValue(prayer.getVarbit()) == 1;
	}

	private void updateAutocast()
	{
		autocastComponent = -1;
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		int index = client.getVarbitValue(VarbitID.AUTOCAST_SPELL);
		if (index <= 0)
		{
			return;
		}

		EnumComposition spells = client.getEnum(AUTOCAST_SPELLS_ENUM);
		int spellObj = spells == null ? -1 : spells.getIntValue(index);
		if (spellObj <= 0)
		{
			return;
		}

		autocastComponent = client.getItemDefinition(spellObj).getIntValue(ParamID.SPELL_BUTTON);
	}
}
