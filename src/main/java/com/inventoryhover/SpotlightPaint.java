package com.inventoryhover;

import java.awt.Color;
import java.awt.Paint;
import java.awt.PaintContext;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Rectangle2D;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

/**
 * Paints the spotlight gradient in shapes other than a circle: each pixel takes
 * the gradient's color at its distance from the mouse, measured the way the
 * gradient type says, such as the larger of the two axes for a square.
 */
final class SpotlightPaint implements Paint
{
	/** Colors along the gradient from the center to its outer radius. */
	private static final int STEPS = 512;

	private final InventoryHoverHighlightConfig.SpotlightType type;
	private final InventoryHoverHighlightConfig.SpotlightMotion motion;
	private final double seconds;
	private final double centerX;
	private final double centerY;
	private final double radius;
	private final int[] lookup = new int[STEPS + 1];

	SpotlightPaint(InventoryHoverHighlightConfig.SpotlightType type, InventoryHoverHighlightConfig.SpotlightMotion motion,
		double seconds, double centerX, double centerY, float radius, float[] stops, Color[] colors)
	{
		this.type = type;
		this.motion = motion;
		this.seconds = seconds;
		this.centerX = centerX;
		this.centerY = centerY;
		this.radius = Math.max(1, radius);
		for (int i = 0; i <= STEPS; i++)
		{
			lookup[i] = colorAt((float) i / STEPS, stops, colors).getRGB();
		}
	}

	/** Whether the type, or the motion, needs the paint drawn pixel by pixel. */
	static boolean needsPaint(InventoryHoverHighlightConfig.SpotlightType type, InventoryHoverHighlightConfig.SpotlightMotion motion)
	{
		boolean circle = type == null || type == InventoryHoverHighlightConfig.SpotlightType.CIRCLE;
		boolean still = motion == null || motion == InventoryHoverHighlightConfig.SpotlightMotion.NONE;
		return !circle || !still;
	}

	/**
	 * Distance from the center the way a gradient type measures it, in pixels,
	 * with the shape turned or rippled by its motion. Used for the whole
	 * gradient, falloff included, so the shape carries on to its outer edge.
	 */
	static double distance(InventoryHoverHighlightConfig.SpotlightType type, InventoryHoverHighlightConfig.SpotlightMotion motion,
		double seconds, double dx, double dy)
	{
		if (motion == InventoryHoverHighlightConfig.SpotlightMotion.SPIN)
		{
			double angle = seconds * 0.8;
			double cos = Math.cos(angle);
			double sin = Math.sin(angle);
			double x = dx * cos - dy * sin;
			dy = dx * sin + dy * cos;
			dx = x;
		}
		double d = distance(type, dx, dy);
		if (motion == InventoryHoverHighlightConfig.SpotlightMotion.WOBBLE)
		{
			d /= 1 + 0.08 * Math.sin(6 * Math.atan2(dy, dx) + seconds * 3);
		}
		return d;
	}

	/** Distance from the center the way a gradient type measures it, in pixels. */
	static double distance(InventoryHoverHighlightConfig.SpotlightType type, double dx, double dy)
	{
		if (type == null)
		{
			return Math.hypot(dx, dy);
		}
		switch (type)
		{
			case ELLIPSE:
				return Math.hypot(dx / 1.6, dy);
			case SQUARE:
				return Math.max(Math.abs(dx), Math.abs(dy));
			case DIAMOND:
				return (Math.abs(dx) + Math.abs(dy)) * 0.75;
			case STAR:
				// Five points: farther reach along each point's direction
				return Math.hypot(dx, dy) / (1 + 0.35 * Math.cos(5 * Math.atan2(dy, dx) + Math.PI / 2));
			case HEXAGON:
				return Math.max(Math.abs(dy), Math.abs(dx) * 0.866 + Math.abs(dy) * 0.5);
			case CROSS:
				return Math.min(Math.abs(dx), Math.abs(dy)) * 1.4;
			case FLOWER:
				return Math.hypot(dx, dy) / (1 + 0.25 * Math.cos(6 * Math.atan2(dy, dx)));
			case VERTICAL_BAND:
				return Math.abs(dx);
			case HORIZONTAL_BAND:
				return Math.abs(dy);
			default:
				return Math.hypot(dx, dy);
		}
	}

	/**
	 * How much of the gradient's opacity a pattern keeps at a point, 0 to 1.
	 * Patterns sit on a circle and only change brightness, so the falloff still
	 * shows through them.
	 */
	static double pattern(InventoryHoverHighlightConfig.SpotlightType type, double seconds, double dx, double dy,
		double radius)
	{
		if (type == null)
		{
			return 1;
		}
		double d = Math.hypot(dx, dy);
		switch (type)
		{
			case RINGS:
			{
				double spacing = Math.max(8, radius / 6);
				return 0.55 + 0.45 * Math.cos(2 * Math.PI * (d / spacing - seconds * 0.5));
			}
			case SPIRAL:
			{
				double spacing = Math.max(10, radius / 4);
				double turn = Math.atan2(dy, dx) / (2 * Math.PI);
				return 0.5 + 0.5 * Math.cos(2 * Math.PI * (d / spacing + turn * 3 - seconds * 0.4));
			}
			case RADAR:
			{
				// A bright wedge turning about once every three seconds, fading behind it
				double behind = (seconds * 2.1 - Math.atan2(dy, dx)) % (2 * Math.PI);
				if (behind < 0)
				{
					behind += 2 * Math.PI;
				}
				return 0.3 + 0.7 * Math.max(0, 1 - behind / (2 * Math.PI * 0.4));
			}
			default:
				return 1;
		}
	}

	/** A gradient's color at a share of its radius, from its stops. */
	static Color colorAt(float at, float[] stops, Color[] colors)
	{
		at = Math.max(0, Math.min(1, at));
		for (int i = 1; i < stops.length; i++)
		{
			if (at <= stops[i])
			{
				float span = stops[i] - stops[i - 1];
				float t = span <= 0 ? 1f : (at - stops[i - 1]) / span;
				Color a = colors[i - 1];
				Color b = colors[i];
				return new Color(
					Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
					Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
					Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t),
					Math.round(a.getAlpha() + (b.getAlpha() - a.getAlpha()) * t));
			}
		}
		return colors[colors.length - 1];
	}

	@Override
	public PaintContext createContext(ColorModel cm, Rectangle deviceBounds, Rectangle2D userBounds,
		AffineTransform xform, RenderingHints hints)
	{
		AffineTransform toUser;
		try
		{
			toUser = xform.createInverse();
		}
		catch (NoninvertibleTransformException e)
		{
			toUser = new AffineTransform();
		}
		AffineTransform inverse = toUser;
		ColorModel model = ColorModel.getRGBdefault();
		return new PaintContext()
		{
			@Override
			public void dispose()
			{
			}

			@Override
			public ColorModel getColorModel()
			{
				return model;
			}

			@Override
			public Raster getRaster(int x, int y, int w, int h)
			{
				WritableRaster raster = model.createCompatibleWritableRaster(w, h);
				int[] pixels = new int[w * h];
				double[] point = new double[2];
				for (int row = 0; row < h; row++)
				{
					for (int col = 0; col < w; col++)
					{
						point[0] = x + col + 0.5;
						point[1] = y + row + 0.5;
						inverse.transform(point, 0, point, 0, 1);
						double dx = point[0] - centerX;
						double dy = point[1] - centerY;
						double share = distance(type, motion, seconds, dx, dy) / radius;
						int index = share >= 1 ? STEPS : (int) (share * STEPS);
						int argb = lookup[index];
						double keep = pattern(type, seconds, dx, dy, radius);
						if (keep < 1)
						{
							int alpha = (int) ((argb >>> 24) * keep);
							argb = (alpha << 24) | (argb & 0xFFFFFF);
						}
						pixels[row * w + col] = argb;
					}
				}
				raster.setDataElements(0, 0, w, h, pixels);
				return raster;
			}
		};
	}

	@Override
	public int getTransparency()
	{
		return Transparency.TRANSLUCENT;
	}
}
