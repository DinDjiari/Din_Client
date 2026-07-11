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
 * The client's single-line text input: rounded {@link Theme#BUTTON} field with
 * an accent border while focused, a blinking caret, full caret movement
 * (arrows / Home / End, click-to-position), shift/ctrl+A text selection with an
 * accent-tinted highlight, and clipboard cut/copy/paste. Reused by every place
 * the client needs text input (search bars, world name/seed, custom branding
 * text).
 */
public class ThemedTextField extends AbstractWidget {

    private final Consumer<String> onChange;
    private final Component placeholder;
    private final StringBuilder value = new StringBuilder();

    /** Caret position, in [0, length]. */
    private int caret;
    /** Selection anchor; equals {@link #caret} when nothing is selected. */
    private int anchor;

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

    /**
     * Replaces the field's text (without firing {@code onChange}); used to
     * restore state when a screen rebuilds its widgets.
     *
     * @param text the text to set
     */
    public void setValue(String text) {
        value.setLength(0);
        value.append(text);
        caret = value.length();
        anchor = caret;
    }

    // ------------------------------------------------------------------
    // Rendering
    // ------------------------------------------------------------------

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
            return;
        }

        // Selection highlight behind the text.
        if (hasSelection() && isFocused()) {
            float selStart = textX + widthOf(0, selectionStart());
            float selEnd = textX + widthOf(0, selectionEnd());
            Render2D.fillRounded(g, selStart, textY - 1.5F, selEnd - selStart, 12,
                    Theme.px(2), ColorUtil.withAlpha(Theme.accent(), 70));
        }

        Fonts.draw(g, Fonts.ui(value.toString()), textX, textY, Theme.TEXT_PRIMARY, false);

        if (isFocused() && (System.currentTimeMillis() / 500) % 2 == 0) {
            float caretX = textX + widthOf(0, caret);
            Render2D.fillRect(g, caretX, textY - 1.5F, 1, 12, Theme.accent());
        }
    }

    /** Rendered width of the substring [from, to). */
    private float widthOf(int from, int to) {
        if (to <= from) {
            return 0;
        }
        return Fonts.width(Fonts.ui(value.substring(from, to)));
    }

    // ------------------------------------------------------------------
    // Editing
    // ------------------------------------------------------------------

    private boolean hasSelection() {
        return caret != anchor;
    }

    private int selectionStart() {
        return Math.min(caret, anchor);
    }

    private int selectionEnd() {
        return Math.max(caret, anchor);
    }

    private void deleteSelection() {
        value.delete(selectionStart(), selectionEnd());
        caret = selectionStart();
        anchor = caret;
    }

    private void insert(String text) {
        if (hasSelection()) {
            deleteSelection();
        }
        StringBuilder clean = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (StringUtil.isAllowedChatCharacter(c)) {
                clean.append(c);
            }
        }
        value.insert(caret, clean);
        caret += clean.length();
        anchor = caret;
        onChange.accept(getValue());
    }

    private void moveCaret(int position, boolean extendSelection) {
        caret = Math.max(0, Math.min(value.length(), position));
        if (!extendSelection) {
            anchor = caret;
        }
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        if (!isFocused() || !StringUtil.isAllowedChatCharacter(c)) {
            return false;
        }
        insert(String.valueOf(c));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!isFocused()) {
            return false;
        }
        boolean shift = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;
        boolean ctrl = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
        Minecraft minecraft = Minecraft.getInstance();
        switch (keyCode) {
            case GLFW.GLFW_KEY_LEFT -> {
                moveCaret(caret - 1, shift);
                return true;
            }
            case GLFW.GLFW_KEY_RIGHT -> {
                moveCaret(caret + 1, shift);
                return true;
            }
            case GLFW.GLFW_KEY_HOME -> {
                moveCaret(0, shift);
                return true;
            }
            case GLFW.GLFW_KEY_END -> {
                moveCaret(value.length(), shift);
                return true;
            }
            case GLFW.GLFW_KEY_BACKSPACE -> {
                if (hasSelection()) {
                    deleteSelection();
                    onChange.accept(getValue());
                } else if (caret > 0) {
                    value.deleteCharAt(caret - 1);
                    moveCaret(caret - 1, false);
                    onChange.accept(getValue());
                }
                return true;
            }
            case GLFW.GLFW_KEY_DELETE -> {
                if (hasSelection()) {
                    deleteSelection();
                    onChange.accept(getValue());
                } else if (caret < value.length()) {
                    value.deleteCharAt(caret);
                    onChange.accept(getValue());
                }
                return true;
            }
            case GLFW.GLFW_KEY_A -> {
                if (ctrl) {
                    anchor = 0;
                    caret = value.length();
                    return true;
                }
                return false;
            }
            case GLFW.GLFW_KEY_C -> {
                if (ctrl && hasSelection()) {
                    minecraft.keyboardHandler.setClipboard(
                            value.substring(selectionStart(), selectionEnd()));
                    return true;
                }
                return false;
            }
            case GLFW.GLFW_KEY_X -> {
                if (ctrl && hasSelection()) {
                    minecraft.keyboardHandler.setClipboard(
                            value.substring(selectionStart(), selectionEnd()));
                    deleteSelection();
                    onChange.accept(getValue());
                    return true;
                }
                return false;
            }
            case GLFW.GLFW_KEY_V -> {
                if (ctrl) {
                    insert(minecraft.keyboardHandler.getClipboard());
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
    public void onClick(double mouseX, double mouseY) {
        // Place the caret at the clicked character boundary.
        float textX = getX() + Theme.px(10);
        int best = value.length();
        for (int i = 0; i <= value.length(); i++) {
            if (textX + widthOf(0, i) >= mouseX - 2) {
                best = i;
                break;
            }
        }
        moveCaret(best, false);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narration) {
        defaultButtonNarrationText(narration);
    }
}
