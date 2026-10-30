package dev.mapimport.convert;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

final class ImageScaler {
	private ImageScaler() {
	}

	/** Draws {@code source} onto a transparent {@code width × height} canvas according to {@code mode}. */
	static BufferedImage scale(BufferedImage source, int width, int height, ConvertOptions.ScaleMode mode, boolean smooth) {
		int sw = source.getWidth(), sh = source.getHeight();
		int dw, dh;
		if (mode == ConvertOptions.ScaleMode.STRETCH) {
			dw = width;
			dh = height;
		} else {
			double sx = width / (double) sw, sy = height / (double) sh;
			double s = mode == ConvertOptions.ScaleMode.FIT ? Math.min(sx, sy) : Math.max(sx, sy);
			dw = Math.max(1, (int) Math.round(sw * s));
			dh = Math.max(1, (int) Math.round(sh * s));
		}
		int dx = (width - dw) / 2, dy = (height - dh) / 2;

		BufferedImage image = toArgb(source);
		if (smooth) {
			image = halveTowards(image, dw, dh);
		}

		BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = out.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, smooth
				? RenderingHints.VALUE_INTERPOLATION_BICUBIC
				: RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g.drawImage(image, dx, dy, dw, dh, null);
		g.dispose();
		return out;
	}

	/**
	 * Bicubic interpolation only looks at a 4×4 neighbourhood, so a single big reduction skips most
	 * source pixels and aliases. Halving step by step first keeps every pixel's contribution.
	 */
	private static BufferedImage halveTowards(BufferedImage image, int targetWidth, int targetHeight) {
		while (true) {
			int w = image.getWidth(), h = image.getHeight();
			int nw = w / 2 >= targetWidth ? w / 2 : w;
			int nh = h / 2 >= targetHeight ? h / 2 : h;
			if (nw == w && nh == h) {
				return image;
			}
			BufferedImage next = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = next.createGraphics();
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			g.drawImage(image, 0, 0, nw, nh, null);
			g.dispose();
			image = next;
		}
	}

	private static BufferedImage toArgb(BufferedImage source) {
		if (source.getType() == BufferedImage.TYPE_INT_ARGB) {
			return source;
		}
		BufferedImage copy = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = copy.createGraphics();
		g.drawImage(source, 0, 0, null);
		g.dispose();
		return copy;
	}
}
