package dev.mapimport.convert;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.awt.image.BufferedImage;

import static dev.mapimport.convert.MapImageConverter.TILE_BYTES;
import static dev.mapimport.convert.MapImageConverter.TILE_SIZE;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MapImageConverterTest {
	private static final int RED = 0xFF0000, GREEN = 0x00FF00, BLUE = 0x0000FF, WHITE = 0xFFFFFF, BLACK = 0x000000;
	private static final MapPalette PALETTE = new MapPalette(
			new int[]{RED, GREEN, BLUE, WHITE, BLACK},
			new byte[]{10, 20, 30, 40, 50});

	private static final ConvertOptions EXACT = new ConvertOptions(
			ConvertOptions.ScaleMode.STRETCH, false, ConvertOptions.ColorDistance.COMPUPHASE, false, 128);

	private final MapImageConverter converter = new MapImageConverter(PALETTE);

	@ParameterizedTest
	@EnumSource(ConvertOptions.ColorDistance.class)
	void paletteColorsComeOutUnchanged(ConvertOptions.ColorDistance distance) {
		BufferedImage image = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < TILE_SIZE; y++) {
			for (int x = 0; x < TILE_SIZE; x++) {
				image.setRGB(x, y, 0xFF000000 | PALETTE.rgb((x + y) % PALETTE.size()));
			}
		}

		byte[] tile = converter.convert(image, 1, 1, EXACT.withDistance(distance))[0];

		for (int y = 0; y < TILE_SIZE; y++) {
			for (int x = 0; x < TILE_SIZE; x++) {
				assertEquals(PALETTE.code((x + y) % PALETTE.size()), tile[x + y * TILE_SIZE], "pixel " + x + "," + y);
			}
		}
	}

	@Test
	void pixelsBelowAlphaThresholdBecomeTransparent() {
		BufferedImage image = filled(TILE_SIZE, TILE_SIZE, 0x7FFF0000);

		byte[] tile = converter.convert(image, 1, 1, EXACT)[0];

		assertArrayEquals(new byte[TILE_BYTES], tile);
	}

	@Test
	void tilesAreCutInRowOrder() {
		BufferedImage image = new BufferedImage(2 * TILE_SIZE, 2 * TILE_SIZE, BufferedImage.TYPE_INT_ARGB);
		int[] quadrants = {RED, GREEN, BLUE, WHITE};
		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				image.setRGB(x, y, 0xFF000000 | quadrants[(y / TILE_SIZE) * 2 + x / TILE_SIZE]);
			}
		}

		byte[][] tiles = converter.convert(image, 2, 2, EXACT);

		assertEquals(4, tiles.length);
		for (int i = 0; i < 4; i++) {
			byte[] expected = new byte[TILE_BYTES];
			java.util.Arrays.fill(expected, PALETTE.code(i));
			assertArrayEquals(expected, tiles[i], "tile " + i);
		}
	}

	@Test
	void ditheringMixesBlackAndWhiteIntoGray() {
		MapImageConverter blackWhite = new MapImageConverter(new MapPalette(new int[]{BLACK, WHITE}, new byte[]{4, 8}));
		BufferedImage gray = filled(TILE_SIZE, TILE_SIZE, 0xFF808080);

		byte[] tile = blackWhite.convert(gray, 1, 1, EXACT.withDither(true))[0];

		long white = 0;
		for (byte b : tile) {
			if (b == 8) {
				white++;
			}
		}
		assertEquals(128 / 255.0, white / (double) TILE_BYTES, 0.02);
	}

	@Test
	void fitKeepsAspectRatioWithTransparentBorder() {
		// 2:1 picture on a square map: 128 × 64 centred, 32 transparent rows above and below.
		BufferedImage wide = filled(256, 128, 0xFF0000FF);

		byte[] tile = converter.convert(wide, 1, 1, EXACT.withScale(ConvertOptions.ScaleMode.FIT))[0];

		for (int y = 0; y < TILE_SIZE; y++) {
			byte expected = y < 32 || y >= 96 ? 0 : PALETTE.code(2);
			for (int x = 0; x < TILE_SIZE; x++) {
				assertEquals(expected, tile[x + y * TILE_SIZE], "pixel " + x + "," + y);
			}
		}
	}

	@Test
	void previewRendersThePaletteColors() {
		byte[] tile = new byte[TILE_BYTES];
		tile[0] = PALETTE.code(0);

		BufferedImage preview = MapPreview.render(new byte[][]{tile}, 1, 1, PALETTE);

		assertEquals(0xFF000000 | RED, preview.getRGB(0, 0));
		assertEquals(0, preview.getRGB(1, 0));
	}

	private static BufferedImage filled(int width, int height, int argb) {
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				image.setRGB(x, y, argb);
			}
		}
		return image;
	}
}
