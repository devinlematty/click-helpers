package com.inventoryhover;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import javax.inject.Inject;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.components.ComponentConstants;

/**
 * Floating chart of recent clicks: a clean click rate at the top, one stacked
 * bar per few seconds (good, missed, refused, double), and a legend with the
 * counts for the window. Movable like any RuneLite overlay box.
 */
class ClickChartOverlay extends Overlay
{
	private static final int WIDTH = 190;
	private static final int PAD = 6;
	private static final int CHART_HEIGHT = 50;
	private static final int BAR_GAP = 1;

	private static final Color GOOD = new Color(80, 200, 120);
	private static final Color MISSED = new Color(230, 70, 70);
	private static final Color REFUSED = new Color(240, 200, 40);
	private static final Color DOUBLE = new Color(240, 130, 40);
	private static final Color AXIS = new Color(255, 255, 255, 60);
	private static final Color LABEL = new Color(200, 200, 200);

	// Draw order from the baseline up; misses sit on top so they stand out
	private static final Color[] STACK = {GOOD, DOUBLE, REFUSED, MISSED};
	private static final String[] NAMES = {"Good", "Double", "Refused", "Missed"};

	private final InventoryHoverHighlightPlugin plugin;
	private final InventoryHoverHighlightConfig config;

	@Inject
	ClickChartOverlay(InventoryHoverHighlightPlugin plugin, InventoryHoverHighlightConfig config)
	{
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.TOP_LEFT);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setPriority(OverlayPriority.LOW);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.clickChart())
		{
			return null;
		}

		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		int[][] history = plugin.stats.history();

		// Per bar: good, double, refused, missed. Good excludes the clicks that went wrong.
		int[][] bars = new int[history.length][4];
		int[] totals = new int[4];
		for (int i = 0; i < history.length; i++)
		{
			int[] h = history[i];
			int useful = h[ClickStats.Kind.USEFUL.ordinal()];
			int doubles = h[ClickStats.Kind.DOUBLE.ordinal()];
			int refused = h[ClickStats.Kind.REJECTED.ordinal()];
			int missed = h[ClickStats.Kind.MISSED.ordinal()];
			bars[i][0] = Math.max(0, useful - doubles - refused);
			bars[i][1] = doubles;
			bars[i][2] = refused;
			bars[i][3] = missed;
			for (int k = 0; k < 4; k++)
			{
				totals[k] += bars[i][k];
			}
		}

		Font small = FontManager.getRunescapeSmallFont();
		Font bold = FontManager.getRunescapeBoldFont();
		graphics.setFont(small);
		FontMetrics smallMetrics = graphics.getFontMetrics(small);
		FontMetrics boldMetrics = graphics.getFontMetrics(bold);
		int lineHeight = smallMetrics.getHeight();

		int titleHeight = boldMetrics.getHeight();
		int legendHeight = lineHeight * 2;
		int height = PAD + titleHeight + 4 + CHART_HEIGHT + lineHeight + 4 + legendHeight + PAD;

		graphics.setColor(ComponentConstants.STANDARD_BACKGROUND_COLOR);
		graphics.fill(new RoundRectangle2D.Float(0, 0, WIDTH, height, 8, 8));

		// Title and clean click rate
		int y = PAD + boldMetrics.getAscent();
		graphics.setFont(bold);
		graphics.setColor(Color.WHITE);
		long seconds = ClickStats.BUCKET_MILLIS * ClickStats.BUCKETS / 1000;
		graphics.drawString("Clicks, last " + seconds + "s", PAD, y);

		int all = totals[0] + totals[1] + totals[2] + totals[3];
		String rate = all == 0 ? "-" : (totals[0] * 100 / all) + "% clean";
		graphics.setColor(all == 0 ? LABEL : rateColor(totals[0] * 100 / all));
		graphics.drawString(rate, WIDTH - PAD - boldMetrics.stringWidth(rate), y);

		// Bars
		int chartTop = PAD + titleHeight + 4;
		int chartBottom = chartTop + CHART_HEIGHT;
		int max = 1;
		for (int[] bar : bars)
		{
			max = Math.max(max, bar[0] + bar[1] + bar[2] + bar[3]);
		}

		int chartWidth = WIDTH - PAD * 2;
		float slot = (float) chartWidth / bars.length;
		for (int i = 0; i < bars.length; i++)
		{
			int x = PAD + Math.round(i * slot);
			int w = Math.max(1, Math.round((i + 1) * slot) - Math.round(i * slot) - BAR_GAP);
			int top = chartBottom;
			for (int k = 0; k < 4; k++)
			{
				int h = Math.round((float) bars[i][k] / max * CHART_HEIGHT);
				if (h <= 0)
				{
					continue;
				}
				top -= h;
				graphics.setColor(STACK[k]);
				graphics.fillRect(x, top, w, h);
			}
		}

		graphics.setColor(AXIS);
		graphics.drawLine(PAD, chartBottom, WIDTH - PAD, chartBottom);

		graphics.setFont(small);
		if (all == 0)
		{
			String empty = "No clicks yet";
			graphics.setColor(LABEL);
			graphics.drawString(empty, (WIDTH - smallMetrics.stringWidth(empty)) / 2,
				chartTop + CHART_HEIGHT / 2 + smallMetrics.getAscent() / 2);
		}

		// Time axis
		y = chartBottom + smallMetrics.getAscent() + 1;
		graphics.setColor(LABEL);
		graphics.drawString(seconds + "s ago", PAD, y);
		String now = "now";
		graphics.drawString(now, WIDTH - PAD - smallMetrics.stringWidth(now), y);

		// Legend: two columns, colored square and count
		int legendTop = chartBottom + lineHeight + 4;
		int column = (WIDTH - PAD * 2) / 2;
		for (int k = 0; k < 4; k++)
		{
			int x = PAD + (k % 2) * column;
			int rowY = legendTop + (k / 2) * lineHeight;
			graphics.setColor(STACK[k]);
			graphics.fillRect(x, rowY + 2, 7, 7);
			graphics.setColor(Color.WHITE);
			graphics.drawString(NAMES[k] + " " + totals[k], x + 11, rowY + smallMetrics.getAscent());
		}

		return new Dimension(WIDTH, height);
	}

	private static Color rateColor(int percent)
	{
		return percent >= 90 ? GOOD : percent >= 75 ? REFUSED : MISSED;
	}
}
