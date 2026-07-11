package gg.dindijari.client.render;

import net.minecraft.util.Mth;

/**
 * ARGB colour helpers for render code.
 *
 * <p>All colours are packed {@code 0xAARRGGBB} ints, matching what
 * {@code GuiGraphics} and vertex consumers expect. Every method is a pure
 * function with no allocation, so all of them are safe in per-frame paths.
 */
public final class ColorUtil {

    private ColorUtil() {
    }

    /**
     * Packs colour components into an ARGB int.
     *
     * @param a alpha 0–255
     * @param r red 0–255
     * @param g green 0–255
     * @param b blue 0–255
     * @return the packed colour
     */
    public static int argb(int a, int r, int g, int b) {
        return (a & 0xFF) << 24 | (r & 0xFF) << 16 | (g & 0xFF) << 8 | (b & 0xFF);
    }

    /**
     * Returns the colour with its alpha channel replaced.
     *
     * @param color the base colour
     * @param alpha the new alpha, 0–255
     * @return the colour with the given alpha
     */
    public static int withAlpha(int color, int alpha) {
        return (alpha & 0xFF) << 24 | (color & 0x00FFFFFF);
    }

    /**
     * Returns the colour with its alpha channel scaled.
     *
     * @param color  the base colour
     * @param factor multiplier applied to the existing alpha, clamped to [0, 1]
     * @return the colour with scaled alpha
     */
    public static int scaleAlpha(int color, float factor) {
        float f = Mth.clamp(factor, 0.0F, 1.0F);
        int a = (int) (((color >>> 24) & 0xFF) * f);
        return withAlpha(color, a);
    }

    /**
     * Linearly interpolates between two ARGB colours, per channel.
     *
     * @param from the colour at {@code t = 0}
     * @param to   the colour at {@code t = 1}
     * @param t    the mix factor, clamped to [0, 1]
     * @return the blended colour
     */
    public static int lerp(int from, int to, float t) {
        float f = Mth.clamp(t, 0.0F, 1.0F);
        int a = (int) Mth.lerp(f, (from >>> 24) & 0xFF, (to >>> 24) & 0xFF);
        int r = (int) Mth.lerp(f, (from >>> 16) & 0xFF, (to >>> 16) & 0xFF);
        int g = (int) Mth.lerp(f, (from >>> 8) & 0xFF, (to >>> 8) & 0xFF);
        int b = (int) Mth.lerp(f, from & 0xFF, to & 0xFF);
        return argb(a, r, g, b);
    }

    /**
     * Produces an animated hue-cycling ("RGB"/rainbow) colour from wall-clock
     * time, at full alpha.
     *
     * @param periodMs   milliseconds for one full trip around the hue wheel
     * @param offset     phase offset in [0, 1); shifts the hue (use per-element
     *                   offsets for a "wave" effect across several elements)
     * @param saturation colour saturation in [0, 1]
     * @param brightness colour brightness in [0, 1]
     * @return the packed ARGB colour for the current instant
     */
    public static int hueCycle(long periodMs, float offset, float saturation, float brightness) {
        float hue = (System.currentTimeMillis() % periodMs) / (float) periodMs + offset;
        hue -= (int) hue; // wrap into [0, 1)
        return 0xFF000000 | Mth.hsvToRgb(hue, saturation, brightness);
    }

    /**
     * Convenience full-saturation, full-brightness {@link #hueCycle}.
     *
     * @param periodMs milliseconds per hue revolution
     * @param offset   phase offset in [0, 1)
     * @return the packed ARGB colour
     */
    public static int hueCycle(long periodMs, float offset) {
        return hueCycle(periodMs, offset, 0.72F, 1.0F);
    }
}
