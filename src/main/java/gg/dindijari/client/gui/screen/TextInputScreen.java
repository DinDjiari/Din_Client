package gg.dindijari.client.gui.screen;

import gg.dindijari.client.gui.widget.ThemedButton;
import gg.dindijari.client.gui.widget.ThemedTextField;
import gg.dindijari.client.render.Fonts;
import gg.dindijari.client.render.Render2D;
import gg.dindijari.client.render.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

/**
 * A small themed dialog editing a single line of text (used for string
 * settings such as the window title or the Ollama endpoint): a centred panel
 * with an accent header, the client's own text field, and Save / Cancel
 * buttons. Enter saves, Esc cancels.
 */
public final class TextInputScreen extends ThemedScreen {

    private final Screen parentScreen;
    private final Component heading;
    private final Component hint;
    private final String initialValue;
    private final Consumer<String> onSave;

    private ThemedTextField field;

    /**
     * Creates the dialog.
     *
     * @param parent the screen to return to
     * @param title  the dialog heading (e.g. the setting name)
     * @param hint   a short secondary line under the heading; may be empty
     * @param value  the current value to edit
     * @param onSave receives the new value when Save is pressed
     */
    public TextInputScreen(Screen parent, String title, String hint,
                           String value, Consumer<String> onSave) {
        super(Component.literal(title));
        this.parentScreen = parent;
        this.heading = Fonts.ui(title);
        this.hint = Fonts.ui(hint == null ? "" : hint);
        this.initialValue = value;
        this.onSave = onSave;
    }

    @Override
    protected void init() {
        int fieldW = Math.round(Theme.px(312));
        int fieldH = Math.round(Theme.px(30));
        int x = (this.width - fieldW) / 2;
        int y = this.height / 2 - fieldH / 2;

        String previous = field == null ? initialValue : field.getValue();
        field = new ThemedTextField(x, y, fieldW, fieldH, "", s -> {
        });
        field.setValue(previous);
        addRenderableWidget(field);
        setFocused(field);

        int bw = Math.round(Theme.px(120));
        int bh = Math.round(Theme.px(30));
        int gap = Math.round(Theme.px(8));
        int by = y + fieldH + Math.round(Theme.px(16));
        addRenderableWidget(new ThemedButton(this.width / 2 - bw - gap / 2, by, bw, bh,
                "Save", true, this::save));
        addRenderableWidget(new ThemedButton(this.width / 2 + gap / 2, by, bw, bh,
                "Cancel", this::onClose));
    }

    private void save() {
        onSave.accept(field.getValue().trim());
        this.minecraft.setScreen(parentScreen);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);

        float panelW = Theme.px(360);
        float panelH = Theme.px(160);
        float px = (this.width - panelW) / 2.0F;
        float py = this.height / 2.0F - panelH / 2.0F - Theme.px(10);
        Render2D.dropShadow(g, px, py, panelW, panelH, Theme.px(Theme.PANEL_RADIUS),
                Theme.px(12), Theme.SHADOW);
        Render2D.fillRounded(g, px, py, panelW, panelH, Theme.px(Theme.PANEL_RADIUS), Theme.PANEL);
        Render2D.rgbLine(g, px + Theme.px(Theme.PANEL_RADIUS), py,
                panelW - 2 * Theme.px(Theme.PANEL_RADIUS), Theme.px(3));

        Fonts.drawCentered(g, heading, this.width / 2.0F, py + Theme.px(18), 1.0F,
                Theme.TEXT_PRIMARY, false);
        if (!hint.getString().isEmpty()) {
            Fonts.drawCentered(g, hint, this.width / 2.0F, py + Theme.px(36), 0.85F,
                    Theme.TEXT_SECONDARY, false);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            save();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parentScreen);
    }
}
