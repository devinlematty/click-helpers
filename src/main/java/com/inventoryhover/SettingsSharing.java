package com.inventoryhover;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.awt.Color;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
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

	/** Config key holding the loadouts as a JSON object of name to exported settings. */
	private static final String LOADOUTS_KEY = "loadouts";

	/** Settings that start something and must never be copied or pasted. */
	private static final Set<String> SKIPPED_KEYS = Set.of("copySettings", "pasteSettings", "practiceMode");

	private static final List<String> TAB_PREFIXES = TabSettings.PREFIXES;

	private final InventoryHoverHighlightConfig config;
	private final ConfigManager configManager;

	/** Keys of the tab settings, found on first use. */
	private Set<String> tabKeys;
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

	/** Copies a changed tab setting to the same setting of the other tabs; other keys are ignored. */
	void syncTabSetting(String key, String value)
	{
		String prefix = tabPrefixOf(key);
		if (prefix == null)
		{
			return;
		}

		String suffix = key.substring(prefix.length());
		for (String other : TAB_PREFIXES)
		{
			String target = other + suffix;
			// Stop when the value already matches, so the change events of the copies end here
			if (other.equals(prefix) || Objects.equals(value, configManager.getConfiguration(InventoryHoverHighlightConfig.GROUP, target)))
			{
				continue;
			}
			if (value == null)
			{
				configManager.unsetConfiguration(InventoryHoverHighlightConfig.GROUP, target);
			}
			else
			{
				configManager.setConfiguration(InventoryHoverHighlightConfig.GROUP, target, value);
			}
		}
	}

	/** Copies every inventory tab setting to the other tabs. */
	void syncAllTabs()
	{
		for (String key : tabKeys())
		{
			if (key.startsWith(TabSettings.INVENTORY))
			{
				syncTabSetting(key, configManager.getConfiguration(InventoryHoverHighlightConfig.GROUP, key));
			}
		}
	}

	/** The tab prefix of a tab setting key, or null for any other key. */
	private String tabPrefixOf(String key)
	{
		if (!tabKeys().contains(key))
		{
			return null;
		}
		for (String prefix : TAB_PREFIXES)
		{
			if (key.startsWith(prefix))
			{
				return prefix;
			}
		}
		return null;
	}

	/** Keys of every setting in a tab section. Matched by section, since general keys like "prayers" share a prefix. */
	private Set<String> tabKeys()
	{
		if (tabKeys == null)
		{
			Set<String> sections = TabSettings.SECTIONS;
			tabKeys = configManager.getConfigDescriptor(config).getItems().stream()
				.filter(item -> sections.contains(item.getItem().section()))
				.map(ConfigItemDescriptor::key)
				.collect(Collectors.toSet());
		}
		return tabKeys;
	}

	private Set<String> settingKeys()
	{
		return configManager.getConfigDescriptor(config).getItems().stream()
			.map(ConfigItemDescriptor::key)
			.filter(key -> !SKIPPED_KEYS.contains(key))
			.collect(Collectors.toSet());
	}

	/** Sections randomize leaves alone: the click chart and the import/export switches. */
	private static final Set<String> NOT_RANDOMIZED_SECTIONS = Set.of(InventoryHoverHighlightConfig.chartSection,
		InventoryHoverHighlightConfig.shareSection);

	/**
	 * Sets every on/off, number, color and choice setting to a random value,
	 * with each tab randomized on its own; returns a message. The click chart,
	 * import/export switches, hotkeys, text lists and hidden settings stay.
	 */
	String randomizeAll()
	{
		Random random = new Random();
		// Sync would copy each tab's random values over the others, so turn it off first
		configManager.setConfiguration(InventoryHoverHighlightConfig.GROUP, "syncTabSettings", false);
		int changed = 0;
		for (ConfigItemDescriptor item : configManager.getConfigDescriptor(config).getItems())
		{
			if (SKIPPED_KEYS.contains(item.key()) || item.key().equals("syncTabSettings") || item.getItem().hidden()
				|| NOT_RANDOMIZED_SECTIONS.contains(item.getItem().section()))
			{
				continue;
			}

			Object value;
			Type type = item.getType();
			if (type == boolean.class || type == Boolean.class)
			{
				value = random.nextBoolean();
			}
			else if (type == int.class || type == Integer.class)
			{
				int min = item.getRange() == null ? 0 : item.getRange().min();
				int max = item.getRange() == null ? 100 : Math.min(item.getRange().max(), 100_000);
				value = min + random.nextInt(Math.max(1, max - min + 1));
			}
			else if (type == Color.class)
			{
				value = new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256),
					item.getAlpha() == null ? 255 : 40 + random.nextInt(216));
			}
			else if (type instanceof Class && ((Class<?>) type).isEnum())
			{
				Object[] options = ((Class<?>) type).getEnumConstants();
				value = options[random.nextInt(options.length)];
			}
			else
			{
				continue;
			}
			configManager.setConfiguration(InventoryHoverHighlightConfig.GROUP, item.key(), value);
			changed++;
		}
		return "Randomized " + changed + " settings and turned off tab sync. Load a loadout to go back.";
	}

	// Loadouts: named snapshots of every setting, kept in the plugin's own config, never in files

	/** Loadout names, in the order they were saved. */
	List<String> loadoutNames()
	{
		return new ArrayList<>(loadouts().keySet());
	}

	/** Saves every current setting under a name, replacing a loadout with that name; returns a message. */
	String saveLoadout(String name)
	{
		name = name == null ? "" : name.trim();
		if (name.isEmpty())
		{
			return "Type a name for the loadout first.";
		}
		JsonObject all = loadouts();
		boolean replaced = all.has(name);
		all.add(name, gson.fromJson(exportJson(), JsonObject.class));
		configManager.setConfiguration(InventoryHoverHighlightConfig.GROUP, LOADOUTS_KEY, gson.toJson(all));
		return (replaced ? "Updated loadout " : "Saved loadout ") + name + ".";
	}

	/** Applies a saved loadout; returns a message. */
	String loadLoadout(String name)
	{
		JsonObject all = loadouts();
		if (name == null || !all.has(name))
		{
			return "Pick a loadout to load.";
		}
		int applied = importJson(gson.toJson(all.get(name)));
		return "Loaded loadout " + name + " (" + applied + " settings).";
	}

	/** Deletes a saved loadout; returns a message. */
	String deleteLoadout(String name)
	{
		JsonObject all = loadouts();
		if (name == null || all.remove(name) == null)
		{
			return "Pick a loadout to delete.";
		}
		configManager.setConfiguration(InventoryHoverHighlightConfig.GROUP, LOADOUTS_KEY, gson.toJson(all));
		return "Deleted loadout " + name + ".";
	}

	private JsonObject loadouts()
	{
		String json = configManager.getConfiguration(InventoryHoverHighlightConfig.GROUP, LOADOUTS_KEY);
		try
		{
			JsonObject all = json == null ? null : gson.fromJson(json, JsonObject.class);
			return all == null ? new JsonObject() : all;
		}
		catch (JsonParseException e)
		{
			return new JsonObject();
		}
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
