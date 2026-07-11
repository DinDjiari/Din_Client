package gg.dindijari.client.gui.screen.loading;

import gg.dindijari.client.render.Fonts;
import gg.dindijari.client.util.animation.Animation;
import gg.dindijari.client.util.animation.Easing;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.progress.StoringChunkProgressListener;

/**
 * Branded replacement for the vanilla world-loading screen (chunk generation
 * progress while creating/opening a singleplayer world). Swapped in by
 * {@link gg.dindijari.client.gui.ScreenManager}; the vanilla screen's private
 * progress listener is reachable through a NeoForge access transformer.
 *
 * <p>Extends {@link LevelLoadingScreen} so narration and lifecycle behaviour
 * are inherited unchanged; only rendering is replaced — dark backdrop, the
 * wordmark, a smoothly animated accent progress bar and a percentage readout
 * instead of the vanilla chunk-status pixel map.
 */
public final class BrandedLevelLoadingScreen extends LevelLoadingScreen {

    private static final Component STATUS = Fonts.ui("Generating world");

    private final StoringChunkProgressListener listener;
    private final Animation smoothProgress = new Animation(0.0, 350, Easing.CUBIC_OUT);

    private int lastPercent = -1;
    private Component percentText = Component.empty();

    /**
     * Creates the branded loading screen.
     *
     * @param listener the chunk progress listener taken from the replaced
     *                 vanilla screen
     */
    public BrandedLevelLoadingScreen(StoringChunkProgressListener listener) {
        super(listener);
        this.listener = listener;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int progress = listener.getProgress();
        smoothProgress.animateTo(Math.max(0, Math.min(100, progress)));
        if (progress != lastPercent) {
            lastPercent = progress;
            percentText = Fonts.ui(Math.max(0, progress) + "%");
        }
        BrandedLoadingRenderer.render(g, this.width, this.height,
                smoothProgress.valueF(), STATUS, percentText);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Fully painted by render(); no vanilla background.
    }
}
