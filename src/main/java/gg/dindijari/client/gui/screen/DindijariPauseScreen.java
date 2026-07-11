package gg.dindijari.client.gui.screen;

import gg.dindijari.client.gui.clickgui.ClickGuiScreen;
import gg.dindijari.client.gui.widget.ThemedButton;
import gg.dindijari.client.render.Fonts;
import gg.dindijari.client.render.Render2D;
import gg.dindijari.client.render.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.ShareToLanScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;

/**
 * The client's in-game pause menu, replacing the vanilla {@code PauseScreen}
 * (swapped in by {@link gg.dindijari.client.gui.ScreenManager}; the minimal
 * F3+Esc pause passes through untouched).
 *
 * <p>Per the design reference: a floating rounded panel over the blurred,
 * dimmed game with the animated RGB gradient line along its top edge, a
 * "Game Paused" header, the accent <em>Back to Game</em> primary button, flat
 * buttons for Client Settings / Options / Open to LAN / Achievements, and
 * <em>Disconnect</em> in red.
 */
public final class DindijariPauseScreen extends ThemedScreen {

    /** Red used for the destructive Disconnect label. */
    private static final int DANGER = 0xFFFF5555;

    private final Component header = Fonts.ui("Game Paused");

    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;

    /**
     * Creates the themed pause menu.
     */
    public DindijariPauseScreen() {
        super(Component.translatable("menu.game"));
    }

    @Override
    protected void init() {
        int bw = Math.round(Theme.px(240));
        int bh = Math.round(Theme.px(32));
        int gap = Math.round(Theme.px(Theme.GRID));
        int pad = Math.round(Theme.px(Theme.PANEL_PADDING));
        int headerH = Math.round(Theme.px(40));
        int count = 6;

        panelW = bw + 2 * pad;
        panelH = headerH + count * bh + (count - 1) * gap + 2 * pad;
        panelX = (this.width - panelW) / 2;
        panelY = (this.height - panelH) / 2;

        int x = panelX + pad;
        int y = panelY + pad + headerH;

        addRenderableWidget(new ThemedButton(x, y, bw, bh, "Back to Game", true, this::onClose));
        y += bh + gap;
        addRenderableWidget(new ThemedButton(x, y, bw, bh, "Client Settings",
                () -> this.minecraft.setScreen(new ClickGuiScreen(this))));
        y += bh + gap;
        addRenderableWidget(new ThemedButton(x, y, bw, bh, "Options",
                () -> this.minecraft.setScreen(new OptionsScreen(this, this.minecraft.options))));
        y += bh + gap;
        ThemedButton lan = new ThemedButton(x, y, bw, bh, "Open to LAN",
                () -> this.minecraft.setScreen(new ShareToLanScreen(this)));
        lan.active = this.minecraft.hasSingleplayerServer()
                && !this.minecraft.getSingleplayerServer().isPublished();
        addRenderableWidget(lan);
        y += bh + gap;
        addRenderableWidget(new ThemedButton(x, y, bw, bh, "Achievements",
                () -> this.minecraft.setScreen(
                        new AdvancementsScreen(this.minecraft.player.connection.getAdvancements(), this))));
        y += bh + gap;
        addRenderableWidget(new ThemedButton(x, y, bw, bh, "Disconnect",
                false, DANGER, this::requestDisconnect));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);

        float pad = Theme.px(Theme.PANEL_PADDING);
        Fonts.drawCentered(g, header, panelX + panelW / 2.0F, panelY + pad + Theme.px(8),
                1.25F, Theme.TEXT_PRIMARY, false);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);

        float radius = Theme.px(Theme.PANEL_RADIUS);
        Render2D.dropShadow(g, panelX, panelY, panelW, panelH, radius, Theme.px(14), Theme.SHADOW);
        Render2D.fillRounded(g, panelX, panelY, panelW, panelH, radius, Theme.PANEL);
        // Signature RGB gradient line along the panel's top edge.
        Render2D.rgbLine(g, panelX + radius, panelY, panelW - 2 * radius, Theme.px(3));
    }

    /**
     * Routes the disconnect through the abuse-report draft check, exactly like
     * the vanilla pause menu, so an unfinished report is never dropped
     * silently.
     */
    private void requestDisconnect() {
        this.minecraft.getReportingContext().draftReportHandled(this.minecraft, this, this::disconnect, true);
    }

    /** Mirrors vanilla {@code PauseScreen#onDisconnect}. */
    private void disconnect() {
        boolean local = this.minecraft.isLocalServer();
        ServerData server = this.minecraft.getCurrentServer();
        this.minecraft.level.disconnect();
        if (local) {
            this.minecraft.disconnect(new GenericMessageScreen(Component.translatable("menu.savingLevel")));
        } else {
            this.minecraft.disconnect();
        }
        // The Opening event swaps these for the themed screens.
        TitleScreen title = new TitleScreen();
        if (local) {
            this.minecraft.setScreen(title);
        } else {
            this.minecraft.setScreen(new JoinMultiplayerScreen(title));
        }
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }
}
