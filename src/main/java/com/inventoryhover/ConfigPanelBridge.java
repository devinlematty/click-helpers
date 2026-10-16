package com.inventoryhover;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.LayoutManager;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import net.runelite.client.config.ConfigDescriptor;
import net.runelite.client.config.ConfigItemDescriptor;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.ConfigSectionDescriptor;
import net.runelite.client.ui.components.ColorJButton;

/**
 * Works with RuneLite's config panel while it shows this plugin's settings,
 * which RuneLite only redraws when the panel is opened. Updates the fields of
 * settings changed behind the panel, such as by tab sync, and adds a search
 * box that filters the settings as you type. Rows are found by their section
 * and label text, since the panel does not tag them with their keys. When the
 * panel looks different than expected, nothing happens.
 */
@Singleton
class ConfigPanelBridge
{
	private static final String CONFIG_PANEL_CLASS = "net.runelite.client.plugins.config.ConfigPanel";
	private static final String SEARCH_MARKER = "clickHelpersSearch";
	private static final String SAVED_VISIBILITY = "clickHelpersVisible";

	private final ConfigManager configManager;
	private final InventoryHoverHighlightConfig config;
	private final Timer timer = new Timer(500, e -> tick());
	private String query = "";

	@Inject
	ConfigPanelBridge(ConfigManager configManager, InventoryHoverHighlightConfig config)
	{
		this.configManager = configManager;
		this.config = config;
	}

	void start()
	{
		SwingUtilities.invokeLater(timer::start);
	}

	void stop()
	{
		SwingUtilities.invokeLater(() ->
		{
			timer.stop();
			Container panel = findPanel();
			if (panel != null)
			{
				query = "";
				applyFilter(panel);
				removeSearch(panel);
			}
		});
	}

	/** Shows the stored values of these settings in the open panel. */
	void refresh(Collection<String> keys)
	{
		List<String> copy = new ArrayList<>(keys);
		SwingUtilities.invokeLater(() ->
		{
			Container panel = findPanel();
			if (panel == null)
			{
				return;
			}
			Map<String, ConfigItemDescriptor> items = itemsByKey();
			for (Row row : rows(panel))
			{
				if (copy.contains(row.item.key()))
				{
					show(row, configManager.getConfiguration(InventoryHoverHighlightConfig.GROUP, row.item.key()), items);
				}
			}
		});
	}

	// Finding the panel and its rows

	/** One setting's row: its descriptor, the row panel and the field that edits it. */
	private static class Row
	{
		final ConfigItemDescriptor item;
		final String section;
		final JComponent panel;
		final Component field;

		Row(ConfigItemDescriptor item, String section, JComponent panel, Component field)
		{
			this.item = item;
			this.section = section;
			this.panel = panel;
			this.field = field;
		}
	}

	/** The config panel when it is showing this plugin, or null. */
	private Container findPanel()
	{
		String someSection = sectionNames().values().stream().findFirst().orElse(null);
		for (Frame frame : Frame.getFrames())
		{
			Container panel = findPanel(frame, someSection);
			if (panel != null)
			{
				return panel;
			}
		}
		return null;
	}

	private static Container findPanel(Component component, String someSection)
	{
		if (!(component instanceof Container) || !component.isShowing())
		{
			return null;
		}
		Container container = (Container) component;
		if (container.getClass().getName().equals(CONFIG_PANEL_CLASS))
		{
			return someSection != null && hasLabel(container, someSection) ? container : null;
		}
		for (Component child : container.getComponents())
		{
			Container found = findPanel(child, someSection);
			if (found != null)
			{
				return found;
			}
		}
		return null;
	}

	private static boolean hasLabel(Container container, String text)
	{
		for (Component child : container.getComponents())
		{
			if (child instanceof JLabel && text.equals(((JLabel) child).getText()))
			{
				return true;
			}
			if (child instanceof Container && hasLabel((Container) child, text))
			{
				return true;
			}
		}
		return false;
	}

	/** Section keys to the names the panel shows. */
	private Map<String, String> sectionNames()
	{
		Map<String, String> names = new HashMap<>();
		for (ConfigSectionDescriptor section : descriptor().getSections())
		{
			names.put(section.key(), section.name());
		}
		return names;
	}

	private ConfigDescriptor descriptor()
	{
		return configManager.getConfigDescriptor(config);
	}

	private Map<String, ConfigItemDescriptor> itemsByKey()
	{
		Map<String, ConfigItemDescriptor> items = new HashMap<>();
		for (ConfigItemDescriptor item : descriptor().getItems())
		{
			items.put(item.key(), item);
		}
		return items;
	}

	/**
	 * Every setting row in the panel, in order. A row is a panel with the
	 * setting's name as a label in its center and the field beside it; the
	 * section is the last section header seen before it.
	 */
	private List<Row> rows(Container panel)
	{
		Map<String, String> sections = sectionNames();
		Map<String, ConfigItemDescriptor> byLabel = new HashMap<>();
		for (ConfigItemDescriptor item : descriptor().getItems())
		{
			if (!item.getItem().hidden())
			{
				byLabel.put(sections.getOrDefault(item.getItem().section(), "") + "\u0000" + item.name(), item);
			}
		}
		List<Row> rows = new ArrayList<>();
		collectRows(panel, new String[]{""}, sections.values(), byLabel, rows);
		return rows;
	}

	private static void collectRows(Container container, String[] section, Collection<String> sectionNames,
		Map<String, ConfigItemDescriptor> byLabel, List<Row> rows)
	{
		LayoutManager layout = container.getLayout();
		if (layout instanceof BorderLayout && container instanceof JComponent)
		{
			Component center = ((BorderLayout) layout).getLayoutComponent(BorderLayout.CENTER);
			if (center instanceof JLabel)
			{
				String text = ((JLabel) center).getText();
				if (sectionNames.contains(text))
				{
					section[0] = text;
					return;
				}
				ConfigItemDescriptor item = byLabel.get(section[0] + "\u0000" + text);
				if (item == null)
				{
					// Settings outside any section come before the first section
					item = byLabel.get("\u0000" + text);
				}
				if (item != null)
				{
					BorderLayout border = (BorderLayout) layout;
					Component field = border.getLayoutComponent(BorderLayout.EAST);
					if (field == null)
					{
						field = border.getLayoutComponent(BorderLayout.SOUTH);
					}
					rows.add(new Row(item, section[0], (JComponent) container, field));
					return;
				}
			}
		}
		for (Component child : container.getComponents())
		{
			if (child instanceof Container)
			{
				collectRows((Container) child, section, sectionNames, byLabel, rows);
			}
		}
	}

	/** Puts a stored value into a row's field. Setting the same value again changes nothing. */
	private static void show(Row row, String value, Map<String, ConfigItemDescriptor> items)
	{
		if (value == null || row.field == null)
		{
			return;
		}
		Type type = row.item.getType();
		try
		{
			if (row.field instanceof JCheckBox)
			{
				((JCheckBox) row.field).setSelected(Boolean.parseBoolean(value));
			}
			else if (row.field instanceof ColorJButton)
			{
				Color color = new Color((int) Long.parseLong(value), true);
				ColorJButton button = (ColorJButton) row.field;
				button.setColor(color);
				// The button's text is the hex code, which setColor leaves alone; match RuneLite's own format
				button.setText(row.item.getAlpha() != null
					? String.format("#%08X", color.getRGB())
					: String.format("#%06X", color.getRGB() & 0xFFFFFF));
				button.repaint();
			}
			else if (row.field instanceof JSpinner)
			{
				Object current = ((JSpinner) row.field).getValue();
				Object next = current instanceof Double ? (Object) Double.parseDouble(value) : (Object) Integer.parseInt(value);
				if (!next.equals(current))
				{
					((JSpinner) row.field).setValue(next);
				}
			}
			else if (row.field instanceof JComboBox && type instanceof Class && ((Class<?>) type).isEnum())
			{
				for (Object option : ((Class<?>) type).getEnumConstants())
				{
					if (((Enum<?>) option).name().equals(value) && option != ((JComboBox<?>) row.field).getSelectedItem())
					{
						((JComboBox<?>) row.field).setSelectedItem(option);
					}
				}
			}
			else if (row.field instanceof JTextComponent && !((JTextComponent) row.field).getText().equals(value))
			{
				((JTextComponent) row.field).setText(value);
			}
		}
		catch (RuntimeException e)
		{
			// Leave the field as it is
		}
	}

	// Search

	private void tick()
	{
		Container panel = findPanel();
		if (panel != null && !hasSearch(panel))
		{
			// The panel was opened or rebuilt, which also clears any filter
			query = "";
			installSearch(panel);
		}
	}

	private static boolean hasSearch(Container container)
	{
		for (Component child : container.getComponents())
		{
			if (child instanceof JComponent && ((JComponent) child).getClientProperty(SEARCH_MARKER) != null)
			{
				return true;
			}
			if (child instanceof Container && hasSearch((Container) child))
			{
				return true;
			}
		}
		return false;
	}

	/** Adds the search box above the first setting or section. */
	private void installSearch(Container panel)
	{
		List<Row> rows = rows(panel);
		if (rows.isEmpty())
		{
			return;
		}
		Container list = listOf(rows.get(0).panel, panel);
		if (list == null)
		{
			return;
		}

		JTextField field = new JTextField();
		field.setToolTipText("Type to show only the settings whose name, description or section matches");
		field.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent e)
			{
				search(field.getText());
			}

			@Override
			public void removeUpdate(DocumentEvent e)
			{
				search(field.getText());
			}

			@Override
			public void changedUpdate(DocumentEvent e)
			{
				search(field.getText());
			}
		});
		JPanel searchRow = new JPanel(new BorderLayout(6, 0));
		searchRow.add(new JLabel("Search settings"), BorderLayout.WEST);
		searchRow.add(field, BorderLayout.CENTER);

		JPanel buttons = new JPanel(new GridLayout(1, 2, 6, 0));
		JButton expand = new JButton("Expand all");
		expand.setToolTipText("Open every settings section");
		expand.addActionListener(e -> setSectionsOpen(true));
		JButton collapse = new JButton("Collapse all");
		collapse.setToolTipText("Close every settings section");
		collapse.addActionListener(e -> setSectionsOpen(false));
		buttons.add(expand);
		buttons.add(collapse);

		JPanel box = new JPanel(new BorderLayout(0, 6));
		box.putClientProperty(SEARCH_MARKER, true);
		box.setBorder(new EmptyBorder(0, 0, 8, 0));
		box.add(searchRow, BorderLayout.NORTH);
		box.add(buttons, BorderLayout.SOUTH);
		list.add(box, 0);
		list.revalidate();
		list.repaint();
	}

	private static void removeSearch(Container container)
	{
		for (Component child : container.getComponents())
		{
			if (child instanceof JComponent && ((JComponent) child).getClientProperty(SEARCH_MARKER) != null)
			{
				container.remove(child);
				container.revalidate();
				container.repaint();
				return;
			}
			if (child instanceof Container)
			{
				removeSearch((Container) child);
			}
		}
	}

	/**
	 * The panel that lists the sections and loose settings: the highest
	 * ancestor of the first row that is still inside the config panel and
	 * stacks its children.
	 */
	private static Container listOf(Component row, Container configPanel)
	{
		Container best = null;
		for (Container parent = row.getParent(); parent != null && parent != configPanel; parent = parent.getParent())
		{
			if (parent instanceof JPanel && parent.getComponentCount() > 1)
			{
				best = parent;
			}
			if (parent.getClass().getName().startsWith("javax.swing.JViewport"))
			{
				break;
			}
		}
		return best;
	}

	/**
	 * Opens or closes every section by pressing its own toggle button, so the
	 * panel remembers the state as if each were clicked. The button's tooltip
	 * says what pressing it does: "Retract" when open, "Expand" when closed.
	 */
	private void setSectionsOpen(boolean open)
	{
		Container panel = findPanel();
		if (panel == null)
		{
			return;
		}
		List<JButton> toggles = new ArrayList<>();
		collectSectionToggles(panel, sectionNames().values(), toggles);
		for (JButton toggle : toggles)
		{
			String action = toggle.getToolTipText();
			if (open ? "Expand".equals(action) : "Retract".equals(action))
			{
				toggle.doClick(0);
			}
		}
	}

	private static void collectSectionToggles(Container container, Collection<String> sectionNames, List<JButton> toggles)
	{
		LayoutManager layout = container.getLayout();
		if (layout instanceof BorderLayout)
		{
			Component center = ((BorderLayout) layout).getLayoutComponent(BorderLayout.CENTER);
			Component west = ((BorderLayout) layout).getLayoutComponent(BorderLayout.WEST);
			if (center instanceof JLabel && sectionNames.contains(((JLabel) center).getText()) && west instanceof JButton)
			{
				toggles.add((JButton) west);
				return;
			}
		}
		for (Component child : container.getComponents())
		{
			if (child instanceof Container)
			{
				collectSectionToggles((Container) child, sectionNames, toggles);
			}
		}
	}

	private void search(String text)
	{
		query = text.trim().toLowerCase();
		Container panel = findPanel();
		if (panel != null)
		{
			applyFilter(panel);
		}
	}

	/** Hides rows that do not match, and sections left with none; opens sections while searching. */
	private void applyFilter(Container panel)
	{
		boolean searching = !query.isEmpty();
		Map<Container, Boolean> sectionHasMatch = new HashMap<>();
		for (Row row : rows(panel))
		{
			String description = row.item.getItem().description();
			boolean match = !searching || row.item.name().toLowerCase().contains(query)
				|| (description != null && description.toLowerCase().contains(query))
				|| row.section.toLowerCase().contains(query);
			row.panel.setVisible(match);

			// The row's parent holds a section's settings; open it while searching, restore it after
			Container contents = row.panel.getParent();
			if (contents instanceof JComponent && !row.section.isEmpty())
			{
				JComponent box = (JComponent) contents;
				if (searching)
				{
					if (box.getClientProperty(SAVED_VISIBILITY) == null)
					{
						box.putClientProperty(SAVED_VISIBILITY, box.isVisible());
					}
					box.setVisible(true);
				}
				else if (box.getClientProperty(SAVED_VISIBILITY) != null)
				{
					box.setVisible((Boolean) box.getClientProperty(SAVED_VISIBILITY));
					box.putClientProperty(SAVED_VISIBILITY, null);
				}
				Container section = box.getParent();
				if (section != null)
				{
					sectionHasMatch.merge(section, match, Boolean::logicalOr);
				}
			}
		}
		for (Map.Entry<Container, Boolean> section : sectionHasMatch.entrySet())
		{
			section.getKey().setVisible(!searching || section.getValue());
		}
		panel.revalidate();
		panel.repaint();
	}
}
