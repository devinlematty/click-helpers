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
		return new Color(255, 255, 0, 220);
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
		return new Color(255, 255, 0, 50);
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
		keyName = "hideWhenMenuOpen",
		name = "Hide when menu open",
		description = "Do not highlight while a right-click menu is open",
		position = 6
	)
	default boolean hideWhenMenuOpen()
	{
		return true;
	}
}
