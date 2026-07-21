package gg.dindijari.client.render;

import gg.dindijari.client.module.modules.BrandingModule;
import gg.dindijari.client.setting.ColorSetting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws the configurable branding block (title, optional subtitle, optional
 * underlines) from a {@link BrandingModule}'s live settings. Used by the main
 * menu and by the Branding panel's preview, so both always match.
 *
 * <p>Text renders through the vanilla font renderer (never baked); letter
 * spacing draws per-glyph with cached per-character components, rebuilt only
 * when text, font or case change — steady-state rendering allocates nothing.
 */
public final class BrandingRenderer {

    /** Per-line glyph cache. */
    private static final class Line {
        String cachedText = "";
        BrandingModule.BrandFont cachedFont;
        final List<Component> chars = new ArrayList<>();
        final List<Float> advances = new ArrayList<>();
        Component whole = Component.empty();
        float baseWidth;

        void refresh(String text, BrandingModule.BrandFont font) {
            if (text.equals(cachedText) && font == cachedFont) {
                return;
            }
            cachedText = text;
            cachedFont = font;
            chars.clear();
            advances.clear();
            Style style = styleOf(font);
            whole = Component.literal(text).withStyle(style);
            baseWidth = Fonts.width(whole);
            text.codePoints().forEach(cp -> {
                Component c = Component.literal(new String(Character.toChars(cp))).withStyle(style);
                chars.add(c);
                advances.add((float) Fonts.width(c));
            });
        }
    }

    private final BrandingModule branding;
    private final Line title = new Line();
    private final Line subtitle = new Line();

    /**
     * Creates a renderer bound to the branding settings.
     *
     * @param branding the branding module
     */
    public BrandingRenderer(BrandingModule branding) {
        this.branding = branding;
    }

    private static Style styleOf(BrandingModule.BrandFont font) {
        return switch (font) {
            case INTER -> Fonts.DISPLAY_STYLE;
            case JETBRAINS_MONO -> Fonts.MONO_STYLE;
            case MINECRAFT -> Style.EMPTY;
        };
    }

    /** Glyph raster height (GUI units at scale 1) per font, for size scaling. */
    private static float baseHeight(BrandingModule.BrandFont font) {
        return font == BrandingModule.BrandFont.INTER ? 30.0F : 10.0F;
    }

    private static int resolveColor(ColorSetting color) {
        return color.isRgbCycle()
                ? ColorUtil.hueCycle(Theme.rgbPeriodMs(), 0.0F)
                : color.get();
    }

    /**
     * Renders the branding block centred on {@code cx} starting at {@code y}.
     *
     * @param g  the draw context
     * @param cx horizontal centre in GUI units
     * @param y  top of the title in GUI units
     * @return the total height consumed, in GUI units
     */
    public float render(GuiGraphics g, float cx, float y) {
        float cursor = y;
        cursor += renderLine(g, cx, cursor, title,
                branding.titleText(), branding.titleFont().get(),
                resolveColor(branding.titleColor()),
                branding.titleSize().getAsFloat(),
                Theme.px(branding.titleSpacing().getAsFloat()),
                branding.titleUnderline().get(),
                resolveColor(branding.titleUnderlineColor()));
        if (branding.subtitleVisible().get()) {
            cursor += Theme.px(10);
            cursor += renderLine(g, cx, cursor, subtitle,
                    branding.subtitleText(), branding.subtitleFont().get(),
                    resolveColor(branding.subtitleColor()),
                    branding.subtitleSize().getAsFloat(),
                    Theme.px(branding.subtitleSpacing().getAsFloat()),
                    branding.subtitleUnderline().get(),
                    resolveColor(branding.subtitleUnderlineColor()));
        }
        return cursor - y;
    }

    private float renderLine(GuiGraphics g, float cx, float y, Line line,
                             String text, BrandingModule.BrandFont font, int color,
                             float sizeDesignPx, float spacing, boolean underline,
                             int underlineColor) {
        line.refresh(text, font);
        // Scale so the rendered glyph height ~= px(size) regardless of face.
        float scale = Theme.px(sizeDesignPx) / baseHeight(font);

        float glyphWidth = line.baseWidth * scale;
        float totalWidth = glyphWidth + Math.max(0, line.chars.size() - 1) * spacing;
        float x = cx - totalWidth / 2;

        if (spacing <= 0.01F) {
            Fonts.drawScaled(g, line.whole, x, y, scale, color, false);
        } else {
            for (int i = 0; i < line.chars.size(); i++) {
                Fonts.drawScaled(g, line.chars.get(i), x, y, scale, color, false);
                x += line.advances.get(i) * scale + spacing;
            }
        }

        // Visual glyph height: ~0.9x the em size for all three faces.
        float visualHeight = baseHeight(font) * scale * 0.9F;
        float consumed = visualHeight;
        if (underline) {
            float lineY = Theme.snap(y + visualHeight + Theme.px(6));
            Render2D.fillRounded(g, cx - totalWidth / 2, lineY, totalWidth,
                    Theme.px(4), Theme.px(2), underlineColor);
            consumed += Theme.px(10);
        }
        return consumed;
    }
}
