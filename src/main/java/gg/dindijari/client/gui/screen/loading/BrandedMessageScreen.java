package gg.dindijari.client.gui.screen.loading;

import gg.dindijari.client.render.Fonts;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.Screen;

/**
 * Branded replacement for vanilla {@code GenericMessageScreen}s — the simple
 * "Saving world", "Preparing for world creation...", "Reading world data..."
 * style interstitials. Swapped in by
 * {@link gg.dindijari.client.gui.ScreenManager}; the original screen's title
 * becomes the status line under an indeterminate accent bar.
 */
public final class BrandedMessageScreen extends Screen {

    private final Component status;

    /**
     * Creates the branded message screen.
     *
     * @param message the message from the replaced vanilla screen
     */
    public BrandedMessageScreen(Component message) {
        super(message);
        // Restyle into the client's UI font and secondary colour treatment.
        this.status = Fonts.ui(message.getString());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        BrandedLoadingRenderer.render(g, this.width, this.height, -1.0F, status, null);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Fully painted by render(); no vanilla background.
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
