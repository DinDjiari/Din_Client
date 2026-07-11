package gg.dindijari.client.gui.options;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.PlayerModelPart;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory for the client's themed options screens. Each screen mirrors the
 * option list of its vanilla counterpart 1:1 (wrapped, never reimplemented —
 * see {@link OptionRow}); Key Binds and Resource Packs deliberately open the
 * vanilla screens (capture UI and drag-and-drop lists; see LIMITATIONS.md).
 */
public final class DindijariOptions {

    private DindijariOptions() {
    }

    private static Options options() {
        return Minecraft.getInstance().options;
    }

    /**
     * The root Options screen.
     *
     * @param parent the screen to return to
     * @return the themed screen
     */
    public static Screen root(Screen parent) {
        Options o = options();
        List<OptionRow> rows = new ArrayList<>();
        rows.add(OptionRow.of(o.fov()));
        rows.add(OptionRow.nav("Video Settings", () -> video(current())));
        rows.add(OptionRow.nav("Music & Sounds", () -> sound(current())));
        rows.add(OptionRow.nav("Controls", () -> controls(current())));
        rows.add(OptionRow.nav("Chat Settings", () -> chat(current())));
        rows.add(OptionRow.nav("Skin Customization", () -> skin(current())));
        rows.add(OptionRow.nav("Language", () -> new DindijariLanguageScreen(current())));
        rows.add(OptionRow.nav("Accessibility", () -> accessibility(current())));
        rows.add(OptionRow.nav("Resource Packs", DindijariOptions::vanillaPacks));
        rows.add(OptionRow.nav("Mods", () ->
                new gg.dindijari.client.gui.screen.DindijariModsScreen(current())));
        rows.add(OptionRow.nav("Online Options", DindijariOptions::vanillaOnline));
        return new ThemedOptionsScreen(parent, "Options", rows);
    }

    /**
     * Video Settings — mirrors the vanilla list. Mipmap changes trigger the
     * same texture reload vanilla performs on close.
     *
     * @param parent the screen to return to
     * @return the themed screen
     */
    public static Screen video(Screen parent) {
        Options o = options();
        List<OptionRow> rows = List.of(
                OptionRow.of(o.graphicsMode()),
                OptionRow.of(o.renderDistance()),
                OptionRow.of(o.simulationDistance()),
                OptionRow.of(o.prioritizeChunkUpdates()),
                OptionRow.of(o.ambientOcclusion()),
                OptionRow.of(o.framerateLimit()),
                OptionRow.of(o.enableVsync()),
                OptionRow.of(o.bobView()),
                OptionRow.of(o.guiScale()),
                OptionRow.of(o.attackIndicator()),
                OptionRow.of(o.gamma()),
                OptionRow.of(o.cloudStatus()),
                OptionRow.of(o.fullscreen()),
                OptionRow.of(o.particles()),
                OptionRow.of(o.mipmapLevels()),
                OptionRow.of(o.entityShadows()),
                OptionRow.of(o.screenEffectScale()),
                OptionRow.of(o.entityDistanceScaling()),
                OptionRow.of(o.fovEffectScale()),
                OptionRow.of(o.showAutosaveIndicator()),
                OptionRow.of(o.glintSpeed()),
                OptionRow.of(o.glintStrength()),
                OptionRow.of(o.menuBackgroundBlurriness()));
        return new ThemedOptionsScreen(parent, "Video Settings", rows) {
            private final int mipmapsAtOpen = o.mipmapLevels().get();

            @Override
            public void removed() {
                super.removed();
                if (o.mipmapLevels().get() != mipmapsAtOpen) {
                    Minecraft minecraft = Minecraft.getInstance();
                    minecraft.updateMaxMipLevel(o.mipmapLevels().get());
                    minecraft.delayTextureReload();
                }
            }
        };
    }

    /**
     * Music &amp; Sounds — volume sliders per source plus the device and
     * subtitle options, mirroring vanilla.
     *
     * @param parent the screen to return to
     * @return the themed screen
     */
    public static Screen sound(Screen parent) {
        Options o = options();
        List<OptionRow> rows = new ArrayList<>();
        for (SoundSource source : SoundSource.values()) {
            rows.add(OptionRow.of(o.getSoundSourceOptionInstance(source)));
        }
        rows.add(OptionRow.of(o.soundDevice()));
        rows.add(OptionRow.of(o.showSubtitles()));
        rows.add(OptionRow.of(o.directionalAudio()));
        return new ThemedOptionsScreen(parent, "Music & Sounds", rows);
    }

    /**
     * Controls — mouse settings, key binds entry (vanilla capture screen) and
     * the vanilla toggle options.
     *
     * @param parent the screen to return to
     * @return the themed screen
     */
    public static Screen controls(Screen parent) {
        Options o = options();
        List<OptionRow> rows = List.of(
                OptionRow.nav("Mouse Settings", () -> mouse(current())),
                OptionRow.nav("Key Binds", () -> new KeyBindsScreen(current(), options())),
                OptionRow.of(o.toggleCrouch()),
                OptionRow.of(o.toggleSprint()),
                OptionRow.of(o.autoJump()),
                OptionRow.of(o.operatorItemsTab()));
        return new ThemedOptionsScreen(parent, "Controls", rows);
    }

    /**
     * Mouse Settings — mirrors the vanilla list.
     *
     * @param parent the screen to return to
     * @return the themed screen
     */
    public static Screen mouse(Screen parent) {
        Options o = options();
        List<OptionRow> rows = List.of(
                OptionRow.of(o.sensitivity()),
                OptionRow.of(o.invertYMouse()),
                OptionRow.of(o.mouseWheelSensitivity()),
                OptionRow.of(o.discreteMouseScroll()),
                OptionRow.of(o.touchscreen()),
                OptionRow.of(o.rawMouseInput()));
        return new ThemedOptionsScreen(parent, "Mouse Settings", rows);
    }

    /**
     * Chat Settings — mirrors the vanilla list.
     *
     * @param parent the screen to return to
     * @return the themed screen
     */
    public static Screen chat(Screen parent) {
        Options o = options();
        List<OptionRow> rows = List.of(
                OptionRow.of(o.chatVisibility()),
                OptionRow.of(o.chatColors()),
                OptionRow.of(o.chatLinks()),
                OptionRow.of(o.chatLinksPrompt()),
                OptionRow.of(o.chatOpacity()),
                OptionRow.of(o.textBackgroundOpacity()),
                OptionRow.of(o.chatScale()),
                OptionRow.of(o.chatLineSpacing()),
                OptionRow.of(o.chatDelay()),
                OptionRow.of(o.chatWidth()),
                OptionRow.of(o.chatHeightFocused()),
                OptionRow.of(o.chatHeightUnfocused()),
                OptionRow.of(o.narrator()),
                OptionRow.of(o.autoSuggestions()),
                OptionRow.of(o.hideMatchedNames()),
                OptionRow.of(o.reducedDebugInfo()),
                OptionRow.of(o.onlyShowSecureChat()));
        return new ThemedOptionsScreen(parent, "Chat Settings", rows);
    }

    /**
     * Skin Customization — model-part toggles (vanilla state, themed toggles)
     * plus the main-hand option.
     *
     * @param parent the screen to return to
     * @return the themed screen
     */
    public static Screen skin(Screen parent) {
        Options o = options();
        List<OptionRow> rows = new ArrayList<>();
        for (PlayerModelPart part : PlayerModelPart.values()) {
            rows.add(OptionRow.toggle(part.getName().getString(),
                    () -> o.isModelPartEnabled(part),
                    enabled -> o.toggleModelPart(part, enabled)));
        }
        rows.add(OptionRow.of(o.mainHand()));
        return new ThemedOptionsScreen(parent, "Skin Customization", rows);
    }

    /**
     * Accessibility — mirrors the vanilla list (options only; the panorama
     * ones remain functional even though the client menus don't use panoramas).
     *
     * @param parent the screen to return to
     * @return the themed screen
     */
    public static Screen accessibility(Screen parent) {
        Options o = options();
        List<OptionRow> rows = List.of(
                OptionRow.of(o.narrator()),
                OptionRow.of(o.showSubtitles()),
                OptionRow.of(o.highContrast()),
                OptionRow.of(o.autoJump()),
                OptionRow.of(o.menuBackgroundBlurriness()),
                OptionRow.of(o.textBackgroundOpacity()),
                OptionRow.of(o.backgroundForChatOnly()),
                OptionRow.of(o.chatOpacity()),
                OptionRow.of(o.chatLineSpacing()),
                OptionRow.of(o.chatDelay()),
                OptionRow.of(o.notificationDisplayTime()),
                OptionRow.of(o.toggleCrouch()),
                OptionRow.of(o.toggleSprint()),
                OptionRow.of(o.screenEffectScale()),
                OptionRow.of(o.fovEffectScale()),
                OptionRow.of(o.darknessEffectScale()),
                OptionRow.of(o.damageTiltStrength()),
                OptionRow.of(o.glintSpeed()),
                OptionRow.of(o.glintStrength()),
                OptionRow.of(o.hideLightningFlash()),
                OptionRow.of(o.hideSplashTexts()),
                OptionRow.of(o.panoramaSpeed()),
                OptionRow.of(o.narratorHotkey()));
        return new ThemedOptionsScreen(parent, "Accessibility", rows);
    }

    /** Vanilla Resource Packs screen (drag-and-drop list; see LIMITATIONS.md). */
    private static Screen vanillaPacks() {
        Minecraft minecraft = Minecraft.getInstance();
        return new PackSelectionScreen(minecraft.getResourcePackRepository(), repository -> {
            minecraft.options.updateResourcePacks(repository);
            minecraft.setScreen(current());
        }, minecraft.getResourcePackDirectory(),
                net.minecraft.network.chat.Component.translatable("resourcePack.title"));
    }

    /** Vanilla Online Options screen (Realms/telemetry specifics). */
    private static Screen vanillaOnline() {
        return new net.minecraft.client.gui.screens.options.OnlineOptionsScreen(
                current(), options());
    }

    /** The screen that is open right now (used as the Back target). */
    private static Screen current() {
        return Minecraft.getInstance().screen;
    }
}
