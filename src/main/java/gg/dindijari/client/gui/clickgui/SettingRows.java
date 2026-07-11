package gg.dindijari.client.gui.clickgui;

import gg.dindijari.client.render.ColorUtil;
import gg.dindijari.client.render.Fonts;
import gg.dindijari.client.render.Render2D;
import gg.dindijari.client.render.Theme;
import gg.dindijari.client.setting.BooleanSetting;
import gg.dindijari.client.setting.ColorSetting;
import gg.dindijari.client.setting.EnumSetting;
import gg.dindijari.client.setting.KeybindSetting;
import gg.dindijari.client.setting.NumberSetting;
import gg.dindijari.client.setting.Setting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Immediate-mode renderer and input handler for a vertical list of typed
 * setting rows inside a Click GUI panel:
 *
 * <ul>
 *   <li>{@link BooleanSetting} — label + mini accent toggle;</li>
 *   <li>{@link NumberSetting} — small label above an accent slider with a
 *       live value readout (drag to change);</li>
 *   <li>{@link ColorSetting} — label + preset swatch row + "RGB" cycle mini
 *       toggle;</li>
 *   <li>{@link EnumSetting} — label + value chip (click cycles, right-click
 *       cycles backwards);</li>
 *   <li>{@link KeybindSetting} — label + key chip; click, then press a key to
 *       rebind (Esc unbinds).</li>
 * </ul>
 *
 * <p>Row layout is pure math shared by render and hit-testing; label
 * components are cached per setting so steady-state rendering does not
 * allocate.
 */
final class SettingRows {

    /** Accent swatch presets offered for colour settings (per the design reference). */
    private static final int[] SWATCHES = {0xFF55FFFF, 0xFF4CD964, 0xFFFF4F9A, 0xFFFF9F43, 0xFF9B59FF};

    private final Map<Setting<?>, Component> labels = new HashMap<>();
    private final Map<Setting<?>, Component> values = new HashMap<>();
    private final Map<Setting<?>, String> valueTexts = new HashMap<>();

    private NumberSetting draggingSlider;
    private float dragTrackX;
    private float dragTrackW;
    private KeybindSetting listening;

    /**
     * Height of one setting's row in GUI units.
     *
     * @param setting the setting
     * @return the row height
     */
    float rowHeight(Setting<?> setting) {
        if (setting instanceof NumberSetting) {
            return Theme.px(40);
        }
        if (setting instanceof ColorSetting) {
            return Theme.px(42);
        }
        return Theme.px(26);
    }

    /**
     * Total height of a settings list in GUI units.
     *
     * @param settings the settings
     * @return the stacked height
     */
    float totalHeight(List<Setting<?>> settings) {
        float h = 0;
        for (Setting<?> s : settings) {
            h += rowHeight(s);
        }
        return h;
    }

    /**
     * Renders the rows.
     *
     * @param g        the draw context
     * @param x        left edge
     * @param y        top edge
     * @param w        row width
     * @param settings the settings to render
     */
    void render(GuiGraphics g, float x, float y, float w, List<Setting<?>> settings) {
        float cy = y;
        for (Setting<?> s : settings) {
            renderRow(g, x, cy, w, s);
            cy += rowHeight(s);
        }
    }

    private void renderRow(GuiGraphics g, float x, float y, float w, Setting<?> s) {
        Component label = labels.computeIfAbsent(s, k -> Fonts.ui(k.getName()));

        if (s instanceof NumberSetting num) {
            Fonts.drawScaled(g, label, x, y + Theme.px(3), 0.8F, Theme.TEXT_SECONDARY, false);
            float trackY = y + Theme.px(26);
            float valueW = Theme.px(44);
            float trackW = w - valueW - Theme.px(8);
            Render2D.fillRounded(g, x, trackY, trackW, Theme.px(4), Theme.px(2), Theme.BUTTON_HOVER);
            float t = (float) ((num.get() - num.getMin()) / (num.getMax() - num.getMin()));
            Render2D.fillRounded(g, x, trackY, trackW * t, Theme.px(4), Theme.px(2), Theme.accent());
            Render2D.fillCircle(g, x + trackW * t, trackY + Theme.px(2), Theme.px(6), Theme.accent());
            Fonts.drawScaled(g, valueComponent(num), x + trackW + Theme.px(8), trackY - Theme.px(3),
                    0.8F, Theme.TEXT_SECONDARY, false);
        } else if (s instanceof BooleanSetting bool) {
            Fonts.drawScaled(g, label, x, y + Theme.px(9), 0.9F, Theme.TEXT_PRIMARY, false);
            drawToggle(g, x + w - Theme.px(34), y + Theme.px(5), bool.get());
        } else if (s instanceof ColorSetting color) {
            // Two-line layout, per the design reference: small label above,
            // swatch row (left-aligned) with the RGB-cycle toggle at the right.
            Fonts.drawScaled(g, label, x, y + Theme.px(3), 0.8F, Theme.TEXT_SECONDARY, false);
            float rowY = y + Theme.px(18);
            for (int i = 0; i < SWATCHES.length; i++) {
                float swatchX = x + i * Theme.px(20);
                Render2D.fillCircle(g, swatchX + Theme.px(8), rowY + Theme.px(8), Theme.px(8), SWATCHES[i]);
                if (!color.isRgbCycle() && color.get() == SWATCHES[i]) {
                    Render2D.outlineRounded(g, swatchX, rowY, Theme.px(16), Theme.px(16),
                            Theme.px(8), 1.0F, Theme.TEXT_PRIMARY);
                }
            }
            drawToggle(g, x + w - Theme.px(30), rowY, color.isRgbCycle());
        } else if (s instanceof EnumSetting<?> en) {
            Fonts.drawScaled(g, label, x, y + Theme.px(9), 0.9F, Theme.TEXT_PRIMARY, false);
            Component value = valueComponent(en);
            float chipW = Fonts.width(value) * 0.9F + Theme.px(16);
            float chipX = x + w - chipW;
            Render2D.fillRounded(g, chipX, y + Theme.px(4), chipW, Theme.px(18), Theme.px(4), Theme.BUTTON);
            Fonts.drawScaled(g, value, chipX + Theme.px(8), y + Theme.px(9), 0.9F, Theme.accent(), false);
        } else if (s instanceof KeybindSetting key) {
            Fonts.drawScaled(g, label, x, y + Theme.px(9), 0.9F, Theme.TEXT_PRIMARY, false);
            Component value = listening == key ? valueComponent(key, "press a key")
                    : valueComponent(key, key.getKeyName());
            float chipW = Fonts.width(value) * 0.9F + Theme.px(16);
            float chipX = x + w - chipW;
            int chipColor = listening == key ? ColorUtil.scaleAlpha(Theme.accent(), 0.35F) : Theme.BUTTON;
            Render2D.fillRounded(g, chipX, y + Theme.px(4), chipW, Theme.px(18), Theme.px(4), chipColor);
            Fonts.drawScaled(g, value, chipX + Theme.px(8), y + Theme.px(9), 0.9F,
                    listening == key ? Theme.TEXT_PRIMARY : Theme.TEXT_SECONDARY, false);
        }
    }

    /**
     * Routes a mouse press into the rows.
     *
     * @param mx       mouse x
     * @param my       mouse y
     * @param x        rows left edge
     * @param y        rows top edge
     * @param w        row width
     * @param settings the settings
     * @param button   GLFW mouse button
     * @return {@code true} if consumed
     */
    boolean mouseClicked(double mx, double my, float x, float y, float w,
                         List<Setting<?>> settings, int button) {
        float cy = y;
        for (Setting<?> s : settings) {
            float rh = rowHeight(s);
            if (my >= cy && my < cy + rh && mx >= x && mx <= x + w) {
                return rowClicked(mx, my, x, cy, w, s, button);
            }
            cy += rh;
        }
        return false;
    }

    private boolean rowClicked(double mx, double my, float x, float y, float w, Setting<?> s, int button) {
        if (s instanceof NumberSetting num) {
            float trackY = y + Theme.px(26);
            float trackW = w - Theme.px(44) - Theme.px(8);
            if (my >= trackY - Theme.px(8) && my <= trackY + Theme.px(10)) {
                draggingSlider = num;
                dragTrackX = x;
                dragTrackW = trackW;
                applySlider(mx);
                return true;
            }
            return false;
        }
        if (s instanceof BooleanSetting bool) {
            bool.toggle();
            return true;
        }
        if (s instanceof ColorSetting color) {
            float toggleX = x + w - Theme.px(34);
            if (mx >= toggleX) {
                color.setRgbCycle(!color.isRgbCycle());
                return true;
            }
            for (int i = 0; i < SWATCHES.length; i++) {
                float swX = x + i * Theme.px(20);
                if (mx >= swX && mx <= swX + Theme.px(16)) {
                    color.setRgbCycle(false);
                    color.set(SWATCHES[i]);
                    return true;
                }
            }
            return false;
        }
        if (s instanceof EnumSetting<?> en) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                en.cycleBackward();
            } else {
                en.cycle();
            }
            valueTexts.remove(en);
            return true;
        }
        if (s instanceof KeybindSetting key) {
            listening = listening == key ? null : key;
            valueTexts.remove(key);
            return true;
        }
        return false;
    }

    /**
     * Continues a slider drag.
     *
     * @param mx mouse x
     * @return {@code true} while a slider is being dragged
     */
    boolean mouseDragged(double mx) {
        if (draggingSlider == null) {
            return false;
        }
        applySlider(mx);
        return true;
    }

    /**
     * Ends any slider drag.
     */
    void mouseReleased() {
        draggingSlider = null;
    }

    /**
     * Feeds a key press to a listening keybind chip.
     *
     * @param keyCode the pressed GLFW key
     * @return {@code true} if the key was captured for a rebind
     */
    boolean keyPressed(int keyCode) {
        if (listening == null) {
            return false;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            listening.unbind();
        } else {
            listening.set(keyCode);
        }
        valueTexts.remove(listening);
        listening = null;
        return true;
    }

    /** @return whether a keybind chip is waiting for a key press */
    boolean isListening() {
        return listening != null;
    }

    private void applySlider(double mx) {
        float t = (float) ((mx - dragTrackX) / dragTrackW);
        t = Math.max(0.0F, Math.min(1.0F, t));
        draggingSlider.set(draggingSlider.getMin()
                + t * (draggingSlider.getMax() - draggingSlider.getMin()));
        valueTexts.remove(draggingSlider);
    }

    /** Cached value component for a number setting, rebuilt only when the text changes. */
    private Component valueComponent(NumberSetting num) {
        double v = num.get();
        String text = num.getStep() >= 1.0
                ? String.valueOf((int) Math.round(v))
                : String.format(Locale.ROOT, "%.2f", v);
        return valueComponent(num, text);
    }

    private Component valueComponent(EnumSetting<?> en) {
        String name = en.get().name().toLowerCase(Locale.ROOT).replace('_', ' ');
        return valueComponent(en, Character.toUpperCase(name.charAt(0)) + name.substring(1));
    }

    private Component valueComponent(Setting<?> s, String text) {
        String previous = valueTexts.get(s);
        if (!text.equals(previous)) {
            valueTexts.put(s, text);
            values.put(s, Fonts.ui(text));
        }
        return values.get(s);
    }

    /**
     * Draws the small pill toggle used by setting rows and module rows.
     *
     * @param g  the draw context
     * @param x  left edge
     * @param y  top edge
     * @param on the state to depict
     */
    static void drawToggle(GuiGraphics g, float x, float y, boolean on) {
        float w = Theme.px(28);
        float h = Theme.px(16);
        float r = h / 2;
        Render2D.fillRounded(g, x, y, w, h, r, on ? Theme.accent() : Theme.BUTTON_HOVER);
        float knobX = on ? x + w - r : x + r;
        Render2D.fillCircle(g, knobX, y + r, r - Theme.px(2.5F), on ? 0xFF0E0E10 : Theme.TEXT_SECONDARY);
    }
}
