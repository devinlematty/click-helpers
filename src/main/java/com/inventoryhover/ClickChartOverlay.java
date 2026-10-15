package com.inventoryhover;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import javax.inject.Inject;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.components.ComponentConstants;

/**
 * Floating chart of recent clicks: a clean click rate at the top, stacked bars
 * over the chosen time window (good, missed, refused, double), and a legend
 * with the counts for the window. Size, window and parts are configurable, and
 * text shortens to fit narrow charts. Movable like any RuneLite overlay box.
 */
class ClickChartOverlay extends Overlay
{
	private static final int PAD = 6;
	private static final int BAR_GAP = 1;
	// Space kept between the title and the clean click rate
	private static final int TITLE_GAP = 8;

	private static final Color GOOD = new Color(80, 200, 120);
	private static final Color MISSED = new Color(230, 70, 70);
	private static final Color REFUSED = new Color(240, 200, 40);
	private static final Color DOUBLE = new Color(240, 130, 40);
	private static final Color AXIS = new Color(255, 255, 255, 60);
	private static final Color LABEL = new Color(200, 200, 200);
	private static final Color AVERAGE = new Color(255, 255, 255, 200);

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
		boolean ticks = config.clickChartUnit() == InventoryHoverHighlightConfig.ChartUnit.TICKS;
		int interval = config.clickChartInterval();
		int barCount = config.clickChartBars();
		int[][] history = ticks
			? plugin.stats.tickHistory(interval, barCount)
			: plugin.stats.secondHistory(interval, barCount);

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
		FontMetrics smallMetrics = graphics.getFontMetrics(small);
		FontMetrics boldMetrics = graphics.getFontMetrics(bold);
		int lineHeight = smallMetrics.getHeight();
		int width = config.clickChartWidth();
		int inner = width - PAD * 2;
		int chartHeight = config.clickChartHeight();

		int all = totals[0] + totals[1] + totals[2] + totals[3];
		String rate = all == 0 ? "-" : (totals[0] * 100 / all) + "%" + (inner >= 140 ? " clean" : "");
		String window = ticks ? interval * barCount + "t" : formatWindow(interval * barCount);
		// Shorten the title until it fits beside the rate, or drop it
		String title = null;
		for (String option : new String[]{"Clicks, last " + window, "Clicks " + window, window})
		{
			if (boldMetrics.stringWidth(option) + TITLE_GAP + boldMetrics.stringWidth(rate) <= inner)
			{
				title = option;
				break;
			}
		}

		boolean legend = config.clickChartLegend();
		boolean axis = config.clickChartTimeAxis();
		// Two legend columns when they fit, else one
		int legendColumns = 2;
		for (int k = 0; k < 4; k++)
		{
			if (11 + smallMetrics.stringWidth(NAMES[k] + " " + totals[k]) > inner / 2)
			{
				legendColumns = 1;
			}
		}
		int legendRows = 4 / legendColumns;

		int titleHeight = boldMetrics.getHeight();
		int height = PAD + titleHeight + 4 + chartHeight
			+ (axis ? lineHeight + 2 : 2)
			+ (legend ? lineHeight * legendRows + 2 : 0) + PAD;

		graphics.setColor(ComponentConstants.STANDARD_BACKGROUND_COLOR);
		graphics.fill(new RoundRectangle2D.Float(0, 0, width, height, 8, 8));

		// Title and clean click rate
		int y = PAD + boldMetrics.getAscent();
		graphics.setFont(bold);
		if (title != null)
		{
			graphics.setColor(Color.WHITE);
			graphics.drawString(title, PAD, y);
		}
		graphics.setColor(all == 0 ? LABEL : rateColor(totals[0] * 100 / all));
		graphics.drawString(rate, width - PAD - boldMetrics.stringWidth(rate), y);

		// Bars
		int chartTop = PAD + titleHeight + 4;
		int chartBottom = chartTop + chartHeight;
		int max = 1;
		for (int[] bar : bars)
		{
			max = Math.max(max, bar[0] + bar[1] + bar[2] + bar[3]);
		}

		float slot = (float) inner / bars.length;
		for (int i = 0; i < bars.length; i++)
		{
			int x = PAD + Math.round(i * slot);
			int w = Math.max(1, Math.round((i + 1) * slot) - Math.round(i * slot) - BAR_GAP);
			int top = chartBottom;
			for (int k = 0; k < 4; k++)
			{
				int h = Math.round((float) bars[i][k] / max * chartHeight);
				if (h <= 0)
				{
					continue;
				}
				top -= h;
				graphics.setColor(STACK[k]);
				graphics.fillRect(x, top, w, h);
			}
		}

		if (config.clickChartAverage())
		{
			drawAverage(graphics, bars, max, slot, chartBottom, chartHeight, config.clickChartAverageBars());
		}

		graphics.setColor(AXIS);
		graphics.drawLine(PAD, chartBottom, width - PAD, chartBottom);

		graphics.setFont(small);
		if (all == 0)
		{
			String empty = "No clicks yet";
			graphics.setColor(LABEL);
			graphics.drawString(empty, Math.max(PAD, (width - smallMetrics.stringWidth(empty)) / 2),
				chartTop + chartHeight / 2 + smallMetrics.getAscent() / 2);
		}

		int below = chartBottom + 2;
		if (axis)
		{
			y = chartBottom + smallMetrics.getAscent() + 1;
			graphics.setColor(LABEL);
			graphics.drawString(window + " ago", PAD, y);
			String now = "now";
			graphics.drawString(now, width - PAD - smallMetrics.stringWidth(now), y);
			below = chartBottom + lineHeight + 2;
		}

		if (legend)
		{
			int column = inner / legendColumns;
			for (int k = 0; k < 4; k++)
			{
				int x = PAD + (k % legendColumns) * column;
				int rowY = below + (k / legendColumns) * lineHeight;
				graphics.setColor(STACK[k]);
				graphics.fillRect(x, rowY + 2, 7, 7);
				graphics.setColor(Color.WHITE);
				graphics.drawString(NAMES[k] + " " + totals[k], x + 11, rowY + smallMetrics.getAscent());
			}
		}

		return new Dimension(width, height);
	}

	/** Line through the average of the last {@code span} bar totals, at each bar's center. */
	private static void drawAverage(Graphics2D graphics, int[][] bars, int max, float slot, int chartBottom,
		int chartHeight, int span)
	{
		Path2D line = new Path2D.Float();
		double sum = 0;
		for (int i = 0; i < bars.length; i++)
		{
			sum += bars[i][0] + bars[i][1] + bars[i][2] + bars[i][3];
			if (i >= span)
			{
				int[] old = bars[i - span];
				sum -= old[0] + old[1] + old[2] + old[3];
			}
			double average = sum / Math.min(i + 1, span);
			float x = PAD + (i + 0.5f) * slot;
			float y = (float) (chartBottom - average / max * chartHeight);
			if (i == 0)
			{
				line.moveTo(x, y);
			}
			else
			{
				line.lineTo(x, y);
			}
		}
		graphics.setStroke(new BasicStroke(1.5f));
		graphics.setColor(AVERAGE);
		graphics.draw(line);
	}

	/** A window length like 45s, 2m or 2m30s. */
	private static String formatWindow(int seconds)
	{
		if (seconds < 120)
		{
			return seconds + "s";
		}
		return (seconds / 60) + "m" + (seconds % 60 == 0 ? "" : (seconds % 60) + "s");
	}

	private static Color rateColor(int percent)
	{
		return percent >= 90 ? GOOD : percent >= 75 ? REFUSED : MISSED;
	}
}
