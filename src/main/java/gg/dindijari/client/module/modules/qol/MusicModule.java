package gg.dindijari.client.module.modules.qol;

import gg.dindijari.client.gui.clickgui.ClickGuiScreen;
import gg.dindijari.client.module.Category;
import gg.dindijari.client.module.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;

/**
 * Convenience switch over the vanilla Music volume: toggling it on in the
 * Click GUI sets the vanilla Music slider to 100%, toggling it off sets it to
 * 0%. The vanilla slider in Music &amp; Sounds stays fully authoritative — the
 * module only writes on user interaction in the Click GUI, never at startup,
 * so a custom volume is never stomped.
 *
 * <p>Background: the client mutes music once on the very first launch (see
 * README); this switch is the advertised way to turn it back on.
 */
public final class MusicModule extends Module {

    /**
     * Creates the module.
     */
    public MusicModule() {
        super("Music", "Vanilla music on/off (first launch defaults to off).",
                Category.CLIENT);
    }

    @Override
    protected void onEnable() {
        applyVolume(1.0);
    }

    @Override
    protected void onDisable() {
        applyVolume(0.0);
    }

    private static void applyVolume(double volume) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null && minecraft.options != null
                && minecraft.screen instanceof ClickGuiScreen) {
            minecraft.options.getSoundSourceOptionInstance(SoundSource.MUSIC).set(volume);
            minecraft.options.save();
        }
    }
}
