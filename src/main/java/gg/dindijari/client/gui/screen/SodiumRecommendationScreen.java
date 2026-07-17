package gg.dindijari.client.gui.screen;

import gg.dindijari.client.core.ClientSounds;
import gg.dindijari.client.core.ClientState;
import gg.dindijari.client.gui.widget.ThemedButton;
import gg.dindijari.client.gui.widget.ThemedCheckbox;
import gg.dindijari.client.render.Fonts;
import gg.dindijari.client.render.Render2D;
import gg.dindijari.client.render.Theme;
import gg.dindijari.client.util.SodiumIntegration;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * The Sodium recommendation dialog, shown once at the main menu when Sodium is
 * not installed (and not suppressed): a themed panel recommending Sodium for
 * significantly better FPS, with a link to the Modrinth page and a
 * "Nicht mehr anzeigen" checkbox persisted via {@link ClientState}.
 */
public final class SodiumRecommendationScreen extends ThemedScreen {

    /** ClientState flag suppressing the dialog permanently. */
    public static final String DISMISS_FLAG = "sodiumPromptDismissed";

    private final Screen parentScreen;
    private final Component heading = Fonts.ui("Sodium empfohlen");
    private final Component body = Fonts.ui("Für deutlich bessere FPS empfehlen wir Sodium.");

    private ThemedCheckbox dontShowAgain;

    /**
     * Creates the dialog.
     *
     * @param parent the screen to return to
     */
    public SodiumRecommendationScreen(Screen parent) {
        super(Component.literal("Sodium empfohlen"));
        this.parentScreen = parent;
        ClientSounds.dialogOpen();
    }

    @Override
    protected void init() {
        int panelW = Math.round(panelW());
        int px = (this.width - panelW) / 2;
        int py = panelCenterY() - Math.round(panelH() / 2);

        boolean checked = dontShowAgain != null && dontShowAgain.isChecked();
        int boxSize = Math.round(Theme.px(14));
        dontShowAgain = new ThemedCheckbox(px + Math.round(Theme.px(24)),
                py + Math.round(Theme.px(76)), panelW - Math.round(Theme.px(48)), boxSize,
                "Nicht mehr anzeigen", checked);
        addRenderableWidget(dontShowAgain);

        int bw = Math.round(Theme.px(150));
        int bh = Math.round(Theme.px(30));
        int gap = Math.round(Theme.px(8));
        int by = py + Math.round(Theme.px(102));
        addRenderableWidget(new ThemedButton(this.width / 2 - bw - gap / 2, by, bw, bh,
                "Zu Modrinth", true, SodiumIntegration::openModrinth));
        addRenderableWidget(new ThemedButton(this.width / 2 + gap / 2, by, bw, bh,
                "Schließen", this::onClose));
    }

    private float panelW() {
        return Theme.px(480);
    }

    private float panelH() {
        return Theme.px(148);
    }

    private int panelCenterY() {
        return this.height / 2;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);

        float panelW = panelW();
        float panelH = panelH();
        float px = (this.width - panelW) / 2.0F;
        float py = panelCenterY() - panelH / 2.0F;
        Render2D.dropShadow(g, px, py, panelW, panelH, Theme.px(Theme.PANEL_RADIUS),
                Theme.px(12), Theme.SHADOW);
        Render2D.fillRounded(g, px, py, panelW, panelH, Theme.px(Theme.PANEL_RADIUS), Theme.PANEL);
        Render2D.rgbLine(g, px + Theme.px(Theme.PANEL_RADIUS), py,
                panelW - 2 * Theme.px(Theme.PANEL_RADIUS), Theme.px(3));

        Fonts.drawCentered(g, heading, this.width / 2.0F, py + Theme.px(20), 1.0F,
                Theme.accent(), false);
        Fonts.drawCentered(g, body, this.width / 2.0F, py + Theme.px(42), 0.95F,
                Theme.TEXT_PRIMARY, false);
    }

    @Override
    public void onClose() {
        if (isClosing()) {
            return;
        }
        if (dontShowAgain != null && dontShowAgain.isChecked()) {
            ClientState.setBool(DISMISS_FLAG, true);
        }
        animateClose(() -> this.minecraft.setScreen(parentScreen));
    }
}
