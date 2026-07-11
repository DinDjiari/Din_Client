package gg.dindijari.client.gui.screen;

import gg.dindijari.client.gui.widget.ThemedButton;
import gg.dindijari.client.gui.widget.ThemedCheckbox;
import gg.dindijari.client.module.modules.qol.NotificationsModule;
import gg.dindijari.client.render.ColorUtil;
import gg.dindijari.client.render.Fonts;
import gg.dindijari.client.render.Render2D;
import gg.dindijari.client.render.Theme;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/**
 * Modal Embeddium recommendation dialog, shown after the main menu loads on
 * every start — unless Embeddium is installed or the user checked "Nicht mehr
 * anzeigen" (persisted via the Notifications module). Contains the client's
 * own procedurally drawn lightning icon (no third-party assets), the accent
 * "Download Embeddium" action (vanilla confirm-link flow to Modrinth), a flat
 * "Sp\u00e4ter" button and the themed checkbox.
 */
public final class EmbeddiumDialogScreen extends ThemedScreen {

    private static final String EMBEDDIUM_URL = "https://modrinth.com/mod/embeddium";

    private final Screen parent;
    private final NotificationsModule notifications;
    private final Component title = Fonts.ui("Embeddium empfohlen");
    private List<FormattedCharSequence> body = List.of();

    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;

    /**
     * Creates the dialog.
     *
     * @param parent        the screen to return to (the main menu)
     * @param notifications holds the persistent "don't show again" setting
     */
    public EmbeddiumDialogScreen(Screen parent, NotificationsModule notifications) {
        super(Component.literal("Embeddium empfohlen"));
        this.parent = parent;
        this.notifications = notifications;
    }

    @Override
    protected void init() {
        panelW = Math.round(Theme.px(440));
        panelH = Math.round(Theme.px(240));
        panelX = (this.width - panelW) / 2;
        panelY = (this.height - panelH) / 2;

        int pad = Math.round(Theme.px(Theme.PANEL_PADDING));
        // Body wraps across the full panel width (below the icon+title row).
        body = this.font.split(Fonts.ui(
                "Embeddium verbessert die FPS deutlich (optimierter Weltrenderer) "
                        + "und wird f\u00fcr diesen Client empfohlen. Es ist ein separater "
                        + "Mod und wird nicht mitgeliefert."),
                panelW - 2 * pad);

        int bh = Math.round(Theme.px(32));
        int by = panelY + panelH - bh - Math.round(Theme.px(38));
        int downloadW = Math.round(Theme.px(210));
        int laterW = Math.round(Theme.px(100));
        int gap = Math.round(Theme.px(Theme.GRID));
        int total = downloadW + gap + laterW;
        addRenderableWidget(new ThemedButton(panelX + (panelW - total) / 2, by,
                downloadW, bh, "Download Embeddium", true, this::openDownload));
        addRenderableWidget(new ThemedButton(panelX + (panelW - total) / 2 + downloadW + gap, by,
                laterW, bh, "Sp\u00e4ter", this::onClose));

        addRenderableWidget(new ThemedCheckbox(panelX + pad,
                panelY + panelH - Math.round(Theme.px(26)),
                panelW - 2 * pad, Math.round(Theme.px(20)), "Nicht mehr anzeigen",
                () -> !notifications.embeddiumHintEnabled(),
                dontShow -> notifications.hintSetting().set(!dontShow)));
    }

    private void openDownload() {
        this.minecraft.setScreen(new ConfirmLinkScreen(open -> {
            if (open) {
                Util.getPlatform().openUri(EMBEDDIUM_URL);
            }
            this.minecraft.setScreen(this);
        }, EMBEDDIUM_URL, true));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);

        float pad = Theme.px(Theme.PANEL_PADDING);
        float iconSize = Theme.px(34);
        float ix = panelX + pad;
        float iy = panelY + pad;

        // Procedural lightning icon (our own asset): accent bolt on dark tile.
        Render2D.fillRounded(g, ix, iy, iconSize, iconSize, Theme.px(8), 0xFF0E0E10);
        float cx = ix + iconSize / 2;
        float cy = iy + iconSize / 2;
        int accent = Theme.accent();
        Render2D.fillTriangle(g, cx + Theme.px(3), cy - Theme.px(12),
                cx - Theme.px(7), cy + Theme.px(2), cx + Theme.px(1), cy + Theme.px(2), accent);
        Render2D.fillTriangle(g, cx - Theme.px(3), cy + Theme.px(12),
                cx + Theme.px(7), cy - Theme.px(2), cx - Theme.px(1), cy - Theme.px(2), accent);

        float textX = ix + iconSize + Theme.px(12);
        Fonts.drawScaled(g, title, textX, iy + Theme.px(8), 1.2F, Theme.TEXT_PRIMARY, false);
        // Body starts below the icon/title row, spanning the full width.
        float ty = iy + iconSize + Theme.px(8);
        for (FormattedCharSequence line : body) {
            g.drawString(this.font, line, Math.round(ix), Math.round(ty),
                    Theme.TEXT_SECONDARY, false);
            ty += 11;
        }
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        // Dialog panel: #16161A, 10 px radius per the dialog spec.
        float radius = Theme.px(10);
        Render2D.dropShadow(g, panelX, panelY, panelW, panelH, radius, Theme.px(14), Theme.SHADOW);
        Render2D.fillRounded(g, panelX, panelY, panelW, panelH, radius, Theme.PANEL);
        Render2D.rgbLine(g, panelX + radius, panelY, panelW - 2 * radius, Theme.px(3));
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
