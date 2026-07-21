package gg.dindijari.client.gui.clickgui;

import gg.dindijari.client.gui.widget.ScrollPanel;
import gg.dindijari.client.gui.widget.ThemedTextField;
import gg.dindijari.client.module.Module;
import gg.dindijari.client.module.modules.BrandingModule;
import gg.dindijari.client.render.BrandingRenderer;
import gg.dindijari.client.render.ColorUtil;
import gg.dindijari.client.render.Fonts;
import gg.dindijari.client.render.Icons;
import gg.dindijari.client.render.Render2D;
import gg.dindijari.client.render.Theme;
import gg.dindijari.client.setting.KeybindSetting;
import gg.dindijari.client.setting.Setting;
import gg.dindijari.client.setting.StringSetting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * The settings surface for a single module, shown as a centred, scrollable
 * overlay panel in the {@link ClickGuiScreen}. Non-text settings (toggles,
 * sliders, colour pickers, enum cyclers, keybind capture) render via the shared
 * {@link SettingRows}; free-text {@link StringSetting}s get a themed text field.
 * The {@link BrandingModule} additionally shows a live preview at the top.
 *
 * <p>Content that exceeds the panel scrolls (mouse wheel) inside a
 * {@link ScrollPanel} with a custom scrollbar and is clipped to the panel. The
 * overlay renders and focus-manages its own text fields (rather than screen
 * widgets) so they draw above the panel and are clipped by the scroll region.
 */
final class ModuleSettingsView {

    private final Module module;
    private final SettingRows rows = new SettingRows();
    private final ScrollPanel scroll = new ScrollPanel();
    private final List<Setting<?>> nonText = new ArrayList<>();
    private final List<StringSetting> textSettings = new ArrayList<>();
    private final List<ThemedTextField> textFields = new ArrayList<>();
    private final Component title;
    private final Component description;
    private final BrandingRenderer preview;

    private int focusedField = -1;

    private float panelX;
    private float panelY;
    private float panelW;
    private float panelH;
    private float headerH;

    ModuleSettingsView(Module module) {
        this.module = module;
        this.title = Fonts.ui(module.getName());
        this.description = Fonts.ui(module.getDescription());
        this.preview = module instanceof BrandingModule bm ? new BrandingRenderer(bm) : null;

        for (Setting<?> setting : module.getSettings()) {
            if (!module.isToggleable() && setting instanceof KeybindSetting) {
                continue; // settings-only modules have no meaningful toggle key
            }
            if (setting instanceof StringSetting str) {
                textSettings.add(str);
                ThemedTextField field = new ThemedTextField(0, 0, 10,
                        Math.round(Theme.px(22)), str.getName() + "...", str::set);
                field.setValue(str.get());
                textFields.add(field);
            } else {
                nonText.add(setting);
            }
        }
        scroll.reset();
    }

    private float previewH() {
        return preview != null ? Theme.px(70) : 0;
    }

    private float textRowH() {
        return Theme.px(34);
    }

    private float contentHeight() {
        return previewH() + textSettings.size() * textRowH() + rows.totalHeight(nonText);
    }

    /**
     * Lays out and renders the overlay centred in the given screen bounds.
     */
    void render(GuiGraphics g, int screenW, int screenH, int mouseX, int mouseY) {
        float pad = Theme.px(Theme.PANEL_PADDING);
        panelW = Math.min(Theme.px(360), screenW - Theme.px(64));
        headerH = Theme.px(44);
        float wanted = headerH + contentHeight() + pad;
        panelH = Math.min(wanted, screenH - Theme.px(80));
        panelX = (screenW - panelW) / 2;
        panelY = (screenH - panelH) / 2;

        Render2D.dropShadow(g, panelX, panelY, panelW, panelH, Theme.px(10), Theme.px(14), Theme.SHADOW);
        Render2D.fillRounded(g, panelX, panelY, panelW, panelH, Theme.px(10), Theme.PANEL);
        Render2D.rgbLine(g, panelX + Theme.px(10), panelY, panelW - 2 * Theme.px(10), Theme.px(3));

        float x = panelX + pad;
        float textW = panelW - 2 * pad - Theme.px(16);
        Fonts.draw(g, Fonts.fit(title, textW, 1.0F), x, panelY + Theme.px(12), Theme.TEXT_PRIMARY, false);
        boolean closeHover = mouseX >= panelX + panelW - Theme.px(28) && mouseX <= panelX + panelW
                && mouseY >= panelY && mouseY <= panelY + Theme.px(30);
        Icons.close(g, panelX + panelW - Theme.px(16), panelY + Theme.px(16), Theme.px(12),
                closeHover ? Theme.TEXT_PRIMARY : Theme.TEXT_SECONDARY);
        Fonts.drawScaled(g, Fonts.fit(description, panelW - 2 * pad, 0.8F), x, panelY + Theme.px(28),
                0.8F, Theme.TEXT_SECONDARY, false);

        // Scrollable content region below the header.
        float vpX = panelX + pad;
        float vpY = panelY + headerH;
        float vpW = panelW - 2 * pad;
        float vpH = panelH - headerH - pad / 2;
        float top = scroll.begin(g, vpX, vpY, vpW, vpH, contentHeight());

        float cy = top;
        float w = vpW - Theme.px(8); // leave room for the scrollbar
        if (preview != null) {
            Render2D.fillRounded(g, vpX, cy, w, previewH() - Theme.px(6), Theme.px(6),
                    ColorUtil.withAlpha(0xFF0E0E10, 220));
            g.pose().pushPose();
            g.pose().translate(vpX + w / 2, cy + Theme.px(10), 0);
            g.pose().scale(0.5F, 0.5F, 1.0F);
            preview.render(g, 0, 0);
            g.pose().popPose();
            cy += previewH();
        }
        for (int i = 0; i < textSettings.size(); i++) {
            Fonts.drawScaled(g, Fonts.ui(textSettings.get(i).getName()), vpX, cy + Theme.px(1),
                    0.8F, Theme.TEXT_SECONDARY, false);
            ThemedTextField field = textFields.get(i);
            field.setX(Math.round(vpX));
            field.setY(Math.round(cy + Theme.px(11)));
            field.setWidth(Math.round(w));
            field.setFocused(i == focusedField);
            field.render(g, mouseX, mouseY, 0.0F);
            cy += textRowH();
        }
        rows.render(g, vpX, cy, w, nonText);

        scroll.end(g);
    }

    // ------------------------------------------------------------------
    // Input
    // ------------------------------------------------------------------

    boolean isCloseClick(double mx, double my) {
        return mx >= panelX + panelW - Theme.px(28) && mx <= panelX + panelW
                && my >= panelY && my <= panelY + Theme.px(30);
    }

    boolean isOutside(double mx, double my) {
        return mx < panelX || mx > panelX + panelW || my < panelY || my > panelY + panelH;
    }

    boolean mouseClicked(double mx, double my, int button) {
        float pad = Theme.px(Theme.PANEL_PADDING);
        float vpX = panelX + pad;
        float vpY = panelY + headerH;
        float vpW = panelW - 2 * pad;
        float vpH = panelH - headerH - pad / 2;
        if (mx < vpX || mx > vpX + vpW || my < vpY || my > vpY + vpH) {
            focusedField = -1;
            return false;
        }
        float top = vpY - scroll.scroll();
        float cy = top + previewH();
        float w = vpW - Theme.px(8);

        // Text fields first.
        for (int i = 0; i < textSettings.size(); i++) {
            float fy = cy + Theme.px(11);
            if (my >= fy && my <= fy + Theme.px(22) && mx >= vpX && mx <= vpX + w) {
                focusedField = i;
                textFields.get(i).mouseClicked(mx, my, button);
                return true;
            }
            cy += textRowH();
        }
        focusedField = -1;
        // Setting rows.
        return rows.mouseClicked(mx, my, vpX, cy, w, nonText, button);
    }

    boolean charTyped(char c, int modifiers) {
        if (focusedField >= 0) {
            return textFields.get(focusedField).charTyped(c, modifiers);
        }
        return false;
    }

    boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (rows.isListening()) {
            return rows.keyPressed(keyCode);
        }
        if (focusedField >= 0) {
            return textFields.get(focusedField).keyPressed(keyCode, scanCode, modifiers);
        }
        return false;
    }

    boolean isListening() {
        return rows.isListening();
    }

    boolean mouseDragged(double mx) {
        return rows.mouseDragged(mx);
    }

    void mouseReleased() {
        rows.mouseReleased();
    }

    boolean mouseScrolled(double mx, double my, double delta) {
        return scroll.mouseScrolled(mx, my, delta);
    }
}
