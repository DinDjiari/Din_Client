package gg.dindijari.client.gui.screen.loading;

import gg.dindijari.client.render.Fonts;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;

/**
 * Branded replacement for the vanilla "Loading terrain..." screen shown while
 * the client waits for the first chunks after joining a world or server.
 * Swapped in by {@link gg.dindijari.client.gui.ScreenManager} — but only for
 * the {@link Reason#OTHER} case (login); Nether/End portal transitions keep
 * the vanilla portal visuals, which are part of the game's look, not a menu.
 *
 * <p>Extends {@link ReceivingLevelScreen} so the level-received polling,
 * 30-second timeout and close/narration behaviour are inherited unchanged;
 * only rendering is replaced. There is no progress signal on this screen, so
 * the accent bar runs in its indeterminate sweep mode.
 */
public final class BrandedReceivingLevelScreen extends ReceivingLevelScreen {

    private static final Component STATUS = Fonts.ui("Loading terrain");

    /**
     * Creates the branded receiving screen.
     *
     * @param levelReceived supplier polled each tick, taken from the replaced
     *                      vanilla screen (via access transformer)
     * @param reason        the transition reason (always {@code OTHER} for the
     *                      swapped case)
     */
    public BrandedReceivingLevelScreen(BooleanSupplier levelReceived, Reason reason) {
        super(levelReceived, reason);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        BrandedLoadingRenderer.render(g, this.width, this.height, -1.0F, STATUS, null);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Fully painted by render(); no vanilla background.
    }
}
