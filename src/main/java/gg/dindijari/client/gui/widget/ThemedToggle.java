package gg.dindijari.client.gui.widget;

import gg.dindijari.client.render.ColorUtil;
import gg.dindijari.client.render.Render2D;
import gg.dindijari.client.render.Theme;
import gg.dindijari.client.util.animation.Animation;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * A pill-shaped on/off switch. The track blends to {@link Theme#ACCENT} when
 * on, and the knob glides between ends with the theme's 150&nbsp;ms ease-out
 * motion. State is read from and written through the supplied accessors, so
 * external changes (keybind toggles, config loads) are reflected live.
 */
public class ThemedToggle extends AbstractWidget {

    private final BooleanSupplier getter;
    private final Consumer<Boolean> setter;
    private final Animation knob;

    /**
     * Creates a toggle bound to external state.
     *
     * @param x      left x in GUI units
     * @param y      top y in GUI units
     * @param width  width in GUI units (pill track width)
     * @param height height in GUI units (pill track height)
     * @param label  accessible name announced by narration
     * @param getter reads the current state
     * @param setter writes the new state when clicked
     */
    public ThemedToggle(int x, int y, int width, int height, Component label,
                        BooleanSupplier getter, Consumer<Boolean> setter) {
        super(x, y, width, height, label);
        this.getter = getter;
        this.setter = setter;
        this.knob = new Animation(getter.getAsBoolean() ? 1.0 : 0.0, Theme.hoverMs(), Theme.HOVER_EASING);
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        boolean on = getter.getAsBoolean();
        knob.animateTo(on ? 1.0 : 0.0);
        float t = knob.valueF();

        float radius = this.height / 2.0F;
        int track = ColorUtil.lerp(Theme.BUTTON_HOVER, Theme.accent(), t);
        Render2D.fillRounded(g, getX(), getY(), this.width, this.height, radius, track);

        float inset = Theme.px(3.0F);
        float knobRadius = radius - inset;
        float knobX = getX() + radius + t * (this.width - 2 * radius);
        int knobColor = ColorUtil.lerp(Theme.TEXT_SECONDARY, 0xFF0E0E10, t);
        Render2D.fillCircle(g, knobX, getY() + radius, knobRadius, knobColor);

        if (isFocused()) {
            Render2D.outlineRounded(g, getX() - 1, getY() - 1, this.width + 2, this.height + 2,
                    radius + 1, 1.0F, ColorUtil.scaleAlpha(Theme.accent(), 0.8F));
        }
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        setter.accept(!getter.getAsBoolean());
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.active && this.visible && (keyCode == 257 || keyCode == 32 || keyCode == 335)) {
            playDownSound(net.minecraft.client.Minecraft.getInstance().getSoundManager());
            setter.accept(!getter.getAsBoolean());
            return true;
        }
        return false;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narration) {
        defaultButtonNarrationText(narration);
    }
}
