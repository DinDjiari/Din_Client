package gg.dindijari.client.module.modules.performance;

import gg.dindijari.client.module.Category;
import gg.dindijari.client.module.Module;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Removes distance/water/lava fog by cancelling NeoForge's
 * {@link ViewportEvent.RenderFog} while enabled. This is the supported
 * client-side fog hook; when the module is disabled the listener is a single
 * boolean check.
 */
public final class NoFogModule extends Module {

    /**
     * Creates the module and registers its (enabled-gated) fog listener.
     */
    public NoFogModule() {
        super("No Fog",
                "Removes fog rendering (distance, water and lava fog).",
                Category.PERFORMANCE);
        NeoForge.EVENT_BUS.addListener(this::onFog);
    }

    private void onFog(ViewportEvent.RenderFog event) {
        if (isEnabled()) {
            event.setCanceled(true);
        }
    }
}
