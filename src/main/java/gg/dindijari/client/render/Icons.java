package gg.dindijari.client.render;

import gg.dindijari.client.module.Category;
import net.minecraft.client.gui.GuiGraphics;

/**
 * The client's own flat icon set, drawn procedurally from {@link Render2D}
 * primitives (rounded rects, circles, triangles) — no raster assets, no
 * third-party sprites, crisp at any GUI scale. Each icon is drawn centred on a
 * point at a given size in GUI units.
 *
 * <p>Module cards use {@link #module} to pick a glyph per category; chrome uses
 * the named icons (gear, star, close, search, favorite).
 */
public final class Icons {

    private Icons() {
    }

    /**
     * Draws a gear/settings icon.
     *
     * @param g     draw context
     * @param cx    centre x
     * @param cy    centre y
     * @param size  overall diameter in GUI units
     * @param color packed ARGB
     */
    public static void gear(GuiGraphics g, float cx, float cy, float size, int color) {
        float r = size / 2;
        // Eight teeth as small rounded rects around the rim.
        for (int i = 0; i < 8; i++) {
            double a = Math.PI * i / 4.0;
            float tx = cx + (float) Math.cos(a) * r * 0.82F;
            float ty = cy + (float) Math.sin(a) * r * 0.82F;
            Render2D.fillRounded(g, tx - size * 0.09F, ty - size * 0.09F,
                    size * 0.18F, size * 0.18F, size * 0.05F, color);
        }
        Render2D.fillCircle(g, cx, cy, r * 0.62F, color);
        Render2D.fillCircle(g, cx, cy, r * 0.28F, Theme.PANEL);
    }

    /**
     * Draws a five-point star, filled (favorite on) or outline (off).
     *
     * @param g      draw context
     * @param cx     centre x
     * @param cy     centre y
     * @param size   overall diameter
     * @param color  packed ARGB
     * @param filled {@code true} for a solid star, {@code false} for a dim dot ring
     */
    public static void star(GuiGraphics g, float cx, float cy, float size, int color, boolean filled) {
        float r = size / 2;
        int c = filled ? color : ColorUtil.withAlpha(color, 90);
        // Approximate a star with 5 triangles from the centre to outer points,
        // plus a small core, using outer/inner radii.
        float outer = r;
        float inner = r * 0.42F;
        float prevX = 0, prevY = 0, firstX = 0, firstY = 0;
        for (int i = 0; i <= 10; i++) {
            double a = -Math.PI / 2 + Math.PI * i / 5.0;
            float rad = (i % 2 == 0) ? outer : inner;
            float px = cx + (float) Math.cos(a) * rad;
            float py = cy + (float) Math.sin(a) * rad;
            if (i == 0) {
                firstX = px;
                firstY = py;
            } else {
                Render2D.fillTriangle(g, cx, cy, prevX, prevY, px, py, c);
            }
            prevX = px;
            prevY = py;
        }
        Render2D.fillTriangle(g, cx, cy, prevX, prevY, firstX, firstY, c);
        if (!filled) {
            // Hollow it out to read as an outline.
            Render2D.fillCircle(g, cx, cy, inner * 0.9F, Theme.PANEL);
        }
    }

    /**
     * Draws a close (X) icon.
     *
     * @param g     draw context
     * @param cx    centre x
     * @param cy    centre y
     * @param size  overall diameter
     * @param color packed ARGB
     */
    public static void close(GuiGraphics g, float cx, float cy, float size, int color) {
        float r = size / 2;
        float t = size * 0.13F;
        stroke(g, cx - r, cy - r, cx + r, cy + r, t, color);
        stroke(g, cx - r, cy + r, cx + r, cy - r, t, color);
    }

    /**
     * Draws a magnifier/search icon.
     *
     * @param g     draw context
     * @param cx    centre x
     * @param cy    centre y
     * @param size  overall diameter
     * @param color packed ARGB
     */
    public static void search(GuiGraphics g, float cx, float cy, float size, int color) {
        float r = size * 0.32F;
        float ox = cx - size * 0.1F;
        float oy = cy - size * 0.1F;
        Render2D.fillCircle(g, ox, oy, r, color);
        Render2D.fillCircle(g, ox, oy, r * 0.55F, Theme.BUTTON);
        stroke(g, ox + r * 0.7F, oy + r * 0.7F, cx + size * 0.42F, cy + size * 0.42F,
                size * 0.12F, color);
    }

    /**
     * Draws a small heart, for the favorites filter toggle.
     *
     * @param g      draw context
     * @param cx     centre x
     * @param cy     centre y
     * @param size   overall diameter
     * @param color  packed ARGB
     */
    public static void heart(GuiGraphics g, float cx, float cy, float size, int color) {
        float r = size * 0.26F;
        Render2D.fillCircle(g, cx - r * 0.85F, cy - r * 0.4F, r, color);
        Render2D.fillCircle(g, cx + r * 0.85F, cy - r * 0.4F, r, color);
        Render2D.fillTriangle(g, cx - size * 0.44F, cy - r * 0.1F,
                cx + size * 0.44F, cy - r * 0.1F, cx, cy + size * 0.45F, color);
    }

    /**
     * Draws the glyph representing a module, chosen by its category.
     *
     * @param g        draw context
     * @param category the module's category
     * @param cx       centre x
     * @param cy       centre y
     * @param size     overall diameter
     * @param color    packed ARGB
     */
    public static void module(GuiGraphics g, Category category, float cx, float cy,
                              float size, int color) {
        switch (category) {
            case HUD -> hud(g, cx, cy, size, color);
            case VISUALS -> eye(g, cx, cy, size, color);
            case PERFORMANCE -> bolt(g, cx, cy, size, color);
            case UTILITY -> wrench(g, cx, cy, size, color);
            case CLIENT -> gear(g, cx, cy, size, color);
        }
    }

    /**
     * Draws a lightning bolt (performance).
     *
     * @param g     draw context
     * @param cx    centre x
     * @param cy    centre y
     * @param size  overall diameter
     * @param color packed ARGB
     */
    public static void bolt(GuiGraphics g, float cx, float cy, float size, int color) {
        float u = size / 2;
        Render2D.fillTriangle(g, cx + u * 0.3F, cy - u,
                cx - u * 0.5F, cy + u * 0.15F, cx + u * 0.1F, cy + u * 0.15F, color);
        Render2D.fillTriangle(g, cx - u * 0.1F, cy + u,
                cx + u * 0.5F, cy - u * 0.15F, cx - u * 0.1F, cy - u * 0.15F, color);
    }

    private static void hud(GuiGraphics g, float cx, float cy, float size, int color) {
        float r = size / 2;
        Render2D.outlineRounded(g, cx - r, cy - r * 0.7F, size, size * 0.7F,
                size * 0.12F, size * 0.06F, color);
        Render2D.fillRounded(g, cx - r * 0.8F, cy - r * 0.45F, size * 0.35F, size * 0.12F,
                size * 0.04F, color);
        Render2D.fillRounded(g, cx - r * 0.8F, cy - r * 0.1F, size * 0.5F, size * 0.12F,
                size * 0.04F, color);
    }

    private static void eye(GuiGraphics g, float cx, float cy, float size, int color) {
        float r = size / 2;
        Render2D.fillCircle(g, cx, cy, r * 0.95F, color);
        Render2D.fillCircle(g, cx, cy, r * 0.6F, Theme.PANEL);
        Render2D.fillCircle(g, cx, cy, r * 0.28F, color);
    }

    private static void wrench(GuiGraphics g, float cx, float cy, float size, int color) {
        float r = size / 2;
        stroke(g, cx - r * 0.7F, cy + r * 0.7F, cx + r * 0.5F, cy - r * 0.5F, size * 0.16F, color);
        Render2D.fillCircle(g, cx + r * 0.6F, cy - r * 0.6F, r * 0.34F, color);
        Render2D.fillCircle(g, cx + r * 0.6F, cy - r * 0.6F, r * 0.16F, Theme.PANEL);
    }

    /** Draws a thick line segment as a thin rotated rounded rect via a triangle pair. */
    private static void stroke(GuiGraphics g, float x1, float y1, float x2, float y2,
                               float thickness, int color) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 0.0001F) {
            return;
        }
        float nx = -dy / len * thickness / 2;
        float ny = dx / len * thickness / 2;
        Render2D.fillTriangle(g, x1 + nx, y1 + ny, x1 - nx, y1 - ny, x2 + nx, y2 + ny, color);
        Render2D.fillTriangle(g, x2 + nx, y2 + ny, x2 - nx, y2 - ny, x1 - nx, y1 - ny, color);
    }
}
