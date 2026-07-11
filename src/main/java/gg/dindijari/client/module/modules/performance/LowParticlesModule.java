package gg.dindijari.client.module.modules.performance;

import gg.dindijari.client.module.Category;
import gg.dindijari.client.module.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;

/**
 * Forces the vanilla Particles video setting to <em>Minimal</em> while
 * enabled and restores the setting it replaced when disabled.
 *
 * <p>What it actually changes: exactly the vanilla option — nothing more. If
 * the module stays enabled across sessions, vanilla persists Minimal, so
 * disabling later restores the value seen when it was last enabled.
 */
public final class LowParticlesModule extends Module {

    private ParticleStatus previous = ParticleStatus.ALL;

    /**
     * Creates the module.
     */
    public LowParticlesModule() {
        super("Low Particles",
                "Sets the vanilla particle level to Minimal while enabled.",
                Category.PERFORMANCE);
    }

    @Override
    protected void onEnable() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.options == null) {
            return;
        }
        previous = minecraft.options.particles().get();
        minecraft.options.particles().set(ParticleStatus.MINIMAL);
    }

    @Override
    protected void onDisable() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.options == null) {
            return;
        }
        minecraft.options.particles().set(previous);
    }
}
