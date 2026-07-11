package gg.dindijari.client.gui.clickgui;

import gg.dindijari.client.gui.widget.ThemedTextField;
import gg.dindijari.client.module.modules.BrandingModule;
import gg.dindijari.client.render.BrandingRenderer;
import gg.dindijari.client.render.ColorUtil;
import gg.dindijari.client.render.Fonts;
import gg.dindijari.client.render.Render2D;
import gg.dindijari.client.render.Theme;
import gg.dindijari.client.setting.Setting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * The Click GUI's Branding panel: full control over the main-menu title and
 * subtitle. Two collapsible sections (Title / Subtitle) expose the source
 * cycler — with a custom-text input that appears only while CUSTOM is
 * selected — and the always-applied styling rows (font, colour + RGB cycle,
 * size, letter spacing, uppercase, underline + underline colour). A live
 * preview at the bottom renders exactly what the main menu will show.
 *
 * <p>The two custom-text inputs are real {@link ThemedTextField} widgets owned
 * by the Click GUI screen (for focus and keyboard routing); this panel
 * positions them and reports when they should be visible.
 */
public final class BrandingPanel extends Panel {

    private final BrandingModule branding;
    private final BrandingRenderer preview;
    private final SettingRows rows = new SettingRows();
    private final Component titleHeader = Fonts.ui("Title");
    private final Component subtitleHeader = Fonts.ui("Subtitle");

    private boolean titleOpen = true;
    private boolean subtitleOpen;

    private final ThemedTextField titleField;
    private final ThemedTextField subtitleField;

    /**
     * Creates the panel.
     *
     * @param branding the branding module
     * @param x        initial left edge
     * @param y        initial top edge
     */
    public BrandingPanel(BrandingModule branding, float x, float y) {
        super("Branding", x, y);
        this.branding = branding;
        this.preview = new BrandingRenderer(branding);
        int fieldW = Math.round(width() - Theme.px(20));
        int fieldH = Math.round(Theme.px(24));
        titleField = new ThemedTextField(0, 0, fieldW, fieldH, "Custom title...",
                value -> branding.titleCustom().set(value));
        titleField.setValue(branding.titleCustom().get());
        subtitleField = new ThemedTextField(0, 0, fieldW, fieldH, "Custom subtitle...",
                value -> branding.subtitleCustom().set(value));
        subtitleField.setValue(branding.subtitleCustom().get());
    }

    /** @return the custom-title input widget (owned/rendered by the screen) */
    public ThemedTextField titleField() {
        return titleField;
    }

    /** @return the custom-subtitle input widget (owned/rendered by the screen) */
    public ThemedTextField subtitleField() {
        return subtitleField;
    }

    private List<Setting<?>> titleRows() {
        List<Setting<?>> list = new ArrayList<>();
        list.add(branding.titleSource());
        list.add(branding.titleFont());
        list.add(branding.titleColor());
        list.add(branding.titleSize());
        list.add(branding.titleSpacing());
        list.add(branding.titleUppercase());
        list.add(branding.titleUnderline());
        list.add(branding.titleUnderlineColor());
        return list;
    }

    private List<Setting<?>> subtitleRows() {
        List<Setting<?>> list = new ArrayList<>();
        list.add(branding.subtitleVisible());
        list.add(branding.subtitleSource());
        list.add(branding.subtitleFont());
        list.add(branding.subtitleColor());
        list.add(branding.subtitleSize());
        list.add(branding.subtitleSpacing());
        list.add(branding.subtitleUppercase());
        list.add(branding.subtitleUnderline());
        list.add(branding.subtitleUnderlineColor());
        return list;
    }

    private boolean titleFieldVisible() {
        return titleOpen && branding.titleSource().get() == BrandingModule.Source.CUSTOM;
    }

    private boolean subtitleFieldVisible() {
        return subtitleOpen && branding.subtitleSource().get() == BrandingModule.Source.CUSTOM;
    }

    private float sectionHeaderH() {
        return Theme.px(22);
    }

    private float fieldRowH() {
        return Theme.px(30);
    }

    private float previewH() {
        return Theme.px(80);
    }

    @Override
    public float width() {
        return Theme.px(250);
    }

    @Override
    protected float bodyHeight() {
        float h = sectionHeaderH();
        if (titleOpen) {
            if (titleFieldVisible()) {
                h += fieldRowH();
            }
            h += rows.totalHeight(titleRows());
        }
        h += sectionHeaderH();
        if (subtitleOpen) {
            if (subtitleFieldVisible()) {
                h += fieldRowH();
            }
            h += rows.totalHeight(subtitleRows());
        }
        return h + previewH() + Theme.px(6);
    }

    @Override
    protected void renderBody(GuiGraphics g, float bx, float by, int mouseX, int mouseY) {
        float w = width() - Theme.px(20);
        float cy = by;

        cy = renderSection(g, bx, cy, w, titleHeader, titleOpen);
        if (titleOpen) {
            if (titleFieldVisible()) {
                titleField.setX(Math.round(bx));
                titleField.setY(Math.round(cy + Theme.px(2)));
                cy += fieldRowH();
            }
            rows.render(g, bx, cy, w, titleRows());
            cy += rows.totalHeight(titleRows());
        }
        titleField.visible = titleFieldVisible();

        cy = renderSection(g, bx, cy, w, subtitleHeader, subtitleOpen);
        if (subtitleOpen) {
            if (subtitleFieldVisible()) {
                subtitleField.setX(Math.round(bx));
                subtitleField.setY(Math.round(cy + Theme.px(2)));
                cy += fieldRowH();
            }
            rows.render(g, bx, cy, w, subtitleRows());
            cy += rows.totalHeight(subtitleRows());
        }
        subtitleField.visible = subtitleFieldVisible();

        // Live preview: exactly what the main menu draws, scaled to fit.
        float ph = previewH();
        Render2D.fillRounded(g, bx, cy + Theme.px(2), w, ph - Theme.px(4), Theme.px(4),
                ColorUtil.withAlpha(0xFF0E0E10, 220));
        g.pose().pushPose();
        g.pose().translate(bx + w / 2, cy + Theme.px(12), 0);
        g.pose().scale(0.55F, 0.55F, 1.0F);
        preview.render(g, 0, 0);
        g.pose().popPose();
    }

    private float renderSection(GuiGraphics g, float bx, float cy, float w,
                                Component header, boolean open) {
        Fonts.drawScaled(g, header, bx + Theme.px(2), cy + Theme.px(6), 0.95F,
                Theme.accent(), false);
        Component marker = Fonts.ui(open ? "-" : "+");
        Fonts.draw(g, marker, bx + w - Theme.px(10), cy + Theme.px(6), Theme.TEXT_SECONDARY, false);
        return cy + sectionHeaderH();
    }

    @Override
    protected boolean bodyClicked(double mx, double my, int button) {
        float bx = getX() + Theme.px(10);
        float w = width() - Theme.px(20);
        float cy = getY() + headerHeight();

        if (my >= cy && my < cy + sectionHeaderH()) {
            titleOpen = !titleOpen;
            return true;
        }
        cy += sectionHeaderH();
        if (titleOpen) {
            if (titleFieldVisible()) {
                cy += fieldRowH(); // clicks on the field go to the widget itself
                if (my < cy && my >= cy - fieldRowH()) {
                    return false;
                }
            }
            float h = rows.totalHeight(titleRows());
            if (my >= cy && my < cy + h) {
                return rows.mouseClicked(mx, my, bx, cy, w, titleRows(), button);
            }
            cy += h;
        }
        if (my >= cy && my < cy + sectionHeaderH()) {
            subtitleOpen = !subtitleOpen;
            return true;
        }
        cy += sectionHeaderH();
        if (subtitleOpen) {
            if (subtitleFieldVisible()) {
                cy += fieldRowH();
                if (my < cy && my >= cy - fieldRowH()) {
                    return false;
                }
            }
            float h = rows.totalHeight(subtitleRows());
            if (my >= cy && my < cy + h) {
                return rows.mouseClicked(mx, my, bx, cy, w, subtitleRows(), button);
            }
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
