package gg.dindijari.client.gui.clickgui;

import gg.dindijari.client.core.Services;
import gg.dindijari.client.gui.screen.ThemedScreen;
import gg.dindijari.client.gui.widget.ScrollPanel;
import gg.dindijari.client.gui.widget.ThemedTextField;
import gg.dindijari.client.module.Category;
import gg.dindijari.client.module.Module;
import gg.dindijari.client.render.ColorUtil;
import gg.dindijari.client.render.Fonts;
import gg.dindijari.client.render.Icons;
import gg.dindijari.client.render.Render2D;
import gg.dindijari.client.render.Theme;
import gg.dindijari.client.util.animation.Animation;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The Client Settings surface — a full-screen overlay over the dimmed/blurred
 * game (or the charcoal backdrop at the main menu), opened with Right-Shift
 * in-game and from the main-menu / pause-menu buttons.
 *
 * <p>Layout (client design tokens throughout): a top bar with the wordmark, the
 * section tabs and a close button; a filter row of category chips plus a search
 * field and a favorites toggle; and a responsive, scrollable {@link ScrollPanel}
 * grid of module cards (up to four per row). Each card shows the module name
 * (ellipsised with a hover tooltip if it does not fit), a favorite star, a
 * large category glyph, a gear that opens the module's settings overlay and a
 * wide status button that toggles it. Cards lighten and scale slightly on hover.
 */
public final class ClickGuiScreen extends ThemedScreen {

    /** Top-bar sections. */
    private enum Tab {
        MODULES("Modules"), HUD("HUD"), THEME("Theme"),
        COSMETICS("Cosmetics"), BADGES("Badges"), SETTINGS("Settings");

        final String label;

        Tab(String label) {
            this.label = label;
        }
    }

    private final Screen parent;
    private final Component wordmark = Fonts.display("DINDIJARI");
    private final Map<Module, Animation> hover = new IdentityHashMap<>();
    private final ScrollPanel gridScroll = new ScrollPanel();

    private Tab tab = Tab.MODULES;
    private Category chip; // null = "All"
    private boolean favoritesOnly;
    private String search = "";

    private ThemedTextField searchField;
    private ModuleSettingsView settingsView;

    // Deferred tooltip drawn after the grid (truncated card name under cursor).
    private Component pendingTooltip;
    private int tooltipX;
    private int tooltipY;

    /**
     * Creates the Click GUI.
     *
     * @param parent screen to return to on close ({@code null} returns to the game)
     */
    public ClickGuiScreen(Screen parent) {
        super(Component.literal("Client Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int sw = Math.round(Theme.px(200));
        searchField = new ThemedTextField(
                Math.round(this.width - Theme.px(24) - sw), Math.round(topBarH() + Theme.px(8)),
                sw, Math.round(Theme.px(24)), "Search modules...", q -> {
            search = q.toLowerCase(Locale.ROOT);
            gridScroll.reset();
        });
        addRenderableWidget(searchField);
    }

    // ------------------------------------------------------------------
    // Layout metrics
    // ------------------------------------------------------------------

    private float topBarH() {
        return Theme.px(44);
    }

    private float chipRowH() {
        return Theme.px(40);
    }

    private float gridTop() {
        return topBarH() + chipRowH() + Theme.px(8);
    }

    private float gridBottom() {
        return this.height - Theme.px(16);
    }

    private float gridX() {
        return Theme.px(24);
    }

    private float gridW() {
        return this.width - 2 * gridX();
    }

    /** Card area excludes a gutter for the scrollbar. */
    private float cardsW() {
        return gridW() - Theme.px(12);
    }

    private int columns() {
        int c = (int) (cardsW() / Theme.px(170));
        return Math.max(2, Math.min(4, c));
    }

    private float cardGap() {
        return Theme.px(Theme.GRID);
    }

    private float cardWidth() {
        int cols = columns();
        return (cardsW() - (cols - 1) * cardGap()) / cols;
    }

    private float cardHeight() {
        return Theme.px(112);
    }

    // ------------------------------------------------------------------
    // Model
    // ------------------------------------------------------------------

    private List<Module> visibleModules() {
        List<Module> list = new ArrayList<>();
        for (Module m : Services.modules().getModules()) {
            if (tab == Tab.THEME && m.getCategory() != Category.CLIENT) {
                continue;
            }
            if (chip != null && m.getCategory() != chip) {
                continue;
            }
            if (favoritesOnly && !m.isFavorite()) {
                continue;
            }
            if (!search.isEmpty() && !m.getName().toLowerCase(Locale.ROOT).contains(search)) {
                continue;
            }
            list.add(m);
        }
        return list;
    }

    private Animation hoverAnim(Module m) {
        return hover.computeIfAbsent(m, k -> new Animation(0.0, Theme.hoverMs(), Theme.HOVER_EASING));
    }

    // ------------------------------------------------------------------
    // Rendering
    // ------------------------------------------------------------------

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        pendingTooltip = null;
        super.render(g, mouseX, mouseY, partialTick);

        renderTopBar(g, mouseX, mouseY);
        boolean gridTab = tab == Tab.MODULES || tab == Tab.THEME;
        searchField.visible = gridTab && settingsView == null;

        if (gridTab) {
            renderChipRow(g, mouseX, mouseY);
            renderGrid(g, mouseX, mouseY);
        } else {
            renderPlaceholder(g);
        }

        if (pendingTooltip != null && settingsView == null) {
            drawTooltip(g, pendingTooltip, tooltipX, tooltipY);
        }

        if (settingsView != null) {
            Render2D.fillRect(g, 0, 0, this.width, this.height, 0x88000000);
            settingsView.render(g, this.width, this.height, mouseX, mouseY);
        }
    }

    private void renderTopBar(GuiGraphics g, int mouseX, int mouseY) {
        Render2D.fillRect(g, 0, 0, this.width, topBarH(), ColorUtil.withAlpha(Theme.PANEL, 235));
        Render2D.rgbLine(g, 0, topBarH() - Theme.px(2), this.width, Theme.px(2));

        Fonts.drawScaled(g, wordmark, Theme.px(20), Theme.px(9), 0.42F, Theme.TEXT_PRIMARY, false);

        float tx = Theme.px(150);
        for (Tab t : Tab.values()) {
            Component label = Fonts.ui(t.label);
            float w = Fonts.width(label) + Theme.px(16);
            boolean active = t == tab || (t == Tab.HUD && tab == Tab.MODULES && chip == Category.HUD);
            boolean hovered = mouseX >= tx && mouseX <= tx + w && mouseY <= topBarH();
            if (active) {
                Render2D.fillRounded(g, tx, Theme.px(10), w, Theme.px(24), Theme.px(6),
                        ColorUtil.withAlpha(Theme.accent(), 40));
            }
            Fonts.draw(g, label, tx + Theme.px(8), Theme.px(17),
                    active ? Theme.accent() : (hovered ? Theme.TEXT_PRIMARY : Theme.TEXT_SECONDARY), false);
            tx += w + Theme.px(4);
        }

        boolean closeHover = mouseX >= this.width - Theme.px(34) && mouseY <= topBarH();
        Icons.close(g, this.width - Theme.px(20), topBarH() / 2, Theme.px(14),
                closeHover ? Theme.TEXT_PRIMARY : Theme.TEXT_SECONDARY);
    }

    private void renderChipRow(GuiGraphics g, int mouseX, int mouseY) {
        float y = topBarH() + Theme.px(8);
        float x = gridX();
        x = chip(g, "All", chip == null, x, y, mouseX, mouseY);
        for (Category c : Category.values()) {
            x = chip(g, c.getDisplayName(), chip == c, x, y, mouseX, mouseY);
        }

        float favX = this.width - Theme.px(24) - Theme.px(200) - Theme.px(30);
        Render2D.fillRounded(g, favX, y, Theme.px(24), Theme.px(24), Theme.px(6),
                favoritesOnly ? ColorUtil.withAlpha(Theme.accent(), 60) : Theme.BUTTON);
        Icons.heart(g, favX + Theme.px(12), y + Theme.px(12), Theme.px(13),
                favoritesOnly ? Theme.accent() : Theme.TEXT_SECONDARY);
    }

    private float chip(GuiGraphics g, String label, boolean active, float x, float y,
                       int mouseX, int mouseY) {
        Component c = Fonts.ui(label);
        float w = Fonts.width(c) * 0.9F + Theme.px(16);
        int fill = active ? Theme.accent()
                : (mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + Theme.px(24)
                        ? Theme.BUTTON_HOVER : Theme.BUTTON);
        Render2D.fillRounded(g, x, y, w, Theme.px(24), Theme.px(6), fill);
        Fonts.drawScaled(g, c, x + Theme.px(8), y + Theme.px(8), 0.9F,
                active ? 0xFF0E0E10 : Theme.TEXT_PRIMARY, false);
        return x + w + Theme.px(6);
    }

    private void renderGrid(GuiGraphics g, int mouseX, int mouseY) {
        List<Module> modules = visibleModules();
        int cols = columns();
        float cw = cardWidth();
        float ch = cardHeight();
        float gap = cardGap();
        float x0 = gridX();
        float top = gridTop();
        float h = gridBottom() - top;
        int rows = (modules.size() + cols - 1) / cols;
        float contentH = rows * (ch + gap);

        float originY = gridScroll.begin(g, x0, top, gridW(), h, contentH);
        for (int i = 0; i < modules.size(); i++) {
            float cardX = x0 + (i % cols) * (cw + gap);
            float cardY = originY + (i / cols) * (ch + gap);
            if (cardY + ch >= top && cardY <= top + h) {
                renderCard(g, modules.get(i), cardX, cardY, cw, ch, mouseX, mouseY);
            }
        }
        gridScroll.end(g);

        if (modules.isEmpty()) {
            Fonts.drawCentered(g, Fonts.ui("No modules match your filter"), this.width / 2.0F,
                    top + h / 2, 1.0F, Theme.TEXT_SECONDARY, false);
        }
    }

    private void renderCard(GuiGraphics g, Module m, float x, float y, float w, float h,
                            int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h
                && mouseY >= gridTop() && mouseY <= gridBottom();
        Animation anim = hoverAnim(m);
        anim.animateTo(hovered ? 1.0 : 0.0);
        float t = anim.valueF();

        float scale = 1.0F + 0.02F * t;
        g.pose().pushPose();
        g.pose().translate(x + w / 2, y + h / 2, 0);
        g.pose().scale(scale, scale, 1.0F);
        g.pose().translate(-(x + w / 2), -(y + h / 2), 0);

        int fill = ColorUtil.lerp(Theme.PANEL, Theme.BUTTON, t);
        Render2D.fillRounded(g, x, y, w, h, Theme.px(10), fill);
        if (t > 0.01F) {
            Render2D.outlineRounded(g, x, y, w, h, Theme.px(10), 1.0F,
                    ColorUtil.scaleAlpha(Theme.accent(), 0.5F * t));
        }

        // Name (ellipsised) + favorite star; tooltip if truncated.
        Component name = Fonts.ui(m.getName());
        float nameMax = w - Theme.px(30);
        if (hovered && !Fonts.fits(name, nameMax, 1.0F)) {
            pendingTooltip = name;
            tooltipX = mouseX;
            tooltipY = mouseY;
        }
        Fonts.draw(g, Fonts.fit(name, nameMax, 1.0F), x + Theme.px(10), y + Theme.px(9),
                Theme.TEXT_PRIMARY, false);
        Icons.star(g, x + w - Theme.px(12), y + Theme.px(13), Theme.px(12),
                m.isFavorite() ? Theme.accent() : Theme.TEXT_SECONDARY, m.isFavorite());

        Icons.module(g, m.getCategory(), x + w / 2, y + h / 2 - Theme.px(2), Theme.px(30),
                ColorUtil.lerp(Theme.TEXT_SECONDARY, Theme.accent(), m.isEnabled() ? 1.0F : 0.3F));

        float rowY = y + h - Theme.px(26);
        boolean gearHover = hovered && mouseX <= x + Theme.px(30) && mouseY >= rowY;
        Render2D.fillRounded(g, x + Theme.px(8), rowY, Theme.px(20), Theme.px(20), Theme.px(5),
                gearHover ? Theme.BUTTON_HOVER : Theme.BUTTON);
        Icons.gear(g, x + Theme.px(18), rowY + Theme.px(10), Theme.px(13),
                gearHover ? Theme.accent() : Theme.TEXT_SECONDARY);

        float btnX = x + Theme.px(34);
        float btnW = w - Theme.px(42);
        boolean toggle = m.isToggleable();
        boolean on = toggle && m.isEnabled();
        String status = toggle ? (on ? "Enabled" : "Disabled") : "Configure";
        Render2D.fillRounded(g, btnX, rowY, btnW, Theme.px(20), Theme.px(6),
                on ? Theme.accent() : Theme.BUTTON);
        Fonts.drawCentered(g, Fonts.fit(Fonts.ui(status), btnW - Theme.px(6), 0.85F),
                btnX + btnW / 2, rowY + Theme.px(6), 0.85F,
                on ? 0xFF0E0E10 : Theme.TEXT_SECONDARY, false);

        g.pose().popPose();
    }

    private void renderPlaceholder(GuiGraphics g) {
        String text = switch (tab) {
            case COSMETICS -> "Cosmetics — coming soon";
            case BADGES -> "Badges — coming soon";
            case SETTINGS -> "Active profile: " + Services.config().getActiveProfileName();
            default -> "";
        };
        Fonts.drawCentered(g, Fonts.ui(text), this.width / 2.0F, this.height / 2.0F, 1.0F,
                Theme.TEXT_SECONDARY, false);
    }

    private void drawTooltip(GuiGraphics g, Component text, int mx, int my) {
        float w = Fonts.width(text) + Theme.px(12);
        float x = Math.min(mx + Theme.px(10), this.width - w - Theme.px(4));
        float y = Math.max(my - Theme.px(4), Theme.px(2));
        Render2D.dropShadow(g, x, y, w, Theme.px(18), Theme.px(4), Theme.px(6), Theme.SHADOW);
        Render2D.fillRounded(g, x, y, w, Theme.px(18), Theme.px(4), Theme.BUTTON_HOVER);
        Fonts.draw(g, text, x + Theme.px(6), y + Theme.px(5), Theme.TEXT_PRIMARY, false);
    }

    // ------------------------------------------------------------------
    // Input
    // ------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (settingsView != null) {
            if (settingsView.isCloseClick(mx, my) || settingsView.isOutside(mx, my)) {
                closeSettings();
                return true;
            }
            settingsView.mouseClicked(mx, my, button);
            return true;
        }

        if (my <= topBarH()) {
            if (mx >= this.width - Theme.px(34)) {
                onClose();
                return true;
            }
            float tx = Theme.px(150);
            for (Tab t : Tab.values()) {
                float w = Fonts.width(Fonts.ui(t.label)) + Theme.px(16);
                if (mx >= tx && mx <= tx + w) {
                    if (t == Tab.HUD) {
                        tab = Tab.MODULES;
                        chip = Category.HUD;
                    } else {
                        tab = t;
                        if (t == Tab.MODULES) {
                            chip = null;
                        }
                    }
                    gridScroll.reset();
                    setFocused(null);
                    return true;
                }
                tx += w + Theme.px(4);
            }
        }

        boolean gridTab = tab == Tab.MODULES || tab == Tab.THEME;
        if (gridTab && my >= topBarH() && my <= topBarH() + chipRowH() && handleChipClick(mx, my)) {
            return true;
        }
        if (gridTab && my >= gridTop() && my <= gridBottom() && handleGridClick(mx, my, button)) {
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    private boolean handleChipClick(double mx, double my) {
        float y = topBarH() + Theme.px(8);
        float x = gridX();
        float allW = Fonts.width(Fonts.ui("All")) * 0.9F + Theme.px(16);
        if (mx >= x && mx <= x + allW && my >= y && my <= y + Theme.px(24)) {
            chip = null;
            gridScroll.reset();
            return true;
        }
        x += allW + Theme.px(6);
        for (Category c : Category.values()) {
            float w = Fonts.width(Fonts.ui(c.getDisplayName())) * 0.9F + Theme.px(16);
            if (mx >= x && mx <= x + w && my >= y && my <= y + Theme.px(24)) {
                chip = c;
                gridScroll.reset();
                return true;
            }
            x += w + Theme.px(6);
        }
        float favX = this.width - Theme.px(24) - Theme.px(200) - Theme.px(30);
        if (mx >= favX && mx <= favX + Theme.px(24) && my >= y && my <= y + Theme.px(24)) {
            favoritesOnly = !favoritesOnly;
            gridScroll.reset();
            return true;
        }
        return false;
    }

    private boolean handleGridClick(double mx, double my, int button) {
        List<Module> modules = visibleModules();
        int cols = columns();
        float cw = cardWidth();
        float ch = cardHeight();
        float gap = cardGap();
        float x0 = gridX();
        float top = gridTop();
        float originY = top - gridScroll.scroll();
        for (int i = 0; i < modules.size(); i++) {
            float cardX = x0 + (i % cols) * (cw + gap);
            float cardY = originY + (i / cols) * (ch + gap);
            if (mx < cardX || mx > cardX + cw || my < cardY || my > cardY + ch) {
                continue;
            }
            Module m = modules.get(i);
            if (mx >= cardX + cw - Theme.px(22) && my <= cardY + Theme.px(24)) {
                m.setFavorite(!m.isFavorite());
                return true;
            }
            float rowY = cardY + ch - Theme.px(26);
            if (my >= rowY) {
                if (mx <= cardX + Theme.px(30)) {
                    openSettings(m);
                } else if (m.isToggleable()) {
                    m.toggle();
                } else {
                    openSettings(m);
                }
                return true;
            }
            openSettings(m);
            return true;
        }
        return false;
    }

    private void openSettings(Module m) {
        settingsView = new ModuleSettingsView(m);
        searchField.visible = false;
        setFocused(null);
    }

    private void closeSettings() {
        settingsView = null;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (settingsView != null && settingsView.mouseDragged(mx)) {
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (settingsView != null) {
            settingsView.mouseReleased();
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        if (settingsView != null) {
            return settingsView.mouseScrolled(mx, my, dy);
        }
        if (gridScroll.mouseScrolled(mx, my, dy)) {
            return true;
        }
        return super.mouseScrolled(mx, my, dx, dy);
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        if (settingsView != null) {
            return settingsView.charTyped(c, modifiers);
        }
        return super.charTyped(c, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (settingsView != null) {
            if (settingsView.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                closeSettings();
                return true;
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT && !(getFocused() instanceof ThemedTextField)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }
}
