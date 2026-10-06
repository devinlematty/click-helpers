package com.inventoryhover;

import com.google.inject.Provides;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.EnumComposition;
import net.runelite.api.GameState;
import net.runelite.api.ParamID;
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
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

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
	private InventoryHoverHighlightOverlay overlay;

	/** Clicks to flash, oldest first. */
	final List<Click> flashes = new ArrayList<>();

	/** Clicks sent since the last game tick, keyed by widget. */
	final Map<Long, Click> queuedClicks = new LinkedHashMap<>();

	long lastTickNanos;

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
		lastTickNanos = System.nanoTime();
		clientThread.invoke(this::updateAutocast);
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		flashes.clear();
		queuedClicks.clear();
		autocastComponent = -1;
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		lastTickNanos = System.nanoTime();
		queuedClicks.clear();
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
