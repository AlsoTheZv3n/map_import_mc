package dev.mapimport.convert;

/** Weighted RGB distance from https://www.compuphase.com/cmetric.htm ("redmean"). */
final class CompuPhaseMatcher implements ColorMatcher {
	private final int[] red;
	private final int[] green;
	private final int[] blue;

	CompuPhaseMatcher(MapPalette palette) {
		int size = palette.size();
		red = new int[size];
		green = new int[size];
		blue = new int[size];
		for (int i = 0; i < size; i++) {
			int rgb = palette.rgb(i);
			red[i] = rgb >> 16 & 0xFF;
			green[i] = rgb >> 8 & 0xFF;
			blue[i] = rgb & 0xFF;
		}
	}

	@Override
	public int nearest(int r, int g, int b) {
		int best = 0;
		int bestDistance = Integer.MAX_VALUE;
		for (int i = 0; i < red.length; i++) {
			int rMean = (r + red[i]) >> 1;
			int dr = r - red[i], dg = g - green[i], db = b - blue[i];
			int distance = ((512 + rMean) * dr * dr >> 8) + 4 * dg * dg + ((767 - rMean) * db * db >> 8);
			if (distance < bestDistance) {
				bestDistance = distance;
				best = i;
			}
		}
		return best;
	}
}
