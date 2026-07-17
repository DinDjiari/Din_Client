package gg.dindijari.client.gui.clickgui;

import gg.dindijari.client.branding.WindowBranding;
import gg.dindijari.client.core.ClientSounds;
import gg.dindijari.client.gui.screen.DindijariFilePickerScreen;
import gg.dindijari.client.module.modules.client.BrandingModule;
import gg.dindijari.client.render.ColorUtil;
import gg.dindijari.client.render.Fonts;
import gg.dindijari.client.render.Render2D;
import gg.dindijari.client.render.Theme;
import gg.dindijari.client.setting.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * The Branding panel of the Click GUI: the window-title setting (edited via
 * the themed text dialog) plus the window-icon section — a live preview of the
 * converted 32&times;32 icon, a <em>Choose Icon…</em> button opening the
 * client's file picker, and a <em>Remove</em> button restoring the vanilla
 * icon. Non-square images are center-cropped by the converter; the hint says
 * so.
 */
public final class BrandingPanel extends Panel {

    private final BrandingModule module;
    private final SettingRows rows = new SettingRows();
    private final List<Setting<?>> settings;
    private final Component iconLabel = Fonts.ui("Window Icon");
    private final Component iconHint = Fonts.ui("Center-crops non-square images");
    private final Component chooseLabel = Fonts.ui("Choose Icon…");
    private final Component removeLabel = Fonts.ui("Remove");
    private final Component noIcon = Fonts.ui("none");

    /**
     * Creates the branding panel.
     *
     * @param module the branding module
     * @param x      initial left edge
     * @param y      initial top edge
     */
    public BrandingPanel(BrandingModule module, float x, float y) {
        super("Branding", x, y);
        this.module = module;
        this.settings = List.of(module.windowTitle());
    }

    private float previewSize() {
        return Theme.px(32);
    }

    private float buttonHeight() {
        return Theme.px(26);
    }

    @Override
    protected float bodyHeight() {
        return rows.totalHeight(settings)
                + Theme.px(16)                       // icon label
                + previewSize() + Theme.px(8)        // preview + gap
                + buttonHeight()                     // buttons
                + Theme.px(18);                      // hint line
    }

    @Override
    protected void renderBody(GuiGraphics g, float bx, float by, int mouseX, int mouseY) {
        float w = width() - Theme.px(20);
        rows.render(g, bx, by, w, settings);

        float cy = by + rows.totalHeight(settings);
        Fonts.drawScaled(g, iconLabel, bx, cy + Theme.px(4), 0.8F, Theme.TEXT_SECONDARY, false);
        cy += Theme.px(16);

        // Preview: the converted icon, or a placeholder well.
        float ps = previewSize();
        Render2D.fillRounded(g, bx, cy, ps, ps, Theme.px(4), Theme.BUTTON);
        ResourceLocation preview = WindowBranding.previewTexture();
        if (preview != null) {
            int ix = Math.round(bx + Theme.px(2));
            int iy = Math.round(cy + Theme.px(2));
            int size = Math.round(ps - Theme.px(4));
            g.blit(preview, ix, iy, size, size, 0.0F, 0.0F, 32, 32, 32, 32);
        } else {
            Fonts.drawCentered(g, noIcon, bx + ps / 2, cy + ps / 2 - 4, 0.8F,
                    Theme.TEXT_SECONDARY, false);
        }

        // Buttons to the right of the preview.
        float btnX = bx + ps + Theme.px(10);
        float btnW = w - ps - Theme.px(10);
        float chooseY = cy;
        boolean chooseHover = mouseX >= btnX && mouseX <= btnX + btnW
                && mouseY >= chooseY && mouseY <= chooseY + buttonHeight();
        Render2D.fillRounded(g, btnX, chooseY, btnW, buttonHeight(), Theme.px(Theme.BUTTON_RADIUS),
                ColorUtil.lerp(Theme.accent(), 0xFFFFFFFF, chooseHover ? 0.2F : 0.0F));
        Fonts.drawCentered(g, chooseLabel, btnX + btnW / 2,
                chooseY + (buttonHeight() - 9) / 2 + 0.5F, 0.9F, 0xFF0E0E10, false);

        float removeY = cy + ps + Theme.px(8);
        boolean removeActive = WindowBranding.hasCustomIcon();
        boolean removeHover = removeActive && mouseX >= bx && mouseX <= bx + w
                && mouseY >= removeY && mouseY <= removeY + buttonHeight();
        int removeFill = removeHover ? Theme.BUTTON_HOVER : Theme.BUTTON;
        if (!removeActive) {
            removeFill = ColorUtil.scaleAlpha(removeFill, 0.5F);
        }
        Render2D.fillRounded(g, bx, removeY, w, buttonHeight(), Theme.px(Theme.BUTTON_RADIUS),
                removeFill);
        Fonts.drawCentered(g, removeLabel, bx + w / 2, removeY + (buttonHeight() - 9) / 2 + 0.5F,
                0.9F, removeActive ? Theme.TEXT_PRIMARY : Theme.TEXT_SECONDARY, false);

        Fonts.drawScaled(g, iconHint, bx, removeY + buttonHeight() + Theme.px(6), 0.7F,
                Theme.TEXT_SECONDARY, false);
    }

    @Override
    protected boolean bodyClicked(double mx, double my, int button) {
        float bx = getX() + Theme.px(10);
        float w = width() - Theme.px(20);
        float by = getY() + headerHeight();

        if (rows.mouseClicked(mx, my, bx, by, w, settings, button)) {
            return true;
        }

        float cy = by + rows.totalHeight(settings) + Theme.px(16);
        float ps = previewSize();
        float btnX = bx + ps + Theme.px(10);
        float btnW = w - ps - Theme.px(10);
        if (mx >= btnX && mx <= btnX + btnW && my >= cy && my <= cy + buttonHeight()) {
            ClientSounds.click();
            Minecraft minecraft = Minecraft.getInstance();
            minecraft.setScreen(new DindijariFilePickerScreen(minecraft.screen,
                    "Choose Window Icon", WindowBranding::setCustomIcon));
            return true;
        }
        float removeY = cy + ps + Theme.px(8);
        if (mx >= bx && mx <= bx + w && my >= removeY && my <= removeY + buttonHeight()) {
            if (WindowBranding.hasCustomIcon()) {
                ClientSounds.click();
                WindowBranding.removeCustomIcon();
            }
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
