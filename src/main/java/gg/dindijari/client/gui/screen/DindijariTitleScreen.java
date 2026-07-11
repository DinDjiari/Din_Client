package gg.dindijari.client.gui.screen;

import gg.dindijari.client.core.DindijariClient;
import gg.dindijari.client.gui.clickgui.ClickGuiScreen;
import gg.dindijari.client.gui.widget.ThemedButton;
import gg.dindijari.client.render.ColorUtil;
import gg.dindijari.client.render.Fonts;
import gg.dindijari.client.render.Render2D;
import gg.dindijari.client.render.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.Component;

/**
 * The client's main menu, replacing the vanilla {@code TitleScreen} (swapped
 * in by {@link gg.dindijari.client.gui.ScreenManager}) and laid out per the
 * client's design reference: centred "DINDIJARI" wordmark with accent
 * underline, a stacked column of flat buttons with <em>Client Settings</em> as
 * the filled-accent primary action, a subtle animated gradient over the solid
 * charcoal background (no panorama, no dirt), and a version footer.
 *
 * <p>Singleplayer/Multiplayer intentionally construct the <em>vanilla</em>
 * screens; the screen manager swaps them for the themed versions, keeping a
 * single replacement path.
 */
public final class DindijariTitleScreen extends ThemedScreen {

    private final Component wordmark = Fonts.ui("DINDIJARI");
    private final Component wordmarkSub = Fonts.ui("C L I E N T");
    private final Component versionLine = Fonts.ui("dindijari client v" + DindijariClient.MOD_VERSION);

    /**
     * Creates the themed main menu.
     */
    public DindijariTitleScreen() {
        super(Component.literal(DindijariClient.MOD_NAME));
    }

    @Override
    protected void init() {
        int bw = Math.round(Theme.px(270));
        int bh = Math.round(Theme.px(36));
        int gap = Math.round(Theme.px(Theme.GRID));
        int x = (this.width - bw) / 2;
        int count = 5;
        int stack = count * bh + (count - 1) * gap;
        int y = Math.min(this.height / 2 - Math.round(Theme.px(40)),
                this.height - stack - Math.round(Theme.px(32)));

        addRenderableWidget(new ThemedButton(x, y, bw, bh, "Singleplayer",
                () -> this.minecraft.setScreen(new SelectWorldScreen(this))));
        y += bh + gap;
        addRenderableWidget(new ThemedButton(x, y, bw, bh, "Multiplayer",
                () -> this.minecraft.setScreen(new JoinMultiplayerScreen(this))));
        y += bh + gap;
        addRenderableWidget(new ThemedButton(x, y, bw, bh, "Client Settings", true,
                () -> this.minecraft.setScreen(new ClickGuiScreen(this))));
        y += bh + gap;
        addRenderableWidget(new ThemedButton(x, y, bw, bh, "Options",
                () -> this.minecraft.setScreen(new OptionsScreen(this, this.minecraft.options))));
        y += bh + gap;
        addRenderableWidget(new ThemedButton(x, y, bw, bh, "Quit",
                () -> this.minecraft.stop()));
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        Render2D.fillRect(g, 0, 0, this.width, this.height, Theme.BACKGROUND);

        // Subtle animated gradient: two large, very low-alpha radial glows that
        // drift slowly across the backdrop. Pure math per frame, no allocation.
        float seconds = (System.currentTimeMillis() % 3_600_000L) / 1000.0F;
        float glowR = this.width * 0.65F;
        int accent = Theme.accent();

        float ax = this.width * (0.5F + 0.28F * (float) Math.sin(seconds * (Math.PI * 2 / 37.0)));
        float ay = this.height * (0.30F + 0.12F * (float) Math.cos(seconds * (Math.PI * 2 / 29.0)));
        Render2D.radialGradient(g, ax, ay, glowR,
                ColorUtil.withAlpha(accent, 16), ColorUtil.withAlpha(accent, 0));

        float bx = this.width * (0.5F - 0.30F * (float) Math.sin(seconds * (Math.PI * 2 / 41.0) + 1.7F));
        float by = this.height * (0.75F + 0.10F * (float) Math.sin(seconds * (Math.PI * 2 / 23.0)));
        Render2D.radialGradient(g, bx, by, glowR, 0x0EFFFFFF, 0x00FFFFFF);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);

        // Wordmark with accent underline, per the design reference.
        float cx = this.width / 2.0F;
        float logoScale = 3.0F;
        float logoY = this.height / 4.0F - Theme.px(52);
        Fonts.drawCentered(g, wordmark, cx, logoY, logoScale, Theme.TEXT_PRIMARY, false);

        float logoW = Fonts.width(wordmark) * logoScale;
        float underlineY = Theme.snap(logoY + 9 * logoScale + Theme.px(6));
        Render2D.fillRounded(g, cx - logoW / 2, underlineY, logoW, Theme.px(4), Theme.px(2), Theme.accent());

        Fonts.drawCentered(g, wordmarkSub, cx, underlineY + Theme.px(14), 1.0F, Theme.TEXT_SECONDARY, false);

        // Footer.
        float pad = Theme.px(Theme.GRID);
        Fonts.draw(g, versionLine, pad, this.height - 9 - pad, Theme.TEXT_SECONDARY, false);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }
}
