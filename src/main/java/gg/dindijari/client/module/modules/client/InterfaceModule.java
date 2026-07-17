package gg.dindijari.client.module.modules.client;

import gg.dindijari.client.module.Category;
import gg.dindijari.client.module.Module;
import gg.dindijari.client.setting.BooleanSetting;
import gg.dindijari.client.setting.NumberSetting;

/**
 * Settings-only module holding the client's interface polish switches: the
 * "UI Animations" master toggle (screen/dialog transitions, toggle ripples,
 * toast slides) and the "UI Sounds" master toggle with its volume slider.
 *
 * <p>Both persist through the ordinary config pipeline. Animations are
 * additionally suppressed while Performance Mode is enabled (see
 * {@code Theme.animationsEnabled()}), and UI sounds always play through the
 * vanilla sound system, so they respect the vanilla master volume.
 */
public final class InterfaceModule extends Module {

    private final BooleanSetting uiAnimations = new BooleanSetting(
            "UI Animations", "Smooth transitions for screens, dialogs, toggles and toasts.", true);
    private final BooleanSetting uiSounds = new BooleanSetting(
            "UI Sounds", "Subtle sound effects for hovers, clicks, toggles and notifications.", true);
    private final NumberSetting uiVolume = new NumberSetting(
            "UI Volume", "Volume of the client's UI sounds (percent).", 80, 0, 100, 5);

    /**
     * Creates the interface module.
     */
    public InterfaceModule() {
        super("Interface", "Animations and sound effects of the client UI.", Category.CLIENT);
        setToggleable(false);
        addSetting(uiAnimations, uiSounds, uiVolume);
    }

    /**
     * Returns the "UI Animations" master toggle.
     *
     * @return the animations setting
     */
    public BooleanSetting uiAnimations() {
        return uiAnimations;
    }

    /**
     * Returns the "UI Sounds" master toggle.
     *
     * @return the sounds setting
     */
    public BooleanSetting uiSounds() {
        return uiSounds;
    }

    /**
     * Returns the UI sound volume setting (percent).
     *
     * @return the volume setting
     */
    public NumberSetting uiVolume() {
        return uiVolume;
    }
}
