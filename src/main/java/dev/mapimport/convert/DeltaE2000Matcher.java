package dev.mapimport.convert;

/**
 * CIEDE2000 nearest color. Only the few entries closest by plain CIELAB distance are scored with
 * the full formula; the two metrics agree on the neighbourhood, and this is about 30× faster.
 */
final class DeltaE2000Matcher implements ColorMatcher {
	private static final int CANDIDATES = 8;

	private final double[][] lab;

	DeltaE2000Matcher(MapPalette palette) {
		lab = new double[palette.size()][];
		for (int i = 0; i < lab.length; i++) {
			int rgb = palette.rgb(i);
			lab[i] = CieLab.fromSrgb(rgb >> 16 & 0xFF, rgb >> 8 & 0xFF, rgb & 0xFF);
		}
	}

	@Override
	public int nearest(int r, int g, int b) {
		double[] target = CieLab.fromSrgb(r, g, b);

		int count = Math.min(CANDIDATES, lab.length);
		int[] candidates = new int[count];
		double[] candidateDistances = new double[count];
		int filled = 0;
		for (int i = 0; i < lab.length; i++) {
			double d = CieLab.deltaE76Squared(target, lab[i]);
			if (filled == count && d >= candidateDistances[count - 1]) {
				continue;
			}
			// Insertion into the sorted candidate list.
			int pos = filled < count ? filled++ : count - 1;
			while (pos > 0 && candidateDistances[pos - 1] > d) {
				candidateDistances[pos] = candidateDistances[pos - 1];
				candidates[pos] = candidates[pos - 1];
				pos--;
			}
			candidateDistances[pos] = d;
			candidates[pos] = i;
		}

		int best = candidates[0];
		double bestDistance = Double.MAX_VALUE;
		for (int i = 0; i < filled; i++) {
			double d = CieLab.deltaE2000(target, lab[candidates[i]]);
			if (d < bestDistance) {
				bestDistance = d;
				best = candidates[i];
			}
		}
		return best;
	}
}
