package dev.mapimport.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.mapimport.network.MapTilePayload;
import dev.mapimport.server.MapCreator;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

/** {@code /mapimport <columns> <rows> [flags…]}, e.g. {@code /mapimport 2 2 fill deltae2000}. */
final class ImportCommand {
	private ImportCommand() {
	}

	static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
		dispatcher.register(literal("mapimport")
				.then(argument("columns", IntegerArgumentType.integer(1, MapCreator.MAX_GRID))
						.then(argument("rows", IntegerArgumentType.integer(1, MapCreator.MAX_GRID))
								.executes(ctx -> start(ctx, ""))
								.then(argument("flags", StringArgumentType.greedyString())
										.suggests(ImportCommand::suggestFlags)
										.executes(ctx -> start(ctx, StringArgumentType.getString(ctx, "flags")))))));
	}

	private static int start(CommandContext<FabricClientCommandSource> ctx, String flags) {
		FabricClientCommandSource source = ctx.getSource();
		ImportRequest request;
		try {
			request = ImportRequest.parse(
					IntegerArgumentType.getInteger(ctx, "columns"),
					IntegerArgumentType.getInteger(ctx, "rows"),
					flags);
		} catch (IllegalArgumentException e) {
			source.sendError(Component.translatable("mapimport.error.unknown_flag", e.getMessage(), String.join(", ", ImportRequest.FLAGS)));
			return 0;
		}
		if (!request.previewOnly() && !ClientPlayNetworking.canSend(MapTilePayload.TYPE)) {
			source.sendError(Component.translatable("mapimport.error.server_missing"));
			return 0;
		}
		if (!ImportTask.tryBegin()) {
			source.sendError(Component.translatable("mapimport.error.busy"));
			return 0;
		}
		ImageFileDialog.open(source.getClient(),
				file -> ImportTask.run(source.getClient(), file, request),
				() -> ImportTask.abort(source.getClient(), Component.translatable("mapimport.cancelled")),
				error -> ImportTask.abort(source.getClient(), Component.translatable("mapimport.error.dialog", error)));
		return 1;
	}

	/** Suggests the flags not used yet, for the word being typed. */
	private static CompletableFuture<Suggestions> suggestFlags(CommandContext<FabricClientCommandSource> ctx, SuggestionsBuilder builder) {
		String typed = builder.getRemaining();
		int wordStart = typed.lastIndexOf(' ') + 1;
		Set<String> used = Arrays.stream(typed.substring(0, wordStart).trim().split("\\s+"))
				.map(word -> word.toLowerCase(Locale.ROOT))
				.collect(Collectors.toSet());
		String prefix = typed.substring(wordStart).toLowerCase(Locale.ROOT);

		SuggestionsBuilder word = builder.createOffset(builder.getStart() + wordStart);
		for (String flag : ImportRequest.FLAGS) {
			if (flag.startsWith(prefix) && !used.contains(flag)) {
				word.suggest(flag);
			}
		}
		return word.buildFuture();
	}
}
