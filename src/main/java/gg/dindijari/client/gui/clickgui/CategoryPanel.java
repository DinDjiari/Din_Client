package gg.dindijari.client.gui.clickgui;

import gg.dindijari.client.core.Services;
import gg.dindijari.client.module.Category;
import gg.dindijari.client.module.Module;
import gg.dindijari.client.render.ColorUtil;
import gg.dindijari.client.render.Fonts;
import gg.dindijari.client.render.Render2D;
import gg.dindijari.client.render.Theme;
import gg.dindijari.client.setting.KeybindSetting;
import gg.dindijari.client.setting.Setting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * A Click GUI panel listing one {@link Category}'s modules: hover-lightened
 * rows with the module name and an accent mini-toggle, expanding on click to
 * that module's typed setting rows (see {@link SettingRows}).
 *
 * <p>Settings-only modules (e.g. Theme) render without a toggle. Categories
 * with no modules yet state that honestly instead of showing fake entries.
 */
public final class CategoryPanel extends Panel {

    /** Modules whose settings are expanded; session-persistent. */
    private static final Set<String> EXPANDED = new HashSet<>();

    private final Category category;
    private final SettingRows rows = new SettingRows();
    private final Map<Module, Component> names = new HashMap<>();
    private final Map<Module, List<Setting<?>>> visibleSettings = new HashMap<>();
    private final Component emptyLabel = Fonts.ui("No modules yet");

    private String filter = "";

    /**
     * Creates the panel for a category.
     *
     * @param category the category to list
     * @param x        initial left edge
     * @param y        initial top edge
     */
    public CategoryPanel(Category category, float x, float y) {
        super(category.getDisplayName(), x, y);
        this.category = category;
    }

    /**
     * Applies the search filter (case-insensitive module-name match).
     *
     * @param filter the query; empty shows everything
     */
    public void setFilter(String filter) {
        this.filter = filter.toLowerCase(Locale.ROOT);
    }

    private List<Module> modules() {
        List<Module> all = Services.modules().getByCategory(category);
        if (filter.isEmpty()) {
            return all;
        }
        List<Module> matching = new ArrayList<>(all.size());
        for (Module m : all) {
            if (m.getName().toLowerCase(Locale.ROOT).contains(filter)) {
                matching.add(m);
            }
        }
        return matching;
    }

    /** Settings shown when a module is expanded (keybind hidden for settings-only modules). */
    private List<Setting<?>> settingsOf(Module module) {
        return visibleSettings.computeIfAbsent(module, m -> {
            List<Setting<?>> list = new ArrayList<>(m.getSettings());
            if (!m.isToggleable()) {
                list.removeIf(s -> s instanceof KeybindSetting);
            }
            return list;
        });
    }

    private float moduleRowHeight() {
        return Theme.px(27);
    }

    @Override
    protected float bodyHeight() {
        List<Module> modules = modules();
        if (modules.isEmpty()) {
            return Theme.px(24);
        }
        float h = 0;
        for (Module m : modules) {
            h += moduleRowHeight();
            if (EXPANDED.contains(m.getName())) {
                h += rows.totalHeight(settingsOf(m)) + Theme.px(6);
            }
        }
        return h;
    }

    @Override
    protected void renderBody(GuiGraphics g, float bx, float by, int mouseX, int mouseY) {
        float w = width() - Theme.px(20);
        List<Module> modules = modules();
        if (modules.isEmpty()) {
            Fonts.drawScaled(g, emptyLabel, bx + Theme.px(4), by + Theme.px(4),
                    0.85F, Theme.TEXT_SECONDARY, false);
            return;
        }
        float cy = by;
        for (Module m : modules) {
            float rh = moduleRowHeight();
            boolean hover = mouseX >= bx && mouseX <= bx + w && mouseY >= cy && mouseY < cy + rh;
            if (hover) {
                Render2D.fillRounded(g, bx, cy, w, rh - Theme.px(2), Theme.px(4),
                        ColorUtil.withAlpha(Theme.BUTTON_HOVER, 200));
            }
            Component name = names.computeIfAbsent(m, k -> Fonts.ui(k.getName()));
            Fonts.drawScaled(g, name, bx + Theme.px(6), cy + Theme.px(9), 0.9F, Theme.TEXT_PRIMARY, false);
            if (m.isToggleable()) {
                SettingRows.drawToggle(g, bx + w - Theme.px(34), cy + Theme.px(6), m.isEnabled());
            }
            cy += rh;
            if (EXPANDED.contains(m.getName())) {
                float settingsH = rows.totalHeight(settingsOf(m));
                Render2D.fillRounded(g, bx, cy - Theme.px(2), w, settingsH + Theme.px(6),
                        Theme.px(4), ColorUtil.withAlpha(0xFF0E0E10, 160));
                rows.render(g, bx + Theme.px(8), cy + Theme.px(2), w - Theme.px(16), settingsOf(m));
                cy += settingsH + Theme.px(6);
            }
        }
    }

    @Override
    protected boolean bodyClicked(double mx, double my, int button) {
        float bx = getX() + Theme.px(10);
        float w = width() - Theme.px(20);
        float cy = getY() + headerHeight();
        for (Module m : modules()) {
            float rh = moduleRowHeight();
            if (my >= cy && my < cy + rh) {
                if (m.isToggleable() && mx >= bx + w - Theme.px(38)) {
                    m.toggle();
                } else if (!settingsOf(m).isEmpty()) {
                    toggleExpanded(m);
                }
                return true;
            }
            cy += rh;
            if (EXPANDED.contains(m.getName())) {
                float settingsH = rows.totalHeight(settingsOf(m));
                if (my >= cy && my < cy + settingsH + Theme.px(6)) {
                    return rows.mouseClicked(mx, my, bx + Theme.px(8), cy + Theme.px(2),
                            w - Theme.px(16), settingsOf(m), button);
                }
                cy += settingsH + Theme.px(6);
            }
        }
        return false;
    }

    private void toggleExpanded(Module m) {
        if (!EXPANDED.remove(m.getName())) {
            EXPANDED.add(m.getName());
        }
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

    /**
     * Routes key presses to a listening keybind chip.
     *
     * @param keyCode the pressed key
     * @return {@code true} if captured
     */
    public boolean keyPressed(int keyCode) {
        return rows.keyPressed(keyCode);
    }

    /** @return whether one of this panel's keybind chips awaits a key */
    public boolean isListening() {
        return rows.isListening();
    }
}
