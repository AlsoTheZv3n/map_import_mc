package dev.mapimport.client;

import dev.mapimport.MapImport;
import dev.mapimport.convert.MapImageConverter;
import dev.mapimport.convert.MapPalette;
import dev.mapimport.convert.MapPreview;
import dev.mapimport.network.MapTilePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

/** Reads, converts and uploads one picture, off the render thread. One import at a time. */
final class ImportTask {
	private static final AtomicBoolean RUNNING = new AtomicBoolean();
	private static MapImageConverter converter;

	private ImportTask() {
	}

	static boolean tryBegin() {
		return RUNNING.compareAndSet(false, true);
	}

	static void abort(Minecraft client, Component reason) {
		RUNNING.set(false);
		message(client, reason);
	}

	static void run(Minecraft client, Path file, ImportRequest request) {
		Thread.ofPlatform().daemon().name("mapimport-convert").start(() -> {
			try {
				convertAndSend(client, file, request);
			} catch (Exception e) {
				MapImport.LOGGER.error("Import of {} failed", file, e);
				message(client, Component.translatable("mapimport.error.failed", String.valueOf(e.getMessage())));
			} finally {
				RUNNING.set(false);
			}
		});
	}

	private static void convertAndSend(Minecraft client, Path file, ImportRequest request) throws IOException {
		BufferedImage image = ImageIO.read(file.toFile());
		if (image == null) {
			message(client, Component.translatable("mapimport.error.read", file.getFileName().toString()));
			return;
		}

		String title = title(file);
		int columns = request.columns(), rows = request.rows();
		message(client, Component.translatable("mapimport.converting", title, columns, rows));
		byte[][] tiles = converter().convert(image, columns, rows, request.options());

		if (request.previewOnly()) {
			Path out = client.gameDirectory.toPath().resolve(MapImport.MOD_ID).resolve(title + "-preview.png");
			Files.createDirectories(out.getParent());
			ImageIO.write(MapPreview.render(tiles, columns, rows, converter().palette()), "png", out.toFile());
			message(client, Component.translatable("mapimport.preview_saved", out.toString()));
			return;
		}

		client.execute(() -> {
			// The player may have left the server while the picture was converting.
			if (!ClientPlayNetworking.canSend(MapTilePayload.TYPE)) {
				message(client, Component.translatable("mapimport.error.server_missing"));
				return;
			}
			for (int row = 0; row < rows; row++) {
				for (int column = 0; column < columns; column++) {
					ClientPlayNetworking.send(new MapTilePayload(title, column, row, columns, rows, tiles[row * columns + column]));
				}
			}
			message(client, Component.translatable("mapimport.sent", tiles.length));
		});
	}

	private static synchronized MapImageConverter converter() {
		if (converter == null) {
			converter = new MapImageConverter(MapPalette.vanilla());
		}
		return converter;
	}

	private static String title(Path file) {
		String name = file.getFileName().toString();
		int dot = name.lastIndexOf('.');
		if (dot > 0) {
			name = name.substring(0, dot);
		}
		return name.length() > MapTilePayload.MAX_TITLE_LENGTH ? name.substring(0, MapTilePayload.MAX_TITLE_LENGTH) : name;
	}

	private static void message(Minecraft client, Component text) {
		client.execute(() -> {
			if (client.player != null) {
				client.player.sendSystemMessage(text);
			}
		});
	}
}
