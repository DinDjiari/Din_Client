package gg.dindijari.client.gui.widget;

import gg.dindijari.client.render.ColorUtil;
import gg.dindijari.client.render.Fonts;
import gg.dindijari.client.render.Render2D;
import gg.dindijari.client.render.Theme;
import gg.dindijari.client.util.animation.Animation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/**
 * The client's flat button, matching the design reference. Two variants:
 *
 * <ul>
 *   <li><b>Flat</b> (default): {@link Theme#BUTTON} fill blending to
 *       {@link Theme#BUTTON_HOVER} over the theme's 150&nbsp;ms ease-out
 *       hover transition, white Inter label;</li>
 *   <li><b>Accent</b>: filled with the live accent colour and a dark label —
 *       the "primary action" style (Client Settings, Play Selected, Back to
 *       Game, Join Server).</li>
 * </ul>
 *
 * <p>An optional label colour override supports special cases like the red
 * "Disconnect" action. No vanilla button sprites are used anywhere.
 */
public class ThemedButton extends AbstractWidget {

    private final Runnable onPress;
    private final Animation hover;
    private final boolean accent;
    private final int labelColorOverride;
    private Component label;

    /**
     * Creates a standard flat themed button.
     *
     * @param x       left x in GUI units
     * @param y       top y in GUI units
     * @param width   width in GUI units
     * @param height  height in GUI units
     * @param text    the label text
     * @param onPress action invoked on click / activation
     */
    public ThemedButton(int x, int y, int width, int height, String text, Runnable onPress) {
        this(x, y, width, height, text, false, 0, onPress);
    }

    /**
     * Creates a themed button, optionally in the filled accent style.
     *
     * @param x       left x in GUI units
     * @param y       top y in GUI units
     * @param width   width in GUI units
     * @param height  height in GUI units
     * @param text    the label text
     * @param accent  {@code true} for the filled-accent primary style
     * @param onPress action invoked on click / activation
     */
    public ThemedButton(int x, int y, int width, int height, String text, boolean accent, Runnable onPress) {
        this(x, y, width, height, text, accent, 0, onPress);
    }

    /**
     * Creates a themed button with full styling control.
     *
     * @param x          left x in GUI units
     * @param y          top y in GUI units
     * @param width      width in GUI units
     * @param height     height in GUI units
     * @param text       the label text
     * @param accent     {@code true} for the filled-accent primary style
     * @param labelColor packed ARGB label colour override, or 0 for the
     *                   variant default (e.g. {@code 0xFFFF5555} for the red
     *                   Disconnect label)
     * @param onPress    action invoked on click / activation
     */
    public ThemedButton(int x, int y, int width, int height, String text,
                        boolean accent, int labelColor, Runnable onPress) {
        super(x, y, width, height, Fonts.ui(text));
        this.label = Fonts.ui(text);
        this.accent = accent;
        this.labelColorOverride = labelColor;
        this.onPress = onPress;
        this.hover = new Animation(0.0, Theme.hoverMs(), Theme.HOVER_EASING);
    }

    /**
     * Replaces the button label.
     *
     * @param text the new label text
     */
    public void setLabel(String text) {
        this.label = Fonts.ui(text);
        setMessage(this.label);
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        hover.animateTo(isHoveredOrFocused() && this.active ? 1.0 : 0.0);
        float t = hover.valueF();
        float radius = Theme.px(Theme.BUTTON_RADIUS);

        int fill;
        int textColor;
        if (accent) {
            int base = Theme.accent();
            fill = ColorUtil.lerp(base, 0xFFFFFFFF, t * 0.25F);
            textColor = 0xFF0E0E10;
        } else {
            fill = ColorUtil.lerp(Theme.BUTTON, Theme.BUTTON_HOVER, t);
            textColor = Theme.TEXT_PRIMARY;
        }
        if (labelColorOverride != 0) {
            textColor = labelColorOverride;
        }
        if (!this.active) {
            fill = ColorUtil.scaleAlpha(fill, 0.5F);
            textColor = Theme.TEXT_SECONDARY;
        }
        Render2D.fillRounded(g, getX(), getY(), this.width, this.height, radius, fill);

        float textY = getY() + (this.height - 9) / 2.0F + 0.5F;
        Fonts.drawCentered(g, label, getX() + this.width / 2.0F, textY, 1.0F, textColor, false);

        if (isFocused()) {
            Render2D.outlineRounded(g, getX(), getY(), this.width, this.height,
                    radius, 1.0F, ColorUtil.scaleAlpha(accent ? 0xFFFFFFFF : Theme.accent(), 0.8F));
        }
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        this.onPress.run();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Enter / space / numpad-enter activate the focused button.
        if (this.active && this.visible && (keyCode == 257 || keyCode == 32 || keyCode == 335)) {
            playDownSound(Minecraft.getInstance().getSoundManager());
            this.onPress.run();
            return true;
        }
        return false;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narration) {
        defaultButtonNarrationText(narration);
    }
}
