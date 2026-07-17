package gg.dindijari.client.gui.clickgui;

import gg.dindijari.client.core.ClientSounds;
import gg.dindijari.client.core.Services;
import gg.dindijari.client.module.Category;
import gg.dindijari.client.module.Module;
import gg.dindijari.client.render.ColorUtil;
import gg.dindijari.client.render.Fonts;
import gg.dindijari.client.render.Render2D;
import gg.dindijari.client.render.Theme;
import gg.dindijari.client.setting.KeybindSetting;
import gg.dindijari.client.setting.Setting;
import gg.dindijari.client.util.SodiumIntegration;
import gg.dindijari.client.util.animation.Animation;
import gg.dindijari.client.util.animation.Easing;
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

    /** Accent glow flashed over a module row when it is toggled. */
    private final Map<Module, Animation> toggleGlow = new HashMap<>();
    /** Smooth expansion state (0..1) of each module's settings section. */
    private final Map<String, Animation> expandAnim = new HashMap<>();

    // Sodium recommendation row (Performance panel only).
    private final Component sodiumLoaded = Fonts.ui("Sodium erkannt ✓");
    private final Component sodiumMissing = Fonts.ui("Sodium nicht installiert —");
    private final Component sodiumHint = Fonts.ui("empfohlen für beste FPS");
    private final Component sodiumLink = Fonts.ui("Modrinth öffnen ↗");

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

    /** Height of the Sodium status row (Performance panel only). */
    private float infoRowHeight() {
        if (category != Category.PERFORMANCE) {
            return 0;
        }
        return SodiumIntegration.isLoaded() ? Theme.px(20) : Theme.px(46);
    }

    /** Animated expansion progress (0..1) for a module's settings section. */
    private float expandProgress(Module m) {
        boolean expanded = EXPANDED.contains(m.getName());
        Animation anim = expandAnim.computeIfAbsent(m.getName(),
                k -> new Animation(expanded ? 1.0 : 0.0, 200, Easing.CUBIC_OUT));
        anim.animateTo(expanded ? 1.0 : 0.0);
        if (!Theme.animationsEnabled()) {
            anim.snapTo(expanded ? 1.0 : 0.0);
        }
        return anim.valueF();
    }

    /** Current (possibly mid-animation) height of a module's settings section. */
    private float expandedHeight(Module m) {
        float t = expandProgress(m);
        if (t <= 0.001F) {
            return 0;
        }
        return (rows.totalHeight(settingsOf(m)) + Theme.px(6)) * t;
    }

    @Override
    protected float bodyHeight() {
        List<Module> modules = modules();
        if (modules.isEmpty()) {
            return infoRowHeight() + Theme.px(24);
        }
        float h = infoRowHeight();
        for (Module m : modules) {
            h += moduleRowHeight();
            h += expandedHeight(m);
        }
        return h;
    }

    @Override
    protected void renderBody(GuiGraphics g, float bx, float by, int mouseX, int mouseY) {
        float w = width() - Theme.px(20);
        float cy = by + renderInfoRow(g, bx, by, w, mouseX, mouseY);
        List<Module> modules = modules();
        if (modules.isEmpty()) {
            Fonts.drawScaled(g, emptyLabel, bx + Theme.px(4), cy + Theme.px(4),
                    0.85F, Theme.TEXT_SECONDARY, false);
            return;
        }
        for (Module m : modules) {
            float rh = moduleRowHeight();
            boolean hover = mouseX >= bx && mouseX <= bx + w && mouseY >= cy && mouseY < cy + rh;
            if (hover) {
                Render2D.fillRounded(g, bx, cy, w, rh - Theme.px(2), Theme.px(4),
                        ColorUtil.withAlpha(Theme.BUTTON_HOVER, 200));
            }
            // Accent glow rippling out after a toggle.
            Animation glow = toggleGlow.get(m);
            if (glow != null) {
                float gt = glow.valueF();
                if (gt > 0.01F) {
                    Render2D.fillRounded(g, bx, cy, w, rh - Theme.px(2), Theme.px(4),
                            ColorUtil.withAlpha(Theme.accent(), (int) (gt * 70)));
                    Render2D.outlineRounded(g, bx, cy, w, rh - Theme.px(2), Theme.px(4), 1.0F,
                            ColorUtil.withAlpha(Theme.accent(), (int) (gt * 180)));
                } else {
                    toggleGlow.remove(m);
                }
            }
            Component name = names.computeIfAbsent(m, k -> Fonts.ui(k.getName()));
            Fonts.drawScaled(g, name, bx + Theme.px(6), cy + Theme.px(9), 0.9F, Theme.TEXT_PRIMARY, false);
            if (m.isToggleable()) {
                SettingRows.drawToggle(g, bx + w - Theme.px(34), cy + Theme.px(6), m.isEnabled());
            }
            cy += rh;
            float sectionH = expandedHeight(m);
            if (sectionH > 0) {
                float fullH = rows.totalHeight(settingsOf(m)) + Theme.px(6);
                boolean clipped = sectionH < fullH - 0.5F;
                if (clipped) {
                    g.enableScissor((int) Math.floor(bx), (int) Math.floor(cy - Theme.px(2)),
                            (int) Math.ceil(bx + w), (int) Math.ceil(cy - Theme.px(2) + sectionH));
                }
                Render2D.fillRounded(g, bx, cy - Theme.px(2), w, fullH,
                        Theme.px(4), ColorUtil.withAlpha(0xFF0E0E10, 160));
                rows.render(g, bx + Theme.px(8), cy + Theme.px(2), w - Theme.px(16), settingsOf(m));
                if (clipped) {
                    g.disableScissor();
                }
                cy += sectionH;
            }
        }
    }

    /**
     * Renders the Sodium status row at the top of the Performance panel:
     * green confirmation when Sodium is loaded, otherwise the recommendation
     * with a clickable Modrinth link.
     *
     * @return the height consumed
     */
    private float renderInfoRow(GuiGraphics g, float bx, float by, float w, int mouseX, int mouseY) {
        float h = infoRowHeight();
        if (h <= 0) {
            return 0;
        }
        if (SodiumIntegration.isLoaded()) {
            Fonts.drawScaled(g, sodiumLoaded, bx + Theme.px(4), by + Theme.px(5),
                    0.85F, 0xFF4CD964, false);
        } else {
            Fonts.drawScaled(g, sodiumMissing, bx + Theme.px(4), by + Theme.px(4),
                    0.8F, Theme.TEXT_SECONDARY, false);
            Fonts.drawScaled(g, sodiumHint, bx + Theme.px(4), by + Theme.px(16), 0.8F,
                    Theme.TEXT_SECONDARY, false);
            boolean hover = mouseX >= bx && mouseX <= bx + w
                    && mouseY >= by && mouseY < by + h;
            Fonts.drawScaled(g, sodiumLink, bx + Theme.px(4), by + Theme.px(30), 0.8F,
                    hover ? ColorUtil.lerp(Theme.accent(), 0xFFFFFFFF, 0.3F) : Theme.accent(),
                    false);
        }
        return h;
    }

    @Override
    protected boolean bodyClicked(double mx, double my, int button) {
        float bx = getX() + Theme.px(10);
        float w = width() - Theme.px(20);
        float cy = getY() + headerHeight();

        float infoH = infoRowHeight();
        if (infoH > 0) {
            if (my >= cy && my < cy + infoH) {
                if (!SodiumIntegration.isLoaded()) {
                    ClientSounds.click();
                    SodiumIntegration.openModrinth();
                }
                return true;
            }
            cy += infoH;
        }

        for (Module m : modules()) {
            float rh = moduleRowHeight();
            if (my >= cy && my < cy + rh) {
                if (m.isToggleable() && mx >= bx + w - Theme.px(38)) {
                    m.toggle();
                    ClientSounds.toggle(m.isEnabled());
                    if (Theme.animationsEnabled()) {
                        Animation glow = new Animation(1.0, 350, Easing.CUBIC_OUT);
                        glow.animateTo(0.0);
                        toggleGlow.put(m, glow);
                    }
                } else if (!settingsOf(m).isEmpty()) {
                    toggleExpanded(m);
                    ClientSounds.click();
                }
                return true;
            }
            cy += rh;
            float sectionH = expandedHeight(m);
            if (sectionH > 0) {
                if (my >= cy && my < cy + sectionH) {
                    return rows.mouseClicked(mx, my, bx + Theme.px(8), cy + Theme.px(2),
                            w - Theme.px(16), settingsOf(m), button);
                }
                cy += sectionH;
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
