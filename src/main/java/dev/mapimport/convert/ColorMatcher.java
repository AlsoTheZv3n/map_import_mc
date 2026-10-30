package dev.mapimport.convert;

/** Finds the palette entry closest to an sRGB color. */
interface ColorMatcher {
	/** @return index into the palette, for channels 0..255 */
	int nearest(int r, int g, int b);

	static ColorMatcher create(ConvertOptions.ColorDistance distance, MapPalette palette) {
		ColorMatcher matcher = switch (distance) {
			case COMPUPHASE -> new CompuPhaseMatcher(palette);
			case DELTA_E_2000 -> new DeltaE2000Matcher(palette);
		};
		return new Cached(matcher, palette.size());
	}

	/**
	 * Remembers the answer for every 24-bit color it has seen (16 MB). Dithered photos hit the
	 * same colors again and again, and DeltaE 2000 is too slow to recompute per pixel.
	 */
	final class Cached implements ColorMatcher {
		private final ColorMatcher delegate;
		/** Palette index + 1, 0 = not computed yet. */
		private final byte[] cache = new byte[1 << 24];

		Cached(ColorMatcher delegate, int paletteSize) {
			if (paletteSize > 255) {
				throw new IllegalArgumentException("Palette too large for the byte cache: " + paletteSize);
			}
			this.delegate = delegate;
		}

		@Override
		public int nearest(int r, int g, int b) {
			int key = r << 16 | g << 8 | b;
			int hit = cache[key] & 0xFF;
			if (hit != 0) {
				return hit - 1;
			}
			int index = delegate.nearest(r, g, b);
			cache[key] = (byte) (index + 1);
			return index;
		}
	}
}
