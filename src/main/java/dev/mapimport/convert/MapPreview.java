package dev.mapimport.convert;

import java.awt.image.BufferedImage;

/** Renders converted tiles back into an image, to check a conversion without the game. */
public final class MapPreview {
	private MapPreview() {
	}

	public static BufferedImage render(byte[][] tiles, int tilesWide, int tilesHigh, MapPalette palette) {
		int size = MapImageConverter.TILE_SIZE;
		BufferedImage image = new BufferedImage(tilesWide * size, tilesHigh * size, BufferedImage.TYPE_INT_ARGB);
		for (int ty = 0; ty < tilesHigh; ty++) {
			for (int tx = 0; tx < tilesWide; tx++) {
				byte[] tile = tiles[ty * tilesWide + tx];
				for (int y = 0; y < size; y++) {
					for (int x = 0; x < size; x++) {
						image.setRGB(tx * size + x, ty * size + y, palette.argbForCode(tile[x + y * size]));
					}
				}
			}
		}
		return image;
	}
}
