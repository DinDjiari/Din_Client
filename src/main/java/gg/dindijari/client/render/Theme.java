package gg.dindijari.client.render;

import gg.dindijari.client.util.animation.Easing;
import net.minecraft.client.Minecraft;

/**
 * The Dindijari Client design system: a single source of truth for every
 * colour, radius, spacing and motion token used by client-owned UI.
 *
 * <p>Visual language: dark charcoal surfaces, subtle rounded corners, one
 * strong cyan accent, flat buttons with smooth hover transitions, and the
 * bundled Inter typeface for all text. No vanilla button textures or
 * dirt/stone backgrounds appear in client screens.
 *
 * <h2>Design-pixel scale</h2>
 * Token dimensions (radii, spacing) are specified in <em>design pixels</em>,
 * i.e. physical framebuffer pixels at the reference GUI scale of 2 — the same
 * unit a CSS mockup would use. {@link #px(float)} converts a design-pixel value
 * into GUI units, so layouts keep their designed proportions at the reference
 * scale while still respecting the user's GUI-scale option.
 */
public final class Theme {

    private Theme() {
    }

    /** Live theme configuration, installed at start-up; null until then. */
    private static gg.dindijari.client.module.modules.ThemeModule config;

    /** Performance Mode module, installed at start-up; null until then. */
    private static gg.dindijari.client.module.Module performanceMode;

    /**
     * Binds the Performance Mode module so {@link #reducedEffects()} reflects
     * its toggle. Called once during mod construction.
     *
     * @param module the performance-mode module
     */
    public static void installPerformanceMode(gg.dindijari.client.module.Module module) {
        performanceMode = module;
    }

    /**
     * Whether expensive UI effects (blur, drop shadows, RGB line animation,
     * accent hue-cycling) should be skipped. Driven by the Performance Mode
     * module; a single boolean check, so consulting it costs nothing.
     *
     * @return {@code true} while Performance Mode is enabled
     */
    public static boolean reducedEffects() {
        return performanceMode != null && performanceMode.isEnabled();
    }

    /**
     * Binds the live {@code ThemeModule} so the tokens below become dynamic.
     * Called once during mod construction.
     *
     * @param module the theme module
     */
    public static void install(gg.dindijari.client.module.modules.ThemeModule module) {
        config = module;
    }

    /** Interface module (animations/sounds), installed at start-up; null until then. */
    private static gg.dindijari.client.module.modules.client.InterfaceModule interfaceConfig;

    /**
     * Binds the Interface module so {@link #animationsEnabled()} reflects its
     * "UI Animations" master toggle. Called once during mod construction.
     *
     * @param module the interface module
     */
    public static void installInterface(
            gg.dindijari.client.module.modules.client.InterfaceModule module) {
        interfaceConfig = module;
    }

    /**
     * Whether UI animations (screen/dialog transitions, toggle ripples, toast
     * slides) should run. False while Performance Mode is enabled or the
     * "UI Animations" master toggle is off; effects then snap to their final
     * state instead of transitioning.
     *
     * @return {@code true} while UI animations are enabled
     */
    public static boolean animationsEnabled() {
        if (reducedEffects()) {
            return false;
        }
        return interfaceConfig == null || interfaceConfig.uiAnimations().get();
    }

    // ------------------------------------------------------------------
    // Colour tokens
    // ------------------------------------------------------------------

    /** Screen background: near-black charcoal (#0E0E10). */
    public static final int BACKGROUND = 0xFF0E0E10;

    /** Panel surface: #16161A at 95% opacity. */
    public static final int PANEL = 0xF216161A;

    /** Flat button fill (#1E1E24). */
    public static final int BUTTON = 0xFF1E1E24;

    /** Button fill while hovered (#2A2A32). */
    public static final int BUTTON_HOVER = 0xFF2A2A32;

    /** Default accent (#55FFFF) used until/unless the theme module overrides it. */
    public static final int ACCENT = 0xFF55FFFF;

    /**
     * The single strong accent: toggles ON, sliders, selection, header
     * underlines and primary buttons. Follows the Theme module's accent
     * setting (including its RGB-cycle mode) once installed.
     *
     * @return the current accent colour, packed ARGB
     */
    public static int accent() {
        if (config == null) {
            return ACCENT;
        }
        if (config.accent().isRgbCycle() && !reducedEffects()) {
            return ColorUtil.hueCycle(rgbPeriodMs(), 0.0F);
        }
        return config.accent().get();
    }

    /**
     * Period of one full hue revolution for RGB gradient lines and cycling
     * accents, derived from the theme's RGB Speed setting (100% = 4s).
     *
     * @return the hue period in milliseconds
     */
    public static long rgbPeriodMs() {
        double speed = config == null ? 50.0 : config.rgbSpeed().get();
        return Math.max(500L, (long) (4000.0 * 100.0 / Math.max(10.0, speed)));
    }

    /**
     * Duration for hover/micro transitions, shaped by the theme's Animation
     * Speed multiplier.
     *
     * @return the transition duration in milliseconds
     */
    public static long hoverMs() {
        double mult = config == null ? 1.0 : config.animationSpeed().get();
        return Math.max(30L, (long) (HOVER_MS / Math.max(0.25, mult)));
    }

    /** Primary text (#FFFFFF). */
    public static final int TEXT_PRIMARY = 0xFFFFFFFF;

    /** Secondary/muted text (#A0A0A8). */
    public static final int TEXT_SECONDARY = 0xFFA0A0A8;

    /** Drop shadow base colour (alpha is shaped by the shadow renderer). */
    public static final int SHADOW = 0x66000000;

    /** Dark overlay laid over the blurred world behind in-game screens. */
    public static final int INWORLD_OVERLAY = 0xA00E0E10;

    // ------------------------------------------------------------------
    // Shape and spacing tokens (design pixels; convert with px())
    // ------------------------------------------------------------------

    /** Panel corner radius: 8px. */
    public static final float PANEL_RADIUS = 8.0F;

    /** Button corner radius: 6px. */
    public static final float BUTTON_RADIUS = 6.0F;

    /** Base spacing grid step: 8px. */
    public static final float GRID = 8.0F;

    /** Interior panel padding: 16px. */
    public static final float PANEL_PADDING = 16.0F;

    // ------------------------------------------------------------------
    // Motion tokens
    // ------------------------------------------------------------------

    /** Hover/press transition length: 150 ms. */
    public static final long HOVER_MS = 150L;

    /** Ease-out curve used for hover and other micro-interactions. */
    public static final Easing HOVER_EASING = Easing.CUBIC_OUT;

    /** Reference GUI scale the design-pixel tokens were authored against. */
    private static final float REFERENCE_GUI_SCALE = 2.0F;

    /**
     * The currently selected UI typeface.
     *
     * @return the theme font choice; Inter until the module is installed
     */
    public static gg.dindijari.client.module.modules.ThemeModule.UiFont uiFont() {
        return config == null
                ? gg.dindijari.client.module.modules.ThemeModule.UiFont.INTER
                : config.font().get();
    }

    /**
     * Converts a design-pixel dimension into GUI units at the reference scale,
     * additionally applying the theme's UI Scale setting.
     *
     * <p>GUI units are what {@code GuiGraphics} coordinates use; one GUI unit is
     * {@code guiScale} physical pixels. Authoring at scale 2 means a token value
     * of 8 design px equals 4 GUI units, which renders as exactly 8 physical
     * pixels at GUI scale 2 and proportionally elsewhere.
     *
     * @param designPx a dimension in design pixels
     * @return the equivalent dimension in GUI units
     */
    public static float px(float designPx) {
        float userScale = config == null ? 1.0F : (float) (config.uiScale().get() / 100.0);
        return designPx * userScale / REFERENCE_GUI_SCALE;
    }

    /**
     * Snaps a GUI-unit coordinate to the physical pixel grid, avoiding blurry
     * half-pixel edges on crisp elements like underlines.
     *
     * @param guiUnits a coordinate in GUI units
     * @return the coordinate rounded to the nearest physical pixel
     */
    public static float snap(float guiUnits) {
        double scale = Minecraft.getInstance().getWindow().getGuiScale();
        if (scale <= 0) {
            return guiUnits;
        }
        return (float) (Math.round(guiUnits * scale) / scale);
    }
}
