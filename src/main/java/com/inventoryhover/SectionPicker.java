package com.inventoryhover;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/** A dialog of checkboxes for picking settings sections. */
final class SectionPicker
{
	private SectionPicker()
	{
	}

	/**
	 * Asks which of the sections to use. Returns the picked sections, or null
	 * when the dialog was cancelled. {@code selected} null ticks every box.
	 */
	static Set<String> pick(Component parent, String title, String note, List<String> names, Set<String> selected)
	{
		JPanel boxes = new JPanel(new GridLayout(0, 1));
		List<JCheckBox> checks = new ArrayList<>();
		for (String name : names)
		{
			JCheckBox check = new JCheckBox(name, selected == null || selected.contains(name));
			checks.add(check);
			boxes.add(check);
		}
		JPanel quick = new JPanel(new GridLayout(1, 2, 6, 0));
		JButton all = new JButton("All");
		all.addActionListener(e -> checks.forEach(c -> c.setSelected(true)));
		JButton none = new JButton("None");
		none.addActionListener(e -> checks.forEach(c -> c.setSelected(false)));
		quick.add(all);
		quick.add(none);

		JPanel content = new JPanel(new BorderLayout(0, 6));
		content.add(new JLabel("<html><body style='width: 240px'>" + note + "</body></html>"), BorderLayout.NORTH);
		JScrollPane scroll = new JScrollPane(boxes);
		scroll.setPreferredSize(new Dimension(260, Math.min(320, 28 * names.size() + 10)));
		content.add(scroll, BorderLayout.CENTER);
		content.add(quick, BorderLayout.SOUTH);

		int answer = JOptionPane.showConfirmDialog(parent, content, title, JOptionPane.OK_CANCEL_OPTION,
			JOptionPane.PLAIN_MESSAGE);
		if (answer != JOptionPane.OK_OPTION)
		{
			return null;
		}
		Set<String> picked = new LinkedHashSet<>();
		checks.stream().filter(JCheckBox::isSelected).forEach(c -> picked.add(c.getText()));
		return picked;
	}
}
