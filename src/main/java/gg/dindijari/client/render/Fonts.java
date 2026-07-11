package gg.dindijari.client.render;

import gg.dindijari.client.core.DindijariClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;

/**
 * Access to the client's bundled typefaces and a small text-drawing wrapper
 * with font, scale and shadow control.
 *
 * <p>Two OFL-licensed fonts ship with the mod as Minecraft TTF font providers
 * (see {@code assets/dindijariclient/font/}): <b>Inter</b>, the UI typeface
 * used for all client screens, and <b>JetBrains Mono</b> for tabular/code-like
 * text. Licence texts are bundled under {@code /licenses} in the jar.
 *
 * <p>Text styled here still renders through the vanilla {@code Font} renderer
 * and {@link GuiGraphics}, so it batches with the rest of the GUI. Callers on
 * hot paths should build their {@link Component}s once and cache them; the
 * {@code drawString(String, ...)} overloads here restyle per call and are
 * intended for values that change every frame (e.g. an FPS readout).
 */
public final class Fonts {

    /** Font id of the bundled Inter Regular face. */
    public static final ResourceLocation INTER =
            ResourceLocation.fromNamespaceAndPath(DindijariClient.MOD_ID, "inter");

    /** Font id of the bundled JetBrains Mono Regular face. */
    public static final ResourceLocation MONO =
            ResourceLocation.fromNamespaceAndPath(DindijariClient.MOD_ID, "jetbrains_mono");

    /**
     * Font id of the display-size Inter face (30 GUI units, oversampled 4x =
     * 120&nbsp;px glyph rasters). Used for the wordmark and loading-screen logo
     * so large text is rasterised at (or above) its physical pixel size up to
     * GUI scale 4 / 4K — never upscaled from body-text rasters.
     */
    public static final ResourceLocation INTER_DISPLAY =
            ResourceLocation.fromNamespaceAndPath(DindijariClient.MOD_ID, "inter_display");

    /** Reusable style carrying the Inter font. */
    public static final Style INTER_STYLE = Style.EMPTY.withFont(INTER);

    /** Reusable style carrying the JetBrains Mono font. */
    public static final Style MONO_STYLE = Style.EMPTY.withFont(MONO);

    /** Reusable style carrying the display-size Inter font. */
    public static final Style DISPLAY_STYLE = Style.EMPTY.withFont(INTER_DISPLAY);

    private Fonts() {
    }

    /**
     * Builds a component rendered in Inter. Build once and cache when the text
     * is static.
     *
     * @param text the literal text
     * @return the styled component
     */
    public static MutableComponent inter(String text) {
        return Component.literal(text).withStyle(INTER_STYLE);
    }

    /**
     * Builds a component rendered in JetBrains Mono. Build once and cache when
     * the text is static.
     *
     * @param text the literal text
     * @return the styled component
     */
    public static MutableComponent mono(String text) {
        return Component.literal(text).withStyle(MONO_STYLE);
    }

    /**
     * Builds a component in the display-size Inter face (for wordmarks and
     * hero text; draw at scale 1 — the glyphs are already ~3x body size).
     *
     * @param text the literal text
     * @return the styled component
     */
    public static MutableComponent display(String text) {
        return Component.literal(text).withStyle(DISPLAY_STYLE);
    }

    /**
     * Builds a component in the theme's currently selected UI typeface (see the
     * Theme module's Font setting). Screens rebuild their labels on
     * {@code init}, so a font change applies to every screen (re)opened after
     * it.
     *
     * @param text the literal text
     * @return the styled component
     */
    public static MutableComponent ui(String text) {
        return Component.literal(text).withStyle(
                Theme.uiFont() == gg.dindijari.client.module.modules.ThemeModule.UiFont.JETBRAINS_MONO
                        ? MONO_STYLE : INTER_STYLE);
    }

    /**
     * Measures a component's width in GUI units.
     *
     * @param text the component
     * @return its rendered width
     */
    public static int width(Component text) {
        return Minecraft.getInstance().font.width(text);
    }

    /**
     * Draws a component at native size.
     *
     * @param g      the draw context
     * @param text   the (pre-styled) component
     * @param x      left x in GUI units
     * @param y      top y in GUI units
     * @param color  packed ARGB text colour
     * @param shadow whether to draw the vanilla text shadow
     */
    public static void draw(GuiGraphics g, Component text, float x, float y, int color, boolean shadow) {
        g.drawString(Minecraft.getInstance().font, text.getVisualOrderText(),
                Math.round(x), Math.round(y), color, shadow);
    }

    /**
     * Draws a component scaled about its top-left corner. Scaling happens on
     * the pose stack, so any scale keeps sub-pixel precision.
     *
     * @param g      the draw context
     * @param text   the (pre-styled) component
     * @param x      left x in GUI units
     * @param y      top y in GUI units
     * @param scale  uniform scale factor (1 = native size)
     * @param color  packed ARGB text colour
     * @param shadow whether to draw the vanilla text shadow
     */
    public static void drawScaled(GuiGraphics g, Component text, float x, float y,
                                  float scale, int color, boolean shadow) {
        if (scale == 1.0F) {
            draw(g, text, x, y, color, shadow);
            return;
        }
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1.0F);
        g.drawString(Minecraft.getInstance().font, text.getVisualOrderText(), 0, 0, color, shadow);
        g.pose().popPose();
    }

    /**
     * Draws a component horizontally centred on {@code centerX}.
     *
     * @param g       the draw context
     * @param text    the (pre-styled) component
     * @param centerX centre x in GUI units
     * @param y       top y in GUI units
     * @param scale   uniform scale factor
     * @param color   packed ARGB text colour
     * @param shadow  whether to draw the vanilla text shadow
     */
    public static void drawCentered(GuiGraphics g, Component text, float centerX, float y,
                                    float scale, int color, boolean shadow) {
        drawScaled(g, text, centerX - width(text) * scale / 2.0F, y, scale, color, shadow);
    }
}
