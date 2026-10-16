package com.inventoryhover;

import java.util.List;

/**
 * The settings sections, in the order the side panel shows them. Their names
 * are also the group names in exported and published settings, so they must
 * not change. Only the import/export section is still a RuneLite config
 * section; every other setting is hidden from RuneLite's panel and edited in
 * the side panel instead.
 */
final class SettingsSections
{
	/** One section: its key, as in each setting's section, and its shown name. */
	static final class Section
	{
		final String key;
		final String name;
		final String description;

		Section(String key, String name, String description)
		{
			this.key = key;
			this.name = name;
			this.description = description;
		}
	}

	static final List<Section> ALL = List.of(
		new Section(InventoryHoverHighlightConfig.areasSection, "Areas", "Where the highlight and clickable grid are shown"),
		new Section(InventoryHoverHighlightConfig.clickSection, "Click feedback", "Flash and marker on the slot you click"),
		new Section(InventoryHoverHighlightConfig.missSection, "Missed clicks", "Markers for clicks that do nothing or are refused"),
		new Section(InventoryHoverHighlightConfig.deadSection, "Dead areas", "Shading over the gaps where a click does nothing, per panel"),
		new Section(InventoryHoverHighlightConfig.prayerSection, "Prayers and spells", "Active prayer, selected spell and autocast highlights"),
		new Section(InventoryHoverHighlightConfig.tickSection, "Tick cue", "Timing cue on prayers to help with prayer flicking"),
		new Section(InventoryHoverHighlightConfig.chartSection, "Click chart", "Floating chart of your recent good, missed, refused and double clicks"),
		new Section(InventoryHoverHighlightConfig.itemsSection, "Hide items", "Hide items you do not click in a fight"),
		new Section(InventoryHoverHighlightConfig.delaySection, "Eat and drink delay", "Shade food and potions while another one would be ignored"),
		new Section(InventoryHoverHighlightConfig.practiceSection, "Switch practice", "Hotkey for switch practice; the other practice settings are in the Click Helpers side panel"),
		new Section(InventoryHoverHighlightConfig.shareSection, "Import/export settings", "Export all settings to the clipboard as JSON, import shared JSON settings, and the side panel"),
		new Section(InventoryHoverHighlightConfig.invHoverSection, "Inventory: hover", "Inventory and bank inventory hover settings"),
		new Section(InventoryHoverHighlightConfig.invSpotlightSection, "Inventory: spotlight", "Inventory and bank inventory spotlight settings"),
		new Section(InventoryHoverHighlightConfig.invMissDotsSection, "Inventory: miss dots", "Inventory and bank inventory miss dots settings"),
		new Section(InventoryHoverHighlightConfig.prayerHoverSection, "Prayer: hover", "Prayer tab hover settings"),
		new Section(InventoryHoverHighlightConfig.prayerSpotlightSection, "Prayer: spotlight", "Prayer tab spotlight settings"),
		new Section(InventoryHoverHighlightConfig.prayerMissDotsSection, "Prayer: miss dots", "Prayer tab miss dots settings"),
		new Section(InventoryHoverHighlightConfig.spellHoverSection, "Spellbook: hover", "Spellbook hover settings"),
		new Section(InventoryHoverHighlightConfig.spellSpotlightSection, "Spellbook: spotlight", "Spellbook spotlight settings"),
		new Section(InventoryHoverHighlightConfig.spellMissDotsSection, "Spellbook: miss dots", "Spellbook miss dots settings")
	);

	private SettingsSections()
	{
	}

	/** The shown name of a section key, or null when there is no such section. */
	static String nameOf(String key)
	{
		for (Section section : ALL)
		{
			if (section.key.equals(key))
			{
				return section.name;
			}
		}
		return null;
	}
}
