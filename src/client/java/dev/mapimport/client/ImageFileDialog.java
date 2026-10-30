package dev.mapimport.client;

import net.minecraft.client.Minecraft;
import org.lwjgl.sdl.SDLDialog;
import org.lwjgl.sdl.SDLError;
import org.lwjgl.sdl.SDL_DialogFileCallback;
import org.lwjgl.sdl.SDL_DialogFileFilter;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.function.Consumer;

import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.system.MemoryUtil.memFree;
import static org.lwjgl.system.MemoryUtil.memGetAddress;
import static org.lwjgl.system.MemoryUtil.memUTF8;

/**
 * The native "open file" dialog through SDL3, which the game ships since it moved off GLFW
 * (the LWJGL tinyfd bindings are no longer included).
 */
final class ImageFileDialog {
	private ImageFileDialog() {
	}

	/**
	 * Must be called on the render thread. Exactly one of the callbacks runs once the dialog
	 * closes, possibly on another thread.
	 */
	static void open(Minecraft client, Consumer<Path> onChosen, Runnable onCancelled, Consumer<String> onError) {
		// The game captures the cursor once chat closes; the dialog needs it free.
		client.mouseHandler.releaseMouse();

		// SDL reads the filters and calls the callback after this method returns, so all of it is
		// heap-allocated and freed only once the dialog has answered.
		ByteBuffer name = memUTF8("Images");
		ByteBuffer pattern = memUTF8("png;jpg;jpeg;bmp;gif");
		SDL_DialogFileFilter.Buffer filters = SDL_DialogFileFilter.calloc(1);
		filters.get(0).name(name).pattern(pattern);

		SDL_DialogFileCallback[] callback = new SDL_DialogFileCallback[1];
		callback[0] = SDL_DialogFileCallback.create((userdata, fileList, filter) -> {
			try {
				if (fileList == NULL) {
					onError.accept(SDLError.SDL_GetError());
				} else {
					long first = memGetAddress(fileList);
					if (first == NULL) {
						onCancelled.run();
					} else {
						onChosen.accept(Path.of(memUTF8(first)));
					}
				}
			} finally {
				// Not from inside the native callback itself.
				client.execute(() -> {
					callback[0].free();
					filters.free();
					memFree(name);
					memFree(pattern);
				});
			}
		});

		SDLDialog.SDL_ShowOpenFileDialog(callback[0], NULL, client.getWindow().handle(), filters, (CharSequence) null, false);
	}
}
