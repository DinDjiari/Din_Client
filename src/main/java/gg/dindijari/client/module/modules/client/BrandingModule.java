package gg.dindijari.client.module.modules.client;

import gg.dindijari.client.core.DindijariClient;
import gg.dindijari.client.module.Category;
import gg.dindijari.client.module.Module;
import gg.dindijari.client.setting.StringSetting;

/**
 * Settings-only module holding the client's branding customization: the OS
 * window caption and (via the Branding panel / {@code WindowBranding}) a
 * custom window icon.
 *
 * <p>The window title defaults to {@value DindijariClient#MOD_NAME}; an empty
 * value falls back to that default. The icon itself is not a setting value —
 * the converted PNGs live under {@code config/dindijariclient/icon/} and are
 * re-applied on every launch.
 */
public final class BrandingModule extends Module {

    private final StringSetting windowTitle = new StringSetting(
            "Window Title", "Caption of the OS game window. Empty uses the default.",
            DindijariClient.MOD_NAME);

    /**
     * Creates the branding module.
     */
    public BrandingModule() {
        super("Branding", "Window title and window icon customization.", Category.CLIENT);
        setToggleable(false);
        addSetting(windowTitle);
    }

    /**
     * Returns the window title setting.
     *
     * @return the window title setting
     */
    public StringSetting windowTitle() {
        return windowTitle;
    }

    /**
     * The title that should actually be applied to the window: the setting's
     * value, or the default when the value is blank.
     *
     * @return the effective window caption
     */
    public String effectiveTitle() {
        String value = windowTitle.get().trim();
        return value.isEmpty() ? DindijariClient.MOD_NAME : value;
    }
}
