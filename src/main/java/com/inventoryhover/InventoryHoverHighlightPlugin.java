package com.inventoryhover;

import com.google.inject.Provides;
import javax.inject.Inject;
import net.runelite.client.config.ConfigManager;
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
	@Inject
	private OverlayManager overlayManager;

	@Inject
	private InventoryHoverHighlightOverlay overlay;

	@Provides
	InventoryHoverHighlightConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(InventoryHoverHighlightConfig.class);
	}

	@Override
	protected void startUp()
	{
		overlayManager.add(overlay);
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
	}
}
