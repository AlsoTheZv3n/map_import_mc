package dev.mapimport.client;

import dev.mapimport.convert.ConvertOptions;

import java.util.List;
import java.util.Locale;

/** What {@code /mapimport} was asked to do. */
record ImportRequest(int columns, int rows, ConvertOptions options, boolean previewOnly) {
	static final List<String> FLAGS = List.of(
			"fit", "fill", "stretch",
			"smooth", "pixel",
			"dither", "nodither",
			"compuphase", "deltae2000",
			"preview");

	/** @throws IllegalArgumentException with the unknown flag as message */
	static ImportRequest parse(int columns, int rows, String flags) {
		ConvertOptions options = ConvertOptions.DEFAULT;
		boolean preview = false;
		for (String flag : flags.trim().split("\\s+")) {
			switch (flag.toLowerCase(Locale.ROOT)) {
				case "" -> {
				}
				case "fit" -> options = options.withScale(ConvertOptions.ScaleMode.FIT);
				case "fill" -> options = options.withScale(ConvertOptions.ScaleMode.FILL);
				case "stretch" -> options = options.withScale(ConvertOptions.ScaleMode.STRETCH);
				case "smooth" -> options = options.withSmooth(true);
				case "pixel" -> options = options.withSmooth(false);
				case "dither" -> options = options.withDither(true);
				case "nodither" -> options = options.withDither(false);
				case "compuphase" -> options = options.withDistance(ConvertOptions.ColorDistance.COMPUPHASE);
				case "deltae2000" -> options = options.withDistance(ConvertOptions.ColorDistance.DELTA_E_2000);
				case "preview" -> preview = true;
				default -> throw new IllegalArgumentException(flag);
			}
		}
		return new ImportRequest(columns, rows, options, preview);
	}
}
