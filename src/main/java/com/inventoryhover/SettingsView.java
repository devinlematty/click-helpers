package com.inventoryhover;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.DefaultListCellRenderer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.runelite.client.config.ConfigItemDescriptor;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.Keybind;
import net.runelite.client.config.Range;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.components.colorpicker.ColorPickerManager;
import net.runelite.client.ui.components.colorpicker.RuneliteColorPicker;

/**
 * The plugin's settings, edited in the side panel: every setting grouped in
 * collapsible sections, a search box that filters them as you type, and
 * fields that update the moment a setting changes anywhere, such as by tab
 * sync. Settings are read and saved through the config manager, as RuneLite's
 * own panel does; they are hidden from that panel so they live in one place.
 */
class SettingsView extends JPanel
{
	/** Shown in RuneLite's panel instead. */
	private static final Set<String> NOT_LISTED_SECTIONS = Set.of(InventoryHoverHighlightConfig.shareSection);

	/** Set on the side panel's Practice tab instead; the practice hotkey is listed here. */
	private static final Set<String> PRACTICE_TAB_KEYS = Set.of("practiceMode", "practiceTargets", "practiceRoundDelay",
		"practiceRandomDelay", "practiceTargetColor", "practiceHitColor", "practiceTimeLimitSeconds", "practiceTimerBar",
		"practiceRounds", "practiceOrdered", "practiceNumbers", "practiceStatsMode");
	private static final String GENERAL = "General";

	private final ConfigManager configManager;
	private final InventoryHoverHighlightConfig config;
	private final ColorPickerManager colorPickerManager;

	private final JTextField search = new JTextField();
	private final JPanel list = new JPanel();
	private final List<SectionBox> sections = new ArrayList<>();
	/** Puts each setting's stored value into its field. */
	private final Map<String, Runnable> refreshers = new HashMap<>();
	/** Set while fields are being filled in, so that doing so saves nothing. */
	private boolean updating;

	/** One collapsible section and its setting rows. */
	private static class SectionBox
	{
		final String name;
		final JPanel panel = new JPanel(new BorderLayout());
		final JButton header = new JButton();
		final JPanel contents = new JPanel();
		final List<Row> rows = new ArrayList<>();
		boolean open;

		SectionBox(String name)
		{
			this.name = name;
		}
	}

	/** One setting's row, with the text it can be found by. */
	private static class Row
	{
		final JComponent panel;
		final String text;

		Row(JComponent panel, String text)
		{
			this.panel = panel;
			this.text = text;
		}
	}

	SettingsView(ConfigManager configManager, InventoryHoverHighlightConfig config, ColorPickerManager colorPickerManager)
	{
		this.configManager = configManager;
		this.config = config;
		this.colorPickerManager = colorPickerManager;
		setLayout(new BorderLayout(0, 6));
		// Settings never changed have no stored value yet; store their defaults so each one can be read
		configManager.setDefaultConfiguration(config, false);

		JPanel top = new JPanel(new BorderLayout(0, 6));
		search.setToolTipText("Type to show only the settings whose name, description or section matches");
		search.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent e)
			{
				applyFilter();
			}

			@Override
			public void removeUpdate(DocumentEvent e)
			{
				applyFilter();
			}

			@Override
			public void changedUpdate(DocumentEvent e)
			{
				applyFilter();
			}
		});
		JPanel searchRow = new JPanel(new BorderLayout(6, 0));
		searchRow.add(small(new JLabel("Search")), BorderLayout.WEST);
		searchRow.add(search, BorderLayout.CENTER);
		top.add(searchRow, BorderLayout.NORTH);

		JPanel buttons = new JPanel(new GridLayout(1, 2, 6, 0));
		JButton expand = new JButton("Expand all");
		expand.addActionListener(e -> setAllOpen(true));
		JButton collapse = new JButton("Collapse all");
		collapse.addActionListener(e -> setAllOpen(false));
		buttons.add(expand);
		buttons.add(collapse);
		top.add(buttons, BorderLayout.SOUTH);
		add(top, BorderLayout.NORTH);

		list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
		add(list, BorderLayout.CENTER);
		build();
	}

	/** Shows a setting's new stored value; safe to call from any thread. */
	void settingChanged(String key)
	{
		Runnable refresher = refreshers.get(key);
		if (refresher != null)
		{
			SwingUtilities.invokeLater(() ->
			{
				updating = true;
				try
				{
					refresher.run();
				}
				finally
				{
					updating = false;
				}
			});
		}
	}

	/** Shows every setting's stored value, such as after loading a loadout. */
	void refreshAll()
	{
		new ArrayList<>(refreshers.keySet()).forEach(this::settingChanged);
	}

	private void build()
	{
		Map<String, List<ConfigItemDescriptor>> bySection = new LinkedHashMap<>();
		bySection.put("", new ArrayList<>());
		for (SettingsSections.Section section : SettingsSections.ALL)
		{
			bySection.put(section.key, new ArrayList<>());
		}
		configManager.getConfigDescriptor(config).getItems().stream()
			.sorted(Comparator.comparingInt(ConfigItemDescriptor::position))
			.forEach(item ->
			{
				String section = item.getItem().section();
				if (NOT_LISTED_SECTIONS.contains(section) || PRACTICE_TAB_KEYS.contains(item.key()))
				{
					return;
				}
				bySection.computeIfAbsent(bySection.containsKey(section) ? section : "", k -> new ArrayList<>()).add(item);
			});

		for (Map.Entry<String, List<ConfigItemDescriptor>> entry : bySection.entrySet())
		{
			if (entry.getValue().isEmpty())
			{
				continue;
			}
			String name = entry.getKey().isEmpty() ? GENERAL : SettingsSections.nameOf(entry.getKey());
			SectionBox box = new SectionBox(name);
			for (ConfigItemDescriptor item : entry.getValue())
			{
				JComponent row = row(item);
				if (row != null)
				{
					box.rows.add(new Row(row, (item.name() + " " + item.getItem().description() + " " + name).toLowerCase()));
					box.contents.add(row);
				}
			}
			addSection(box);
		}
	}

	private void addSection(SectionBox box)
	{
		box.header.setFont(FontManager.getRunescapeBoldFont());
		box.header.setHorizontalAlignment(JButton.LEFT);
		box.header.setFocusPainted(false);
		box.header.addActionListener(e -> setOpen(box, !box.open));
		box.contents.setLayout(new BoxLayout(box.contents, BoxLayout.Y_AXIS));
		box.contents.setBorder(new EmptyBorder(4, 4, 6, 0));
		box.panel.add(box.header, BorderLayout.NORTH);
		box.panel.add(box.contents, BorderLayout.CENTER);
		box.panel.setBorder(new EmptyBorder(0, 0, 4, 0));
		box.panel.setAlignmentX(LEFT_ALIGNMENT);
		setOpen(box, false);
		sections.add(box);
		list.add(box.panel);
	}

	private void setOpen(SectionBox box, boolean open)
	{
		box.open = open;
		box.header.setText((open ? "▾ " : "▸ ") + box.name);
		applyFilter();
	}

	private void setAllOpen(boolean open)
	{
		for (SectionBox box : sections)
		{
			box.open = open;
			box.header.setText((open ? "▾ " : "▸ ") + box.name);
		}
		applyFilter();
	}

	/** Hides rows that do not match the search, and sections left with none; opens sections while searching. */
	private void applyFilter()
	{
		String query = search.getText().trim().toLowerCase();
		boolean searching = !query.isEmpty();
		for (SectionBox box : sections)
		{
			boolean any = false;
			for (Row row : box.rows)
			{
				boolean match = !searching || row.text.contains(query);
				row.panel.setVisible(match);
				any |= match;
			}
			box.panel.setVisible(!searching || any);
			box.contents.setVisible(searching ? any : box.open);
		}
		list.revalidate();
		list.repaint();
	}

	// Rows

	/** A row for one setting, or null for a type the side panel cannot edit. */
	private JComponent row(ConfigItemDescriptor item)
	{
		Type type = item.getType();
		String key = item.key();
		JLabel label = new JLabel("<html>" + item.name() + "</html>");
		label.setToolTipText("<html><body style='width: 220px'>" + item.getItem().description()
			+ "<br><br>Right-click to reset to the default.</body></html>");
		label.setForeground(Color.WHITE);
		label.setFont(FontManager.getRunescapeSmallFont());

		JComponent field;
		boolean wide;
		if (type == boolean.class || type == Boolean.class)
		{
			JCheckBox box = new JCheckBox();
			box.addActionListener(e -> save(key, box.isSelected()));
			refreshers.put(key, () -> box.setSelected(Boolean.TRUE.equals(value(key, Boolean.class))));
			field = box;
			wide = false;
		}
		else if (type == int.class || type == Integer.class)
		{
			Range range = item.getRange();
			int min = range == null ? 0 : range.min();
			int max = range == null ? Integer.MAX_VALUE : range.max();
			Integer current = value(key, Integer.class);
			int start = Math.max(min, Math.min(max, current == null ? min : current));
			JSpinner spinner = new JSpinner(new SpinnerNumberModel(start, min, max, 1));
			spinner.addChangeListener(e -> save(key, spinner.getValue()));
			refreshers.put(key, () ->
			{
				Integer v = value(key, Integer.class);
				if (v != null && !v.equals(spinner.getValue()))
				{
					spinner.setValue(Math.max(min, Math.min(max, v)));
				}
			});
			JPanel holder = new JPanel(new BorderLayout(4, 0));
			holder.add(spinner, BorderLayout.CENTER);
			if (item.getUnits() != null)
			{
				holder.add(small(new JLabel(item.getUnits().value().trim())), BorderLayout.EAST);
			}
			field = holder;
			wide = false;
		}
		else if (type == Color.class)
		{
			JButton swatch = new JButton();
			swatch.setToolTipText("Click to pick a color");
			boolean alpha = item.getAlpha() != null;
			refreshers.put(key, () ->
			{
				Color c = value(key, Color.class);
				if (c != null)
				{
					swatch.setBackground(c);
					swatch.setText(alpha ? String.format("%d%%", Math.round(c.getAlpha() * 100 / 255f)) : "");
				}
			});
			swatch.addMouseListener(new MouseAdapter()
			{
				@Override
				public void mouseClicked(MouseEvent e)
				{
					if (!SwingUtilities.isLeftMouseButton(e))
					{
						return;
					}
					RuneliteColorPicker picker = colorPickerManager.create(swatch, swatch.getBackground(), item.name(),
						!alpha);
					picker.setOnColorChange(c ->
					{
						swatch.setBackground(c);
						save(key, c);
					});
					picker.setVisible(true);
				}
			});
			field = swatch;
			wide = false;
		}
		else if (type instanceof Class && ((Class<?>) type).isEnum())
		{
			Object[] options = ((Class<?>) type).getEnumConstants();
			JComboBox<Object> combo = new JComboBox<>(options);
			combo.setRenderer(new DefaultListCellRenderer()
			{
				@Override
				public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected,
					boolean focus)
				{
					return super.getListCellRendererComponent(list, readable(value), index, selected, focus);
				}
			});
			combo.addActionListener(e -> save(key, combo.getSelectedItem()));
			refreshers.put(key, () ->
			{
				Object v = value(key, type);
				if (v != null && v != combo.getSelectedItem())
				{
					combo.setSelectedItem(v);
				}
			});
			field = combo;
			wide = true;
		}
		else if (type == String.class)
		{
			JTextField text = new JTextField();
			Runnable store = () -> save(key, text.getText());
			text.addActionListener(e -> store.run());
			text.addFocusListener(new FocusAdapter()
			{
				@Override
				public void focusLost(FocusEvent e)
				{
					store.run();
				}
			});
			refreshers.put(key, () ->
			{
				String v = value(key, String.class);
				if (v != null && !v.equals(text.getText()))
				{
					text.setText(v);
				}
			});
			field = text;
			wide = true;
		}
		else if (type == Keybind.class)
		{
			field = hotkey(key);
			wide = true;
		}
		else
		{
			return null;
		}

		JPanel row = new JPanel(new BorderLayout(6, 2));
		row.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));
		row.add(label, wide ? BorderLayout.NORTH : BorderLayout.CENTER);
		row.add(field, wide ? BorderLayout.CENTER : BorderLayout.EAST);
		row.setAlignmentX(LEFT_ALIGNMENT);
		addReset(label, key);
		addReset(row, key);

		updating = true;
		try
		{
			Runnable refresher = refreshers.get(key);
			if (refresher != null)
			{
				refresher.run();
			}
		}
		finally
		{
			updating = false;
		}
		return row;
	}

	/** A button that records the next key pressed as the hotkey; Backspace or Delete clears it. */
	private JComponent hotkey(String key)
	{
		JButton button = new JButton();
		boolean[] waiting = {false};
		refreshers.put(key, () ->
		{
			Keybind v = value(key, Keybind.class);
			button.setText(v == null || Keybind.NOT_SET.equals(v) ? "Not set" : v.toString());
		});
		button.setToolTipText("Click, then press the keys; Backspace or Delete clears it");
		button.addActionListener(e ->
		{
			waiting[0] = true;
			button.setText("Press keys...");
			button.requestFocusInWindow();
		});
		button.addKeyListener(new KeyAdapter()
		{
			@Override
			public void keyPressed(KeyEvent e)
			{
				if (!waiting[0])
				{
					return;
				}
				e.consume();
				Keybind bind = e.getKeyCode() == KeyEvent.VK_BACK_SPACE || e.getKeyCode() == KeyEvent.VK_DELETE
					? Keybind.NOT_SET : new Keybind(e);
				// Keep waiting while only a modifier is held, so combinations can be pressed
				if (Keybind.getModifierForKeyCode(e.getKeyCode()) == null || bind == Keybind.NOT_SET)
				{
					waiting[0] = false;
				}
				save(key, bind);
				button.setText(Keybind.NOT_SET.equals(bind) ? "Not set" : bind.toString());
			}
		});
		button.addFocusListener(new FocusAdapter()
		{
			@Override
			public void focusLost(FocusEvent e)
			{
				if (waiting[0])
				{
					waiting[0] = false;
					settingChanged(key);
				}
			}
		});
		return button;
	}

	private void addReset(JComponent component, String key)
	{
		JPopupMenu menu = new JPopupMenu();
		JMenuItem reset = new JMenuItem("Reset to default");
		reset.addActionListener(e ->
		{
			configManager.unsetConfiguration(InventoryHoverHighlightConfig.GROUP, key);
			// Store the default again, so the setting reads like the others
			configManager.setDefaultConfiguration(config, false);
			settingChanged(key);
		});
		menu.add(reset);
		component.setComponentPopupMenu(menu);
	}

	private void save(String key, Object value)
	{
		if (!updating && value != null)
		{
			configManager.setConfiguration(InventoryHoverHighlightConfig.GROUP, key, value);
		}
	}

	private <T> T value(String key, Type type)
	{
		try
		{
			return configManager.getConfiguration(InventoryHoverHighlightConfig.GROUP, key, type);
		}
		catch (RuntimeException e)
		{
			return null;
		}
	}

	/** An option's name as words, like "Horizontal band" for HORIZONTAL_BAND. */
	private static String readable(Object value)
	{
		if (!(value instanceof Enum))
		{
			return value == null ? "" : value.toString();
		}
		String words = ((Enum<?>) value).name().replace('_', ' ').toLowerCase();
		return words.isEmpty() ? words : Character.toUpperCase(words.charAt(0)) + words.substring(1);
	}

	private static JLabel small(JLabel label)
	{
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		return label;
	}
}
