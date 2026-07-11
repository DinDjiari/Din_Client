package gg.dindijari.client.gui;

import gg.dindijari.client.render.Fonts;
import gg.dindijari.client.render.Render2D;
import gg.dindijari.client.render.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/**
 * The client's own notification toast: rounded charcoal panel with the RGB
 * accent line on its top edge, an Inter title and wrapped secondary body
 * text. Auto-dismisses after the given duration.
 */
public final class DindijariToast implements Toast {

    private static final int WIDTH = 190;
    private static final int HEIGHT = 40;

    private final Component title;
    private final List<FormattedCharSequence> body;
    private final long durationMs;

    /**
     * Creates a toast.
     *
     * @param title      short title (rendered in the UI font, white)
     * @param body       body text (wrapped, secondary grey)
     * @param durationMs how long the toast stays before auto-dismissing
     */
    public DindijariToast(String title, String body, long durationMs) {
        this.title = Fonts.ui(title);
        this.body = net.minecraft.client.Minecraft.getInstance().font
                .split(Fonts.ui(body), WIDTH - 16);
        this.durationMs = durationMs;
    }

    @Override
    public Visibility render(GuiGraphics g, ToastComponent component, long timeSinceVisible) {
        Render2D.fillRounded(g, 1, 1, WIDTH - 2, HEIGHT - 2, Theme.px(Theme.PANEL_RADIUS), Theme.PANEL);
        Render2D.rgbLine(g, 1 + Theme.px(Theme.PANEL_RADIUS), 1,
                WIDTH - 2 - 2 * Theme.px(Theme.PANEL_RADIUS), Theme.px(3));
        Fonts.draw(g, title, 8, 7, Theme.TEXT_PRIMARY, false);
        float y = 19;
        for (FormattedCharSequence line : body) {
            g.drawString(net.minecraft.client.Minecraft.getInstance().font, line, 8, Math.round(y),
                    Theme.TEXT_SECONDARY, false);
            y += 10;
        }
        return timeSinceVisible >= durationMs ? Visibility.HIDE : Visibility.SHOW;
    }

    @Override
    public int width() {
        return WIDTH;
    }

    @Override
    public int height() {
        return HEIGHT;
    }
}
