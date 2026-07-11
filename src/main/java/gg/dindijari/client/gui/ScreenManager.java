package gg.dindijari.client.gui;

import gg.dindijari.client.gui.clickgui.ClickGuiScreen;
import gg.dindijari.client.gui.screen.DindijariCreateWorldScreen;
import gg.dindijari.client.gui.screen.DindijariJoinMultiplayerScreen;
import gg.dindijari.client.gui.screen.DindijariPauseScreen;
import gg.dindijari.client.gui.screen.DindijariSelectWorldScreen;
import gg.dindijari.client.gui.screen.DindijariTitleScreen;
import gg.dindijari.client.gui.screen.RenderDebugScreen;
import gg.dindijari.client.gui.screen.loading.BrandedLevelLoadingScreen;
import gg.dindijari.client.gui.screen.loading.BrandedMessageScreen;
import gg.dindijari.client.gui.screen.loading.BrandedReceivingLevelScreen;
import gg.dindijari.client.render.Fonts;
import gg.dindijari.client.render.Render2D;
import gg.dindijari.client.render.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.AccessibilityOptionsScreen;
import net.minecraft.client.gui.screens.options.ChatOptionsScreen;
import net.minecraft.client.gui.screens.options.LanguageSelectScreen;
import net.minecraft.client.gui.screens.options.MouseSettingsScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.options.SkinCustomizationScreen;
import net.minecraft.client.gui.screens.options.SoundOptionsScreen;
import net.minecraft.client.gui.screens.options.VideoSettingsScreen;
import net.minecraft.client.gui.screens.options.controls.ControlsScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import gg.dindijari.client.gui.options.DindijariLanguageScreen;
import gg.dindijari.client.gui.options.DindijariOptions;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Owns the client's screen integration:
 *
 * <ul>
 *   <li>swaps the vanilla main menu, pause menu, world-selection and server
 *       browser for their themed replacements via {@link ScreenEvent.Opening}
 *       (exact-class checks, so themed/other-mod subclasses and the minimal
 *       F3+Esc pause pass through untouched);</li>
 *   <li>opens the {@link ClickGuiScreen} on Right&nbsp;Shift in-game;</li>
 *   <li><b>dev only</b> ({@code !FMLLoader.isProduction()}): auto-opens the
 *       Click GUI once the main menu first appears after launch (a
 *       {@code gradle runClient} convenience — never active in a release jar),
 *       and opens the temporary {@link RenderDebugScreen} on F6 in-game.</li>
 * </ul>
 */
public final class ScreenManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("dindijariclient/screens");
    private static final net.minecraft.network.chat.Component WORDMARK = Fonts.display("DINDIJARI");

    /** Set once the dev auto-open has fired so it happens once per launch. */
    private boolean devSettingsShown;

    /** Armed when the themed title screen appears; consumed on the next tick. */
    private boolean devSettingsPending;

    /**
     * Subscribes all screen-related listeners to the game event bus. Call once
     * during mod construction.
     */
    public void registerEvents() {
        NeoForge.EVENT_BUS.addListener(this::onScreenOpening);
        NeoForge.EVENT_BUS.addListener(this::onBackgroundRendered);
        NeoForge.EVENT_BUS.addListener(this::onKey);
        NeoForge.EVENT_BUS.addListener(this::onClientTick);
        LOGGER.info("Screen manager initialized (dev auto-open: {})", !FMLLoader.isProduction());
    }

    private void onScreenOpening(ScreenEvent.Opening event) {
        var next = event.getNewScreen();
        if (next == null) {
            return;
        }
        // Exact-class checks: replace only the vanilla screens, never our own
        // (or another mod's) subclasses, and never the minimal F3+Esc pause.
        if (next.getClass() == TitleScreen.class) {
            event.setNewScreen(new DindijariTitleScreen());
            if (!FMLLoader.isProduction() && !devSettingsShown) {
                devSettingsPending = true;
            }
        } else if (next.getClass() == PauseScreen.class && ((PauseScreen) next).showsPauseMenu()) {
            event.setNewScreen(new DindijariPauseScreen());
        } else if (next.getClass() == SelectWorldScreen.class) {
            // Vanilla flows construct this with varying parents (title screen,
            // create-world cancel, world deletion); the themed version always
            // returns to the themed main menu for a predictable Back target.
            event.setNewScreen(new DindijariSelectWorldScreen(new DindijariTitleScreen()));
        } else if (next.getClass() == JoinMultiplayerScreen.class) {
            event.setNewScreen(new DindijariJoinMultiplayerScreen(new DindijariTitleScreen()));
        } else if (next.getClass() == CreateWorldScreen.class) {
            event.setNewScreen(new DindijariCreateWorldScreen(
                    new DindijariSelectWorldScreen(new DindijariTitleScreen())));
        } else if (next.getClass() == LevelLoadingScreen.class) {
            // The vanilla screen's progress listener is exposed via access
            // transformer; the branded version inherits all its behaviour.
            event.setNewScreen(new BrandedLevelLoadingScreen(
                    ((LevelLoadingScreen) next).progressListener));
        } else if (next.getClass() == ReceivingLevelScreen.class) {
            ReceivingLevelScreen receiving = (ReceivingLevelScreen) next;
            // Only the login case ("Loading terrain"); Nether/End portal
            // transitions keep the vanilla portal visuals.
            if (receiving.reason == ReceivingLevelScreen.Reason.OTHER) {
                event.setNewScreen(new BrandedReceivingLevelScreen(
                        receiving.levelReceived, receiving.reason));
            }
        } else if (next.getClass() == GenericMessageScreen.class) {
            event.setNewScreen(new BrandedMessageScreen(next.getTitle()));
        } else if (next.getClass() == OptionsScreen.class) {
            event.setNewScreen(DindijariOptions.root(event.getCurrentScreen()));
        } else if (next.getClass() == VideoSettingsScreen.class) {
            event.setNewScreen(DindijariOptions.video(event.getCurrentScreen()));
        } else if (next.getClass() == SoundOptionsScreen.class) {
            event.setNewScreen(DindijariOptions.sound(event.getCurrentScreen()));
        } else if (next.getClass() == ControlsScreen.class) {
            event.setNewScreen(DindijariOptions.controls(event.getCurrentScreen()));
        } else if (next.getClass() == MouseSettingsScreen.class) {
            event.setNewScreen(DindijariOptions.mouse(event.getCurrentScreen()));
        } else if (next.getClass() == ChatOptionsScreen.class) {
            event.setNewScreen(DindijariOptions.chat(event.getCurrentScreen()));
        } else if (next.getClass() == SkinCustomizationScreen.class) {
            event.setNewScreen(DindijariOptions.skin(event.getCurrentScreen()));
        } else if (next.getClass() == AccessibilityOptionsScreen.class) {
            event.setNewScreen(DindijariOptions.accessibility(event.getCurrentScreen()));
        } else if (next.getClass() == LanguageSelectScreen.class) {
            event.setNewScreen(new DindijariLanguageScreen(event.getCurrentScreen()));
        } else if (next.getClass() == net.neoforged.neoforge.client.gui.ModListScreen.class) {
            event.setNewScreen(new gg.dindijari.client.gui.screen.DindijariModsScreen(
                    event.getCurrentScreen()));
        }
    }

    /**
     * Brands the backdrop of transition screens that cannot be safely replaced
     * (they own live state such as the server connection): a charcoal fill and
     * the wordmark are painted after the vanilla background, and the vanilla
     * text/widgets render on top.
     */
    private void onBackgroundRendered(ScreenEvent.BackgroundRendered event) {
        var screen = event.getScreen();
        if (screen instanceof ConnectScreen || screen instanceof ProgressScreen) {
            var g = event.getGuiGraphics();
            float cx = screen.width / 2.0F;
            float cy = screen.height / 2.0F;
            Render2D.fillRect(g, 0, 0, screen.width, screen.height, Theme.BACKGROUND);
            Fonts.drawCentered(g, WORDMARK, cx, cy - Theme.px(96), 1.0F, Theme.TEXT_PRIMARY, false);

            if (screen instanceof ConnectScreen) {
                // Server name + address (masked while Server IP Hide is on).
                var server = Minecraft.getInstance().getCurrentServer();
                if (server != null) {
                    String address = gg.dindijari.client.module.modules.qol.ServerIpHideModule.active()
                            ? "\u2022\u2022\u2022 hidden \u2022\u2022\u2022" : server.ip;
                    String line = server.name + " \u00b7 " + address;
                    if (!line.equals(connectLineText)) {
                        connectLineText = line;
                        connectLine = Fonts.ui(line);
                    }
                    Fonts.drawCentered(g, connectLine, cx, cy - Theme.px(44), 1.0F,
                            Theme.TEXT_SECONDARY, false);
                }
            }

            // Indeterminate accent sweep; the vanilla status text renders below.
            float barW = Math.min(Theme.px(440), screen.width * 0.6F);
            float barH = Theme.px(5);
            float barX = cx - barW / 2;
            float barY = cy - Theme.px(24);
            Render2D.fillRounded(g, barX, barY, barW, barH, barH / 2, Theme.BUTTON_HOVER);
            float segW = barW * 0.25F;
            float t = (System.currentTimeMillis() % 1600L) / 1600.0F;
            float ping = t < 0.5F ? t * 2 : (1 - t) * 2;
            Render2D.fillRounded(g, barX + (barW - segW) * (float) Theme.HOVER_EASING.apply(ping),
                    barY, segW, barH, barH / 2, Theme.accent());
        }
    }

    private String connectLineText;
    private net.minecraft.network.chat.Component connectLine;

    private void onClientTick(ClientTickEvent.Post event) {
        if (!devSettingsPending) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof DindijariTitleScreen title) {
            devSettingsPending = false;
            devSettingsShown = true;
            LOGGER.info("Dev environment detected — auto-opening Client Settings");
            minecraft.setScreen(new ClickGuiScreen(title));
        }
    }

    private void onKey(InputEvent.Key event) {
        if (event.getAction() != GLFW.GLFW_PRESS) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null || minecraft.level == null) {
            return;
        }
        if (event.getKey() == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            minecraft.setScreen(new ClickGuiScreen(null));
        } else if (event.getKey() == GLFW.GLFW_KEY_F6 && !FMLLoader.isProduction()) {
            minecraft.setScreen(new RenderDebugScreen());
        }
    }
}
