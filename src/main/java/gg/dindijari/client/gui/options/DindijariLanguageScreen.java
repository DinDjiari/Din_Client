package gg.dindijari.client.gui.options;

import gg.dindijari.client.gui.screen.ThemedScreen;
import gg.dindijari.client.gui.widget.ThemedButton;
import gg.dindijari.client.render.ColorUtil;
import gg.dindijari.client.render.Fonts;
import gg.dindijari.client.render.Render2D;
import gg.dindijari.client.render.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.LanguageManager;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Themed language selection: the available languages as selectable rows with
 * an accent marker on the current choice. Applying goes through the vanilla
 * pipeline — {@code LanguageManager.setSelected}, {@code options.languageCode},
 * options save and a resource reload — exactly like the vanilla screen.
 */
public final class DindijariLanguageScreen extends ThemedScreen {

    private record Entry(String code, Component label) {
    }

    private final Screen parent;
    private final Component header = Fonts.ui("Language");
    private final List<Entry> entries = new ArrayList<>();

    private String selectedCode;
    private float scroll;

    /**
     * Creates the screen.
     *
     * @param parent the screen to return to
     */
    public DindijariLanguageScreen(Screen parent) {
        super(Component.translatable("options.language.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        if (entries.isEmpty()) {
            LanguageManager manager = this.minecraft.getLanguageManager();
            selectedCode = manager.getSelected();
            // Language names span many scripts; the vanilla font stack has the
            // Unicode fallbacks our bundled Inter face lacks.
            manager.getLanguages().forEach((code, info) ->
                    entries.add(new Entry(code, info.toComponent())));
        }
        int bh = Math.round(Theme.px(34));
        int bw = Math.round(Theme.px(160));
        int gap = Math.round(Theme.px(Theme.GRID));
        int total = 2 * bw + gap;
        int by = this.height - bh - Math.round(Theme.px(16));
        addRenderableWidget(new ThemedButton((this.width - total) / 2, by, bw, bh,
                "Apply", true, this::apply));
        addRenderableWidget(new ThemedButton((this.width - total) / 2 + bw + gap, by, bw, bh,
                "Back", this::onClose));
    }

    private float rowH() {
        return Theme.px(28);
    }

    private float listTop() {
        return Theme.px(56);
    }

    private float listBottom() {
        return this.height - Theme.px(60);
    }

    private float rowX() {
        return (this.width - rowW()) / 2;
    }

    private float rowW() {
        return Math.min(Theme.px(480), this.width - Theme.px(64));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);

        float pad = Theme.px(24);
        Fonts.drawScaled(g, header, pad, pad, 1.5F, Theme.TEXT_PRIMARY, false);
        Render2D.fillRounded(g, pad, Theme.snap(pad + 9 * 1.5F + Theme.px(5)),
                Fonts.width(header) * 1.5F * 0.6F, Theme.px(3), Theme.px(1.5F), Theme.accent());

        clampScroll();
        g.enableScissor((int) rowX() - 4, (int) listTop() - 2,
                (int) (rowX() + rowW()) + 4, (int) listBottom() + 2);
        float cy = listTop() - scroll;
        for (Entry entry : entries) {
            if (cy + rowH() >= listTop() && cy <= listBottom()) {
                boolean selected = entry.code().equals(selectedCode);
                boolean hover = mouseX >= rowX() && mouseX <= rowX() + rowW()
                        && mouseY >= cy && mouseY < cy + rowH();
                if (selected || hover) {
                    Render2D.fillRounded(g, rowX(), cy, rowW(), rowH() - Theme.px(3), Theme.px(4),
                            ColorUtil.withAlpha(Theme.BUTTON_HOVER, selected ? 255 : 130));
                }
                if (selected) {
                    Render2D.fillRounded(g, rowX(), cy, Theme.px(3), rowH() - Theme.px(3),
                            Theme.px(1.5F), Theme.accent());
                }
                Fonts.drawScaled(g, entry.label(), rowX() + Theme.px(12), cy + Theme.px(8),
                        0.95F, selected ? Theme.TEXT_PRIMARY : Theme.TEXT_SECONDARY, false);
            }
            cy += rowH();
        }
        g.disableScissor();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && my >= listTop() && my <= listBottom()
                && mx >= rowX() && mx <= rowX() + rowW()) {
            float cy = listTop() - scroll;
            for (Entry entry : entries) {
                if (my >= cy && my < cy + rowH()) {
                    selectedCode = entry.code();
                    return true;
                }
                cy += rowH();
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        scroll -= (float) dy * rowH();
        clampScroll();
        return true;
    }

    private void clampScroll() {
        float max = Math.max(0, entries.size() * rowH() - (listBottom() - listTop()));
        scroll = Math.max(0, Math.min(scroll, max));
    }

    /** Applies the selection through the vanilla language pipeline. */
    private void apply() {
        LanguageManager manager = this.minecraft.getLanguageManager();
        if (!manager.getSelected().equals(selectedCode)) {
            manager.setSelected(selectedCode);
            this.minecraft.options.languageCode = selectedCode;
            this.minecraft.options.save();
            this.minecraft.reloadResourcePacks();
        }
        onClose();
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
