package gg.dindijari.client.gui.screen.loading;

import gg.dindijari.client.render.ColorUtil;
import gg.dindijari.client.render.Fonts;
import gg.dindijari.client.render.Render2D;
import gg.dindijari.client.render.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Shared renderer for the client's branded loading/transition screens: solid
 * charcoal backdrop, the DINDIJARI wordmark (live text in the display-size
 * Inter face — never a baked texture, so it is crisp at any GUI scale), a thin
 * accent progress bar, and a status line in secondary grey.
 *
 * <p>The bar renders in two modes: <b>determinate</b> (0–100, drawn smoothly —
 * callers animate the value) and <b>indeterminate</b> (a sweeping accent
 * segment for screens with no progress signal). All geometry derives from the
 * design tokens in GUI units, so it scales cleanly from GUI scale 1 to 4.
 */
final class BrandedLoadingRenderer {

    private static final Component WORDMARK = Fonts.display("DINDIJARI");
    private static final Component WORDMARK_SUB = Fonts.ui("C L I E N T");

    private BrandedLoadingRenderer() {
    }

    /**
     * Renders the full branded frame.
     *
     * @param g        the draw context
     * @param width    screen width in GUI units
     * @param height   screen height in GUI units
     * @param progress progress in [0, 100], or {@code -1} for the
     *                 indeterminate sweep
     * @param status   status line (pre-styled), e.g. "Generating world"
     * @param detail   optional right-aligned detail (e.g. "42%"); may be null
     */
    static void render(GuiGraphics g, int width, int height, float progress,
                       Component status, Component detail) {
        Render2D.fillRect(g, 0, 0, width, height, Theme.BACKGROUND);

        // Faint accent glow behind the wordmark for depth.
        float cx = width / 2.0F;
        float cy = height / 2.0F;
        Render2D.radialGradient(g, cx, cy - Theme.px(20), width * 0.4F,
                ColorUtil.withAlpha(Theme.accent(), 14), ColorUtil.withAlpha(Theme.accent(), 0));

        // Wordmark: display-size Inter drawn at scale 1 (glyphs rasterised at
        // 120 px — at or above physical size up to GUI scale 4).
        float logoBaseline = cy - Theme.px(36);
        Fonts.drawCentered(g, WORDMARK, cx, logoBaseline, 1.0F, Theme.TEXT_PRIMARY, false);
        Fonts.drawCentered(g, WORDMARK_SUB, cx, logoBaseline + Theme.px(48), 1.0F,
                Theme.TEXT_SECONDARY, false);

        // Progress bar.
        float barW = Math.min(Theme.px(440), width * 0.6F);
        float barH = Theme.px(5);
        float barX = cx - barW / 2;
        float barY = cy + Theme.px(52);
        Render2D.fillRounded(g, barX, barY, barW, barH, barH / 2, Theme.BUTTON_HOVER);
        if (progress >= 0) {
            float w = barW * Math.min(progress, 100.0F) / 100.0F;
            if (w >= barH) {
                Render2D.fillRounded(g, barX, barY, w, barH, barH / 2, Theme.accent());
            }
        } else {
            // Indeterminate: a 25%-wide segment sweeping back and forth.
            float segW = barW * 0.25F;
            float t = (System.currentTimeMillis() % 1600L) / 1600.0F;
            float ping = t < 0.5F ? t * 2 : (1 - t) * 2;
            float eased = (float) Theme.HOVER_EASING.apply(ping);
            Render2D.fillRounded(g, barX + (barW - segW) * eased, barY, segW, barH,
                    barH / 2, Theme.accent());
        }

        // Status line + optional detail.
        float textY = barY + barH + Theme.px(12);
        Fonts.drawCentered(g, status, cx, textY, 1.0F, Theme.TEXT_SECONDARY, false);
        if (detail != null) {
            Fonts.draw(g, detail, barX + barW - Fonts.width(detail), barY - Theme.px(16),
                    Theme.TEXT_SECONDARY, false);
        }
    }
}
