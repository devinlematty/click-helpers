package com.inventoryhover;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Keybind;
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

	enum RingAnimation
	{
		GROW,
		SHRINK,
		PULSE,
		CROSS
	}

	enum CooldownStyle
	{
		FILL,
		DRAIN,
		DIM
	}

	enum TickCue
	{
		OFF,
		PULSE,
		DRAIN,
		COLOR
	}

	enum PracticeStats
	{
		OFF,
		LAST_ROUND,
		SESSION,
		BOTH
	}

	enum ChartUnit
	{
		SECONDS,
		TICKS
	}

	enum MissDotShape
	{
		CIRCLE,
		RING,
		SQUARE,
		DIAMOND,
		TRIANGLE,
		CROSS,
		PLUS,
		HEART
	}


	@ConfigSection(
		name = "Areas",
		description = "Where the highlight and clickable grid are shown",
		position = 1
	)
	String areasSection = "areas";

	@ConfigSection(
		name = "Click feedback",
		description = "Flash and marker on the slot you click",
		position = 2,
		closedByDefault = true
	)
	String clickSection = "click";

	@ConfigSection(
		name = "Missed clicks",
		description = "Markers for clicks that do nothing or are refused",
		position = 3,
		closedByDefault = true
	)
	String missSection = "misses";

	@ConfigSection(
		name = "Dead areas",
		description = "Shading over the gaps where a click does nothing, per panel",
		position = 4,
		closedByDefault = true
	)
	String deadSection = "dead";

	@ConfigSection(
		name = "Prayers and spells",
		description = "Active prayer, selected spell and autocast highlights",
		position = 5,
		closedByDefault = true
	)
	String prayerSection = "prayerspells";

	@ConfigSection(
		name = "Tick cue",
		description = "Timing cue on prayers to help with prayer flicking",
		position = 6,
		closedByDefault = true
	)
	String tickSection = "tick";

	@ConfigSection(
		name = "Click chart",
		description = "Floating chart of your recent good, missed, refused and double clicks",
		position = 7,
		closedByDefault = true
	)
	String chartSection = "stats";

	@ConfigSection(
		name = "Hide items",
		description = "Hide items you do not click in a fight",
		position = 8,
		closedByDefault = true
	)
	String itemsSection = "items";

	@ConfigSection(
		name = "Eat and drink delay",
		description = "Shade food and potions while another one would be ignored",
		position = 9,
		closedByDefault = true
	)
	String delaySection = "delays";


	@ConfigSection(
		name = "Switch practice",
		description = "Hotkey for switch practice; the other practice settings are in the Click Helpers side panel",
		position = 11,
		closedByDefault = true
	)
	String practiceSection = "practice";

	@ConfigSection(
		name = "Import/export settings",
		description = "Export all settings to the clipboard as JSON, import shared JSON settings, and the side panel",
		position = 12,
		closedByDefault = true
	)
	String shareSection = "share";

	@ConfigSection(
		name = "Inventory: hover",
		description = "Inventory and bank inventory hover settings",
		position = 13,
		closedByDefault = true
	)
	String invHoverSection = "invHover";

	@ConfigSection(
		name = "Inventory: spotlight",
		description = "Inventory and bank inventory spotlight settings",
		position = 14,
		closedByDefault = true
	)
	String invSpotlightSection = "invSpotlight";

	@ConfigSection(
		name = "Inventory: miss dots",
		description = "Inventory and bank inventory miss dots settings",
		position = 15,
		closedByDefault = true
	)
	String invMissDotsSection = "invMissDots";

	@ConfigSection(
		name = "Prayer: hover",
		description = "Prayer tab hover settings",
		position = 16,
		closedByDefault = true
	)
	String prayerHoverSection = "prayerHover";

	@ConfigSection(
		name = "Prayer: spotlight",
		description = "Prayer tab spotlight settings",
		position = 17,
		closedByDefault = true
	)
	String prayerSpotlightSection = "prayerSpotlight";

	@ConfigSection(
		name = "Prayer: miss dots",
		description = "Prayer tab miss dots settings",
		position = 18,
		closedByDefault = true
	)
	String prayerMissDotsSection = "prayerMissDots";

	@ConfigSection(
		name = "Spellbook: hover",
		description = "Spellbook hover settings",
		position = 19,
		closedByDefault = true
	)
	String spellHoverSection = "spellHover";

	@ConfigSection(
		name = "Spellbook: spotlight",
		description = "Spellbook spotlight settings",
		position = 20,
		closedByDefault = true
	)
	String spellSpotlightSection = "spellSpotlight";

	@ConfigSection(
		name = "Spellbook: miss dots",
		description = "Spellbook miss dots settings",
		position = 21,
		closedByDefault = true
	)
	String spellMissDotsSection = "spellMissDots";





	// Hover highlight








	@ConfigItem(
		keyName = "roundedAreas",
		name = "Round grid and dead areas to highlight radius",
		description = "Round the clickable grid and the dead area shading with the hover highlight's corner radius."
			+ "<br>Note: the game's clickable areas are square, so slot corners can look dead even though"
			+ " clicks there still work",
		position = 7,
		section = areasSection
	)
	default boolean roundedAreas()
	{
		return true;
	}





	// Areas

	@ConfigItem(
		keyName = "prayers",
		name = "Prayers",
		description = "Also highlight the hovered prayer in the prayer tab",
		position = 0,
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
		position = 1,
		section = areasSection
	)
	default boolean spells()
	{
		return true;
	}

	@ConfigItem(
		keyName = "bankInventory",
		name = "Bank inventory",
		description = "Also highlight the inventory shown next to the bank",
		position = 2,
		section = areasSection
	)
	default boolean bankInventory()
	{
		return true;
	}

	@ConfigItem(
		keyName = "onlyItems",
		name = "Only slots with items",
		description = "Skip empty inventory slots",
		position = 3,
		section = areasSection
	)
	default boolean onlyItems()
	{
		return true;
	}

	@ConfigItem(
		keyName = "grid",
		name = "Clickable grid",
		description = "Outline every clickable area the hover highlight can fill."
			+ "<br>In panel: only the panel under the mouse"
			+ "<br>Always: every visible panel",
		position = 4,
		section = areasSection
	)
	default GridMode grid()
	{
		return GridMode.OFF;
	}

	@ConfigItem(
		keyName = "gridShowEmpty",
		name = "Grid on empty slots",
		description = "Also outline inventory slots with no item, so every clickable slot shows",
		position = 5,
		section = areasSection
	)
	default boolean gridShowEmpty()
	{
		return false;
	}

	@Alpha
	@ConfigItem(
		keyName = "gridColor",
		name = "Grid color",
		description = "Color of the clickable grid outlines",
		position = 6,
		section = areasSection
	)
	default Color gridColor()
	{
		return new Color(255, 255, 255, 40);
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

	@ConfigItem(
		keyName = "clickMarker",
		name = "Queued click marker",
		description = "Mark a clicked slot with a dot until the next game tick. The dot is yellow"
			+ " (rejected click color) when the click lands during the eat or drink delay",
		position = 1,
		section = clickSection
	)
	default boolean clickMarker()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "clickFlashColor",
		name = "Flash color",
		description = "Color of the click flash",
		position = 2,
		section = clickSection
	)
	default Color clickFlashColor()
	{
		return new Color(255, 255, 255, 110);
	}

	@Units(Units.MILLISECONDS)
	@Range(min = 50, max = 1000)
	@ConfigItem(
		keyName = "clickFlashDuration",
		name = "Flash duration",
		description = "How long the click flash takes to fade",
		position = 3,
		section = clickSection
	)
	default int clickFlashDuration()
	{
		return 200;
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

	// Missed clicks

	@ConfigItem(
		keyName = "deadClickMarker",
		name = "Dead click marker",
		description = "Show a red ring where you click an empty slot or a gap with no interaction",
		position = 0,
		section = missSection
	)
	default boolean deadClickMarker()
	{
		return true;
	}

	@ConfigItem(
		keyName = "rejectedClickMarker",
		name = "Rejected click marker",
		description = "Show a yellow ring where a click reached the game but was refused,"
			+ " like equipping an item with no inventory space for the swap",
		position = 1,
		section = missSection
	)
	default boolean rejectedClickMarker()
	{
		return true;
	}


	@ConfigItem(
		keyName = "missOutline",
		name = "Miss outline",
		description = "Outline the slot nearest a dead click, the one you most likely meant to click."
			+ " It replaces the normal outline on that slot until it fades",
		position = 3,
		section = missSection
	)
	default boolean missOutline()
	{
		return false;
	}

	@Alpha
	@ConfigItem(
		keyName = "misclickColor",
		name = "Dead click color",
		description = "Color of the dead click marker",
		position = 4,
		section = missSection
	)
	default Color misclickColor()
	{
		return new Color(255, 60, 60, 180);
	}

	@Alpha
	@ConfigItem(
		keyName = "rejectedClickColor",
		name = "Rejected click color",
		description = "Color of the rejected click marker",
		position = 5,
		section = missSection
	)
	default Color rejectedClickColor()
	{
		return new Color(255, 220, 0, 200);
	}

	@ConfigItem(
		keyName = "ringAnimation",
		name = "Ring animation",
		description = "How dead and rejected click markers animate."
			+ "<br>Grow: ring grows while it fades"
			+ "<br>Shrink: ring shrinks onto the click"
			+ "<br>Pulse: ring beats while it fades"
			+ "<br>Cross: an X instead of a ring",
		position = 6,
		section = missSection
	)
	default RingAnimation ringAnimation()
	{
		return RingAnimation.GROW;
	}

	@Units(Units.MILLISECONDS)
	@Range(min = 100, max = 3000)
	@ConfigItem(
		keyName = "ringDuration",
		name = "Ring duration",
		description = "How long dead and rejected click markers take to fade",
		position = 7,
		section = missSection
	)
	default int ringDuration()
	{
		return 500;
	}



	@Alpha
	@ConfigItem(
		keyName = "missOutlineColor",
		name = "Miss outline color",
		description = "Color of the miss outline",
		position = 10,
		section = missSection
	)
	default Color missOutlineColor()
	{
		return new Color(255, 40, 40, 130);
	}

	@Units(Units.MILLISECONDS)
	@Range(min = 100, max = 10000)
	@ConfigItem(
		keyName = "missOutlineDuration",
		name = "Miss outline duration",
		description = "How long the miss outline stays before it is gone",
		position = 11,
		section = missSection
	)
	default int missOutlineDuration()
	{
		return 1500;
	}



	// Dead areas

	@ConfigItem(
		keyName = "deadAreas",
		name = "Dead areas",
		description = "Shade the gaps where a click does nothing."
			+ "<br>In panel: only the panel under the mouse"
			+ "<br>Always: every visible panel",
		position = 0,
		section = deadSection
	)
	default GridMode deadAreas()
	{
		return GridMode.ALWAYS;
	}

	@ConfigItem(
		keyName = "deadInventory",
		name = "Inventory",
		description = "Shade dead areas in the inventory",
		position = 1,
		section = deadSection
	)
	default boolean deadInventory()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "deadInventoryColor",
		name = "Inventory color",
		description = "Color of the dead areas in the inventory",
		position = 2,
		section = deadSection
	)
	default Color deadInventoryColor()
	{
		return new Color(30, 30, 32, 90);
	}

	@ConfigItem(
		keyName = "deadBank",
		name = "Bank inventory",
		description = "Shade dead areas in the inventory shown next to the bank",
		position = 3,
		section = deadSection
	)
	default boolean deadBank()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "deadBankColor",
		name = "Bank inventory color",
		description = "Color of the dead areas in the bank inventory",
		position = 4,
		section = deadSection
	)
	default Color deadBankColor()
	{
		return new Color(30, 30, 32, 90);
	}

	@ConfigItem(
		keyName = "deadPrayers",
		name = "Prayers",
		description = "Shade dead areas in the prayer tab",
		position = 5,
		section = deadSection
	)
	default boolean deadPrayers()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "deadPrayersColor",
		name = "Prayers color",
		description = "Color of the dead areas in the prayer tab",
		position = 6,
		section = deadSection
	)
	default Color deadPrayersColor()
	{
		return new Color(30, 30, 32, 90);
	}

	@ConfigItem(
		keyName = "deadSpells",
		name = "Spells",
		description = "Shade dead areas in the spellbook",
		position = 7,
		section = deadSection
	)
	default boolean deadSpells()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "deadSpellsColor",
		name = "Spells color",
		description = "Color of the dead areas in the spellbook",
		position = 8,
		section = deadSection
	)
	default Color deadSpellsColor()
	{
		return new Color(30, 30, 32, 90);
	}

	@ConfigItem(
		keyName = "deadOuterAreas",
		name = "Cover whole prayer and spell tab",
		description = "Also shade the rest of the prayer and spellbook tab, like the empty space left by a filter."
			+ " Clicks there count as misses. Filter and back buttons stay unshaded",
		position = 9,
		section = deadSection
	)
	default boolean deadOuterAreas()
	{
		return false;
	}

	// Prayers and spells

	@ConfigItem(
		keyName = "activePrayers",
		name = "Active prayer highlight",
		description = "Replace the game's active prayer glow with a highlight that follows the"
			+ " server-confirmed prayer state, so it does not flicker when clicking fast",
		position = 0,
		section = prayerSection
	)
	default boolean activePrayers()
	{
		return true;
	}

	@ConfigItem(
		keyName = "pendingPrayers",
		name = "Pending prayer state",
		description = "Show a dashed outline from your click until the server confirms the prayer change",
		position = 1,
		section = prayerSection
	)
	default boolean pendingPrayers()
	{
		return true;
	}

	@ConfigItem(
		keyName = "selectedSpell",
		name = "Selected spell highlight",
		description = "Give a spell selected for manual casting the same gold highlight as an active prayer",
		position = 2,
		section = prayerSection
	)
	default boolean selectedSpell()
	{
		return true;
	}

	@ConfigItem(
		keyName = "autocastMarker",
		name = "Autocast marker",
		description = "Outline the spell set to autocast; hovering it shows the hover highlight instead",
		position = 3,
		section = prayerSection
	)
	default boolean autocastMarker()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "activePrayerColor",
		name = "Active outline",
		description = "Outline color of an active prayer or selected spell",
		position = 4,
		section = prayerSection
	)
	default Color activePrayerColor()
	{
		return new Color(255, 235, 120, 230);
	}

	@Alpha
	@ConfigItem(
		keyName = "activePrayerFill",
		name = "Active fill",
		description = "Fill color of an active prayer or selected spell",
		position = 5,
		section = prayerSection
	)
	default Color activePrayerFill()
	{
		return new Color(250, 188, 95, 75);
	}

	@Alpha
	@ConfigItem(
		keyName = "autocastColor",
		name = "Autocast color",
		description = "Color of the autocast marker",
		position = 6,
		section = prayerSection
	)
	default Color autocastColor()
	{
		return new Color(0, 200, 255, 180);
	}

	// Tick cue

	@ConfigItem(
		keyName = "tickCue",
		name = "Mid-tick cue",
		description = "Show progress through the click window on the hovered prayer."
			+ "<br>Pulse: brightens at the cue position"
			+ "<br>Drain: outline refills at the cue position, then shrinks"
			+ "<br>Color: outline turns green at the cue position, then goes to red",
		position = 0,
		section = tickSection
	)
	default TickCue tickCue()
	{
		return TickCue.PULSE;
	}

	@ConfigItem(
		keyName = "tickCueAllPrayers",
		name = "Cue on listed prayers",
		description = "Also show the mid-tick cue on the prayers in the list below, even when not hovered",
		position = 1,
		section = tickSection
	)
	default boolean tickCueAllPrayers()
	{
		return false;
	}

	@ConfigItem(
		keyName = "cuePrayers",
		name = "Cue prayers",
		description = "Comma separated prayer names that get the mid-tick cue; * is a wildcard, so * means every prayer",
		position = 2,
		section = tickSection
	)
	default String cuePrayers()
	{
		return "Protect from Magic, Protect from Missiles, Protect from Melee";
	}

	@Alpha
	@ConfigItem(
		keyName = "pulseColor",
		name = "Pulse color",
		description = "Color of the mid-tick cue pulse",
		position = 3,
		section = tickSection
	)
	default Color pulseColor()
	{
		return new Color(186, 140, 230, 90);
	}

	@Units(Units.MILLISECONDS)
	@Range(max = 300)
	@ConfigItem(
		keyName = "tickLatency",
		name = "Mid-tick cue latency",
		description = "Your round-trip ping to the world. A click must reach the server before"
			+ " its tick, so the click window closes this long before the client sees the next tick",
		position = 4,
		section = tickSection
	)
	default int tickLatency()
	{
		return 20;
	}

	@Units(Units.PERCENT)
	@Range(max = 90)
	@ConfigItem(
		keyName = "pulsePosition",
		name = "Cue position",
		description = "Where in the click window every cue mode kicks off. Slightly before the middle"
			+ " leaves room for reaction time while early clicks stay in the same tick",
		position = 5,
		section = tickSection
	)
	default int pulsePosition()
	{
		return 40;
	}

	// Click chart

	@ConfigItem(
		keyName = "clickChart",
		name = "Click chart",
		description = "Floating chart of your recent clicks: clean click rate, good, missed, refused and"
			+ " double clicks. Alt-drag it to move it",
		position = 0,
		section = chartSection
	)
	default boolean clickChart()
	{
		return false;
	}

	@ConfigItem(
		keyName = "clickChartHotkey",
		name = "Click chart hotkey",
		description = "Hotkey that shows or hides the click chart",
		position = 1,
		section = chartSection
	)
	default Keybind clickChartHotkey()
	{
		return Keybind.NOT_SET;
	}

	@ConfigItem(
		keyName = "clickChartUnit",
		name = "Time unit",
		description = "Measure the chart in seconds or in game ticks (0.6 seconds each)",
		position = 2,
		section = chartSection
	)
	default ChartUnit clickChartUnit()
	{
		return ChartUnit.SECONDS;
	}

	@Range(min = 1, max = 300)
	@ConfigItem(
		keyName = "clickChartInterval",
		name = "Bar interval",
		description = "Seconds or ticks each bar covers",
		position = 3,
		section = chartSection
	)
	default int clickChartInterval()
	{
		return 5;
	}

	@Range(min = 4, max = 120)
	@ConfigItem(
		keyName = "clickChartBars",
		name = "Bars",
		description = "How many bars to show; the chart covers bars times the interval",
		position = 4,
		section = chartSection
	)
	default int clickChartBars()
	{
		return 24;
	}

	@Units(Units.PIXELS)
	@Range(min = 90, max = 600)
	@ConfigItem(
		keyName = "clickChartWidth",
		name = "Width",
		description = "Width of the chart in pixels; text shortens to fit narrow charts",
		position = 5,
		section = chartSection
	)
	default int clickChartWidth()
	{
		return 190;
	}

	@Units(Units.PIXELS)
	@Range(min = 15, max = 300)
	@ConfigItem(
		keyName = "clickChartHeight",
		name = "Bar height",
		description = "Height of the bar area in pixels",
		position = 6,
		section = chartSection
	)
	default int clickChartHeight()
	{
		return 50;
	}

	@ConfigItem(
		keyName = "clickChartLegend",
		name = "Legend",
		description = "Show the colored legend with the counts for the window",
		position = 7,
		section = chartSection
	)
	default boolean clickChartLegend()
	{
		return true;
	}

	@ConfigItem(
		keyName = "clickChartTimeAxis",
		name = "Time labels",
		description = "Show the time labels under the bars",
		position = 8,
		section = chartSection
	)
	default boolean clickChartTimeAxis()
	{
		return true;
	}

	@ConfigItem(
		keyName = "clickChartAverage",
		name = "Moving average",
		description = "Draw a line of the average clicks per bar over the last few bars",
		position = 9,
		section = chartSection
	)
	default boolean clickChartAverage()
	{
		return false;
	}

	@Range(min = 2, max = 60)
	@ConfigItem(
		keyName = "clickChartAverageBars",
		name = "Average over",
		description = "How many bars the moving average covers",
		position = 10,
		section = chartSection
	)
	default int clickChartAverageBars()
	{
		return 5;
	}







	// Hide items

	@ConfigItem(
		keyName = "hideItems",
		name = "Hide non-fight items",
		description = "Hide the icon of items with no fight action, such as items with only Use, Open"
			+ " or Bury, and noted items. They keep a faint outline and still work when clicked",
		position = 0,
		section = itemsSection
	)
	default boolean hideItems()
	{
		return false;
	}

	@ConfigItem(
		keyName = "hideItemsHotkey",
		name = "Hide toggle hotkey",
		description = "Hotkey that turns the item hiding on and off; when unset, hiding stays on",
		position = 1,
		section = itemsSection
	)
	default Keybind hideItemsHotkey()
	{
		return Keybind.NOT_SET;
	}

	@ConfigItem(
		keyName = "fightActions",
		name = "Fight actions",
		description = "Comma separated inventory actions that keep an item visible",
		position = 2,
		section = itemsSection
	)
	default String fightActions()
	{
		return "Eat, Drink, Wield, Wear, Equip, Break, Guzzle, Invoke, Activate, Teleport, Rub";
	}

	@ConfigItem(
		keyName = "alwaysShowItems",
		name = "Always show",
		description = "Comma separated item names that are never hidden; * is a wildcard",
		position = 3,
		section = itemsSection
	)
	default String alwaysShowItems()
	{
		return "";
	}

	@ConfigItem(
		keyName = "hiddenItemShowOutline",
		name = "Show hidden item outline",
		description = "Draw a faint outline of a hidden item's icon; turn off with the backfill to leave the slot looking empty",
		position = 4,
		section = itemsSection
	)
	default boolean hiddenItemShowOutline()
	{
		return true;
	}

	@ConfigItem(
		keyName = "hiddenItemShowFill",
		name = "Show hidden item backfill",
		description = "Shade the slot of a hidden item; turn off with the outline to leave the slot looking empty",
		position = 5,
		section = itemsSection
	)
	default boolean hiddenItemShowFill()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "hiddenItemOutline",
		name = "Hidden item outline",
		description = "Outline drawn in place of a hidden item's icon",
		position = 6,
		section = itemsSection
	)
	default Color hiddenItemOutline()
	{
		return new Color(255, 255, 255, 70);
	}

	@Alpha
	@ConfigItem(
		keyName = "hiddenItemFill",
		name = "Hidden item backfill",
		description = "Fill drawn behind a hidden item so you can tell the slot is taken",
		position = 7,
		section = itemsSection
	)
	default Color hiddenItemFill()
	{
		return new Color(255, 255, 255, 12);
	}

	// Eat and drink delay

	@ConfigItem(
		keyName = "cooldowns",
		name = "Eat and drink delay",
		description = "Mark food or potions while eating or drinking another one would be ignored."
			+ " Every item with the same action shares the delay; karambwans have their own",
		position = 0,
		section = delaySection
	)
	default boolean cooldowns()
	{
		return false;
	}

	@ConfigItem(
		keyName = "cooldownStyle",
		name = "Delay style",
		description = "How the delay is drawn."
			+ "<br>Fill: shade the whole slot"
			+ "<br>Drain: shade that empties as the delay runs out"
			+ "<br>Dim: shade that fades as the delay runs out",
		position = 1,
		section = delaySection
	)
	default CooldownStyle cooldownStyle()
	{
		return CooldownStyle.DRAIN;
	}

	@Alpha
	@ConfigItem(
		keyName = "cooldownColor",
		name = "Delay color",
		description = "Color of the eat and drink delay",
		position = 2,
		section = delaySection
	)
	default Color cooldownColor()
	{
		return new Color(0, 0, 0, 130);
	}

	// Spotlight











	// Switch practice

	@ConfigItem(
		keyName = "practiceMode",
		name = "Switch practice",
		description = "Light up random empty inventory slots to click as fast as you can, like a gear switch."
			+ " Clicks off the targets count as misses. Clicking an item still uses it, so practice with free slots."
			+ " Progress shows in the side panel",
		position = 0,
		section = practiceSection,
		hidden = true
	)
	default boolean practiceMode()
	{
		return false;
	}

	@ConfigItem(
		keyName = "practiceHotkey",
		name = "Practice hotkey",
		description = "Hotkey that starts or stops switch practice",
		position = 1,
		section = practiceSection
	)
	default Keybind practiceHotkey()
	{
		return Keybind.NOT_SET;
	}

	@Range(min = 1, max = 28)
	@ConfigItem(
		keyName = "practiceTargets",
		name = "Slots per switch",
		description = "How many empty slots light up each round, like the number of items in a switch",
		position = 2,
		section = practiceSection,
		hidden = true
	)
	default int practiceTargets()
	{
		return 4;
	}

	@Units(Units.MILLISECONDS)
	@Range(max = 5000)
	@ConfigItem(
		keyName = "practiceRoundDelay",
		name = "Time between rounds",
		description = "Pause after a round is done before the next targets light up",
		position = 3,
		section = practiceSection,
		hidden = true
	)
	default int practiceRoundDelay()
	{
		return 1200;
	}

	@ConfigItem(
		keyName = "practiceRandomDelay",
		name = "Random extra pause",
		description = "Add up to one more round delay at random, so you react instead of anticipating",
		position = 4,
		section = practiceSection,
		hidden = true
	)
	default boolean practiceRandomDelay()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "practiceTargetColor",
		name = "Target color",
		description = "Color of the slots to click",
		position = 5,
		section = practiceSection,
		hidden = true
	)
	default Color practiceTargetColor()
	{
		return new Color(255, 150, 30, 150);
	}

	@Alpha
	@ConfigItem(
		keyName = "practiceHitColor",
		name = "Hit color",
		description = "Color a target turns once it is clicked",
		position = 6,
		section = practiceSection,
		hidden = true
	)
	default Color practiceHitColor()
	{
		return new Color(60, 220, 100, 140);
	}

	@Units(Units.SECONDS)
	@ConfigItem(
		keyName = "practiceTimeLimitSeconds",
		name = "Round time limit",
		description = "Time to click every target before the round fails; unclicked targets count as misses."
			+ " 0 for no limit",
		position = 7,
		section = practiceSection,
		hidden = true
	)
	default double practiceTimeLimitSeconds()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "practiceTimerBar",
		name = "Timer bar",
		description = "Show a bar across the top of the inventory that empties as the round time limit runs out",
		position = 8,
		section = practiceSection,
		hidden = true
	)
	default boolean practiceTimerBar()
	{
		return true;
	}

	@Range(max = 500)
	@ConfigItem(
		keyName = "practiceRounds",
		name = "Rounds per session",
		description = "Stop practice after this many rounds and save the session; 0 to keep going until stopped",
		position = 9,
		section = practiceSection,
		hidden = true
	)
	default int practiceRounds()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "practiceOrdered",
		name = "Click in order",
		description = "Targets must be clicked in their numbered order; a click on any other target is a miss",
		position = 10,
		section = practiceSection,
		hidden = true
	)
	default boolean practiceOrdered()
	{
		return false;
	}

	@ConfigItem(
		keyName = "practiceNumbers",
		name = "Number targets",
		description = "Show each target's number; always shown when clicking in order",
		position = 11,
		section = practiceSection,
		hidden = true
	)
	default boolean practiceNumbers()
	{
		return false;
	}

	@ConfigItem(
		keyName = "practiceStatsMode",
		name = "Stats above inventory",
		description = "Practice stats shown above the inventory."
			+ "<br>Last round: switch time, ticks and misses"
			+ "<br>Session: rounds, average and best time, 1-tick rate, accuracy and timeouts",
		position = 12,
		section = practiceSection,
		hidden = true
	)
	default PracticeStats practiceStatsMode()
	{
		return PracticeStats.BOTH;
	}

	// Import/export settings

	@ConfigItem(
		keyName = "copySettings",
		name = "Export settings",
		description = "Tick to export all Click Helpers settings to the clipboard as JSON, to share them."
			+ " The box unticks itself",
		position = 0,
		section = shareSection
	)
	default boolean copySettings()
	{
		return false;
	}

	@ConfigItem(
		keyName = "pasteSettings",
		name = "Import settings",
		description = "Tick to import Click Helpers settings JSON from the clipboard. The box unticks itself",
		position = 1,
		section = shareSection
	)
	default boolean pasteSettings()
	{
		return false;
	}

	@ConfigItem(
		keyName = "sidePanel",
		name = "Side panel",
		description = "Add a Click Helpers side panel with Export and Import buttons and the switch practice"
			+ " progress chart",
		position = 2,
		section = shareSection
	)
	default boolean sidePanel()
	{
		return true;
	}

	// Tab settings

	@ConfigItem(
		keyName = "syncTabSettings",
		name = "Sync tab settings",
		description = "Keep the inventory, prayer and spellbook settings the same: changing one tab's setting"
			+ " changes it in the other tabs too. Turning it on copies the inventory settings to the others",
		position = 0
	)
	default boolean syncTabSettings()
	{
		return false;
	}

	// Inventory: hover

	@ConfigItem(
		keyName = "invStyle",
		name = "Style",
		description = "How the hovered area is drawn",
		position = 0,
		section = invHoverSection
	)
	default HighlightStyle invStyle()
	{
		return HighlightStyle.BOTH;
	}

	@Alpha
	@ConfigItem(
		keyName = "invOutlineColor",
		name = "Outline color",
		description = "Color of the hover outline",
		position = 1,
		section = invHoverSection
	)
	default Color invOutlineColor()
	{
		return new Color(255, 255, 255, 120);
	}

	@Alpha
	@ConfigItem(
		keyName = "invFillColor",
		name = "Fill color",
		description = "Color of the hover fill",
		position = 2,
		section = invHoverSection
	)
	default Color invFillColor()
	{
		return new Color(98, 189, 125, 60);
	}

	@Range(min = 1, max = 4)
	@ConfigItem(
		keyName = "invOutlineWidth",
		name = "Outline width",
		description = "Thickness of outlines in pixels",
		position = 3,
		section = invHoverSection
	)
	default int invOutlineWidth()
	{
		return 1;
	}

	@Range(max = 10)
	@ConfigItem(
		keyName = "invCornerRadius",
		name = "Corner radius",
		description = "Rounds the corners of highlights and shading; 0 for square",
		position = 4,
		section = invHoverSection
	)
	default int invCornerRadius()
	{
		return 4;
	}

	@Units(Units.MILLISECONDS)
	@Range(max = 5000)
	@ConfigItem(
		keyName = "invHoverFade",
		name = "Hover fade-out",
		description = "How long a highlight, including its tick cue, fades after the mouse leaves; 0 to disable",
		position = 5,
		section = invHoverSection
	)
	default int invHoverFade()
	{
		return 400;
	}

	@Units(Units.MILLISECONDS)
	@Range(max = 1000)
	@ConfigItem(
		keyName = "invHoverDelay",
		name = "Hover delay",
		description = "How long the mouse must rest on an area before it is highlighted; 0 for instant",
		position = 6,
		section = invHoverSection
	)
	default int invHoverDelay()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "invHideWhenMenuOpen",
		name = "Hide when right-click menu open",
		description = "Do not highlight while a right-click menu is open",
		position = 7,
		section = invHoverSection
	)
	default boolean invHideWhenMenuOpen()
	{
		return false;
	}

	@ConfigItem(
		keyName = "invDiscoMode",
		name = "Disco mode",
		description = "Pick a random hover color every time the mouse moves to a new area",
		position = 8,
		section = invHoverSection
	)
	default boolean invDiscoMode()
	{
		return false;
	}

	@Units(Units.MILLISECONDS)
	@Range(max = 5000)
	@ConfigItem(
		keyName = "invDiscoInterval",
		name = "Disco interval",
		description = "In disco mode, also pick a new color this often while the mouse rests; 0 to only change on a new area",
		position = 9,
		section = invHoverSection
	)
	default int invDiscoInterval()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "invKeepIconColors",
		name = "Keep icon colors on hover",
		description = "Draw the icon again over the hover fill, so the fill only colors the slot background",
		position = 10,
		section = invHoverSection
	)
	default boolean invKeepIconColors()
	{
		return false;
	}

	@ConfigItem(
		keyName = "invSlotBackground",
		name = "Slot background",
		description = "Color the background of every slot; icons stay on top",
		position = 11,
		section = invHoverSection
	)
	default boolean invSlotBackground()
	{
		return false;
	}

	@Alpha
	@ConfigItem(
		keyName = "invSlotBackgroundColor",
		name = "Slot background color",
		description = "Background color of the slots",
		position = 12,
		section = invHoverSection
	)
	default Color invSlotBackgroundColor()
	{
		return new Color(60, 60, 70, 90);
	}

	// Inventory: spotlight

	@ConfigItem(
		keyName = "invSpotlight",
		name = "Spotlight",
		description = "Light glow at the mouse that dims the panel outward, to show what is in reach",
		position = 0,
		section = invSpotlightSection
	)
	default boolean invSpotlight()
	{
		return false;
	}

	@Range(min = 20, max = 2000)
	@ConfigItem(
		keyName = "invSpotlightRadius",
		name = "Radius",
		description = "Distance in pixels from the mouse where the dimming is full",
		position = 1,
		section = invSpotlightSection
	)
	default int invSpotlightRadius()
	{
		return 90;
	}

	@Alpha
	@ConfigItem(
		keyName = "invSpotlightDim",
		name = "Dim color",
		description = "Color at the edge of the spotlight and beyond",
		position = 2,
		section = invSpotlightSection
	)
	default Color invSpotlightDim()
	{
		return new Color(0, 0, 0, 110);
	}

	@Alpha
	@ConfigItem(
		keyName = "invSpotlightGlow",
		name = "Glow color",
		description = "Color of the glow at the mouse",
		position = 3,
		section = invSpotlightSection
	)
	default Color invSpotlightGlow()
	{
		return new Color(255, 255, 255, 45);
	}

	@Units(Units.PERCENT)
	@Range(max = 100)
	@ConfigItem(
		keyName = "invSpotlightGlowSize",
		name = "Glow size",
		description = "How far the glow reaches, as a share of the radius; 0 for no glow, so only the dimming shows",
		position = 4,
		section = invSpotlightSection
	)
	default int invSpotlightGlowSize()
	{
		return 30;
	}

	@ConfigItem(
		keyName = "invSpotlightAlways",
		name = "When not hovered",
		description = "Also spotlight this panel while the mouse is outside it",
		position = 5,
		section = invSpotlightSection
	)
	default boolean invSpotlightAlways()
	{
		return false;
	}

	@ConfigItem(
		keyName = "invSpotlightDeadOnly",
		name = "Only dead areas",
		description = "Only dim and light the dead areas, so slots keep their colors",
		position = 6,
		section = invSpotlightSection
	)
	default boolean invSpotlightDeadOnly()
	{
		return false;
	}

	@ConfigItem(
		keyName = "invSpotlightFalloff",
		name = "Falloff",
		description = "Past the falloff start, run the spotlight gradient in reverse: the dim fades out, then the falloff color fades in by the falloff end",
		position = 7,
		section = invSpotlightSection
	)
	default boolean invSpotlightFalloff()
	{
		return false;
	}


	@Range(min = 20, max = 4000)
	@ConfigItem(
		keyName = "invSpotlightFalloffDistance",
		name = "Falloff start",
		description = "Distance in pixels from the mouse where the dimming starts fading back out",
		position = 8,
		section = invSpotlightSection
	)
	default int invSpotlightFalloffDistance()
	{
		return 250;
	}

	@Range(min = 20, max = 4000)
	@ConfigItem(
		keyName = "invSpotlightFalloffEnd",
		name = "Falloff end",
		description = "Distance in pixels from the mouse where the dimming is gone; kept past the falloff start",
		position = 9,
		section = invSpotlightSection
	)
	default int invSpotlightFalloffEnd()
	{
		return 400;
	}

	@Alpha
	@ConfigItem(
		keyName = "invSpotlightFalloffColor",
		name = "Falloff color",
		description = "Color the falloff turns into by the falloff end, mirroring the glow at the mouse; kept beyond it."
			+ " Leave its alpha at 0 for the dimming to just fade out",
		position = 10,
		section = invSpotlightSection
	)
	default Color invSpotlightFalloffColor()
	{
		return new Color(0, 0, 0, 0);
	}

	// Inventory: miss dots

	@ConfigItem(
		keyName = "invMissDots",
		name = "Miss dots",
		description = "Leave a small dot at every dead click, so you can see where your misses land",
		position = 0,
		section = invMissDotsSection
	)
	default boolean invMissDots()
	{
		return false;
	}

	@Alpha
	@ConfigItem(
		keyName = "invMissDotColor",
		name = "Color",
		description = "Color of the miss dots",
		position = 1,
		section = invMissDotsSection
	)
	default Color invMissDotColor()
	{
		return new Color(255, 40, 40, 200);
	}

	@ConfigItem(
		keyName = "invMissDotShape",
		name = "Shape",
		description = "Shape of the miss dots",
		position = 2,
		section = invMissDotsSection
	)
	default MissDotShape invMissDotShape()
	{
		return MissDotShape.CIRCLE;
	}

	@ConfigItem(
		keyName = "invMissDotHollow",
		name = "Hollow",
		description = "Draw the miss dots as outlines instead of filled shapes",
		position = 2,
		section = invMissDotsSection
	)
	default boolean invMissDotHollow()
	{
		return false;
	}

	@Range(min = 2, max = 16)
	@ConfigItem(
		keyName = "invMissDotSize",
		name = "Size",
		description = "Size of the miss dots in pixels",
		position = 3,
		section = invMissDotsSection
	)
	default int invMissDotSize()
	{
		return 4;
	}

	@Units(Units.SECONDS)
	@Range(min = 1, max = 600)
	@ConfigItem(
		keyName = "invMissDotDuration",
		name = "Duration",
		description = "How long each miss dot stays; it fades over this time",
		position = 4,
		section = invMissDotsSection
	)
	default int invMissDotDuration()
	{
		return 15;
	}

	// Prayer: hover

	@ConfigItem(
		keyName = "prayerStyle",
		name = "Style",
		description = "How the hovered area is drawn",
		position = 0,
		section = prayerHoverSection
	)
	default HighlightStyle prayerStyle()
	{
		return HighlightStyle.BOTH;
	}

	@Alpha
	@ConfigItem(
		keyName = "prayerOutlineColor",
		name = "Outline color",
		description = "Color of the hover outline",
		position = 1,
		section = prayerHoverSection
	)
	default Color prayerOutlineColor()
	{
		return new Color(255, 255, 255, 120);
	}

	@Alpha
	@ConfigItem(
		keyName = "prayerFillColor",
		name = "Fill color",
		description = "Color of the hover fill",
		position = 2,
		section = prayerHoverSection
	)
	default Color prayerFillColor()
	{
		return new Color(98, 189, 125, 60);
	}

	@Range(min = 1, max = 4)
	@ConfigItem(
		keyName = "prayerOutlineWidth",
		name = "Outline width",
		description = "Thickness of outlines in pixels",
		position = 3,
		section = prayerHoverSection
	)
	default int prayerOutlineWidth()
	{
		return 1;
	}

	@Range(max = 10)
	@ConfigItem(
		keyName = "prayerCornerRadius",
		name = "Corner radius",
		description = "Rounds the corners of highlights and shading; 0 for square",
		position = 4,
		section = prayerHoverSection
	)
	default int prayerCornerRadius()
	{
		return 4;
	}

	@Units(Units.MILLISECONDS)
	@Range(max = 5000)
	@ConfigItem(
		keyName = "prayerHoverFade",
		name = "Hover fade-out",
		description = "How long a highlight, including its tick cue, fades after the mouse leaves; 0 to disable",
		position = 5,
		section = prayerHoverSection
	)
	default int prayerHoverFade()
	{
		return 400;
	}

	@Units(Units.MILLISECONDS)
	@Range(max = 1000)
	@ConfigItem(
		keyName = "prayerHoverDelay",
		name = "Hover delay",
		description = "How long the mouse must rest on an area before it is highlighted; 0 for instant",
		position = 6,
		section = prayerHoverSection
	)
	default int prayerHoverDelay()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "prayerHideWhenMenuOpen",
		name = "Hide when right-click menu open",
		description = "Do not highlight while a right-click menu is open",
		position = 7,
		section = prayerHoverSection
	)
	default boolean prayerHideWhenMenuOpen()
	{
		return false;
	}

	@ConfigItem(
		keyName = "prayerDiscoMode",
		name = "Disco mode",
		description = "Pick a random hover color every time the mouse moves to a new area",
		position = 8,
		section = prayerHoverSection
	)
	default boolean prayerDiscoMode()
	{
		return false;
	}

	@Units(Units.MILLISECONDS)
	@Range(max = 5000)
	@ConfigItem(
		keyName = "prayerDiscoInterval",
		name = "Disco interval",
		description = "In disco mode, also pick a new color this often while the mouse rests; 0 to only change on a new area",
		position = 9,
		section = prayerHoverSection
	)
	default int prayerDiscoInterval()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "prayerKeepIconColors",
		name = "Keep icon colors on hover",
		description = "Draw the icon again over the hover fill, so the fill only colors the slot background",
		position = 10,
		section = prayerHoverSection
	)
	default boolean prayerKeepIconColors()
	{
		return false;
	}

	@ConfigItem(
		keyName = "prayerSlotBackground",
		name = "Slot background",
		description = "Color the background of every slot; icons stay on top",
		position = 11,
		section = prayerHoverSection
	)
	default boolean prayerSlotBackground()
	{
		return false;
	}

	@Alpha
	@ConfigItem(
		keyName = "prayerSlotBackgroundColor",
		name = "Slot background color",
		description = "Background color of the slots",
		position = 12,
		section = prayerHoverSection
	)
	default Color prayerSlotBackgroundColor()
	{
		return new Color(60, 60, 70, 90);
	}

	// Prayer: spotlight

	@ConfigItem(
		keyName = "prayerSpotlight",
		name = "Spotlight",
		description = "Light glow at the mouse that dims the panel outward, to show what is in reach",
		position = 0,
		section = prayerSpotlightSection
	)
	default boolean prayerSpotlight()
	{
		return false;
	}

	@Range(min = 20, max = 2000)
	@ConfigItem(
		keyName = "prayerSpotlightRadius",
		name = "Radius",
		description = "Distance in pixels from the mouse where the dimming is full",
		position = 1,
		section = prayerSpotlightSection
	)
	default int prayerSpotlightRadius()
	{
		return 90;
	}

	@Alpha
	@ConfigItem(
		keyName = "prayerSpotlightDim",
		name = "Dim color",
		description = "Color at the edge of the spotlight and beyond",
		position = 2,
		section = prayerSpotlightSection
	)
	default Color prayerSpotlightDim()
	{
		return new Color(0, 0, 0, 110);
	}

	@Alpha
	@ConfigItem(
		keyName = "prayerSpotlightGlow",
		name = "Glow color",
		description = "Color of the glow at the mouse",
		position = 3,
		section = prayerSpotlightSection
	)
	default Color prayerSpotlightGlow()
	{
		return new Color(255, 255, 255, 45);
	}

	@Units(Units.PERCENT)
	@Range(max = 100)
	@ConfigItem(
		keyName = "prayerSpotlightGlowSize",
		name = "Glow size",
		description = "How far the glow reaches, as a share of the radius; 0 for no glow, so only the dimming shows",
		position = 4,
		section = prayerSpotlightSection
	)
	default int prayerSpotlightGlowSize()
	{
		return 30;
	}

	@ConfigItem(
		keyName = "prayerSpotlightAlways",
		name = "When not hovered",
		description = "Also spotlight this panel while the mouse is outside it",
		position = 5,
		section = prayerSpotlightSection
	)
	default boolean prayerSpotlightAlways()
	{
		return false;
	}

	@ConfigItem(
		keyName = "prayerSpotlightDeadOnly",
		name = "Only dead areas",
		description = "Only dim and light the dead areas, so slots keep their colors",
		position = 6,
		section = prayerSpotlightSection
	)
	default boolean prayerSpotlightDeadOnly()
	{
		return false;
	}

	@ConfigItem(
		keyName = "prayerSpotlightFalloff",
		name = "Falloff",
		description = "Past the falloff start, run the spotlight gradient in reverse: the dim fades out, then the falloff color fades in by the falloff end",
		position = 7,
		section = prayerSpotlightSection
	)
	default boolean prayerSpotlightFalloff()
	{
		return false;
	}


	@Range(min = 20, max = 4000)
	@ConfigItem(
		keyName = "prayerSpotlightFalloffDistance",
		name = "Falloff start",
		description = "Distance in pixels from the mouse where the dimming starts fading back out",
		position = 8,
		section = prayerSpotlightSection
	)
	default int prayerSpotlightFalloffDistance()
	{
		return 250;
	}

	@Range(min = 20, max = 4000)
	@ConfigItem(
		keyName = "prayerSpotlightFalloffEnd",
		name = "Falloff end",
		description = "Distance in pixels from the mouse where the dimming is gone; kept past the falloff start",
		position = 9,
		section = prayerSpotlightSection
	)
	default int prayerSpotlightFalloffEnd()
	{
		return 400;
	}

	@Alpha
	@ConfigItem(
		keyName = "prayerSpotlightFalloffColor",
		name = "Falloff color",
		description = "Color the falloff turns into by the falloff end, mirroring the glow at the mouse; kept beyond it."
			+ " Leave its alpha at 0 for the dimming to just fade out",
		position = 10,
		section = prayerSpotlightSection
	)
	default Color prayerSpotlightFalloffColor()
	{
		return new Color(0, 0, 0, 0);
	}

	// Prayer: miss dots

	@ConfigItem(
		keyName = "prayerMissDots",
		name = "Miss dots",
		description = "Leave a small dot at every dead click, so you can see where your misses land",
		position = 0,
		section = prayerMissDotsSection
	)
	default boolean prayerMissDots()
	{
		return false;
	}

	@Alpha
	@ConfigItem(
		keyName = "prayerMissDotColor",
		name = "Color",
		description = "Color of the miss dots",
		position = 1,
		section = prayerMissDotsSection
	)
	default Color prayerMissDotColor()
	{
		return new Color(255, 40, 40, 200);
	}

	@ConfigItem(
		keyName = "prayerMissDotShape",
		name = "Shape",
		description = "Shape of the miss dots",
		position = 2,
		section = prayerMissDotsSection
	)
	default MissDotShape prayerMissDotShape()
	{
		return MissDotShape.CIRCLE;
	}

	@ConfigItem(
		keyName = "prayerMissDotHollow",
		name = "Hollow",
		description = "Draw the miss dots as outlines instead of filled shapes",
		position = 2,
		section = prayerMissDotsSection
	)
	default boolean prayerMissDotHollow()
	{
		return false;
	}

	@Range(min = 2, max = 16)
	@ConfigItem(
		keyName = "prayerMissDotSize",
		name = "Size",
		description = "Size of the miss dots in pixels",
		position = 3,
		section = prayerMissDotsSection
	)
	default int prayerMissDotSize()
	{
		return 4;
	}

	@Units(Units.SECONDS)
	@Range(min = 1, max = 600)
	@ConfigItem(
		keyName = "prayerMissDotDuration",
		name = "Duration",
		description = "How long each miss dot stays; it fades over this time",
		position = 4,
		section = prayerMissDotsSection
	)
	default int prayerMissDotDuration()
	{
		return 15;
	}

	// Spellbook: hover

	@ConfigItem(
		keyName = "spellStyle",
		name = "Style",
		description = "How the hovered area is drawn",
		position = 0,
		section = spellHoverSection
	)
	default HighlightStyle spellStyle()
	{
		return HighlightStyle.BOTH;
	}

	@Alpha
	@ConfigItem(
		keyName = "spellOutlineColor",
		name = "Outline color",
		description = "Color of the hover outline",
		position = 1,
		section = spellHoverSection
	)
	default Color spellOutlineColor()
	{
		return new Color(255, 255, 255, 120);
	}

	@Alpha
	@ConfigItem(
		keyName = "spellFillColor",
		name = "Fill color",
		description = "Color of the hover fill",
		position = 2,
		section = spellHoverSection
	)
	default Color spellFillColor()
	{
		return new Color(98, 189, 125, 60);
	}

	@Range(min = 1, max = 4)
	@ConfigItem(
		keyName = "spellOutlineWidth",
		name = "Outline width",
		description = "Thickness of outlines in pixels",
		position = 3,
		section = spellHoverSection
	)
	default int spellOutlineWidth()
	{
		return 1;
	}

	@Range(max = 10)
	@ConfigItem(
		keyName = "spellCornerRadius",
		name = "Corner radius",
		description = "Rounds the corners of highlights and shading; 0 for square",
		position = 4,
		section = spellHoverSection
	)
	default int spellCornerRadius()
	{
		return 4;
	}

	@Units(Units.MILLISECONDS)
	@Range(max = 5000)
	@ConfigItem(
		keyName = "spellHoverFade",
		name = "Hover fade-out",
		description = "How long a highlight, including its tick cue, fades after the mouse leaves; 0 to disable",
		position = 5,
		section = spellHoverSection
	)
	default int spellHoverFade()
	{
		return 400;
	}

	@Units(Units.MILLISECONDS)
	@Range(max = 1000)
	@ConfigItem(
		keyName = "spellHoverDelay",
		name = "Hover delay",
		description = "How long the mouse must rest on an area before it is highlighted; 0 for instant",
		position = 6,
		section = spellHoverSection
	)
	default int spellHoverDelay()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "spellHideWhenMenuOpen",
		name = "Hide when right-click menu open",
		description = "Do not highlight while a right-click menu is open",
		position = 7,
		section = spellHoverSection
	)
	default boolean spellHideWhenMenuOpen()
	{
		return false;
	}

	@ConfigItem(
		keyName = "spellDiscoMode",
		name = "Disco mode",
		description = "Pick a random hover color every time the mouse moves to a new area",
		position = 8,
		section = spellHoverSection
	)
	default boolean spellDiscoMode()
	{
		return false;
	}

	@Units(Units.MILLISECONDS)
	@Range(max = 5000)
	@ConfigItem(
		keyName = "spellDiscoInterval",
		name = "Disco interval",
		description = "In disco mode, also pick a new color this often while the mouse rests; 0 to only change on a new area",
		position = 9,
		section = spellHoverSection
	)
	default int spellDiscoInterval()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "spellKeepIconColors",
		name = "Keep icon colors on hover",
		description = "Draw the icon again over the hover fill, so the fill only colors the slot background",
		position = 10,
		section = spellHoverSection
	)
	default boolean spellKeepIconColors()
	{
		return false;
	}

	@ConfigItem(
		keyName = "spellSlotBackground",
		name = "Slot background",
		description = "Color the background of every slot; icons stay on top",
		position = 11,
		section = spellHoverSection
	)
	default boolean spellSlotBackground()
	{
		return false;
	}

	@Alpha
	@ConfigItem(
		keyName = "spellSlotBackgroundColor",
		name = "Slot background color",
		description = "Background color of the slots",
		position = 12,
		section = spellHoverSection
	)
	default Color spellSlotBackgroundColor()
	{
		return new Color(60, 60, 70, 90);
	}

	// Spellbook: spotlight

	@ConfigItem(
		keyName = "spellSpotlight",
		name = "Spotlight",
		description = "Light glow at the mouse that dims the panel outward, to show what is in reach",
		position = 0,
		section = spellSpotlightSection
	)
	default boolean spellSpotlight()
	{
		return false;
	}

	@Range(min = 20, max = 2000)
	@ConfigItem(
		keyName = "spellSpotlightRadius",
		name = "Radius",
		description = "Distance in pixels from the mouse where the dimming is full",
		position = 1,
		section = spellSpotlightSection
	)
	default int spellSpotlightRadius()
	{
		return 90;
	}

	@Alpha
	@ConfigItem(
		keyName = "spellSpotlightDim",
		name = "Dim color",
		description = "Color at the edge of the spotlight and beyond",
		position = 2,
		section = spellSpotlightSection
	)
	default Color spellSpotlightDim()
	{
		return new Color(0, 0, 0, 110);
	}

	@Alpha
	@ConfigItem(
		keyName = "spellSpotlightGlow",
		name = "Glow color",
		description = "Color of the glow at the mouse",
		position = 3,
		section = spellSpotlightSection
	)
	default Color spellSpotlightGlow()
	{
		return new Color(255, 255, 255, 45);
	}

	@Units(Units.PERCENT)
	@Range(max = 100)
	@ConfigItem(
		keyName = "spellSpotlightGlowSize",
		name = "Glow size",
		description = "How far the glow reaches, as a share of the radius; 0 for no glow, so only the dimming shows",
		position = 4,
		section = spellSpotlightSection
	)
	default int spellSpotlightGlowSize()
	{
		return 30;
	}

	@ConfigItem(
		keyName = "spellSpotlightAlways",
		name = "When not hovered",
		description = "Also spotlight this panel while the mouse is outside it",
		position = 5,
		section = spellSpotlightSection
	)
	default boolean spellSpotlightAlways()
	{
		return false;
	}

	@ConfigItem(
		keyName = "spellSpotlightDeadOnly",
		name = "Only dead areas",
		description = "Only dim and light the dead areas, so slots keep their colors",
		position = 6,
		section = spellSpotlightSection
	)
	default boolean spellSpotlightDeadOnly()
	{
		return false;
	}

	@ConfigItem(
		keyName = "spellSpotlightFalloff",
		name = "Falloff",
		description = "Past the falloff start, run the spotlight gradient in reverse: the dim fades out, then the falloff color fades in by the falloff end",
		position = 7,
		section = spellSpotlightSection
	)
	default boolean spellSpotlightFalloff()
	{
		return false;
	}


	@Range(min = 20, max = 4000)
	@ConfigItem(
		keyName = "spellSpotlightFalloffDistance",
		name = "Falloff start",
		description = "Distance in pixels from the mouse where the dimming starts fading back out",
		position = 8,
		section = spellSpotlightSection
	)
	default int spellSpotlightFalloffDistance()
	{
		return 250;
	}

	@Range(min = 20, max = 4000)
	@ConfigItem(
		keyName = "spellSpotlightFalloffEnd",
		name = "Falloff end",
		description = "Distance in pixels from the mouse where the dimming is gone; kept past the falloff start",
		position = 9,
		section = spellSpotlightSection
	)
	default int spellSpotlightFalloffEnd()
	{
		return 400;
	}

	@Alpha
	@ConfigItem(
		keyName = "spellSpotlightFalloffColor",
		name = "Falloff color",
		description = "Color the falloff turns into by the falloff end, mirroring the glow at the mouse; kept beyond it."
			+ " Leave its alpha at 0 for the dimming to just fade out",
		position = 10,
		section = spellSpotlightSection
	)
	default Color spellSpotlightFalloffColor()
	{
		return new Color(0, 0, 0, 0);
	}

	// Spellbook: miss dots

	@ConfigItem(
		keyName = "spellMissDots",
		name = "Miss dots",
		description = "Leave a small dot at every dead click, so you can see where your misses land",
		position = 0,
		section = spellMissDotsSection
	)
	default boolean spellMissDots()
	{
		return false;
	}

	@Alpha
	@ConfigItem(
		keyName = "spellMissDotColor",
		name = "Color",
		description = "Color of the miss dots",
		position = 1,
		section = spellMissDotsSection
	)
	default Color spellMissDotColor()
	{
		return new Color(255, 40, 40, 200);
	}

	@ConfigItem(
		keyName = "spellMissDotShape",
		name = "Shape",
		description = "Shape of the miss dots",
		position = 2,
		section = spellMissDotsSection
	)
	default MissDotShape spellMissDotShape()
	{
		return MissDotShape.CIRCLE;
	}

	@ConfigItem(
		keyName = "spellMissDotHollow",
		name = "Hollow",
		description = "Draw the miss dots as outlines instead of filled shapes",
		position = 2,
		section = spellMissDotsSection
	)
	default boolean spellMissDotHollow()
	{
		return false;
	}

	@Range(min = 2, max = 16)
	@ConfigItem(
		keyName = "spellMissDotSize",
		name = "Size",
		description = "Size of the miss dots in pixels",
		position = 3,
		section = spellMissDotsSection
	)
	default int spellMissDotSize()
	{
		return 4;
	}

	@Units(Units.SECONDS)
	@Range(min = 1, max = 600)
	@ConfigItem(
		keyName = "spellMissDotDuration",
		name = "Duration",
		description = "How long each miss dot stays; it fades over this time",
		position = 4,
		section = spellMissDotsSection
	)
	default int spellMissDotDuration()
	{
		return 15;
	}
}
