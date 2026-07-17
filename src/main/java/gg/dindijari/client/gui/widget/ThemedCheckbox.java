package gg.dindijari.client.gui.widget;

import gg.dindijari.client.core.ClientSounds;
import gg.dindijari.client.render.ColorUtil;
import gg.dindijari.client.render.Fonts;
import gg.dindijari.client.render.Render2D;
import gg.dindijari.client.render.Theme;
import gg.dindijari.client.util.animation.Animation;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;

/**
 * A themed checkbox: a small rounded box that fills with the accent colour
 * when checked (animated with the theme's micro-transition), with a text label
 * to its right. Used by dialogs such as the Sodium recommendation's
 * "Nicht mehr anzeigen".
 */
public class ThemedCheckbox extends AbstractWidget {

    private final Component label;
    private final Animation check;
    private boolean checked;

    /**
     * Creates a checkbox.
     *
     * @param x       left x in GUI units
     * @param y       top y in GUI units
     * @param width   full widget width (box + label) in GUI units
     * @param height  widget height in GUI units (the box is height-sized)
     * @param text    the label text
     * @param checked the initial state
     */
    public ThemedCheckbox(int x, int y, int width, int height, String text, boolean checked) {
        super(x, y, width, height, Fonts.ui(text));
        this.label = Fonts.ui(text);
        this.checked = checked;
        this.check = new Animation(checked ? 1.0 : 0.0, Theme.hoverMs(), Theme.HOVER_EASING);
    }

    /**
     * Returns the current checked state.
     *
     * @return {@code true} if checked
     */
    public boolean isChecked() {
        return checked;
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        check.animateTo(checked ? 1.0 : 0.0);
        float t = Theme.animationsEnabled() ? check.valueF() : (checked ? 1.0F : 0.0F);

        float box = this.height;
        float radius = Theme.px(4);
        int border = isHoveredOrFocused() ? Theme.TEXT_SECONDARY : Theme.BUTTON_HOVER;
        Render2D.fillRounded(g, getX(), getY(), box, box, radius, Theme.BUTTON);
        Render2D.outlineRounded(g, getX(), getY(), box, box, radius, 1.0F, border);
        if (t > 0.01F) {
            float inset = Theme.px(3) + (1.0F - t) * box * 0.25F;
            Render2D.fillRounded(g, getX() + inset, getY() + inset,
                    box - 2 * inset, box - 2 * inset, Theme.px(2),
                    ColorUtil.scaleAlpha(Theme.accent(), t));
        }

        float textY = getY() + (box - 9) / 2.0F + 0.5F;
        Fonts.draw(g, label, getX() + box + Theme.px(8), textY, Theme.TEXT_SECONDARY, false);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        checked = !checked;
    }

    @Override
    public void playDownSound(SoundManager manager) {
        // Our toggle tones instead of the vanilla button click.
        ClientSounds.toggle(!checked);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.active && this.visible && (keyCode == 257 || keyCode == 32 || keyCode == 335)) {
            ClientSounds.toggle(!checked);
            checked = !checked;
            return true;
        }
        return false;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narration) {
        defaultButtonNarrationText(narration);
    }
}
