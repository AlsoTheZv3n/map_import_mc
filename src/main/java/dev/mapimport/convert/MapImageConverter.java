package dev.mapimport.convert;

import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.Map;

/** Turns an image into map color tiles of {@value #TILE_SIZE} × {@value #TILE_SIZE} bytes each. */
public final class MapImageConverter {
	public static final int TILE_SIZE = 128;
	public static final int TILE_BYTES = TILE_SIZE * TILE_SIZE;

	private final MapPalette palette;
	private final Map<ConvertOptions.ColorDistance, ColorMatcher> matchers = new EnumMap<>(ConvertOptions.ColorDistance.class);

	public MapImageConverter(MapPalette palette) {
		this.palette = palette;
	}

	public MapPalette palette() {
		return palette;
	}

	/**
	 * @return {@code tilesWide * tilesHigh} tiles in row order ({@code tiles[ty * tilesWide + tx]}),
	 * each indexed {@code x + y * 128}
	 */
	public byte[][] convert(BufferedImage source, int tilesWide, int tilesHigh, ConvertOptions options) {
		int width = tilesWide * TILE_SIZE, height = tilesHigh * TILE_SIZE;
		BufferedImage scaled = ImageScaler.scale(source, width, height, options.scale(), options.smooth());
		// Dither the whole picture before cutting it, otherwise the tile edges show.
		byte[] colors = quantize(scaled, options);
		return slice(colors, width, tilesWide, tilesHigh);
	}

	byte[] quantize(BufferedImage image, ConvertOptions options) {
		int w = image.getWidth(), h = image.getHeight();
		int[] argb = image.getRGB(0, 0, w, h, null, 0, w);
		float[] error = options.dither() ? new float[w * h * 3] : null;
		ColorMatcher matcher = matcher(options.distance());
		byte[] out = new byte[w * h];

		for (int y = 0; y < h; y++) {
			// Serpentine scan: alternating direction avoids the diagonal drift of plain Floyd-Steinberg.
			boolean reverse = (y & 1) == 1;
			int dir = reverse ? -1 : 1;
			for (int i = 0; i < w; i++) {
				int x = reverse ? w - 1 - i : i;
				int p = y * w + x;
				int c = argb[p];
				if (c >>> 24 < options.alphaThreshold()) {
					continue; // stays 0 = transparent
				}

				int r = c >> 16 & 0xFF, g = c >> 8 & 0xFF, b = c & 0xFF;
				if (error != null) {
					r = clamp(r + error[p * 3]);
					g = clamp(g + error[p * 3 + 1]);
					b = clamp(b + error[p * 3 + 2]);
				}

				int index = matcher.nearest(r, g, b);
				out[p] = palette.code(index);

				if (error != null) {
					int chosen = palette.rgb(index);
					float er = r - (chosen >> 16 & 0xFF);
					float eg = g - (chosen >> 8 & 0xFF);
					float eb = b - (chosen & 0xFF);
					spread(error, w, h, x + dir, y, er, eg, eb, 7 / 16f);
					spread(error, w, h, x - dir, y + 1, er, eg, eb, 3 / 16f);
					spread(error, w, h, x, y + 1, er, eg, eb, 5 / 16f);
					spread(error, w, h, x + dir, y + 1, er, eg, eb, 1 / 16f);
				}
			}
		}
		return out;
	}

	static byte[][] slice(byte[] colors, int width, int tilesWide, int tilesHigh) {
		byte[][] tiles = new byte[tilesWide * tilesHigh][];
		for (int ty = 0; ty < tilesHigh; ty++) {
			for (int tx = 0; tx < tilesWide; tx++) {
				byte[] tile = new byte[TILE_BYTES];
				for (int row = 0; row < TILE_SIZE; row++) {
					System.arraycopy(colors, (ty * TILE_SIZE + row) * width + tx * TILE_SIZE, tile, row * TILE_SIZE, TILE_SIZE);
				}
				tiles[ty * tilesWide + tx] = tile;
			}
		}
		return tiles;
	}

	private synchronized ColorMatcher matcher(ConvertOptions.ColorDistance distance) {
		return matchers.computeIfAbsent(distance, d -> ColorMatcher.create(d, palette));
	}

	private static void spread(float[] error, int w, int h, int x, int y, float er, float eg, float eb, float factor) {
		if (x < 0 || x >= w || y >= h) {
			return;
		}
		int i = (y * w + x) * 3;
		error[i] += er * factor;
		error[i + 1] += eg * factor;
		error[i + 2] += eb * factor;
	}

	private static int clamp(float value) {
		return Math.clamp(Math.round(value), 0, 255);
	}
}
