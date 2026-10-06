package com.inventoryhover;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

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

	@ConfigItem(
		keyName = "style",
		name = "Style",
		description = "How the hovered slot is drawn",
		position = 0
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
		position = 1
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
		position = 2
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
		position = 3
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
		position = 3
	)
	default int outlineWidth()
	{
		return 1;
	}

	@ConfigItem(
		keyName = "onlyItems",
		name = "Only slots with items",
		description = "Skip empty slots",
		position = 4
	)
	default boolean onlyItems()
	{
		return true;
	}

	@ConfigItem(
		keyName = "bankInventory",
		name = "Bank inventory",
		description = "Also highlight the inventory shown next to the bank",
		position = 5
	)
	default boolean bankInventory()
	{
		return true;
	}

	@ConfigItem(
		keyName = "prayers",
		name = "Prayers",
		description = "Also highlight the hovered prayer in the prayer tab",
		position = 6
	)
	default boolean prayers()
	{
		return true;
	}

	@ConfigItem(
		keyName = "spells",
		name = "Spells",
		description = "Also highlight the hovered spell in the spellbook",
		position = 7
	)
	default boolean spells()
	{
		return true;
	}

	@ConfigItem(
		keyName = "hideWhenMenuOpen",
		name = "Hide when right-click menu open",
		description = "Do not highlight while a right-click menu is open",
		position = 8
	)
	default boolean hideWhenMenuOpen()
	{
		return true;
	}
}
