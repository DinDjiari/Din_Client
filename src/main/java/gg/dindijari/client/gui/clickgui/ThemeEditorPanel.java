package gg.dindijari.client.gui.clickgui;

import gg.dindijari.client.core.Services;
import gg.dindijari.client.render.ColorUtil;
import gg.dindijari.client.render.Fonts;
import gg.dindijari.client.render.Render2D;
import gg.dindijari.client.render.Theme;
import gg.dindijari.client.module.modules.ThemeModule;
import gg.dindijari.client.setting.NumberSetting;
import gg.dindijari.client.setting.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * The Theme Editor panel of the Click GUI, per the design reference: sliders
 * for RGB Speed, Blur Intensity, UI Scale and Animation Speed, accent colour
 * swatches with an RGB-cycle toggle, a font selector, and a
 * <em>Save Theme Preset</em> primary button.
 *
 * <p>Everything edits live state: the theme sliders/swatches drive the
 * {@link ThemeModule} (persisted through the config layer), Blur Intensity
 * writes the vanilla "Menu Background Blurriness" accessibility option, and
 * the save button flushes the active config profile to disk immediately.
 */
public final class ThemeEditorPanel extends Panel {

    private final SettingRows rows = new SettingRows();
    private final List<Setting<?>> settings = new ArrayList<>();
    private final NumberSetting blur;
    private final Component saveLabel = Fonts.ui("Save Theme Preset");
    private final Component savedLabel = Fonts.ui("Saved");

    private long savedFlashUntil;

    /**
     * Creates the theme editor.
     *
     * @param theme the live theme module
     * @param x     initial left edge
     * @param y     initial top edge
     */
    public ThemeEditorPanel(ThemeModule theme, float x, float y) {
        super("Theme Editor", x, y);

        // Bridge to the vanilla blur option: vanilla owns the value and its
        // persistence; this setting only mirrors and writes it.
        Minecraft minecraft = Minecraft.getInstance();
        blur = new NumberSetting("Blur Intensity",
                "Menu background blur (vanilla accessibility option).",
                minecraft.options.menuBackgroundBlurriness().get(), 0, 10, 1);
        blur.onChange(v -> minecraft.options.menuBackgroundBlurriness().set((int) Math.round(v)));

        settings.add(theme.rgbSpeed());
        settings.add(blur);
        settings.add(theme.uiScale());
        settings.add(theme.animationSpeed());
        settings.add(theme.accent());
        settings.add(theme.font());
    }

    @Override
    public float width() {
        return Theme.px(232);
    }

    private float buttonHeight() {
        return Theme.px(30);
    }

    @Override
    protected float bodyHeight() {
        return rows.totalHeight(settings) + buttonHeight() + Theme.px(10);
    }

    @Override
    protected void renderBody(GuiGraphics g, float bx, float by, int mouseX, int mouseY) {
        float w = width() - Theme.px(20);
        rows.render(g, bx, by, w, settings);

        float btnY = by + rows.totalHeight(settings) + Theme.px(6);
        boolean hover = mouseX >= bx && mouseX <= bx + w && mouseY >= btnY && mouseY <= btnY + buttonHeight();
        int fill = ColorUtil.lerp(Theme.accent(), 0xFFFFFFFF, hover ? 0.2F : 0.0F);
        Render2D.fillRounded(g, bx, btnY, w, buttonHeight(), Theme.px(Theme.BUTTON_RADIUS), fill);
        boolean flash = System.currentTimeMillis() < savedFlashUntil;
        Fonts.drawCentered(g, flash ? savedLabel : saveLabel, bx + w / 2,
                btnY + (buttonHeight() - 9) / 2 + 0.5F, 1.0F, 0xFF0E0E10, false);
    }

    @Override
    protected boolean bodyClicked(double mx, double my, int button) {
        float bx = getX() + Theme.px(10);
        float w = width() - Theme.px(20);
        float by = getY() + headerHeight();

        if (rows.mouseClicked(mx, my, bx, by, w, settings, button)) {
            return true;
        }
        float btnY = by + rows.totalHeight(settings) + Theme.px(6);
        if (mx >= bx && mx <= bx + w && my >= btnY && my <= btnY + buttonHeight()) {
            Services.config().saveNow();
            savedFlashUntil = System.currentTimeMillis() + 1200;
            return true;
        }
        return false;
    }

    /**
     * Continues an in-progress slider drag inside this panel.
     *
     * @param mx mouse x
     * @return {@code true} while consumed
     */
    public boolean settingsDragged(double mx) {
        return rows.mouseDragged(mx);
    }

    /**
     * Ends slider drags.
     */
    public void settingsReleased() {
        rows.mouseReleased();
    }
}
