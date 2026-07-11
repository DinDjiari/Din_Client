package gg.dindijari.client.gui.clickgui;

import gg.dindijari.client.gui.widget.ThemedTextField;
import gg.dindijari.client.module.Module;
import gg.dindijari.client.module.modules.BrandingModule;
import gg.dindijari.client.render.BrandingRenderer;
import gg.dindijari.client.render.ColorUtil;
import gg.dindijari.client.render.Fonts;
import gg.dindijari.client.render.Render2D;
import gg.dindijari.client.render.Theme;
import gg.dindijari.client.setting.Setting;
import gg.dindijari.client.setting.StringSetting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The settings surface for a single module, shown as a centred overlay panel in
 * the {@link ClickGuiScreen}. Non-text settings (toggles, sliders, colour
 * pickers, enum cyclers, keybind capture) are rendered by the shared
 * {@link SettingRows}; free-text {@link StringSetting}s get a themed text field.
 * When the module is the {@link BrandingModule}, a live preview of the
 * main-menu branding is drawn at the top.
 *
 * <p>The text-field widgets are owned by the parent screen (for focus and
 * keyboard routing); this view positions them and toggles their visibility.
 */
final class ModuleSettingsView {

    private final Module module;
    private final SettingRows rows = new SettingRows();
    private final List<Setting<?>> nonText = new ArrayList<>();
    private final List<StringSetting> textSettings = new ArrayList<>();
    private final List<ThemedTextField> textFields = new ArrayList<>();
    private final Component title;
    private final Component description;
    private final BrandingRenderer preview;

    private float panelX;
    private float panelY;
    private float panelW;
    private float panelH;

    ModuleSettingsView(Module module, Consumer<ThemedTextField> registerField) {
        this.module = module;
        this.title = Fonts.ui(module.getName());
        this.description = Fonts.ui(module.getDescription());
        this.preview = module instanceof BrandingModule bm ? new BrandingRenderer(bm) : null;

        for (Setting<?> setting : module.getSettings()) {
            // Settings-only modules have no meaningful toggle key; hide it.
            if (!module.isToggleable()
                    && setting instanceof gg.dindijari.client.setting.KeybindSetting) {
                continue;
            }
            if (setting instanceof StringSetting str) {
                textSettings.add(str);
                ThemedTextField field = new ThemedTextField(0, 0, 10, 10, str.getName() + "...",
                        str::set);
                field.setValue(str.get());
                textFields.add(field);
                registerField.accept(field);
            } else {
                nonText.add(setting);
            }
        }
    }

    /** @return the module this view edits */
    Module module() {
        return module;
    }

    /** @return the text-field widgets (owned by the screen) */
    List<ThemedTextField> textFields() {
        return textFields;
    }

    private float previewH() {
        return preview != null ? Theme.px(70) : 0;
    }

    private float textRowH() {
        return Theme.px(34);
    }

    /**
     * Lays out and renders the overlay centred in the given screen bounds.
     */
    void render(GuiGraphics g, int screenW, int screenH, int mouseX, int mouseY) {
        float pad = Theme.px(Theme.PANEL_PADDING);
        panelW = Math.min(Theme.px(360), screenW - Theme.px(64));
        float header = Theme.px(44);
        float content = previewH() + textSettings.size() * textRowH() + rows.totalHeight(nonText);
        panelH = Math.min(header + content + pad, screenH - Theme.px(80));
        panelX = (screenW - panelW) / 2;
        panelY = (screenH - panelH) / 2;

        Render2D.dropShadow(g, panelX, panelY, panelW, panelH, Theme.px(10), Theme.px(14), Theme.SHADOW);
        Render2D.fillRounded(g, panelX, panelY, panelW, panelH, Theme.px(10), Theme.PANEL);
        Render2D.rgbLine(g, panelX + Theme.px(10), panelY, panelW - 2 * Theme.px(10), Theme.px(3));

        float x = panelX + pad;
        Fonts.draw(g, title, x, panelY + Theme.px(12), Theme.TEXT_PRIMARY, false);
        gg.dindijari.client.render.Icons.close(g, panelX + panelW - Theme.px(16), panelY + Theme.px(16),
                Theme.px(14), Theme.TEXT_SECONDARY);
        Fonts.drawScaled(g, description, x, panelY + Theme.px(28), 0.8F, Theme.TEXT_SECONDARY, false);

        float cy = panelY + header;
        float w = panelW - 2 * pad;

        if (preview != null) {
            Render2D.fillRounded(g, x, cy, w, previewH() - Theme.px(6), Theme.px(6),
                    ColorUtil.withAlpha(0xFF0E0E10, 220));
            g.pose().pushPose();
            g.pose().translate(x + w / 2, cy + Theme.px(10), 0);
            g.pose().scale(0.5F, 0.5F, 1.0F);
            preview.render(g, 0, 0);
            g.pose().popPose();
            cy += previewH();
        }

        for (int i = 0; i < textSettings.size(); i++) {
            Fonts.drawScaled(g, Fonts.ui(textSettings.get(i).getName()), x, cy + Theme.px(1),
                    0.8F, Theme.TEXT_SECONDARY, false);
            ThemedTextField field = textFields.get(i);
            field.setX(Math.round(x));
            field.setY(Math.round(cy + Theme.px(11)));
            field.setWidth(Math.round(w));
            field.visible = true;
            cy += textRowH();
        }

        rows.render(g, x, cy, w, nonText);
    }

    boolean mouseClicked(double mx, double my, int button) {
        // Close (X) hit area.
        if (mx >= panelX + panelW - Theme.px(28) && mx <= panelX + panelW
                && my >= panelY && my <= panelY + Theme.px(30)) {
            return false; // signal close to the caller via isCloseClick
        }
        float pad = Theme.px(Theme.PANEL_PADDING);
        float x = panelX + pad;
        float w = panelW - 2 * pad;
        float cy = panelY + Theme.px(44) + previewH() + textSettings.size() * textRowH();
        return rows.mouseClicked(mx, my, x, cy, w, nonText, button);
    }

    boolean isCloseClick(double mx, double my) {
        return mx >= panelX + panelW - Theme.px(28) && mx <= panelX + panelW
                && my >= panelY && my <= panelY + Theme.px(30);
    }

    boolean isOutside(double mx, double my) {
        return mx < panelX || mx > panelX + panelW || my < panelY || my > panelY + panelH;
    }

    boolean mouseDragged(double mx) {
        return rows.mouseDragged(mx);
    }

    void mouseReleased() {
        rows.mouseReleased();
    }

    boolean keyPressed(int keyCode) {
        return rows.keyPressed(keyCode);
    }

    boolean isListening() {
        return rows.isListening();
    }
}
