package gg.dindijari.client.branding;

import com.mojang.blaze3d.platform.NativeImage;
import gg.dindijari.client.gui.notify.Notifications;
import gg.dindijari.client.module.modules.client.BrandingModule;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Applies the Branding module to the OS window: the custom caption and the
 * custom icon.
 *
 * <p><b>Title:</b> written through {@code glfwSetWindowTitle}. Vanilla
 * rewrites the caption on world join/leave, so the custom title is re-applied
 * cheaply every two seconds from the client tick (setting the same string is
 * a no-op for the window manager).
 *
 * <p><b>Icon:</b> the converted PNG set under {@code config/dindijariclient/
 * icon/} (see {@link IconConverter}) is loaded with STB and handed to
 * {@code glfwSetWindowIcon} — the same mechanism vanilla uses at boot — on
 * startup and immediately when the user picks a new image, so no restart is
 * needed on Windows/X11. macOS does not support runtime window icons (GLFW
 * limitation; vanilla has the same restriction) and Wayland compositors ignore
 * them — documented in LIMITATIONS.md.
 */
public final class WindowBranding {

    private static final Logger LOGGER = LoggerFactory.getLogger("dindijariclient/branding");

    /** Texture id of the live 32px icon preview shown in the Branding panel. */
    public static final ResourceLocation PREVIEW_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "dindijariclient", "textures/dynamic/icon_preview");

    private static BrandingModule module;
    private static Path iconDir;
    private static boolean startupApplied;
    private static int tickCounter;
    private static DynamicTexture previewTexture;
    private static boolean previewAvailable;

    private WindowBranding() {
    }

    /**
     * Wires the branding runtime. Called once during mod construction.
     *
     * @param brandingModule the branding module
     * @param configRoot     the client's config root directory
     */
    public static void init(BrandingModule brandingModule, Path configRoot) {
        module = brandingModule;
        iconDir = configRoot.resolve("icon");
        brandingModule.windowTitle().onChange(v -> applyTitle());
        NeoForge.EVENT_BUS.addListener(WindowBranding::onTick);
    }

    private static void onTick(ClientTickEvent.Post event) {
        if (!startupApplied) {
            // First tick after boot: window and GL context exist, re-apply the
            // persisted branding from the last session.
            startupApplied = true;
            applyTitle();
            applyIcon();
            refreshPreview();
            return;
        }
        if (++tickCounter % 40 == 0) {
            applyTitle();
        }
    }

    /** Writes the effective caption to the OS window. */
    public static void applyTitle() {
        if (module == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.getWindow() == null) {
            return;
        }
        GLFW.glfwSetWindowTitle(minecraft.getWindow().getWindow(), module.effectiveTitle());
    }

    /**
     * Indicates whether a converted custom icon set exists on disk.
     *
     * @return {@code true} if a custom icon is stored
     */
    public static boolean hasCustomIcon() {
        return iconDir != null && Files.isRegularFile(iconDir.resolve("icon_32.png"));
    }

    /**
     * The preview texture for the Branding panel, if a custom icon is loaded.
     *
     * @return the texture id, or {@code null} when no custom icon is set
     */
    public static ResourceLocation previewTexture() {
        return previewAvailable ? PREVIEW_TEXTURE : null;
    }

    /**
     * Converts a user-picked image on a background thread, stores the PNG set,
     * applies it to the window and updates the preview. Failures surface as an
     * error toast; nothing is faked.
     *
     * @param source the picked image file
     */
    public static void setCustomIcon(Path source) {
        CompletableFuture
                .runAsync(() -> {
                    try {
                        IconConverter.convert(source, iconDir);
                    } catch (IconConverter.IconConversionException e) {
                        throw new java.util.concurrent.CompletionException(e);
                    }
                }, Util.backgroundExecutor())
                .whenComplete((v, err) -> Minecraft.getInstance().execute(() -> {
                    if (err != null) {
                        Throwable cause = err.getCause() == null ? err : err.getCause();
                        LOGGER.warn("Window icon conversion failed for {}", source, cause);
                        Notifications.error(cause.getMessage() == null
                                ? "Could not convert the image." : cause.getMessage());
                        return;
                    }
                    applyIcon();
                    refreshPreview();
                    Notifications.info("Window icon updated.");
                }));
    }

    /**
     * Deletes the stored icon set, restores the vanilla icon and clears the
     * preview.
     */
    public static void removeCustomIcon() {
        if (iconDir == null) {
            return;
        }
        for (int size : IconConverter.SIZES) {
            try {
                Files.deleteIfExists(iconDir.resolve("icon_" + size + ".png"));
            } catch (IOException e) {
                LOGGER.warn("Could not delete stored icon", e);
            }
        }
        refreshPreview();
        Minecraft minecraft = Minecraft.getInstance();
        if (Minecraft.ON_OSX) {
            return;
        }
        try {
            minecraft.getWindow().setIcon(minecraft.getVanillaPackResources(),
                    com.mojang.blaze3d.platform.IconSet.RELEASE);
        } catch (IOException e) {
            LOGGER.warn("Could not restore the vanilla window icon", e);
        }
        Notifications.info("Window icon removed.");
    }

    /**
     * Loads the stored PNG set and hands it to GLFW. No-op when no custom icon
     * exists or the platform cannot change icons at runtime (macOS).
     */
    public static void applyIcon() {
        if (!hasCustomIcon() || Minecraft.ON_OSX) {
            return;
        }
        List<Path> files = new ArrayList<>();
        for (int size : IconConverter.SIZES) {
            Path file = iconDir.resolve("icon_" + size + ".png");
            if (Files.isRegularFile(file)) {
                files.add(file);
            }
        }
        if (files.isEmpty()) {
            return;
        }
        List<ByteBuffer> pixelBuffers = new ArrayList<>(files.size());
        try (MemoryStack stack = MemoryStack.stackPush()) {
            GLFWImage.Buffer icons = GLFWImage.malloc(files.size(), stack);
            int count = 0;
            for (Path file : files) {
                byte[] bytes = Files.readAllBytes(file);
                ByteBuffer encoded = MemoryUtil.memAlloc(bytes.length);
                encoded.put(bytes).flip();
                IntBuffer w = stack.mallocInt(1);
                IntBuffer h = stack.mallocInt(1);
                IntBuffer channels = stack.mallocInt(1);
                ByteBuffer pixels = STBImage.stbi_load_from_memory(encoded, w, h, channels, 4);
                MemoryUtil.memFree(encoded);
                if (pixels == null) {
                    continue;
                }
                pixelBuffers.add(pixels);
                icons.position(count);
                icons.width(w.get(0));
                icons.height(h.get(0));
                icons.pixels(pixels);
                count++;
            }
            if (count > 0) {
                icons.position(0);
                icons.limit(count);
                GLFW.glfwSetWindowIcon(Minecraft.getInstance().getWindow().getWindow(), icons);
                LOGGER.info("Applied custom window icon ({} sizes)", count);
            }
        } catch (IOException e) {
            LOGGER.warn("Could not apply the stored window icon", e);
        } finally {
            // glfwSetWindowIcon copies the pixel data, so free after the call.
            for (ByteBuffer pixels : pixelBuffers) {
                STBImage.stbi_image_free(pixels);
            }
        }
    }

    /** (Re)loads the 32px preview texture from disk; clears it when absent. */
    private static void refreshPreview() {
        Path file = iconDir == null ? null : iconDir.resolve("icon_32.png");
        if (file == null || !Files.isRegularFile(file)) {
            previewAvailable = false;
            if (previewTexture != null) {
                Minecraft.getInstance().getTextureManager().release(PREVIEW_TEXTURE);
                previewTexture = null;
            }
            return;
        }
        try (InputStream in = Files.newInputStream(file)) {
            NativeImage image = NativeImage.read(in);
            if (previewTexture != null) {
                Minecraft.getInstance().getTextureManager().release(PREVIEW_TEXTURE);
            }
            previewTexture = new DynamicTexture(image);
            Minecraft.getInstance().getTextureManager().register(PREVIEW_TEXTURE, previewTexture);
            previewAvailable = true;
        } catch (IOException e) {
            LOGGER.warn("Could not load the icon preview", e);
            previewAvailable = false;
        }
    }
}
