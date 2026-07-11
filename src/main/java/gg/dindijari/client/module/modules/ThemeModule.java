package gg.dindijari.client.module.modules;

import gg.dindijari.client.module.Category;
import gg.dindijari.client.module.Module;
import gg.dindijari.client.setting.ColorSetting;
import gg.dindijari.client.setting.EnumSetting;
import gg.dindijari.client.setting.NumberSetting;

/**
 * Settings-only module (no on/off toggle) holding the client's live theme
 * configuration: accent colour, RGB-line speed, UI scale, animation speed and
 * UI font. The {@code Theme} facade reads these every frame, so edits apply
 * instantly, and the values persist through the ordinary config pipeline.
 */
public final class ThemeModule extends Module {

    /** The UI typefaces the client bundles. */
    public enum UiFont {
        /** Inter Regular — the default UI face. */
        INTER,
        /** JetBrains Mono Regular. */
        JETBRAINS_MONO
    }

    private final ColorSetting accent = new ColorSetting(
            "Accent Color", "The single strong accent used across the UI.", 0xFF55FFFF);
    private final NumberSetting rgbSpeed = new NumberSetting(
            "RGB Speed", "Speed of the animated RGB gradient lines (percent).", 50, 10, 200, 5);
    private final NumberSetting uiScale = new NumberSetting(
            "UI Scale", "Scale of client panels and widgets (percent).", 100, 75, 150, 5);
    private final NumberSetting animationSpeed = new NumberSetting(
            "Animation Speed", "Multiplier for UI transition speed.", 1.0, 0.25, 2.0, 0.05);
    private final EnumSetting<UiFont> font = new EnumSetting<>(
            "Font", "Typeface used for client UI text.", UiFont.INTER);

    /**
     * Creates the theme module.
     */
    public ThemeModule() {
        super("Theme", "Colors, motion and typography of the client UI.", Category.CLIENT);
        setToggleable(false);
        addSetting(accent, rgbSpeed, uiScale, animationSpeed, font);
    }

    /**
     * Returns the accent colour setting.
     *
     * @return the accent colour setting
     */
    public ColorSetting accent() {
        return accent;
    }

    /**
     * Returns the RGB-line speed setting (percent, 100 = one hue cycle per 4s).
     *
     * @return the RGB speed setting
     */
    public NumberSetting rgbSpeed() {
        return rgbSpeed;
    }

    /**
     * Returns the UI scale setting (percent).
     *
     * @return the UI scale setting
     */
    public NumberSetting uiScale() {
        return uiScale;
    }

    /**
     * Returns the animation speed multiplier setting.
     *
     * @return the animation speed setting
     */
    public NumberSetting animationSpeed() {
        return animationSpeed;
    }

    /**
     * Returns the UI font setting.
     *
     * @return the font setting
     */
    public EnumSetting<UiFont> font() {
        return font;
    }
}
