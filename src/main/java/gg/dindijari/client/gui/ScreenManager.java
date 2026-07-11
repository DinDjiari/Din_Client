package gg.dindijari.client.gui;

import gg.dindijari.client.gui.clickgui.ClickGuiScreen;
import gg.dindijari.client.gui.screen.DindijariJoinMultiplayerScreen;
import gg.dindijari.client.gui.screen.DindijariPauseScreen;
import gg.dindijari.client.gui.screen.DindijariSelectWorldScreen;
import gg.dindijari.client.gui.screen.DindijariTitleScreen;
import gg.dindijari.client.gui.screen.RenderDebugScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
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
        }
    }

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
