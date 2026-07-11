package gg.dindijari.client.gui.widget;

import gg.dindijari.client.render.ColorUtil;
import gg.dindijari.client.render.Fonts;
import gg.dindijari.client.render.Render2D;
import gg.dindijari.client.render.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * A themed checkbox: rounded flat box that fills with the accent colour (dark
 * check mark) when checked, with the label to its right. State is read/written
 * through the supplied accessors so external changes stay in sync.
 */
public class ThemedCheckbox extends AbstractWidget {

    private final BooleanSupplier getter;
    private final Consumer<Boolean> setter;
    private final Component label;

    /**
     * Creates a checkbox.
     *
     * @param x      left x in GUI units
     * @param y      top y in GUI units
     * @param width  clickable width (box + label)
     * @param height row height
     * @param text   the label text
     * @param getter reads the state
     * @param setter writes the new state on click
     */
    public ThemedCheckbox(int x, int y, int width, int height, String text,
                          BooleanSupplier getter, Consumer<Boolean> setter) {
        super(x, y, width, height, Fonts.ui(text));
        this.label = Fonts.ui(text);
        this.getter = getter;
        this.setter = setter;
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        boolean checked = getter.getAsBoolean();
        float box = Theme.px(18);
        float by = getY() + (this.height - box) / 2;
        int fill = checked ? Theme.accent()
                : (isHoveredOrFocused() ? Theme.BUTTON_HOVER : Theme.BUTTON);
        Render2D.fillRounded(g, getX(), by, box, box, Theme.px(4), fill);
        if (!checked) {
            Render2D.outlineRounded(g, getX(), by, box, box, Theme.px(4), 1.0F,
                    ColorUtil.withAlpha(Theme.TEXT_SECONDARY, 120));
        } else {
            // Check mark: two dark strokes.
            float cx = getX() + box / 2;
            float cy = by + box / 2;
            Render2D.fillTriangle(g,
                    cx - Theme.px(5), cy + Theme.px(0.5F),
                    cx - Theme.px(1.5F), cy + Theme.px(4),
                    cx - Theme.px(1.5F), cy + Theme.px(1), 0xFF0E0E10);
            Render2D.fillTriangle(g,
                    cx - Theme.px(1.5F), cy + Theme.px(4),
                    cx + Theme.px(5), cy - Theme.px(4),
                    cx - Theme.px(1.5F), cy + Theme.px(1), 0xFF0E0E10);
        }
        Fonts.drawScaled(g, label, getX() + box + Theme.px(8),
                getY() + (this.height - 9) / 2.0F + 0.5F, 0.95F, Theme.TEXT_SECONDARY, false);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        setter.accept(!getter.getAsBoolean());
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narration) {
        defaultButtonNarrationText(narration);
    }
}
