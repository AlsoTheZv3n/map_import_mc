package dev.mapimport.convert;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CieLabTest {
	/** Reference pairs from Sharma, Wu and Dalal, "The CIEDE2000 Color-Difference Formula" (2005). */
	@Test
	void deltaE2000MatchesSharmaReferenceData() {
		assertDeltaE(2.0425, new double[]{50, 2.6772, -79.7751}, new double[]{50, 0, -82.7485});
		assertDeltaE(2.8615, new double[]{50, 3.1571, -77.2803}, new double[]{50, 0, -82.7485});
		assertDeltaE(3.4412, new double[]{50, 2.8361, -74.0200}, new double[]{50, 0, -82.7485});
		assertDeltaE(2.3669, new double[]{50, 0, 0}, new double[]{50, -1, 2});
		assertDeltaE(27.1492, new double[]{50, 2.5, 0}, new double[]{73, 25, -18});
	}

	@Test
	void deltaE2000IsSymmetricAndZeroForEqualColors() {
		double[] a = {62, 17, -40};
		double[] b = {58, -5, 12};
		assertEquals(0, CieLab.deltaE2000(a, a), 1e-12);
		assertEquals(CieLab.deltaE2000(a, b), CieLab.deltaE2000(b, a), 1e-9);
	}

	@Test
	void srgbWhiteAndBlackMapToLabEndpoints() {
		assertArrayEquals(new double[]{100, 0, 0}, CieLab.fromSrgb(255, 255, 255), 0.01);
		assertArrayEquals(new double[]{0, 0, 0}, CieLab.fromSrgb(0, 0, 0), 0.01);
	}

	private static void assertDeltaE(double expected, double[] lab1, double[] lab2) {
		assertEquals(expected, CieLab.deltaE2000(lab1, lab2), 1e-4);
	}
}
