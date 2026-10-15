package com.inventoryhover;

import java.awt.Color;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.config.ConfigManager;

/**
 * Hover, slot background, spotlight and miss dot settings of one tab, read by
 * the tab's key prefix. Resolved values are cached until a setting changes.
 */
@Singleton
class TabSettings
{
	static final String INVENTORY = "inv";
	static final String PRAYERS = "prayer";
	static final String SPELLS = "spell";

	static final List<String> PREFIXES = List.of(INVENTORY, PRAYERS, SPELLS);

	/** Config sections that hold tab settings. */
	static final Set<String> SECTIONS = Set.of(InventoryHoverHighlightConfig.invHoverSection, InventoryHoverHighlightConfig.invSpotlightSection, InventoryHoverHighlightConfig.invMissDotsSection, InventoryHoverHighlightConfig.prayerHoverSection, InventoryHoverHighlightConfig.prayerSpotlightSection, InventoryHoverHighlightConfig.prayerMissDotsSection, InventoryHoverHighlightConfig.spellHoverSection, InventoryHoverHighlightConfig.spellSpotlightSection, InventoryHoverHighlightConfig.spellMissDotsSection);

	static class Tab
	{
		InventoryHoverHighlightConfig.HighlightStyle style;
		Color outlineColor;
		Color fillColor;
		int outlineWidth;
		int cornerRadius;
		int hoverFade;
		int hoverDelay;
		boolean hideWhenMenuOpen;
		boolean discoMode;
		int discoInterval;
		boolean keepIconColors;
		/** Slot background color, or null when off. */
		Color slotBackground;
		boolean spotlight;
		int spotlightRadius;
		Color spotlightDim;
		Color spotlightGlow;
		int spotlightGlowSize;
		boolean spotlightAlways;
		boolean spotlightDeadOnly;
		boolean spotlightFalloff;
		int spotlightFalloffDistance;
		int spotlightFalloffEnd;
		Color spotlightFalloffColor;
		boolean missDots;
		Color missDotColor;
		InventoryHoverHighlightConfig.MissDotShape missDotShape;
		boolean missDotHollow;
		int missDotSize;
		int missDotDuration;
	}

	private final ConfigManager configManager;
	private final Map<String, Tab> cache = new ConcurrentHashMap<>();

	@Inject
	TabSettings(ConfigManager configManager)
	{
		this.configManager = configManager;
	}

	Tab get(String prefix)
	{
		return cache.computeIfAbsent(prefix, this::resolve);
	}

	void invalidate()
	{
		cache.clear();
	}

	private Tab resolve(String p)
	{
		Tab tab = new Tab();
		tab.style = read(p + "Style", InventoryHoverHighlightConfig.HighlightStyle.class, InventoryHoverHighlightConfig.HighlightStyle.BOTH);
		tab.outlineColor = read(p + "OutlineColor", Color.class, new Color(255, 255, 255, 120));
		tab.fillColor = read(p + "FillColor", Color.class, new Color(98, 189, 125, 60));
		tab.outlineWidth = read(p + "OutlineWidth", int.class, 1);
		tab.cornerRadius = read(p + "CornerRadius", int.class, 4);
		tab.hoverFade = read(p + "HoverFade", int.class, 400);
		tab.hoverDelay = read(p + "HoverDelay", int.class, 0);
		tab.hideWhenMenuOpen = read(p + "HideWhenMenuOpen", boolean.class, false);
		tab.discoMode = read(p + "DiscoMode", boolean.class, false);
		tab.discoInterval = read(p + "DiscoInterval", int.class, 0);
		tab.keepIconColors = read(p + "KeepIconColors", boolean.class, false);
		tab.slotBackground = read(p + "SlotBackground", boolean.class, false)
			? read(p + "SlotBackgroundColor", Color.class, new Color(60, 60, 70, 90))
			: null;
		tab.spotlight = read(p + "Spotlight", boolean.class, false);
		tab.spotlightRadius = read(p + "SpotlightRadius", int.class, 90);
		tab.spotlightDim = read(p + "SpotlightDim", Color.class, new Color(0, 0, 0, 110));
		tab.spotlightGlow = read(p + "SpotlightGlow", Color.class, new Color(255, 255, 255, 45));
		tab.spotlightGlowSize = read(p + "SpotlightGlowSize", int.class, 30);
		tab.spotlightAlways = read(p + "SpotlightAlways", boolean.class, false);
		tab.spotlightDeadOnly = read(p + "SpotlightDeadOnly", boolean.class, false);
		tab.spotlightFalloff = read(p + "SpotlightFalloff", boolean.class, false);
		tab.spotlightFalloffDistance = read(p + "SpotlightFalloffDistance", int.class, 250);
		tab.spotlightFalloffEnd = read(p + "SpotlightFalloffEnd", int.class, 400);
		tab.spotlightFalloffColor = read(p + "SpotlightFalloffColor", Color.class, new Color(0, 0, 0, 0));
		tab.missDots = read(p + "MissDots", boolean.class, false);
		tab.missDotColor = read(p + "MissDotColor", Color.class, new Color(255, 40, 40, 200));
		tab.missDotShape = read(p + "MissDotShape", InventoryHoverHighlightConfig.MissDotShape.class, InventoryHoverHighlightConfig.MissDotShape.CIRCLE);
		tab.missDotHollow = read(p + "MissDotHollow", boolean.class, false);
		tab.missDotSize = read(p + "MissDotSize", int.class, 4);
		tab.missDotDuration = read(p + "MissDotDuration", int.class, 15);
		return tab;
	}

	/** A stored setting, or its default when it was never set. */
	@SuppressWarnings("unchecked")
	private <T> T read(String key, Type type, T fallback)
	{
		Object value = configManager.getConfiguration(InventoryHoverHighlightConfig.GROUP, key, type);
		return value == null ? fallback : (T) value;
	}
}
