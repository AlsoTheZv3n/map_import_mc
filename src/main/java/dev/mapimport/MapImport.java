package dev.mapimport;

import dev.mapimport.network.MapTilePayload;
import dev.mapimport.server.MapCreator;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MapImport implements ModInitializer {
	public static final String MOD_ID = "mapimport";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		PayloadTypeRegistry.serverboundPlay().register(MapTilePayload.TYPE, MapTilePayload.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(MapTilePayload.TYPE, new MapCreator()::receive);
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
