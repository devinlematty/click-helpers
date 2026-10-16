package com.inventoryhover;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.List;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;

/**
 * The switch practice status and stats in a normal RuneLite box, so it can be
 * moved with Alt+drag like other overlays. Starts above the inventory.
 */
class PracticeStatusOverlay extends OverlayPanel
{
	private final InventoryHoverHighlightOverlay overlay;

	@Inject
	PracticeStatusOverlay(InventoryHoverHighlightOverlay overlay)
	{
		this.overlay = overlay;
		setPosition(OverlayPosition.BOTTOM_RIGHT);
		panelComponent.setPreferredSize(new Dimension(230, 0));
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		List<String> lines = overlay.practiceLines();
		if (lines.isEmpty())
		{
			return null;
		}
		for (String line : lines)
		{
			panelComponent.getChildren().add(LineComponent.builder().left(line).build());
		}
		return super.render(graphics);
	}
}
