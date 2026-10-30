package dev.mapimport.convert;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapPaletteTest {
	@Test
	void vanillaPaletteHasEveryBaseColorInFourShades() {
		MapPalette palette = MapPalette.vanilla();

		// 61 base colors since 1.17 (ids 1–61), each in 4 shades.
		assertEquals(61 * 4, palette.size());
		Set<Byte> codes = new HashSet<>();
		for (int i = 0; i < palette.size(); i++) {
			byte code = palette.code(i);
			assertNotEquals(0, (code & 0xFF) >> 2, "transparent color in palette");
			assertTrue(codes.add(code), "duplicate code " + code);
		}
	}

	@Test
	void vanillaShadesFollowTheWikiFormula() {
		MapPalette palette = MapPalette.vanilla();
		// GRASS (id 1) = 127,178,56; shade 2 (×255/255) is the base color, shade 3 is ×135/255.
		assertEquals(0x7FB238, rgbForCode(palette, (byte) (1 * 4 + 2)));
		assertEquals((127 * 135 / 255) << 16 | (178 * 135 / 255) << 8 | 56 * 135 / 255, rgbForCode(palette, (byte) (1 * 4 + 3)));
	}

	private static int rgbForCode(MapPalette palette, byte code) {
		return palette.argbForCode(code) & 0xFFFFFF;
	}
}
