package gg.dindijari.client.module.modules.performance;

import gg.dindijari.client.gui.clickgui.ClickGuiScreen;
import gg.dindijari.client.module.Category;
import gg.dindijari.client.module.Module;
import gg.dindijari.client.setting.NumberSetting;
import net.minecraft.client.Minecraft;

/**
 * A slider shortcut for the vanilla Max Framerate option (settings-only
 * module). Dragging the slider in the Click GUI writes the vanilla
 * {@code framerateLimit} option (260 = vanilla "Unlimited"); the window's
 * limiter updates immediately through the option's own callback. Changing the
 * same option in vanilla Video Settings remains authoritative — this module
 * only writes on user interaction, never at startup.
 */
public final class FpsLimiterModule extends Module {

    private final NumberSetting limit;

    /**
     * Creates the module, initialised from the current vanilla value.
     */
    public FpsLimiterModule() {
        super("FPS Limiter",
                "Sets the vanilla max framerate (260 = unlimited).",
                Category.PERFORMANCE);
        setToggleable(false);
        Minecraft minecraft = Minecraft.getInstance();
        int current = minecraft != null && minecraft.options != null
                ? minecraft.options.framerateLimit().get() : 120;
        limit = new NumberSetting("FPS Limit",
                "Vanilla max framerate; 260 means unlimited.", current, 10, 260, 10);
        addSetting(limit);
        limit.onChange(value -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.options != null && mc.screen instanceof ClickGuiScreen) {
                mc.options.framerateLimit().set((int) Math.round(value));
                mc.options.save();
            }
        });
    }
}
