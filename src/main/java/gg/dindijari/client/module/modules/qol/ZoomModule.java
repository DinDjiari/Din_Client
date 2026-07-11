package gg.dindijari.client.module.modules.qol;

import com.mojang.blaze3d.platform.InputConstants;
import gg.dindijari.client.module.Category;
import gg.dindijari.client.module.Module;
import gg.dindijari.client.setting.KeybindSetting;
import gg.dindijari.client.setting.NumberSetting;
import gg.dindijari.client.util.animation.Animation;
import gg.dindijari.client.util.animation.Easing;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

/**
 * Hold-to-zoom with smooth easing. While the module is enabled and the zoom
 * key (default C) is held, the FOV eases towards {@code fov / zoom factor}
 * through the supported {@link ViewportEvent.ComputeFov} hook, and eases back
 * out on release. Frame-rate independent; a single boolean check when
 * disabled.
 */
public final class ZoomModule extends Module {

    private final KeybindSetting zoomKey = new KeybindSetting(
            "Zoom Key", "Hold to zoom.", GLFW.GLFW_KEY_C);
    private final NumberSetting factor = new NumberSetting(
            "Zoom Factor", "How strongly the view magnifies.", 4.0, 2.0, 8.0, 0.5);
    private final Animation blend = new Animation(0.0, 180, Easing.CUBIC_OUT);

    /**
     * Creates the module and registers its (enabled-gated) FOV listener.
     */
    public ZoomModule() {
        super("Zoom", "Hold a key to smoothly zoom your view.", Category.VISUALS);
        addSetting(zoomKey, factor);
        NeoForge.EVENT_BUS.addListener(this::onComputeFov);
    }

    private void onComputeFov(ViewportEvent.ComputeFov event) {
        if (!isEnabled()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        boolean held = zoomKey.isBound() && minecraft.screen == null
                && InputConstants.isKeyDown(minecraft.getWindow().getWindow(), zoomKey.get());
        blend.animateTo(held ? 1.0 : 0.0);
        double t = blend.value();
        if (t > 0.0001) {
            double zoomed = event.getFOV() / factor.get();
            event.setFOV(event.getFOV() + (zoomed - event.getFOV()) * t);
        }
    }
}
