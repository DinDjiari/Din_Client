package gg.dindijari.client.gui.screen;

import gg.dindijari.client.render.ColorUtil;
import gg.dindijari.client.render.Fonts;
import gg.dindijari.client.render.Render2D;
import gg.dindijari.client.render.Theme;
import gg.dindijari.client.util.animation.Animation;
import gg.dindijari.client.util.animation.Easing;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * TEMPORARY Phase&nbsp;2 acceptance screen (removed in Phase&nbsp;3): renders
 * one of everything the render library provides so it can be verified visually
 * and profiled — rounded rects with per-corner radii, outlines, linear and
 * radial gradients, an animated hue-cycle bar, a drop-shadowed panel, both
 * bundled fonts at several scales, easing-driven motion, and a live FPS
 * readout.
 *
 * <p>Opened with F6 from a dev environment (see
 * {@link gg.dindijari.client.gui.ScreenManager}); it is intentionally not
 * reachable in a release build.
 */
public final class RenderDebugScreen extends ThemedScreen {

    private static final Component TITLE = Fonts.inter("Render Debug");
    private static final Component LBL_ROUNDED = Fonts.inter("Rounded / outline");
    private static final Component LBL_GRADIENT = Fonts.inter("Linear gradients");
    private static final Component LBL_RADIAL = Fonts.inter("Radial");
    private static final Component LBL_RGB = Fonts.inter("Hue cycle");
    private static final Component LBL_SHADOW = Fonts.inter("Shadow + fonts");
    private static final Component LBL_EASING = Fonts.inter("Easing back / elastic");
    private static final Component TXT_INTER = Fonts.inter("Inter — the quick brown fox 0123456789");
    private static final Component TXT_MONO = Fonts.mono("JetBrains Mono — jump(); // 0O1lI");
    private static final int RGB_SLICES = 48;

    private final Animation backDot = new Animation(0.0, 1400, Easing.BACK_OUT);
    private final Animation elasticDot = new Animation(0.0, 1400, Easing.ELASTIC_OUT);

    private int lastFps = -1;
    private Component fpsLine = Component.empty();

    /**
     * Creates the debug screen.
     */
    public RenderDebugScreen() {
        super(Component.literal("Render Debug"));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);

        float pad = Theme.px(Theme.PANEL_PADDING);
        float grid = Theme.px(Theme.GRID);
        float x = pad * 2;
        float y = pad * 2;
        float colW = (this.width - x * 2 - grid * 2) / 3.0F;

        Fonts.drawScaled(g, TITLE, x, y - Theme.px(6), 1.5F, Theme.TEXT_PRIMARY, false);
        refreshFps();
        Fonts.draw(g, fpsLine, x, this.height - 9 - pad, Theme.ACCENT, false);
        y += Theme.px(28);

        // --- Column 1: rounded rects -------------------------------------
        float cy = y;
        Fonts.drawScaled(g, LBL_ROUNDED, x, cy, 0.85F, Theme.TEXT_SECONDARY, false);
        cy += 14;
        Render2D.fillRounded(g, x, cy, colW, Theme.px(56), Theme.px(Theme.PANEL_RADIUS), Theme.PANEL);
        Render2D.fillRounded(g, x + grid, cy + grid, colW - 2 * grid, Theme.px(40),
                Theme.px(16), Theme.px(2), Theme.px(16), Theme.px(2), Theme.BUTTON_HOVER);
        Render2D.outlineRounded(g, x + grid, cy + grid, colW - 2 * grid, Theme.px(40),
                Theme.px(16), Theme.px(2), Theme.px(16), Theme.px(2), 1.0F, Theme.ACCENT);
        cy += Theme.px(56) + grid;

        Fonts.drawScaled(g, LBL_SHADOW, x, cy, 0.85F, Theme.TEXT_SECONDARY, false);
        cy += 14;
        Render2D.dropShadow(g, x + grid, cy + grid / 2, colW - 2 * grid, Theme.px(48),
                Theme.px(Theme.PANEL_RADIUS), Theme.px(14), Theme.SHADOW);
        Render2D.fillRounded(g, x + grid, cy + grid / 2, colW - 2 * grid, Theme.px(48),
                Theme.px(Theme.PANEL_RADIUS), Theme.PANEL);
        Fonts.draw(g, TXT_INTER, x + grid + Theme.px(8), cy + grid / 2 + Theme.px(8),
                Theme.TEXT_PRIMARY, false);
        Fonts.drawScaled(g, TXT_MONO, x + grid + Theme.px(8), cy + grid / 2 + Theme.px(30),
                0.85F, Theme.TEXT_SECONDARY, false);

        // --- Column 2: gradients ------------------------------------------
        float c2 = x + colW + grid;
        cy = y;
        Fonts.drawScaled(g, LBL_GRADIENT, c2, cy, 0.85F, Theme.TEXT_SECONDARY, false);
        cy += 14;
        Render2D.fillRoundedGradientV(g, c2, cy, colW, Theme.px(40), Theme.px(Theme.BUTTON_RADIUS),
                Theme.ACCENT, 0xFF16161A);
        cy += Theme.px(40) + grid;
        Render2D.fillRoundedGradientH(g, c2, cy, colW, Theme.px(40), Theme.px(Theme.BUTTON_RADIUS),
                0xFFFF55AA, 0xFF5555FF);
        cy += Theme.px(40) + grid;

        Fonts.drawScaled(g, LBL_RGB, c2, cy, 0.85F, Theme.TEXT_SECONDARY, false);
        cy += 14;
        float sliceW = colW / RGB_SLICES;
        for (int i = 0; i < RGB_SLICES; i++) {
            int color = ColorUtil.hueCycle(4000, i / (float) RGB_SLICES);
            Render2D.fillRect(g, c2 + i * sliceW, cy, sliceW + 0.5F, Theme.px(24), color);
        }
        cy += Theme.px(24) + grid;
        // Accent underline animated by hue for contrast checking.
        Render2D.fillRounded(g, c2, cy, colW, Theme.px(6), Theme.px(3),
                ColorUtil.hueCycle(4000, 0));

        // --- Column 3: radial + easing ------------------------------------
        float c3 = x + 2 * (colW + grid);
        cy = y;
        Fonts.drawScaled(g, LBL_RADIAL, c3, cy, 0.85F, Theme.TEXT_SECONDARY, false);
        cy += 14;
        float r = Theme.px(44);
        Render2D.radialGradient(g, c3 + colW / 2, cy + r, r,
                ColorUtil.withAlpha(Theme.ACCENT, 200), ColorUtil.withAlpha(Theme.ACCENT, 0));
        Render2D.fillCircle(g, c3 + colW / 2, cy + r, Theme.px(10), Theme.TEXT_PRIMARY);
        cy += 2 * r + grid;

        Fonts.drawScaled(g, LBL_EASING, c3, cy, 0.85F, Theme.TEXT_SECONDARY, false);
        cy += 14;
        // Ping-pong both dots across the column, frame-rate independent.
        if (backDot.isDone()) {
            backDot.animateTo(backDot.target() == 0.0 ? 1.0 : 0.0);
        }
        if (elasticDot.isDone()) {
            elasticDot.animateTo(elasticDot.target() == 0.0 ? 1.0 : 0.0);
        }
        float track = colW - Theme.px(16);
        Render2D.fillRounded(g, c3, cy + Theme.px(6), colW, Theme.px(4), Theme.px(2), Theme.BUTTON);
        Render2D.fillCircle(g, c3 + Theme.px(8) + backDot.valueF() * track, cy + Theme.px(8),
                Theme.px(6), Theme.ACCENT);
        cy += Theme.px(20);
        Render2D.fillRounded(g, c3, cy + Theme.px(6), colW, Theme.px(4), Theme.px(2), Theme.BUTTON);
        Render2D.fillCircle(g, c3 + Theme.px(8) + elasticDot.valueF() * track, cy + Theme.px(8),
                Theme.px(6), 0xFFFF55AA);
    }

    /** Rebuilds the FPS component only when the value changes (no per-frame alloc). */
    private void refreshFps() {
        int fps = this.minecraft.getFps();
        if (fps != lastFps) {
            lastFps = fps;
            fpsLine = Fonts.mono("FPS " + fps);
        }
    }
}
