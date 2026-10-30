package dev.mapimport.convert;

import net.minecraft.world.level.material.MapColor;

import java.util.Arrays;

/** The colors a map can show, each with its RGB value and the byte stored in the map. */
public final class MapPalette {
	private static MapPalette vanilla;

	private final int[] rgb;
	private final byte[] codes;
	/** ARGB per map byte, 0 (transparent) for bytes without a color. */
	private final int[] argbByCode = new int[256];

	public MapPalette(int[] rgb, byte[] codes) {
		if (rgb.length != codes.length || rgb.length == 0) {
			throw new IllegalArgumentException("Need the same, non-zero number of colors and codes");
		}
		this.rgb = rgb.clone();
		this.codes = codes.clone();
		for (int i = 0; i < rgb.length; i++) {
			argbByCode[codes[i] & 0xFF] = 0xFF000000 | rgb[i] & 0xFFFFFF;
		}
	}

	/**
	 * Every visible color of the running game version: each base color in all four shades. Read
	 * from {@link MapColor} so it always matches what the client renders.
	 */
	public static synchronized MapPalette vanilla() {
		if (vanilla == null) {
			int[] rgb = new int[64 * 4];
			byte[] codes = new byte[64 * 4];
			int n = 0;
			for (int id = 1; id < 64; id++) {
				MapColor color = MapColor.byId(id);
				if (color == MapColor.NONE) {
					continue;
				}
				for (MapColor.Brightness brightness : MapColor.Brightness.values()) {
					rgb[n] = color.calculateARGBColor(brightness) & 0xFFFFFF;
					codes[n] = color.getPackedId(brightness);
					n++;
				}
			}
			vanilla = new MapPalette(Arrays.copyOf(rgb, n), Arrays.copyOf(codes, n));
		}
		return vanilla;
	}

	public int size() {
		return rgb.length;
	}

	/** RGB as {@code 0xRRGGBB}. */
	public int rgb(int index) {
		return rgb[index];
	}

	public byte code(int index) {
		return codes[index];
	}

	public int argbForCode(byte code) {
		return argbByCode[code & 0xFF];
	}
}
