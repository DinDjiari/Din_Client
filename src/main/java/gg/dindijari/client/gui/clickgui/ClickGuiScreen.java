package gg.dindijari.client.gui.clickgui;

import gg.dindijari.client.core.Services;
import gg.dindijari.client.gui.screen.ThemedScreen;
import gg.dindijari.client.gui.widget.ThemedTextField;
import gg.dindijari.client.module.Category;
import gg.dindijari.client.render.Fonts;
import gg.dindijari.client.render.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The Click GUI — the client's configuration surface (design reference §5),
 * opened with Right&nbsp;Shift in-game, from the main menu's
 * <em>Client Settings</em> button, or from the pause menu.
 *
 * <p>Layout: a centred search bar (live module filter) with a
 * "Right-Shift to close" hint, one draggable {@link CategoryPanel} per
 * {@link Category} (the Theme Editor lives in the Client category), all floating over the
 * blurred/dimmed game (or the charcoal backdrop at the main menu). Panel
 * positions persist for the session; module expansion state persists with
 * them. Right&nbsp;Shift or Esc closes the screen.
 */
public final class ClickGuiScreen extends ThemedScreen {

    /** Panel positions by title, persisted for the session. */
    private static final Map<String, float[]> POSITIONS = new HashMap<>();

    private final Screen parent;
    private final List<Panel> panels = new ArrayList<>();
    private final List<CategoryPanel> categoryPanels = new ArrayList<>();
    private final Component hint = Fonts.ui("Right-Shift to close");

    private Panel activePanel;
    private ThemedTextField search;

    /**
     * Creates the Click GUI.
     *
     * @param parent screen to return to on close; {@code null} returns to the
     *               game
     */
    public ClickGuiScreen(Screen parent) {
        super(Component.literal("Client Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        panels.clear();
        categoryPanels.clear();

        int searchW = Math.round(Theme.px(280));
        int searchH = Math.round(Theme.px(28));
        search = new ThemedTextField((this.width - searchW) / 2, Math.round(Theme.px(24)),
                searchW, searchH, "Search modules...", this::onSearch);
        addRenderableWidget(search);

        // Default flow layout: panels in rows under the search bar, wrapping at
        // the right screen edge. Dragged positions (kept per session) win.
        float margin = Theme.px(16);
        float cx = margin;
        float cy = Theme.px(72);
        float rowMaxH = 0;

        List<Panel> toPlace = new ArrayList<>();
        for (Category category : Category.values()) {
            CategoryPanel panel = new CategoryPanel(category, 0, 0);
            categoryPanels.add(panel);
            toPlace.add(panel);
        }
        BrandingPanel brandingPanel = new BrandingPanel(
                (gg.dindijari.client.module.modules.BrandingModule)
                        Services.modules().getModule("Branding"), 0, 0);
        toPlace.add(brandingPanel);
        addRenderableWidget(brandingPanel.titleField());
        addRenderableWidget(brandingPanel.subtitleField());

        for (Panel panel : toPlace) {
            float[] saved = POSITIONS.get(panelKey(panel));
            if (saved != null) {
                panel.setPosition(
                        Math.min(saved[0], this.width - panel.width()),
                        Math.min(saved[1], this.height - Theme.px(20)));
            } else {
                if (cx + panel.width() > this.width - margin && cx > margin) {
                    cx = margin;
                    cy += rowMaxH + margin;
                    rowMaxH = 0;
                }
                panel.setPosition(cx, cy);
                cx += panel.width() + margin;
                rowMaxH = Math.max(rowMaxH, panel.height());
            }
            panels.add(panel);
        }
    }

    private void onSearch(String query) {
        for (CategoryPanel panel : categoryPanels) {
            panel.setFilter(query);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        for (Panel panel : panels) {
            panel.render(g, mouseX, mouseY);
        }
        Fonts.drawScaled(g, hint, search.getX() + search.getWidth() + Theme.px(12),
                search.getY() + (search.getHeight() - 9) / 2.0F + 1, 0.85F,
                Theme.TEXT_SECONDARY, false);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        // Panels are drawn above the widgets, so hit-test them first, topmost
        // (= last rendered) panel first; a hit raises that panel.
        for (int i = panels.size() - 1; i >= 0; i--) {
            Panel panel = panels.get(i);
            if (panel.mouseClicked(mx, my, button)) {
                activePanel = panel;
                panels.remove(panel);
                panels.add(panel);
                setFocused(null);
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (activePanel != null) {
            if (activePanel instanceof CategoryPanel cp && cp.settingsDragged(mx)) {
                return true;
            }
            if (activePanel instanceof BrandingPanel bp && bp.settingsDragged(mx)) {
                return true;
            }
            if (activePanel.mouseDragged(mx, my, this.width, this.height)) {
                return true;
            }
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        for (Panel panel : panels) {
            panel.mouseReleased();
            if (panel instanceof CategoryPanel cp) {
                cp.settingsReleased();
            } else if (panel instanceof BrandingPanel bp) {
                bp.settingsReleased();
            }
        }
        activePanel = null;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // A listening keybind chip captures the next key (Esc = unbind).
        for (CategoryPanel panel : categoryPanels) {
            if (panel.isListening()) {
                return panel.keyPressed(keyCode);
            }
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT && !(getFocused() instanceof ThemedTextField)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        for (Panel panel : panels) {
            POSITIONS.put(panelKey(panel), new float[]{panel.getX(), panel.getY()});
        }
        this.minecraft.setScreen(parent);
    }

    private static String panelKey(Panel panel) {
        return panel.key();
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }
}
