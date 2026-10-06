package com.inventoryhover;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class InventoryHoverHighlightPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(InventoryHoverHighlightPlugin.class);
		RuneLite.main(args);
	}
}
