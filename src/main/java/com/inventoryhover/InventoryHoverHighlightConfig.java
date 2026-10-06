package com.inventoryhover;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup(InventoryHoverHighlightConfig.GROUP)
public interface InventoryHoverHighlightConfig extends Config
{
	String GROUP = "inventoryhoverhighlight";

	enum HighlightStyle
	{
		OUTLINE,
		FILL,
		BOTH
	}

	enum GridMode
	{
		OFF,
		IN_PANEL,
		ALWAYS
	}

	enum TickCue
	{
		OFF,
		PULSE,
		DRAIN,
		COLOR
	}

	@ConfigSection(
		name = "Appearance",
		description = "How the hover highlight looks",
		position = 0
	)
	String appearanceSection = "appearance";

	@ConfigSection(
		name = "Areas",
		description = "Where the hover highlight is shown",
		position = 1
	)
	String areasSection = "areas";

	@ConfigSection(
		name = "Click feedback",
		description = "Feedback for clicks and game ticks",
		position = 2
	)
	String clickSection = "click";

	@ConfigSection(
		name = "Extras",
		description = "Clickable grid, autocast marker and active prayers",
		position = 3
	)
	String extrasSection = "extras";

	// Appearance

	@ConfigItem(
		keyName = "style",
		name = "Style",
		description = "How the hovered slot is drawn",
		position = 0,
		section = appearanceSection
	)
	default HighlightStyle style()
	{
		return HighlightStyle.BOTH;
	}

	@Alpha
	@ConfigItem(
		keyName = "outlineColor",
		name = "Outline color",
		description = "Color of the slot outline",
		position = 1,
		section = appearanceSection
	)
	default Color outlineColor()
	{
		return new Color(255, 255, 255, 120);
	}

	@Alpha
	@ConfigItem(
		keyName = "fillColor",
		name = "Fill color",
		description = "Color of the slot fill",
		position = 2,
		section = appearanceSection
	)
	default Color fillColor()
	{
		return new Color(255, 255, 255, 30);
	}

	@Range(max = 10)
	@ConfigItem(
		keyName = "cornerRadius",
		name = "Corner radius",
		description = "Rounds the corners of the highlight; 0 for square",
		position = 3,
		section = appearanceSection
	)
	default int cornerRadius()
	{
		return 4;
	}

	@Range(min = 1, max = 4)
	@ConfigItem(
		keyName = "outlineWidth",
		name = "Outline width",
		description = "Thickness of the outline in pixels",
		position = 4,
		section = appearanceSection
	)
	default int outlineWidth()
	{
		return 1;
	}

	@Units(Units.MILLISECONDS)
	@Range(max = 2000)
	@ConfigItem(
		keyName = "hoverFade",
		name = "Hover fade-out",
		description = "How long a highlight, including its tick cue, fades after the mouse leaves; 0 to disable",
		position = 6,
		section = appearanceSection
	)
	default int hoverFade()
	{
		return 400;
	}

	@ConfigItem(
		keyName = "hideWhenMenuOpen",
		name = "Hide when right-click menu open",
		description = "Do not highlight while a right-click menu is open",
		position = 5,
		section = appearanceSection
	)
	default boolean hideWhenMenuOpen()
	{
		return false;
	}

	// Areas

	@ConfigItem(
		keyName = "onlyItems",
		name = "Only slots with items",
		description = "Skip empty inventory slots",
		position = 0,
		section = areasSection
	)
	default boolean onlyItems()
	{
		return true;
	}

	@ConfigItem(
		keyName = "bankInventory",
		name = "Bank inventory",
		description = "Also highlight the inventory shown next to the bank",
		position = 1,
		section = areasSection
	)
	default boolean bankInventory()
	{
		return true;
	}

	@ConfigItem(
		keyName = "prayers",
		name = "Prayers",
		description = "Also highlight the hovered prayer in the prayer tab",
		position = 2,
		section = areasSection
	)
	default boolean prayers()
	{
		return true;
	}

	@ConfigItem(
		keyName = "spells",
		name = "Spells",
		description = "Also highlight the hovered spell in the spellbook",
		position = 3,
		section = areasSection
	)
	default boolean spells()
	{
		return true;
	}

	// Click feedback

	@ConfigItem(
		keyName = "clickFlash",
		name = "Click flash",
		description = "Briefly flash the slot you clicked",
		position = 0,
		section = clickSection
	)
	default boolean clickFlash()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "clickFlashColor",
		name = "Flash color",
		description = "Color of the click flash",
		position = 1,
		section = clickSection
	)
	default Color clickFlashColor()
	{
		return new Color(255, 255, 255, 140);
	}

	@Units(Units.MILLISECONDS)
	@Range(min = 50, max = 1000)
	@ConfigItem(
		keyName = "clickFlashDuration",
		name = "Flash duration",
		description = "How long the click flash takes to fade",
		position = 2,
		section = clickSection
	)
	default int clickFlashDuration()
	{
		return 200;
	}

	@ConfigItem(
		keyName = "clickMarker",
		name = "Queued click marker",
		description = "Mark a clicked slot with a dot until the next game tick",
		position = 3,
		section = clickSection
	)
	default boolean clickMarker()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "clickMarkerColor",
		name = "Marker color",
		description = "Color of the queued click marker",
		position = 4,
		section = clickSection
	)
	default Color clickMarkerColor()
	{
		return new Color(0, 255, 0, 220);
	}

	@Alpha
	@ConfigItem(
		keyName = "doubleClickColor",
		name = "Double click color",
		description = "Marker color when the same slot is clicked twice in one tick",
		position = 5,
		section = clickSection
	)
	default Color doubleClickColor()
	{
		return new Color(255, 60, 60, 220);
	}

	@ConfigItem(
		keyName = "tickCue",
		name = "Mid-tick cue",
		description = "Show progress through the click window on the hovered prayer."
			+ "<br>Pulse: brightens at the cue position"
			+ "<br>Drain: outline refills at the cue position, then shrinks"
			+ "<br>Color: outline turns green at the cue position, then goes to red",
		position = 6,
		section = clickSection
	)
	default TickCue tickCue()
	{
		return TickCue.PULSE;
	}

	@Units(Units.MILLISECONDS)
	@Range(max = 300)
	@ConfigItem(
		keyName = "tickLatency",
		name = "Mid-tick cue latency",
		description = "Your round-trip ping to the world. A click must reach the server before"
			+ " its tick, so the click window closes this long before the client sees the next tick",
		position = 7,
		section = clickSection
	)
	default int tickLatency()
	{
		return 0;
	}

	@Units(Units.PERCENT)
	@Range(max = 90)
	@ConfigItem(
		keyName = "pulsePosition",
		name = "Cue position",
		description = "Where in the click window every cue mode kicks off. Slightly before the middle"
			+ " leaves room for reaction time while early clicks stay in the same tick",
		position = 8,
		section = clickSection
	)
	default int pulsePosition()
	{
		return 40;
	}

	@ConfigItem(
		keyName = "misclickGuard",
		name = "Misclick guard",
		description = "Show a red marker when the mouse is in a gap where a click does nothing",
		position = 9,
		section = clickSection
	)
	default boolean misclickGuard()
	{
		return true;
	}

	@ConfigItem(
		keyName = "deadClickMarker",
		name = "Dead click marker",
		description = "Show a red ring where you click an empty slot or a gap with no interaction",
		position = 11,
		section = clickSection
	)
	default boolean deadClickMarker()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "misclickColor",
		name = "Misclick color",
		description = "Color of the misclick guard and dead click markers",
		position = 10,
		section = clickSection
	)
	default Color misclickColor()
	{
		return new Color(255, 60, 60, 180);
	}

	// Extras

	@ConfigItem(
		keyName = "grid",
		name = "Clickable grid",
		description = "Outline every clickable area the hover highlight can fill."
			+ "<br>In panel: only the panel under the mouse"
			+ "<br>Always: every visible panel",
		position = 0,
		section = extrasSection
	)
	default GridMode grid()
	{
		return GridMode.ALWAYS;
	}

	@Alpha
	@ConfigItem(
		keyName = "gridColor",
		name = "Grid color",
		description = "Color of the clickable grid outlines",
		position = 1,
		section = extrasSection
	)
	default Color gridColor()
	{
		return new Color(255, 255, 255, 40);
	}

	@ConfigItem(
		keyName = "deadAreas",
		name = "Dead areas",
		description = "Shade the gaps where a click does nothing."
			+ "<br>In panel: only the panel under the mouse"
			+ "<br>Always: every visible panel",
		position = 2,
		section = extrasSection
	)
	default GridMode deadAreas()
	{
		return GridMode.IN_PANEL;
	}

	@Alpha
	@ConfigItem(
		keyName = "deadAreaColor",
		name = "Dead area color",
		description = "Color of the dead area shading",
		position = 3,
		section = extrasSection
	)
	default Color deadAreaColor()
	{
		return new Color(255, 60, 60, 25);
	}

	@ConfigItem(
		keyName = "activePrayers",
		name = "Active prayer highlight",
		description = "Replace the game's active prayer glow with a highlight that follows the"
			+ " server-confirmed prayer state, so it does not flicker when clicking fast",
		position = 4,
		section = extrasSection
	)
	default boolean activePrayers()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "activePrayerColor",
		name = "Active outline",
		description = "Outline color of an active prayer or selected spell",
		position = 5,
		section = extrasSection
	)
	default Color activePrayerColor()
	{
		return new Color(255, 200, 0, 220);
	}

	@Alpha
	@ConfigItem(
		keyName = "activePrayerFill",
		name = "Active fill",
		description = "Fill color of an active prayer or selected spell",
		position = 6,
		section = extrasSection
	)
	default Color activePrayerFill()
	{
		return new Color(255, 200, 0, 50);
	}

	@ConfigItem(
		keyName = "selectedSpell",
		name = "Selected spell highlight",
		description = "Give a spell selected for manual casting the same gold highlight as an active prayer",
		position = 7,
		section = extrasSection
	)
	default boolean selectedSpell()
	{
		return true;
	}

	@ConfigItem(
		keyName = "pendingPrayers",
		name = "Pending prayer state",
		description = "Show a dashed outline from your click until the server confirms the prayer change",
		position = 8,
		section = extrasSection
	)
	default boolean pendingPrayers()
	{
		return true;
	}

	@ConfigItem(
		keyName = "autocastMarker",
		name = "Autocast marker",
		description = "Outline the spell set to autocast; hovering it shows the hover highlight instead",
		position = 9,
		section = extrasSection
	)
	default boolean autocastMarker()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "autocastColor",
		name = "Autocast color",
		description = "Color of the autocast marker",
		position = 10,
		section = extrasSection
	)
	default Color autocastColor()
	{
		return new Color(0, 200, 255, 180);
	}
}
