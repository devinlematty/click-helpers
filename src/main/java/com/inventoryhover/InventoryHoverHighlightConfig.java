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

	@ConfigSection(
		name = "Hover highlight",
		description = "How the highlight under the mouse looks",
		position = 0
	)
	String hoverSection = "hover";

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
		name = "Spotlight",
		description = "Glow at the mouse that dims the panel outward",
		position = 10,
		closedByDefault = true
	)
	String spotlightSection = "spotlight";

	@ConfigSection(
		name = "Import/export settings",
		description = "Export all settings to the clipboard as JSON, or import shared JSON settings",
		position = 11,
		closedByDefault = true
	)
	String shareSection = "share";


	// Hover highlight

	@ConfigItem(
		keyName = "style",
		name = "Style",
		description = "How the hovered slot is drawn",
		position = 0,
		section = hoverSection
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
		section = hoverSection
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
		section = hoverSection
	)
	default Color fillColor()
	{
		return new Color(98, 189, 125, 60);
	}

	@ConfigItem(
		keyName = "hideWhenMenuOpen",
		name = "Hide when right-click menu open",
		description = "Do not highlight while a right-click menu is open",
		position = 3,
		section = hoverSection
	)
	default boolean hideWhenMenuOpen()
	{
		return false;
	}

	@Units(Units.MILLISECONDS)
	@Range(max = 5000)
	@ConfigItem(
		keyName = "hoverFade",
		name = "Hover fade-out",
		description = "How long a highlight, including its tick cue, fades after the mouse leaves; 0 to disable",
		position = 4,
		section = hoverSection
	)
	default int hoverFade()
	{
		return 400;
	}

	@Units(Units.MILLISECONDS)
	@Range(max = 1000)
	@ConfigItem(
		keyName = "hoverDelay",
		name = "Hover delay",
		description = "How long the mouse must rest on an area before it is highlighted; 0 for instant",
		position = 5,
		section = hoverSection
	)
	default int hoverDelay()
	{
		return 0;
	}

	@Range(max = 10)
	@ConfigItem(
		keyName = "cornerRadius",
		name = "Corner radius",
		description = "Rounds the corners of the highlight; 0 for square",
		position = 6,
		section = hoverSection
	)
	default int cornerRadius()
	{
		return 4;
	}

	@ConfigItem(
		keyName = "roundedAreas",
		name = "Force clickable area to highlight radius",
		description = "Round the clickable grid and the dead area shading with the hover highlight's corner radius."
			+ "<br>Note: the game's clickable areas are square, so slot corners can look dead even though"
			+ " clicks there still work",
		position = 7,
		section = hoverSection
	)
	default boolean roundedAreas()
	{
		return true;
	}

	@Range(min = 1, max = 4)
	@ConfigItem(
		keyName = "outlineWidth",
		name = "Outline width",
		description = "Thickness of the outline in pixels",
		position = 8,
		section = hoverSection
	)
	default int outlineWidth()
	{
		return 1;
	}

	@ConfigItem(
		keyName = "discoMode",
		name = "Disco mode",
		description = "Pick a random hover color every time the mouse moves to a new area",
		position = 9,
		section = hoverSection
	)
	default boolean discoMode()
	{
		return false;
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
		keyName = "missDots",
		name = "Miss dots",
		description = "Leave a small dot at every dead click, so you can see where your misses land",
		position = 2,
		section = missSection
	)
	default boolean missDots()
	{
		return false;
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
		keyName = "missDotColor",
		name = "Miss dot color",
		description = "Color of the miss dots",
		position = 8,
		section = missSection
	)
	default Color missDotColor()
	{
		return new Color(255, 40, 40, 200);
	}

	@Units(Units.SECONDS)
	@Range(min = 1, max = 600)
	@ConfigItem(
		keyName = "missDotDuration",
		name = "Miss dot duration",
		description = "How long each miss dot stays before it is gone; it fades over this time",
		position = 9,
		section = missSection
	)
	default int missDotDuration()
	{
		return 15;
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

	@ConfigItem(
		keyName = "spotlight",
		name = "Spotlight",
		description = "Light glow at the mouse that dims the panel outward, to show what is in reach",
		position = 0,
		section = spotlightSection
	)
	default boolean spotlight()
	{
		return false;
	}

	@Range(min = 20, max = 300)
	@ConfigItem(
		keyName = "spotlightRadius",
		name = "Spotlight radius",
		description = "Distance in pixels from the mouse where the dimming is full",
		position = 1,
		section = spotlightSection
	)
	default int spotlightRadius()
	{
		return 90;
	}

	@Alpha
	@ConfigItem(
		keyName = "spotlightDim",
		name = "Spotlight dim",
		description = "Color at the edge of the spotlight and beyond",
		position = 2,
		section = spotlightSection
	)
	default Color spotlightDim()
	{
		return new Color(0, 0, 0, 110);
	}

	@Alpha
	@ConfigItem(
		keyName = "spotlightGlow",
		name = "Spotlight glow",
		description = "Color of the glow at the mouse",
		position = 3,
		section = spotlightSection
	)
	default Color spotlightGlow()
	{
		return new Color(255, 255, 255, 45);
	}

	// Import/export settings

	@ConfigItem(
		keyName = "copySettings",
		name = "Export settings",
		description = "Tick to export all Click Helpers settings to the clipboard as JSON, to share them",
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
		description = "Tick to import Click Helpers settings JSON from the clipboard",
		position = 1,
		section = shareSection
	)
	default boolean pasteSettings()
	{
		return false;
	}
}
