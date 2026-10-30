package dev.mapimport.server;

import dev.mapimport.MapImport;
import dev.mapimport.convert.MapImageConverter;
import dev.mapimport.network.MapTilePayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.util.Prediction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Turns uploaded tiles into locked map items. All methods run on the server thread. */
public final class MapCreator {
	public static final int MAX_GRID = 8;
	/** Tiles one player may upload per minute: one full 8 × 8 picture. */
	private static final int TILES_PER_MINUTE = MAX_GRID * MAX_GRID;
	private static final long MINUTE_MS = 60_000;

	/**
	 * A dimension that does not exist. Maps are only redrawn while held in their own dimension, so
	 * the game never paints terrain over the picture.
	 */
	private static final ResourceKey<Level> DIMENSION = ResourceKey.create(Registries.DIMENSION, MapImport.id("imported"));

	private static final boolean[] VALID_CODES = new boolean[256];

	static {
		for (int code = 0; code < 256; code++) {
			int id = code >> 2;
			VALID_CODES[code] = id == 0 || MapColor.byId(id) != MapColor.NONE;
		}
	}

	private final Map<UUID, Quota> quotas = new HashMap<>();

	public void receive(MapTilePayload payload, ServerPlayNetworking.Context context) {
		ServerPlayer player = context.player();
		boolean firstTile = payload.column() == 0 && payload.row() == 0;

		if (!mayImport(player)) {
			if (firstTile) {
				player.sendSystemMessage(Component.translatable("mapimport.error.permission"));
			}
			return;
		}
		if (!isWellFormed(payload)) {
			MapImport.LOGGER.warn("Rejected malformed map tile from {}", player.getName().getString());
			return;
		}
		Quota quota = quotas.computeIfAbsent(player.getUUID(), uuid -> new Quota());
		if (!quota.tryTake(System.currentTimeMillis())) {
			if (quota.warn()) {
				player.sendSystemMessage(Component.translatable("mapimport.error.rate_limit"));
			}
			return;
		}

		ItemStack stack = createMap(context.server().overworld(), payload);
		if (!player.getInventory().add(stack)) {
			player.drop(stack, false, Prediction.SERVER_ONLY);
		}
	}

	public void forget(UUID player) {
		quotas.remove(player);
	}

	private static boolean mayImport(ServerPlayer player) {
		return player.isCreative() || player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
	}

	static boolean isWellFormed(MapTilePayload payload) {
		if (payload.colors().length != MapImageConverter.TILE_BYTES
				|| payload.columns() < 1 || payload.columns() > MAX_GRID
				|| payload.rows() < 1 || payload.rows() > MAX_GRID
				|| payload.column() < 0 || payload.column() >= payload.columns()
				|| payload.row() < 0 || payload.row() >= payload.rows()) {
			return false;
		}
		// An unknown color id would break rendering on every client that sees the map.
		for (byte code : payload.colors()) {
			if (!VALID_CODES[code & 0xFF]) {
				return false;
			}
		}
		return true;
	}

	private static ItemStack createMap(ServerLevel level, MapTilePayload payload) {
		MapItemSavedData data = MapItemSavedData.createFresh(0, 0, (byte) 0, false, false, DIMENSION);
		System.arraycopy(payload.colors(), 0, data.colors, 0, MapImageConverter.TILE_BYTES);
		data = data.locked();
		data.setDirty();

		MapId id = level.getFreeMapId();
		level.setMapData(id, data);

		ItemStack stack = new ItemStack(Items.FILLED_MAP);
		stack.set(DataComponents.MAP_ID, id);
		if (payload.columns() == 1 && payload.rows() == 1) {
			stack.set(DataComponents.ITEM_NAME, Component.literal(payload.title()));
		} else {
			stack.set(DataComponents.ITEM_NAME, Component.translatable("item.mapimport.tile",
					payload.title(), payload.column() + 1, payload.row() + 1));
			stack.set(DataComponents.LORE, new ItemLore(List.of(Component.translatable("item.mapimport.tile.lore",
					payload.columns(), payload.rows()))));
		}
		return stack;
	}

	private static final class Quota {
		private long windowStart;
		private int taken;
		private boolean warned;

		boolean tryTake(long now) {
			if (now - windowStart >= MINUTE_MS) {
				windowStart = now;
				taken = 0;
				warned = false;
			}
			if (taken >= TILES_PER_MINUTE) {
				return false;
			}
			taken++;
			return true;
		}

		/** True only the first time per window, so a rejected picture produces one message. */
		boolean warn() {
			boolean first = !warned;
			warned = true;
			return first;
		}
	}
}
