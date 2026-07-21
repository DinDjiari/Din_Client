package gg.dindijari.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;

/**
 * The client's 2D shape library: rounded rectangles (filled and outlined, with
 * per-corner radii), linear and radial gradients, and drop shadows.
 *
 * <p>Everything is drawn through {@link GuiGraphics#bufferSource()} with
 * {@link RenderType#gui()}, exactly like vanilla GUI fills — the render type
 * owns all GL state (shader, blend, depth), so nothing here touches
 * {@code RenderSystem} and no GL state can leak. Vertex winding matches
 * vanilla's {@code GuiGraphics.fill} so the shapes respect whatever cull state
 * the GUI pipeline runs with.
 *
 * <p>Geometry is computed on the fly from a precomputed quarter-circle sine
 * table; no arrays, buffers or objects are allocated per call, keeping render
 * loops garbage-free. Curves use {@value #SEGMENTS} segments per corner, which
 * is visually smooth at the radii the {@link Theme} uses.
 *
 * <p>Coordinates are GUI units (floats accepted). Colours are packed ARGB.
 */
public final class Render2D {

    /** Arc segments per 90° corner. */
    private static final int SEGMENTS = 8;

    /** {@code SIN[j] = sin(j / SEGMENTS * 90°)}; cosine is {@code SIN[SEGMENTS - j]}. */
    private static final float[] SIN = new float[SEGMENTS + 1];

    static {
        for (int j = 0; j <= SEGMENTS; j++) {
            SIN[j] = (float) Math.sin(Math.toRadians(90.0 * j / SEGMENTS));
        }
    }

    /** Gradient axis selectors for the internal emitters. */
    private static final int SOLID = 0;
    private static final int VERTICAL = 1;
    private static final int HORIZONTAL = 2;

    private Render2D() {
    }

    // ------------------------------------------------------------------
    // Plain and rounded fills
    // ------------------------------------------------------------------

    /**
     * Fills an axis-aligned rectangle. Float-precision counterpart of
     * {@code GuiGraphics.fill}.
     *
     * @param g     the draw context
     * @param x     left edge
     * @param y     top edge
     * @param w     width
     * @param h     height
     * @param color packed ARGB fill colour
     */
    public static void fillRect(GuiGraphics g, float x, float y, float w, float h, int color) {
        Matrix4f m = g.pose().last().pose();
        VertexConsumer vc = g.bufferSource().getBuffer(RenderType.gui());
        vc.addVertex(m, x, y, 0).setColor(color);
        vc.addVertex(m, x, y + h, 0).setColor(color);
        vc.addVertex(m, x + w, y + h, 0).setColor(color);
        vc.addVertex(m, x + w, y, 0).setColor(color);
    }

    /**
     * Fills a rounded rectangle with a uniform corner radius.
     *
     * @param g      the draw context
     * @param x      left edge
     * @param y      top edge
     * @param w      width
     * @param h      height
     * @param radius corner radius applied to all four corners
     * @param color  packed ARGB fill colour
     */
    public static void fillRounded(GuiGraphics g, float x, float y, float w, float h,
                                   float radius, int color) {
        fillRounded(g, x, y, w, h, radius, radius, radius, radius, color);
    }

    /**
     * Fills a rounded rectangle with per-corner radii.
     *
     * @param g   the draw context
     * @param x   left edge
     * @param y   top edge
     * @param w   width
     * @param h   height
     * @param rTL top-left radius
     * @param rTR top-right radius
     * @param rBR bottom-right radius
     * @param rBL bottom-left radius
     * @param color packed ARGB fill colour
     */
    public static void fillRounded(GuiGraphics g, float x, float y, float w, float h,
                                   float rTL, float rTR, float rBR, float rBL, int color) {
        emitFan(g, x, y, w, h, rTL, rTR, rBR, rBL, SOLID, color, color);
    }

    // ------------------------------------------------------------------
    // Linear gradients
    // ------------------------------------------------------------------

    /**
     * Fills a rounded rectangle with a top-to-bottom linear gradient.
     *
     * @param g      the draw context
     * @param x      left edge
     * @param y      top edge
     * @param w      width
     * @param h      height
     * @param radius uniform corner radius
     * @param top    packed ARGB colour at the top edge
     * @param bottom packed ARGB colour at the bottom edge
     */
    public static void fillRoundedGradientV(GuiGraphics g, float x, float y, float w, float h,
                                            float radius, int top, int bottom) {
        emitFan(g, x, y, w, h, radius, radius, radius, radius, VERTICAL, top, bottom);
    }

    /**
     * Fills a rounded rectangle with a left-to-right linear gradient.
     *
     * @param g      the draw context
     * @param x      left edge
     * @param y      top edge
     * @param w      width
     * @param h      height
     * @param radius uniform corner radius
     * @param left   packed ARGB colour at the left edge
     * @param right  packed ARGB colour at the right edge
     */
    public static void fillRoundedGradientH(GuiGraphics g, float x, float y, float w, float h,
                                            float radius, int left, int right) {
        emitFan(g, x, y, w, h, radius, radius, radius, radius, HORIZONTAL, left, right);
    }

    // ------------------------------------------------------------------
    // Circles and radial gradients
    // ------------------------------------------------------------------

    /**
     * Fills a circle.
     *
     * @param g      the draw context
     * @param cx     centre x
     * @param cy     centre y
     * @param radius circle radius
     * @param color  packed ARGB fill colour
     */
    public static void fillCircle(GuiGraphics g, float cx, float cy, float radius, int color) {
        fillRounded(g, cx - radius, cy - radius, radius * 2, radius * 2, radius, color);
    }

    /**
     * Fills a circle with a radial gradient from its centre to its edge.
     * With an {@code outer} colour whose alpha is zero this doubles as a soft
     * glow / vignette primitive.
     *
     * @param g      the draw context
     * @param cx     centre x
     * @param cy     centre y
     * @param radius circle radius
     * @param inner  packed ARGB colour at the centre
     * @param outer  packed ARGB colour at the rim
     */
    public static void radialGradient(GuiGraphics g, float cx, float cy, float radius,
                                      int inner, int outer) {
        // A rounded rect whose radius is half its size is exactly a circle, so
        // the same fan emitter renders it; the centre vertex carries the inner
        // colour and every rim vertex the outer colour.
        emitRadialFan(g, cx, cy, radius, inner, outer);
    }

    /**
     * Fills a single triangle (emitted as a degenerate GUI quad). Vertices may
     * be given in any order; both windings are emitted so the triangle is
     * visible regardless of the pipeline's cull state.
     *
     * @param g     the draw context
     * @param x1    first vertex x
     * @param y1    first vertex y
     * @param x2    second vertex x
     * @param y2    second vertex y
     * @param x3    third vertex x
     * @param y3    third vertex y
     * @param color packed ARGB fill colour
     */
    public static void fillTriangle(GuiGraphics g, float x1, float y1, float x2, float y2,
                                    float x3, float y3, int color) {
        Matrix4f m = g.pose().last().pose();
        VertexConsumer vc = g.bufferSource().getBuffer(RenderType.gui());
        vc.addVertex(m, x1, y1, 0).setColor(color);
        vc.addVertex(m, x2, y2, 0).setColor(color);
        vc.addVertex(m, x3, y3, 0).setColor(color);
        vc.addVertex(m, x3, y3, 0).setColor(color);
        vc.addVertex(m, x1, y1, 0).setColor(color);
        vc.addVertex(m, x3, y3, 0).setColor(color);
        vc.addVertex(m, x2, y2, 0).setColor(color);
        vc.addVertex(m, x2, y2, 0).setColor(color);
    }

    // ------------------------------------------------------------------
    // Outlines and shadows
    // ------------------------------------------------------------------

    /**
     * Strokes the outline of a rounded rectangle with a uniform radius. The
     * stroke lies fully inside the given bounds.
     *
     * @param g         the draw context
     * @param x         left edge
     * @param y         top edge
     * @param w         width
     * @param h         height
     * @param radius    uniform corner radius
     * @param thickness stroke width, extending inwards
     * @param color     packed ARGB stroke colour
     */
    public static void outlineRounded(GuiGraphics g, float x, float y, float w, float h,
                                      float radius, float thickness, int color) {
        outlineRounded(g, x, y, w, h, radius, radius, radius, radius, thickness, color);
    }

    /**
     * Strokes the outline of a rounded rectangle with per-corner radii. The
     * stroke lies fully inside the given bounds.
     *
     * @param g         the draw context
     * @param x         left edge
     * @param y         top edge
     * @param w         width
     * @param h         height
     * @param rTL       top-left radius
     * @param rTR       top-right radius
     * @param rBR       bottom-right radius
     * @param rBL       bottom-left radius
     * @param thickness stroke width, extending inwards
     * @param color     packed ARGB stroke colour
     */
    public static void outlineRounded(GuiGraphics g, float x, float y, float w, float h,
                                      float rTL, float rTR, float rBR, float rBL,
                                      float thickness, int color) {
        float t = Math.min(thickness, Math.min(w, h) / 2.0F);
        emitBand(g,
                x + t, y + t, w - 2 * t, h - 2 * t,
                Math.max(0, rTL - t), Math.max(0, rTR - t), Math.max(0, rBR - t), Math.max(0, rBL - t),
                x, y, w, h, rTL, rTR, rBR, rBL,
                color, color);
    }

    /**
     * Draws a soft drop shadow around (outside) a rounded rectangle. The shadow
     * fades linearly from {@code color} at the shape's edge to fully
     * transparent {@code spread} units away. Draw it <em>before</em> the panel
     * it belongs to.
     *
     * @param g      the draw context
     * @param x      panel left edge
     * @param y      panel top edge
     * @param w      panel width
     * @param h      panel height
     * @param radius the panel's corner radius
     * @param spread how far the shadow extends beyond the panel
     * @param color  packed ARGB shadow colour at the panel edge (alpha included),
     *               e.g. {@link Theme#SHADOW}
     */
    public static void dropShadow(GuiGraphics g, float x, float y, float w, float h,
                                  float radius, float spread, int color) {
        if (Theme.reducedEffects()) {
            return;
        }
        int transparent = color & 0x00FFFFFF;
        emitBand(g,
                x, y, w, h, radius, radius, radius, radius,
                x - spread, y - spread, w + 2 * spread, h + 2 * spread,
                radius + spread, radius + spread, radius + spread, radius + spread,
                color, transparent);
    }

    /**
     * Draws the client's signature animated RGB gradient line: a horizontal bar
     * whose hue sweeps along its length and drifts with time at the theme's
     * RGB speed. Used on panel header top edges.
     *
     * @param g the draw context
     * @param x left edge
     * @param y top edge
     * @param w width
     * @param h height (thickness)
     */
    public static void rgbLine(GuiGraphics g, float x, float y, float w, float h) {
        if (Theme.reducedEffects()) {
            // Performance Mode: a static accent line instead of the animated sweep.
            fillRect(g, x, y, w, h, Theme.accent());
            return;
        }
        final int slices = 24;
        float sliceW = w / slices;
        long period = Theme.rgbPeriodMs();
        for (int i = 0; i < slices; i++) {
            int c1 = ColorUtil.hueCycle(period, i / (float) slices * 0.5F);
            int c2 = ColorUtil.hueCycle(period, (i + 1) / (float) slices * 0.5F);
            // Horizontal gradient per slice keeps the sweep smooth.
            Matrix4f m = g.pose().last().pose();
            VertexConsumer vc = g.bufferSource().getBuffer(RenderType.gui());
            float sx = x + i * sliceW;
            vc.addVertex(m, sx, y, 0).setColor(c1);
            vc.addVertex(m, sx, y + h, 0).setColor(c1);
            vc.addVertex(m, sx + sliceW, y + h, 0).setColor(c2);
            vc.addVertex(m, sx + sliceW, y, 0).setColor(c2);
        }
    }

    // ------------------------------------------------------------------
    // Internal geometry emitters (allocation-free)
    // ------------------------------------------------------------------

    /**
     * Emits a rounded rectangle as a triangle fan (encoded as degenerate GUI
     * quads) from the shape's centre, colouring vertices per the gradient mode.
     */
    private static void emitFan(GuiGraphics g, float x, float y, float w, float h,
                                float rTL, float rTR, float rBR, float rBL,
                                int mode, int c1, int c2) {
        if (w <= 0 || h <= 0) {
            return;
        }
        float scale = radiusScale(w, h, rTL, rTR, rBR, rBL);
        rTL *= scale;
        rTR *= scale;
        rBR *= scale;
        rBL *= scale;

        Matrix4f m = g.pose().last().pose();
        VertexConsumer vc = g.bufferSource().getBuffer(RenderType.gui());
        float cx = x + w / 2.0F;
        float cy = y + h / 2.0F;
        int centerColor = vertexColor(mode, c1, c2, cx, cy, x, y, w, h);

        int n = 4 * (SEGMENTS + 1);
        float firstX = 0, firstY = 0, prevX = 0, prevY = 0;
        for (int i = 0; i < n; i++) {
            float px = boundaryX(i, x, y, w, h, rTL, rTR, rBR, rBL);
            float py = boundaryY(i, x, y, w, h, rTL, rTR, rBR, rBL);
            if (i == 0) {
                firstX = px;
                firstY = py;
            } else {
                emitFanQuad(vc, m, cx, cy, centerColor, prevX, prevY, px, py, mode, c1, c2, x, y, w, h);
            }
            prevX = px;
            prevY = py;
        }
        emitFanQuad(vc, m, cx, cy, centerColor, prevX, prevY, firstX, firstY, mode, c1, c2, x, y, w, h);
    }

    /** Emits one (centre, a, b) triangle as a degenerate quad, vanilla winding. */
    private static void emitFanQuad(VertexConsumer vc, Matrix4f m,
                                    float cx, float cy, int centerColor,
                                    float ax, float ay, float bx, float by,
                                    int mode, int c1, int c2,
                                    float x, float y, float w, float h) {
        int ca = vertexColor(mode, c1, c2, ax, ay, x, y, w, h);
        int cb = vertexColor(mode, c1, c2, bx, by, x, y, w, h);
        vc.addVertex(m, cx, cy, 0).setColor(centerColor);
        vc.addVertex(m, ax, ay, 0).setColor(ca);
        vc.addVertex(m, bx, by, 0).setColor(cb);
        vc.addVertex(m, bx, by, 0).setColor(cb);
    }

    /** Emits a circle fan with an explicit centre colour (radial gradient). */
    private static void emitRadialFan(GuiGraphics g, float cx, float cy, float r,
                                      int inner, int outer) {
        if (r <= 0) {
            return;
        }
        float x = cx - r;
        float y = cy - r;
        float w = r * 2;

        Matrix4f m = g.pose().last().pose();
        VertexConsumer vc = g.bufferSource().getBuffer(RenderType.gui());
        int n = 4 * (SEGMENTS + 1);
        float firstX = 0, firstY = 0, prevX = 0, prevY = 0;
        for (int i = 0; i < n; i++) {
            float px = boundaryX(i, x, y, w, w, r, r, r, r);
            float py = boundaryY(i, x, y, w, w, r, r, r, r);
            if (i == 0) {
                firstX = px;
                firstY = py;
            } else {
                vc.addVertex(m, cx, cy, 0).setColor(inner);
                vc.addVertex(m, prevX, prevY, 0).setColor(outer);
                vc.addVertex(m, px, py, 0).setColor(outer);
                vc.addVertex(m, px, py, 0).setColor(outer);
            }
            prevX = px;
            prevY = py;
        }
        vc.addVertex(m, cx, cy, 0).setColor(inner);
        vc.addVertex(m, prevX, prevY, 0).setColor(outer);
        vc.addVertex(m, firstX, firstY, 0).setColor(outer);
        vc.addVertex(m, firstX, firstY, 0).setColor(outer);
    }

    /**
     * Emits the band between an inner and an outer rounded-rect contour (both
     * walked with identical point counts), blending from {@code innerColor} at
     * the inner contour to {@code outerColor} at the outer one. Powers both
     * outlines and drop shadows.
     */
    private static void emitBand(GuiGraphics g,
                                 float ix, float iy, float iw, float ih,
                                 float irTL, float irTR, float irBR, float irBL,
                                 float ox, float oy, float ow, float oh,
                                 float orTL, float orTR, float orBR, float orBL,
                                 int innerColor, int outerColor) {
        if (ow <= 0 || oh <= 0) {
            return;
        }
        if (iw < 0) {
            iw = 0;
        }
        if (ih < 0) {
            ih = 0;
        }
        float iScale = radiusScale(iw, ih, irTL, irTR, irBR, irBL);
        irTL *= iScale;
        irTR *= iScale;
        irBR *= iScale;
        irBL *= iScale;
        float oScale = radiusScale(ow, oh, orTL, orTR, orBR, orBL);
        orTL *= oScale;
        orTR *= oScale;
        orBR *= oScale;
        orBL *= oScale;

        Matrix4f m = g.pose().last().pose();
        VertexConsumer vc = g.bufferSource().getBuffer(RenderType.gui());
        int n = 4 * (SEGMENTS + 1);

        float firstInX = 0, firstInY = 0, firstOutX = 0, firstOutY = 0;
        float prevInX = 0, prevInY = 0, prevOutX = 0, prevOutY = 0;
        for (int i = 0; i < n; i++) {
            float inX = boundaryX(i, ix, iy, iw, ih, irTL, irTR, irBR, irBL);
            float inY = boundaryY(i, ix, iy, iw, ih, irTL, irTR, irBR, irBL);
            float outX = boundaryX(i, ox, oy, ow, oh, orTL, orTR, orBR, orBL);
            float outY = boundaryY(i, ox, oy, ow, oh, orTL, orTR, orBR, orBL);
            if (i == 0) {
                firstInX = inX;
                firstInY = inY;
                firstOutX = outX;
                firstOutY = outY;
            } else {
                vc.addVertex(m, prevInX, prevInY, 0).setColor(innerColor);
                vc.addVertex(m, prevOutX, prevOutY, 0).setColor(outerColor);
                vc.addVertex(m, outX, outY, 0).setColor(outerColor);
                vc.addVertex(m, inX, inY, 0).setColor(innerColor);
            }
            prevInX = inX;
            prevInY = inY;
            prevOutX = outX;
            prevOutY = outY;
        }
        vc.addVertex(m, prevInX, prevInY, 0).setColor(innerColor);
        vc.addVertex(m, prevOutX, prevOutY, 0).setColor(outerColor);
        vc.addVertex(m, firstOutX, firstOutY, 0).setColor(outerColor);
        vc.addVertex(m, firstInX, firstInY, 0).setColor(innerColor);
    }

    /**
     * Returns the factor that shrinks the four radii so opposing pairs never
     * exceed the rectangle's dimensions (CSS-style overlap resolution).
     */
    private static float radiusScale(float w, float h, float rTL, float rTR, float rBR, float rBL) {
        float scale = 1.0F;
        scale = limit(scale, w, rTL + rTR);
        scale = limit(scale, w, rBL + rBR);
        scale = limit(scale, h, rTL + rBL);
        scale = limit(scale, h, rTR + rBR);
        return scale;
    }

    private static float limit(float scale, float side, float radiusSum) {
        return radiusSum > side && radiusSum > 0 ? Math.min(scale, side / radiusSum) : scale;
    }

    /**
     * X coordinate of boundary point {@code i} of a rounded rectangle, walking
     * counter-clockwise on screen starting at the top-left corner's top point.
     * Corner order: TL, BL, BR, TR with {@code SEGMENTS + 1} points each.
     */
    private static float boundaryX(int i, float x, float y, float w, float h,
                                   float rTL, float rTR, float rBR, float rBL) {
        int corner = i / (SEGMENTS + 1);
        int j = i % (SEGMENTS + 1);
        float sin = SIN[j];
        float cos = SIN[SEGMENTS - j];
        return switch (corner) {
            case 0 -> x + rTL - sin * rTL;        // TL: dir x = -sin u
            case 1 -> x + rBL - cos * rBL;        // BL: dir x = -cos u
            case 2 -> x + w - rBR + sin * rBR;    // BR: dir x = +sin u
            default -> x + w - rTR + cos * rTR;   // TR: dir x = +cos u
        };
    }

    /**
     * Y counterpart of {@link #boundaryX}.
     */
    private static float boundaryY(int i, float x, float y, float w, float h,
                                   float rTL, float rTR, float rBR, float rBL) {
        int corner = i / (SEGMENTS + 1);
        int j = i % (SEGMENTS + 1);
        float sin = SIN[j];
        float cos = SIN[SEGMENTS - j];
        return switch (corner) {
            case 0 -> y + rTL - cos * rTL;        // TL: dir y = -cos u
            case 1 -> y + h - rBL + sin * rBL;    // BL: dir y = +sin u
            case 2 -> y + h - rBR + cos * rBR;    // BR: dir y = +cos u
            default -> y + rTR - sin * rTR;       // TR: dir y = -sin u
        };
    }

    /** Computes the colour of a vertex under the given gradient mode. */
    private static int vertexColor(int mode, int c1, int c2,
                                   float px, float py,
                                   float x, float y, float w, float h) {
        return switch (mode) {
            case VERTICAL -> ColorUtil.lerp(c1, c2, (py - y) / h);
            case HORIZONTAL -> ColorUtil.lerp(c1, c2, (px - x) / w);
            default -> c1;
        };
    }
}
