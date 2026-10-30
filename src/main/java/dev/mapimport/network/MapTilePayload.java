package dev.mapimport.network;

import dev.mapimport.MapImport;
import dev.mapimport.convert.MapImageConverter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * One 128 × 128 map of an imported picture. A picture of several maps is sent as one payload per
 * tile, because serverbound payloads are capped at 32 KiB.
 *
 * @param column 0-based position of this tile in the picture
 * @param colors map color bytes, indexed {@code x + y * 128}
 */
public record MapTilePayload(String title, int column, int row, int columns, int rows, byte[] colors) implements CustomPacketPayload {
	public static final int MAX_TITLE_LENGTH = 64;

	public static final CustomPacketPayload.Type<MapTilePayload> TYPE = new CustomPacketPayload.Type<>(MapImport.id("tile"));
	public static final StreamCodec<RegistryFriendlyByteBuf, MapTilePayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.stringUtf8(MAX_TITLE_LENGTH), MapTilePayload::title,
			ByteBufCodecs.VAR_INT, MapTilePayload::column,
			ByteBufCodecs.VAR_INT, MapTilePayload::row,
			ByteBufCodecs.VAR_INT, MapTilePayload::columns,
			ByteBufCodecs.VAR_INT, MapTilePayload::rows,
			ByteBufCodecs.byteArray(MapImageConverter.TILE_BYTES), MapTilePayload::colors,
			MapTilePayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
