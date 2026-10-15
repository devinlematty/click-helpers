package com.inventoryhover;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import net.runelite.client.config.ConfigDescriptor;
import net.runelite.client.config.ConfigItemDescriptor;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.ConfigSectionDescriptor;

/**
 * Copies the plugin's settings to the clipboard as readable JSON, grouped by
 * settings section, and applies settings pasted back from the clipboard. Uses
 * only the clipboard and the config manager, never files.
 */
class SettingsSharing
{
	private static final String EXPORT_MARKER = "click-helpers";
	private static final int EXPORT_VERSION = 1;
	private static final int MAX_IMPORT_CHARS = 64 * 1024;

	/** Action toggles that must never be copied or pasted. */
	private static final Set<String> SKIPPED_KEYS = Set.of("copySettings", "pasteSettings");

	private final InventoryHoverHighlightConfig config;
	private final ConfigManager configManager;
	private final Gson gson;

	@Inject
	SettingsSharing(InventoryHoverHighlightConfig config, ConfigManager configManager, Gson gson)
	{
		this.config = config;
		this.configManager = configManager;
		this.gson = gson;
	}

	/** Copies the settings to the clipboard; returns a message for the player. */
	String copyToClipboard()
	{
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(exportJson()), null);
		return "Click Helpers settings exported to the clipboard.";
	}

	/** Applies settings from the clipboard; returns a message for the player. */
	String pasteFromClipboard()
	{
		String json;
		try
		{
			json = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
		}
		catch (Exception e)
		{
			return "Nothing to import: the clipboard has no text.";
		}

		if (json == null || json.length() > MAX_IMPORT_CHARS)
		{
			return "The clipboard does not hold Click Helpers settings.";
		}

		try
		{
			return "Imported " + importJson(json) + " Click Helpers settings.";
		}
		catch (RuntimeException e)
		{
			return "The clipboard does not hold Click Helpers settings.";
		}
	}

	/**
	 * Every stored setting of this plugin, grouped under its settings section
	 * name in the order the config panel shows them, so the export reads like
	 * the panel.
	 */
	private JsonObject currentSettings()
	{
		ConfigDescriptor descriptor = configManager.getConfigDescriptor(config);
		Map<String, String> sectionNames = new HashMap<>();
		JsonObject settings = new JsonObject();
		descriptor.getSections().stream()
			.sorted(Comparator.comparingInt(ConfigSectionDescriptor::position))
			.forEach(section ->
			{
				sectionNames.put(section.key(), section.name());
				settings.add(section.name(), new JsonObject());
			});

		descriptor.getItems().stream()
			.sorted(Comparator.comparingInt(ConfigItemDescriptor::position))
			.filter(item -> !SKIPPED_KEYS.contains(item.key()))
			.forEach(item ->
			{
				String value = configManager.getConfiguration(InventoryHoverHighlightConfig.GROUP, item.key());
				if (value == null)
				{
					return;
				}
				String section = sectionNames.getOrDefault(item.getItem().section(), "Other");
				if (!settings.has(section))
				{
					settings.add(section, new JsonObject());
				}
				settings.getAsJsonObject(section).addProperty(item.key(), value);
			});

		// Leave out sections with nothing stored
		for (String section : new ArrayList<>(settings.keySet()))
		{
			if (settings.getAsJsonObject(section).size() == 0)
			{
				settings.remove(section);
			}
		}
		return settings;
	}

	private Set<String> settingKeys()
	{
		return configManager.getConfigDescriptor(config).getItems().stream()
			.map(ConfigItemDescriptor::key)
			.filter(key -> !SKIPPED_KEYS.contains(key))
			.collect(Collectors.toSet());
	}

	private String exportJson()
	{
		JsonObject root = new JsonObject();
		root.addProperty("plugin", EXPORT_MARKER);
		root.addProperty("version", EXPORT_VERSION);
		root.add("settings", currentSettings());
		return gson.newBuilder().setPrettyPrinting().create().toJson(root);
	}

	/**
	 * Applies exported settings; returns how many were applied. Settings may be
	 * grouped by section or flat. Unknown keys are skipped.
	 */
	private int importJson(String json)
	{
		JsonObject root = gson.fromJson(json, JsonObject.class);
		if (root == null || !root.has("plugin") || !EXPORT_MARKER.equals(root.get("plugin").getAsString())
			|| !root.has("settings") || !root.get("settings").isJsonObject())
		{
			throw new JsonParseException("Not Click Helpers settings");
		}

		Map<String, String> values = new HashMap<>();
		for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("settings").entrySet())
		{
			if (entry.getValue().isJsonObject())
			{
				for (Map.Entry<String, JsonElement> inner : entry.getValue().getAsJsonObject().entrySet())
				{
					if (inner.getValue().isJsonPrimitive())
					{
						values.put(inner.getKey(), inner.getValue().getAsString());
					}
				}
			}
			else if (entry.getValue().isJsonPrimitive())
			{
				values.put(entry.getKey(), entry.getValue().getAsString());
			}
		}

		Set<String> known = settingKeys();
		int applied = 0;
		for (Map.Entry<String, String> entry : values.entrySet())
		{
			if (known.contains(entry.getKey()))
			{
				configManager.setConfiguration(InventoryHoverHighlightConfig.GROUP, entry.getKey(), entry.getValue());
				applied++;
			}
		}
		return applied;
	}
}
