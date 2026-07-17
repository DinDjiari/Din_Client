package gg.dindijari.client.core;

import gg.dindijari.client.module.modules.client.InterfaceModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The client's UI sound layer: registration of the bundled sound events and a
 * small play facade honouring the Interface module's "UI Sounds" toggle and
 * volume slider.
 *
 * <p>All sounds are original, synthesized for this project (CC0 — see the
 * README) and ship as OGG files under {@code assets/dindijariclient/sounds/}.
 * They play through the vanilla sound system as UI sounds on the master
 * category, so the vanilla master volume applies on top of the client's own
 * UI volume.
 */
public final class ClientSounds {

    /** Deferred register wired to the mod event bus during construction. */
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, DindijariClient.MOD_ID);

    /** Soft tick played when a button gains hover. */
    public static final DeferredHolder<SoundEvent, SoundEvent> UI_HOVER = register("ui_hover");
    /** Click played when a button is pressed. */
    public static final DeferredHolder<SoundEvent, SoundEvent> UI_CLICK = register("ui_click");
    /** Rising tone played when a module/toggle turns on. */
    public static final DeferredHolder<SoundEvent, SoundEvent> TOGGLE_ON = register("toggle_on");
    /** Falling tone played when a module/toggle turns off. */
    public static final DeferredHolder<SoundEvent, SoundEvent> TOGGLE_OFF = register("toggle_off");
    /** Pop played when a notification toast appears. */
    public static final DeferredHolder<SoundEvent, SoundEvent> NOTIFY = register("notify");
    /** Soft swell played when a dialog opens. */
    public static final DeferredHolder<SoundEvent, SoundEvent> DIALOG_OPEN = register("dialog_open");
    /** Buzz played for error notifications. */
    public static final DeferredHolder<SoundEvent, SoundEvent> ERROR = register("error");

    private static InterfaceModule config;

    private ClientSounds() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(
                ResourceLocation.fromNamespaceAndPath(DindijariClient.MOD_ID, name)));
    }

    /**
     * Binds the Interface module so playback honours the "UI Sounds" toggle
     * and volume. Called once during mod construction.
     *
     * @param module the interface module
     */
    public static void install(InterfaceModule module) {
        config = module;
    }

    /**
     * Plays a UI sound if UI sounds are enabled, scaled by the UI volume and
     * an additional per-callsite gain.
     *
     * @param sound the sound event holder
     * @param pitch playback pitch (1 = as recorded)
     * @param gain  extra volume factor for this interaction (0..1)
     */
    public static void play(DeferredHolder<SoundEvent, SoundEvent> sound, float pitch, float gain) {
        if (config != null && !config.uiSounds().get()) {
            return;
        }
        float volume = (config == null ? 0.8F : (float) (config.uiVolume().get() / 100.0)) * gain;
        if (volume <= 0.0F || !sound.isBound()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> minecraft.getSoundManager()
                .play(SimpleSoundInstance.forUI(sound.get(), pitch, volume)));
    }

    /** Plays the button hover tick (very subtle). */
    public static void hover() {
        play(UI_HOVER, 1.0F, 0.35F);
    }

    /** Plays the button click. */
    public static void click() {
        play(UI_CLICK, 1.0F, 0.9F);
    }

    /**
     * Plays the rising (enable) or falling (disable) toggle tone.
     *
     * @param enabled the state being switched to
     */
    public static void toggle(boolean enabled) {
        play(enabled ? TOGGLE_ON : TOGGLE_OFF, 1.0F, 0.9F);
    }

    /** Plays the notification pop. */
    public static void notifyPop() {
        play(NOTIFY, 1.0F, 0.9F);
    }

    /** Plays the dialog-open swell. */
    public static void dialogOpen() {
        play(DIALOG_OPEN, 1.0F, 0.8F);
    }

    /** Plays the error buzz. */
    public static void error() {
        play(ERROR, 1.0F, 0.9F);
    }
}
