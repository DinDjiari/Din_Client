package gg.dindijari.client.module.modules;

import gg.dindijari.client.module.Category;
import gg.dindijari.client.module.Module;
import gg.dindijari.client.setting.BooleanSetting;
import gg.dindijari.client.setting.ColorSetting;
import gg.dindijari.client.setting.EnumSetting;
import gg.dindijari.client.setting.NumberSetting;
import gg.dindijari.client.setting.StringSetting;
import net.minecraft.client.Minecraft;

import java.util.Locale;

/**
 * Settings-only module holding the main-menu branding configuration: what the
 * big title and the subtitle show (player name / client name / custom text)
 * and how each is styled (font, colour with RGB cycle, size, letter spacing,
 * uppercase, accent underline with its own colour). Styling always applies to
 * whatever text the selected source produces. All values persist through the
 * ordinary config pipeline; the Branding panel in the Click GUI edits them
 * with a live preview.
 */
public final class BrandingModule extends Module {

    /** Where a branding line's text comes from. */
    public enum Source {
        /** The currently logged-in account name, read live. */
        PLAYER_NAME,
        /** The static client name. */
        CLIENT_NAME,
        /** Free text from the custom field. */
        CUSTOM
    }

    /** Typeface choices for branding text. */
    public enum BrandFont {
        /** Bundled Inter (display-size face for large text). */
        INTER,
        /** Bundled JetBrains Mono. */
        JETBRAINS_MONO,
        /** The vanilla Minecraft font. */
        MINECRAFT
    }

    // ---- Title -----------------------------------------------------------
    private final EnumSetting<Source> titleSource = new EnumSetting<>(
            "Title Source", "What the big title shows.", Source.PLAYER_NAME);
    private final StringSetting titleCustom = new StringSetting(
            "Title Text", "Custom title text.", "");
    private final EnumSetting<BrandFont> titleFont = new EnumSetting<>(
            "Title Font", "Typeface of the title.", BrandFont.INTER);
    private final ColorSetting titleColor = new ColorSetting(
            "Title Color", "Colour of the title.", 0xFFFFFFFF);
    private final NumberSetting titleSize = new NumberSetting(
            "Title Size", "Height of the title text.", 30, 20, 60, 2);
    private final NumberSetting titleSpacing = new NumberSetting(
            "Title Spacing", "Extra space between letters.", 0, 0, 12, 0.5);
    private final BooleanSetting titleUppercase = new BooleanSetting(
            "Title Uppercase", "Force the title to uppercase.", true);
    private final BooleanSetting titleUnderline = new BooleanSetting(
            "Title Underline", "Show the underline below the title.", true);
    private final ColorSetting titleUnderlineColor = new ColorSetting(
            "Title Underline Color", "Colour of the title underline.", 0xFF55FFFF);

    // ---- Subtitle ----------------------------------------------------------
    private final BooleanSetting subtitleVisible = new BooleanSetting(
            "Subtitle Visible", "Show the subtitle line.", true);
    private final EnumSetting<Source> subtitleSource = new EnumSetting<>(
            "Subtitle Source", "What the subtitle shows.", Source.CLIENT_NAME);
    private final StringSetting subtitleCustom = new StringSetting(
            "Subtitle Text", "Custom subtitle text.", "");
    private final EnumSetting<BrandFont> subtitleFont = new EnumSetting<>(
            "Subtitle Font", "Typeface of the subtitle.", BrandFont.INTER);
    private final ColorSetting subtitleColor = new ColorSetting(
            "Subtitle Color", "Colour of the subtitle.", 0xFFA0A0A8);
    private final NumberSetting subtitleSize = new NumberSetting(
            "Subtitle Size", "Height of the subtitle text.", 10, 6, 24, 1);
    private final NumberSetting subtitleSpacing = new NumberSetting(
            "Subtitle Spacing", "Extra space between letters.", 6, 0, 12, 0.5);
    private final BooleanSetting subtitleUppercase = new BooleanSetting(
            "Subtitle Uppercase", "Force the subtitle to uppercase.", true);
    private final BooleanSetting subtitleUnderline = new BooleanSetting(
            "Subtitle Underline", "Show an underline below the subtitle.", false);
    private final ColorSetting subtitleUnderlineColor = new ColorSetting(
            "Subtitle Underline Color", "Colour of the subtitle underline.", 0xFF55FFFF);

    /**
     * Creates the module.
     */
    public BrandingModule() {
        super("Branding", "Main menu title and subtitle text and styling.", Category.CLIENT);
        setToggleable(false);
        addSetting(titleSource, titleCustom, titleFont, titleColor, titleSize, titleSpacing,
                titleUppercase, titleUnderline, titleUnderlineColor,
                subtitleVisible, subtitleSource, subtitleCustom, subtitleFont, subtitleColor,
                subtitleSize, subtitleSpacing, subtitleUppercase, subtitleUnderline,
                subtitleUnderlineColor);
    }

    /**
     * Resolves the title text from the selected source (uppercased if set).
     *
     * @return the text the big title should show
     */
    public String titleText() {
        return resolve(titleSource.get(), titleCustom.get(), "DINDIJARI", titleUppercase.get());
    }

    /**
     * Resolves the subtitle text from the selected source (uppercased if set).
     *
     * @return the text the subtitle should show
     */
    public String subtitleText() {
        return resolve(subtitleSource.get(), subtitleCustom.get(), "CLIENT", subtitleUppercase.get());
    }

    private static String resolve(Source source, String custom, String clientName, boolean uppercase) {
        String text = switch (source) {
            case PLAYER_NAME -> Minecraft.getInstance().getUser().getName();
            case CLIENT_NAME -> clientName;
            case CUSTOM -> custom.isBlank() ? clientName : custom;
        };
        return uppercase ? text.toUpperCase(Locale.ROOT) : text;
    }

    // Accessors used by the Branding panel and renderer. -----------------

    /** @return title source setting */
    public EnumSetting<Source> titleSource() {
        return titleSource;
    }

    /** @return custom title text setting */
    public StringSetting titleCustom() {
        return titleCustom;
    }

    /** @return title font setting */
    public EnumSetting<BrandFont> titleFont() {
        return titleFont;
    }

    /** @return title colour setting */
    public ColorSetting titleColor() {
        return titleColor;
    }

    /** @return title size setting (design px) */
    public NumberSetting titleSize() {
        return titleSize;
    }

    /** @return title letter-spacing setting (design px) */
    public NumberSetting titleSpacing() {
        return titleSpacing;
    }

    /** @return title uppercase toggle */
    public BooleanSetting titleUppercase() {
        return titleUppercase;
    }

    /** @return title underline toggle */
    public BooleanSetting titleUnderline() {
        return titleUnderline;
    }

    /** @return title underline colour setting */
    public ColorSetting titleUnderlineColor() {
        return titleUnderlineColor;
    }

    /** @return subtitle visibility toggle */
    public BooleanSetting subtitleVisible() {
        return subtitleVisible;
    }

    /** @return subtitle source setting */
    public EnumSetting<Source> subtitleSource() {
        return subtitleSource;
    }

    /** @return custom subtitle text setting */
    public StringSetting subtitleCustom() {
        return subtitleCustom;
    }

    /** @return subtitle font setting */
    public EnumSetting<BrandFont> subtitleFont() {
        return subtitleFont;
    }

    /** @return subtitle colour setting */
    public ColorSetting subtitleColor() {
        return subtitleColor;
    }

    /** @return subtitle size setting (design px) */
    public NumberSetting subtitleSize() {
        return subtitleSize;
    }

    /** @return subtitle letter-spacing setting (design px) */
    public NumberSetting subtitleSpacing() {
        return subtitleSpacing;
    }

    /** @return subtitle uppercase toggle */
    public BooleanSetting subtitleUppercase() {
        return subtitleUppercase;
    }

    /** @return subtitle underline toggle */
    public BooleanSetting subtitleUnderline() {
        return subtitleUnderline;
    }

    /** @return subtitle underline colour setting */
    public ColorSetting subtitleUnderlineColor() {
        return subtitleUnderlineColor;
    }
}
