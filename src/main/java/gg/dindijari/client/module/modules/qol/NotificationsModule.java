package gg.dindijari.client.module.modules.qol;

import gg.dindijari.client.module.Category;
import gg.dindijari.client.module.Module;
import gg.dindijari.client.setting.BooleanSetting;

/**
 * Settings-only module holding the client's notification preferences —
 * currently the persistent "don't show again" switch for the Embeddium
 * recommendation toast.
 */
public final class NotificationsModule extends Module {

    private final BooleanSetting embeddiumHint = new BooleanSetting(
            "Embeddium Hint",
            "Show the Embeddium recommendation toast when it is not installed.",
            true);

    /**
     * Creates the module.
     */
    public NotificationsModule() {
        super("Notifications", "Client notification preferences.", Category.CLIENT);
        setToggleable(false);
        addSetting(embeddiumHint);
    }

    /**
     * Whether the Embeddium recommendation toast may be shown.
     *
     * @return {@code true} unless the user disabled the hint
     */
    public boolean embeddiumHintEnabled() {
        return embeddiumHint.get();
    }
}
