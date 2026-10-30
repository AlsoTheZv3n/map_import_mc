package dev.mapimport.convert;

/** sRGB → CIELAB (D65) and the CIEDE2000 color difference. */
final class CieLab {
	private static final double[] LINEAR = new double[256];
	private static final double EPSILON = 216.0 / 24389.0;
	private static final double KAPPA = 24389.0 / 27.0;
	private static final double POW25_7 = Math.pow(25, 7);

	static {
		for (int i = 0; i < 256; i++) {
			double c = i / 255.0;
			LINEAR[i] = c <= 0.04045 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
		}
	}

	private CieLab() {
	}

	/** Returns {L, a, b}. */
	static double[] fromSrgb(int r, int g, int b) {
		double lr = LINEAR[r], lg = LINEAR[g], lb = LINEAR[b];
		double x = (0.4124564 * lr + 0.3575761 * lg + 0.1804375 * lb) / 0.95047;
		double y = 0.2126729 * lr + 0.7151522 * lg + 0.0721750 * lb;
		double z = (0.0193339 * lr + 0.1191920 * lg + 0.9503041 * lb) / 1.08883;
		double fx = f(x), fy = f(y), fz = f(z);
		return new double[]{116 * fy - 16, 500 * (fx - fy), 200 * (fy - fz)};
	}

	private static double f(double t) {
		return t > EPSILON ? Math.cbrt(t) : (KAPPA * t + 16) / 116;
	}

	/** Squared CIE76 distance, used as a cheap pre-filter. */
	static double deltaE76Squared(double[] p, double[] q) {
		double dl = p[0] - q[0], da = p[1] - q[1], db = p[2] - q[2];
		return dl * dl + da * da + db * db;
	}

	/** CIEDE2000 after Sharma, Wu and Dalal (2005), with kL = kC = kH = 1. */
	static double deltaE2000(double[] lab1, double[] lab2) {
		double l1 = lab1[0], a1 = lab1[1], b1 = lab1[2];
		double l2 = lab2[0], a2 = lab2[1], b2 = lab2[2];

		double cBar = (Math.hypot(a1, b1) + Math.hypot(a2, b2)) / 2;
		double cBar7 = Math.pow(cBar, 7);
		double g = 0.5 * (1 - Math.sqrt(cBar7 / (cBar7 + POW25_7)));
		double a1p = (1 + g) * a1, a2p = (1 + g) * a2;
		double c1p = Math.hypot(a1p, b1), c2p = Math.hypot(a2p, b2);
		double h1p = hueDegrees(b1, a1p), h2p = hueDegrees(b2, a2p);

		double dLp = l2 - l1;
		double dCp = c2p - c1p;
		double dhp;
		if (c1p * c2p == 0) {
			dhp = 0;
		} else if (Math.abs(h2p - h1p) <= 180) {
			dhp = h2p - h1p;
		} else if (h2p - h1p > 180) {
			dhp = h2p - h1p - 360;
		} else {
			dhp = h2p - h1p + 360;
		}
		double dHp = 2 * Math.sqrt(c1p * c2p) * Math.sin(Math.toRadians(dhp / 2));

		double lBarP = (l1 + l2) / 2;
		double cBarP = (c1p + c2p) / 2;
		double hBarP;
		if (c1p * c2p == 0) {
			hBarP = h1p + h2p;
		} else if (Math.abs(h1p - h2p) <= 180) {
			hBarP = (h1p + h2p) / 2;
		} else if (h1p + h2p < 360) {
			hBarP = (h1p + h2p + 360) / 2;
		} else {
			hBarP = (h1p + h2p - 360) / 2;
		}

		double t = 1
				- 0.17 * Math.cos(Math.toRadians(hBarP - 30))
				+ 0.24 * Math.cos(Math.toRadians(2 * hBarP))
				+ 0.32 * Math.cos(Math.toRadians(3 * hBarP + 6))
				- 0.20 * Math.cos(Math.toRadians(4 * hBarP - 63));
		double dTheta = 30 * Math.exp(-Math.pow((hBarP - 275) / 25, 2));
		double cBarP7 = Math.pow(cBarP, 7);
		double rc = 2 * Math.sqrt(cBarP7 / (cBarP7 + POW25_7));
		double lBarP50 = (lBarP - 50) * (lBarP - 50);
		double sl = 1 + 0.015 * lBarP50 / Math.sqrt(20 + lBarP50);
		double sc = 1 + 0.045 * cBarP;
		double sh = 1 + 0.015 * cBarP * t;
		double rt = -Math.sin(Math.toRadians(2 * dTheta)) * rc;

		double tl = dLp / sl, tc = dCp / sc, th = dHp / sh;
		return Math.sqrt(tl * tl + tc * tc + th * th + rt * tc * th);
	}

	private static double hueDegrees(double b, double ap) {
		if (b == 0 && ap == 0) {
			return 0;
		}
		double h = Math.toDegrees(Math.atan2(b, ap));
		return h < 0 ? h + 360 : h;
	}
}
