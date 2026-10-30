package dev.mapimport.convert;

/**
 * How an image is turned into map colors.
 *
 * @param scale          how the image is fitted into the map grid
 * @param smooth         bilinear/bicubic scaling for photos, nearest neighbour for pixel art
 * @param distance       color difference used to pick the closest palette entry
 * @param dither         Floyd-Steinberg error diffusion
 * @param alphaThreshold pixels with a lower alpha become transparent (map color 0)
 */
public record ConvertOptions(ScaleMode scale, boolean smooth, ColorDistance distance, boolean dither, int alphaThreshold) {
	public static final ConvertOptions DEFAULT = new ConvertOptions(ScaleMode.FIT, true, ColorDistance.COMPUPHASE, true, 128);

	public ConvertOptions {
		if (alphaThreshold < 0 || alphaThreshold > 256) {
			throw new IllegalArgumentException("alphaThreshold must be 0..256, was " + alphaThreshold);
		}
	}

	public ConvertOptions withScale(ScaleMode scale) {
		return new ConvertOptions(scale, smooth, distance, dither, alphaThreshold);
	}

	public ConvertOptions withSmooth(boolean smooth) {
		return new ConvertOptions(scale, smooth, distance, dither, alphaThreshold);
	}

	public ConvertOptions withDistance(ColorDistance distance) {
		return new ConvertOptions(scale, smooth, distance, dither, alphaThreshold);
	}

	public ConvertOptions withDither(boolean dither) {
		return new ConvertOptions(scale, smooth, distance, dither, alphaThreshold);
	}

	public enum ScaleMode {
		/** Keep the aspect ratio, leave a transparent border. */
		FIT,
		/** Keep the aspect ratio, crop what does not fit. */
		FILL,
		/** Ignore the aspect ratio. */
		STRETCH
	}

	public enum ColorDistance {
		/** Weighted RGB ("redmean"), fast and good. */
		COMPUPHASE,
		/** CIEDE2000 in CIELAB, slowest and most accurate. */
		DELTA_E_2000
	}
}
