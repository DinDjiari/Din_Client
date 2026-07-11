package gg.dindijari.client.module.modules.performance;

import gg.dindijari.client.module.Category;
import gg.dindijari.client.module.Module;
import net.minecraft.client.Minecraft;

/**
 * Turns the vanilla Entity Shadows video setting off while enabled and
 * restores the previous value when disabled. What it actually changes:
 * exactly that vanilla option.
 */
public final class NoEntityShadowsModule extends Module {

    private boolean previous = true;

    /**
     * Creates the module.
     */
    public NoEntityShadowsModule() {
        super("No Entity Shadows",
                "Disables the vanilla entity shadow rendering while enabled.",
                Category.PERFORMANCE);
    }

    @Override
    protected void onEnable() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.options == null) {
            return;
        }
        previous = minecraft.options.entityShadows().get();
        minecraft.options.entityShadows().set(false);
    }

    @Override
    protected void onDisable() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.options == null) {
            return;
        }
        minecraft.options.entityShadows().set(previous);
    }
}
