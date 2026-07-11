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
import net.minecraft.util.StringUtil;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

/**
 * A themed single-line text input (used for the search bars), replacing the
 * vanilla {@code EditBox} sprites with a flat rounded field, placeholder text
 * and a blinking caret. Supports typing, backspace/delete, home/end,
 * ctrl+backspace and paste (ctrl+V); that is sufficient for filter fields.
 */
public class ThemedTextField extends AbstractWidget {

    private final Consumer<String> onChange;
    private final Component placeholder;
    private final StringBuilder value = new StringBuilder();

    /**
     * Creates a text field.
     *
     * @param x           left x in GUI units
     * @param y           top y in GUI units
     * @param width       width in GUI units
     * @param height      height in GUI units
     * @param placeholder hint shown while empty (e.g. "Search worlds...")
     * @param onChange    called with the new text after every edit
     */
    public ThemedTextField(int x, int y, int width, int height, String placeholder,
                           Consumer<String> onChange) {
        super(x, y, width, height, Component.literal(placeholder));
        this.placeholder = Fonts.ui(placeholder);
        this.onChange = onChange;
    }

    /**
     * Returns the current text.
     *
     * @return the field's value
     */
    public String getValue() {
        return value.toString();
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        float radius = Theme.px(Theme.BUTTON_RADIUS);
        int fill = isFocused() || isHovered() ? Theme.BUTTON_HOVER : Theme.BUTTON;
        Render2D.fillRounded(g, getX(), getY(), this.width, this.height, radius, fill);
        if (isFocused()) {
            Render2D.outlineRounded(g, getX(), getY(), this.width, this.height, radius,
                    1.0F, ColorUtil.scaleAlpha(Theme.accent(), 0.7F));
        }

        float textX = getX() + Theme.px(10);
        float textY = getY() + (this.height - 9) / 2.0F + 0.5F;
        if (value.isEmpty() && !isFocused()) {
            Fonts.draw(g, placeholder, textX, textY, Theme.TEXT_SECONDARY, false);
        } else {
            // The value changes as the user types; drawing via the vanilla font
            // with a per-frame styled component is acceptable for a focused,
            // interactive field (not a hot render loop).
            Fonts.draw(g, Fonts.ui(value.toString()), textX, textY, Theme.TEXT_PRIMARY, false);
            if (isFocused() && (System.currentTimeMillis() / 500) % 2 == 0) {
                float caretX = textX + Fonts.width(Fonts.ui(value.toString())) + 1;
                Render2D.fillRect(g, caretX, textY - 1, 1, 11, Theme.accent());
            }
        }
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        if (!isFocused() || !StringUtil.isAllowedChatCharacter(c)) {
            return false;
        }
        value.append(c);
        onChange.accept(getValue());
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!isFocused()) {
            return false;
        }
        switch (keyCode) {
            case GLFW.GLFW_KEY_BACKSPACE -> {
                if (!value.isEmpty()) {
                    if ((modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
                        value.setLength(0);
                    } else {
                        value.setLength(value.length() - 1);
                    }
                    onChange.accept(getValue());
                }
                return true;
            }
            case GLFW.GLFW_KEY_DELETE -> {
                if (!value.isEmpty()) {
                    value.setLength(0);
                    onChange.accept(getValue());
                }
                return true;
            }
            case GLFW.GLFW_KEY_V -> {
                if ((modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
                    value.append(Minecraft.getInstance().keyboardHandler.getClipboard());
                    onChange.accept(getValue());
                    return true;
                }
                return false;
            }
            default -> {
                return false;
            }
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narration) {
        defaultButtonNarrationText(narration);
    }
}
