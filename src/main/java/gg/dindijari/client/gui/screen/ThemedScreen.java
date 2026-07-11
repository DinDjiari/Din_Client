package gg.dindijari.client.gui.screen;

import gg.dindijari.client.render.Render2D;
import gg.dindijari.client.render.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Base class for all client-owned screens, replacing every vanilla background
 * with the theme's dark treatment:
 *
 * <ul>
 *   <li><b>In a world</b>: the vanilla Gaussian-blur post-process (respecting
 *       the user's "Menu Background Blurriness" accessibility option) below a
 *       {@link Theme#INWORLD_OVERLAY} charcoal tint;</li>
 *   <li><b>Outside a world</b>: a solid {@link Theme#BACKGROUND} fill — never
 *       the vanilla dirt/stone tile or panorama.</li>
 * </ul>
 */
public abstract class ThemedScreen extends Screen {

    /**
     * Creates a themed screen.
     *
     * @param title the screen title used for narration
     */
    protected ThemedScreen(Component title) {
        super(title);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (this.minecraft.level != null) {
            // Vanilla's blur post-chain (robust in 1.21.1; strength follows the
            // accessibility slider), then the charcoal tint on top.
            renderBlurredBackground(partialTick);
            Render2D.fillRect(g, 0, 0, this.width, this.height, Theme.INWORLD_OVERLAY);
        } else {
            Render2D.fillRect(g, 0, 0, this.width, this.height, Theme.BACKGROUND);
        }
    }
}
