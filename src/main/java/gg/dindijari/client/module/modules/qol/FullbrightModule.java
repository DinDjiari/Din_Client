package gg.dindijari.client.module.modules.qol;

import gg.dindijari.client.module.Category;
import gg.dindijari.client.module.Module;
import net.minecraft.client.Minecraft;

/**
 * Classic fullbright: raises the vanilla Brightness (gamma) far beyond the
 * slider maximum while enabled and restores the previous value when disabled.
 * The out-of-range write uses the game option's value field directly (exposed
 * via access transformer) because {@code OptionInstance.set} clamps to the
 * 0–1 slider range; the light texture reads the raw value, which is exactly
 * how classic fullbright works. Purely visual and client-side.
 */
public final class FullbrightModule extends Module {

    private static final double FULLBRIGHT_GAMMA = 15.0;

    private double previous = 1.0;

    /**
     * Creates the module.
     */
    public FullbrightModule() {
        super("Fullbright", "Maximises brightness so caves are fully visible.",
                Category.VISUALS);
    }

    @Override
    protected void onEnable() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.options == null) {
            return;
        }
        previous = minecraft.options.gamma().get();
        minecraft.options.gamma().value = FULLBRIGHT_GAMMA;
    }

    @Override
    protected void onDisable() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.options == null) {
            return;
        }
        minecraft.options.gamma().value = previous;
    }
}
