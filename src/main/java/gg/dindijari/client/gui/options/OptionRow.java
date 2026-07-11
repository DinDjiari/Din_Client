package gg.dindijari.client.gui.options;

import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.screens.Screen;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * One row of a themed options screen. Rows either wrap a vanilla
 * {@link OptionInstance} (rendered with the client's own widgets while the
 * vanilla instance keeps full ownership of value, validation, callbacks and
 * persistence), toggle non-option vanilla state (player model parts), or
 * navigate to another screen.
 *
 * <p>The wrapped kind is detected from the option's public value-set type:
 * booleans become toggles, {@code IntRange}/{@code ClampingLazyMaxIntRange}/
 * {@code UnitDouble} become sliders, {@code Enum}/{@code AltEnum}/
 * {@code LazyEnum} become cyclers. Anything unrecognised falls back to the
 * option's own vanilla widget so no functionality is ever lost.
 */
public final class OptionRow {

    /** How the row renders and reacts. */
    public enum Kind {
        /** Boolean option → themed toggle. */
        TOGGLE,
        /** Any sliderable option → themed slider. */
        SLIDER,
        /** List-of-values option → themed value cycler chip. */
        CYCLE,
        /** Unrecognised value set → embed the vanilla widget. */
        VANILLA,
        /** Custom boolean state (e.g. player model parts) → themed toggle. */
        CUSTOM_TOGGLE,
        /** Navigation to a sub-screen → themed button. */
        NAV
    }

    final Kind kind;
    final OptionInstance<?> option;
    final String label;
    final BooleanSupplier customGetter;
    final Consumer<Boolean> customSetter;
    final Supplier<Screen> navTarget;
    final boolean navAccent;

    private OptionRow(Kind kind, OptionInstance<?> option, String label,
                      BooleanSupplier customGetter, Consumer<Boolean> customSetter,
                      Supplier<Screen> navTarget, boolean navAccent) {
        this.kind = kind;
        this.option = option;
        this.label = label;
        this.customGetter = customGetter;
        this.customSetter = customSetter;
        this.navTarget = navTarget;
        this.navAccent = navAccent;
    }

    /**
     * Wraps a vanilla option, detecting the widget kind from its value set.
     *
     * @param option the vanilla option instance
     * @return the row
     */
    public static OptionRow of(OptionInstance<?> option) {
        Kind kind;
        Object values = option.values();
        if (values == OptionInstance.BOOLEAN_VALUES || option.get() instanceof Boolean) {
            kind = Kind.TOGGLE;
        } else if (values instanceof OptionInstance.Enum<?>
                || values instanceof OptionInstance.AltEnum<?>
                || values instanceof OptionInstance.LazyEnum<?>) {
            kind = Kind.CYCLE;
        } else if (values instanceof OptionInstance.SliderableValueSet<?>) {
            // Covers IntRange, ClampingLazyMaxIntRange, UnitDouble and every
            // xmapped wrapper (framerateLimit, chat scales, ...) uniformly via
            // the value set's own slider mapping (interface widened by AT).
            kind = Kind.SLIDER;
        } else {
            kind = Kind.VANILLA;
        }
        return new OptionRow(kind, option, null, null, null, null, false);
    }

    /**
     * A themed toggle over non-option state (e.g. a player model part).
     *
     * @param label  display label
     * @param getter reads the state
     * @param setter writes the state
     * @return the row
     */
    public static OptionRow toggle(String label, BooleanSupplier getter, Consumer<Boolean> setter) {
        return new OptionRow(Kind.CUSTOM_TOGGLE, null, label, getter, setter, null, false);
    }

    /**
     * A navigation button row.
     *
     * @param label  button label
     * @param target supplies the screen to open
     * @return the row
     */
    public static OptionRow nav(String label, Supplier<Screen> target) {
        return new OptionRow(Kind.NAV, null, label, null, null, target, false);
    }

    /**
     * The list of selectable values for {@link Kind#CYCLE} rows.
     *
     * @return the candidate values in cycle order
     */
    @SuppressWarnings("unchecked")
    List<Object> cycleValues() {
        Object values = option.values();
        if (values instanceof OptionInstance.Enum<?> e) {
            return (List<Object>) e.values();
        }
        if (values instanceof OptionInstance.AltEnum<?> e) {
            return (List<Object>) e.values();
        }
        if (values instanceof OptionInstance.LazyEnum<?> e) {
            return (List<Object>) e.values().get();
        }
        return List.of();
    }
}
