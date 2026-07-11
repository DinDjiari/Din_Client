package gg.dindijari.client.module.modules.performance;

import gg.dindijari.client.module.Category;
import gg.dindijari.client.module.Module;

/**
 * Master switch for the client's own UI effect budget. While enabled, client
 * screens skip the Gaussian blur pass (a deeper flat tint replaces it), drop
 * shadows are not drawn, RGB gradient lines render as static accent lines and
 * a hue-cycling accent stops animating.
 *
 * <p>Scope is honest: this reduces the cost of <em>this client's</em> UI/HUD
 * rendering only — it does not change world rendering (use the FPS Presets and
 * the other Performance modules for vanilla video settings). Zero cost when
 * disabled: consumers check a single boolean.
 */
public final class PerformanceModeModule extends Module {

    /**
     * Creates the module.
     */
    public PerformanceModeModule() {
        super("Performance Mode",
                "Disables blur, shadows and RGB animation in the client UI.",
                Category.PERFORMANCE);
    }
}
