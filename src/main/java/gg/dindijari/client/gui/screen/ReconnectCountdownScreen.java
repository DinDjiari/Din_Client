package gg.dindijari.client.gui.screen;

import gg.dindijari.client.gui.widget.ThemedButton;
import gg.dindijari.client.module.modules.qol.AutoReconnectModule;
import gg.dindijari.client.module.modules.qol.ServerIpHideModule;
import gg.dindijari.client.render.Fonts;
import gg.dindijari.client.render.Render2D;
import gg.dindijari.client.render.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;

/**
 * Themed replacement for the vanilla "Disconnected" screen while Auto
 * Reconnect is armed: a floating panel with the server's name and address
 * (masked when Server IP Hide is on), a live countdown, and Reconnect Now /
 * Cancel actions. Reconnecting goes through the vanilla connect flow.
 */
public final class ReconnectCountdownScreen extends ThemedScreen {

    private final AutoReconnectModule module;
    private final ServerData server;
    private final Component title = Fonts.ui("Connection lost");
    private final Component serverLine;
    private final long reconnectAtMs;

    private int lastShownSecond = -1;
    private Component countdownLine = Component.empty();

    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;

    /**
     * Creates the countdown screen.
     *
     * @param module the auto-reconnect module (attempt accounting)
     * @param server the server to reconnect to
     */
    public ReconnectCountdownScreen(AutoReconnectModule module, ServerData server) {
        super(Component.translatable("disconnect.lost"));
        this.module = module;
        this.server = server;
        String address = ServerIpHideModule.active() ? "\u2022\u2022\u2022 hidden \u2022\u2022\u2022" : server.ip;
        this.serverLine = Fonts.ui(server.name + " \u00b7 " + address);
        this.reconnectAtMs = System.currentTimeMillis() + module.delaySeconds() * 1000L;
    }

    @Override
    protected void init() {
        int pad = Math.round(Theme.px(Theme.PANEL_PADDING));
        int bw = Math.round(Theme.px(150));
        int bh = Math.round(Theme.px(32));
        int gap = Math.round(Theme.px(Theme.GRID));
        panelW = Math.round(Theme.px(360));
        panelH = Math.round(Theme.px(150));
        panelX = (this.width - panelW) / 2;
        panelY = (this.height - panelH) / 2;

        int total = 2 * bw + gap;
        int y = panelY + panelH - bh - pad;
        addRenderableWidget(new ThemedButton(panelX + (panelW - total) / 2, y, bw, bh,
                "Reconnect Now", true, this::reconnect));
        addRenderableWidget(new ThemedButton(panelX + (panelW - total) / 2 + bw + gap, y, bw, bh,
                "Cancel", this::onClose));
    }

    @Override
    public void tick() {
        super.tick();
        if (System.currentTimeMillis() >= reconnectAtMs) {
            reconnect();
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);

        float pad = Theme.px(Theme.PANEL_PADDING);
        float cx = panelX + panelW / 2.0F;
        Fonts.drawCentered(g, title, cx, panelY + pad, 1.25F, Theme.TEXT_PRIMARY, false);
        Fonts.drawCentered(g, serverLine, cx, panelY + pad + Theme.px(22), 1.0F,
                Theme.TEXT_SECONDARY, false);

        int seconds = (int) Math.max(0, (reconnectAtMs - System.currentTimeMillis() + 999) / 1000);
        if (seconds != lastShownSecond) {
            lastShownSecond = seconds;
            countdownLine = Fonts.ui("Reconnecting in " + seconds + "s \u00b7 attempt "
                    + (module.attemptsUsed() + 1) + "/" + module.maxAttempts());
        }
        Fonts.drawCentered(g, countdownLine, cx, panelY + pad + Theme.px(44), 1.0F,
                Theme.accent(), false);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        float radius = Theme.px(Theme.PANEL_RADIUS);
        Render2D.dropShadow(g, panelX, panelY, panelW, panelH, radius, Theme.px(14), Theme.SHADOW);
        Render2D.fillRounded(g, panelX, panelY, panelW, panelH, radius, Theme.PANEL);
        Render2D.rgbLine(g, panelX + radius, panelY, panelW - 2 * radius, Theme.px(3));
    }

    private void reconnect() {
        module.countAttempt();
        ConnectScreen.startConnecting(new JoinMultiplayerScreen(new TitleScreen()),
                this.minecraft, ServerAddress.parseString(server.ip), server, false, null);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(new DindijariJoinMultiplayerScreen(new DindijariTitleScreen()));
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}
