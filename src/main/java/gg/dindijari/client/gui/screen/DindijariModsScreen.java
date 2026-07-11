package gg.dindijari.client.gui.screen;

import gg.dindijari.client.gui.widget.ThemedButton;
import gg.dindijari.client.gui.widget.ThemedTextField;
import gg.dindijari.client.render.ColorUtil;
import gg.dindijari.client.render.Fonts;
import gg.dindijari.client.render.Render2D;
import gg.dindijari.client.render.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforgespi.language.IModInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The client's mod list, replacing the NeoForge {@code ModListScreen}
 * (swapped in by {@link gg.dindijari.client.gui.ScreenManager}; also reachable
 * from the themed Options root): a searchable list of loaded mods as rounded
 * cards (initial-glyph icon, name, version), with a detail pane showing id,
 * version, authors, license and description, and a Config button for mods
 * that expose a config screen via {@code IConfigScreenFactory}.
 *
 * <p>Mod names and descriptions render with the vanilla font stack (mods may
 * use any script); the client's Inter face is used for the chrome.
 */
public final class DindijariModsScreen extends ThemedScreen {

    private record Entry(IModInfo info, Component name, Component meta, Component initial) {
    }

    private final Screen parent;
    private final Component header = Fonts.ui("Mods");
    private final List<Entry> all = new ArrayList<>();
    private final List<Entry> visible = new ArrayList<>();

    private String filter = "";
    private Entry selected;
    private float scroll;
    private ThemedButton configButton;

    // Detail pane cache, rebuilt on selection change only.
    private List<FormattedCharSequence> descriptionLines = List.of();
    private Component detailMeta = Component.empty();
    private Component detailAuthors = Component.empty();

    /**
     * Creates the screen.
     *
     * @param parent the screen to return to
     */
    public DindijariModsScreen(Screen parent) {
        super(Component.literal("Mods"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        if (all.isEmpty()) {
            for (IModInfo info : ModList.get().getMods()) {
                all.add(new Entry(info,
                        Component.literal(info.getDisplayName()),
                        Fonts.ui("v" + info.getVersion() + " \u00b7 " + info.getModId()),
                        Fonts.ui(info.getDisplayName().isEmpty() ? "?"
                                : info.getDisplayName().substring(0, 1).toUpperCase(Locale.ROOT))));
            }
            all.sort((a, b) -> a.info().getDisplayName()
                    .compareToIgnoreCase(b.info().getDisplayName()));
            refilter();
        }

        int pad = Math.round(Theme.px(24));
        int searchW = Math.round(Theme.px(200));
        addRenderableWidget(new ThemedTextField(this.width - pad - searchW, pad, searchW,
                Math.round(Theme.px(26)), "Search mods...", q -> {
            filter = q.toLowerCase(Locale.ROOT);
            refilter();
        }));

        int bh = Math.round(Theme.px(32));
        int by = this.height - bh - Math.round(Theme.px(16));
        configButton = new ThemedButton(pad, by, Math.round(Theme.px(120)), bh,
                "Config", true, this::openConfig);
        configButton.active = false;
        addRenderableWidget(configButton);
        addRenderableWidget(new ThemedButton(pad + Math.round(Theme.px(128)), by,
                Math.round(Theme.px(100)), bh, "Done", this::onClose));

        if (selected != null) {
            select(selected);
        }
    }

    private void refilter() {
        visible.clear();
        for (Entry entry : all) {
            if (filter.isEmpty()
                    || entry.info().getDisplayName().toLowerCase(Locale.ROOT).contains(filter)
                    || entry.info().getModId().contains(filter)) {
                visible.add(entry);
            }
        }
        if (selected != null && !visible.contains(selected)) {
            selected = null;
            if (configButton != null) {
                configButton.active = false;
            }
        }
    }

    private void select(Entry entry) {
        selected = entry;
        IModInfo info = entry.info();
        configButton.active = IConfigScreenFactory.getForMod(info).isPresent();

        String authors = info.getConfig().getConfigElement("authors")
                .map(Object::toString).orElse("unknown");
        String license = info.getOwningFile().getLicense();
        detailMeta = Fonts.ui("v" + info.getVersion() + " \u00b7 " + info.getModId());
        detailAuthors = Fonts.ui("by " + authors + " \u00b7 " + license);
        descriptionLines = this.font.split(
                Component.literal(info.getDescription().strip()), (int) (detailW() - Theme.px(24)));
    }

    private void openConfig() {
        if (selected == null) {
            return;
        }
        ModContainer container = ModList.get()
                .getModContainerById(selected.info().getModId()).orElse(null);
        if (container != null) {
            IConfigScreenFactory.getForMod(selected.info())
                    .map(factory -> factory.createScreen(container, this))
                    .ifPresent(this.minecraft::setScreen);
        }
    }

    // ------------------------------------------------------------------
    // Layout: list left, detail pane right
    // ------------------------------------------------------------------

    private float listX() {
        return Theme.px(24);
    }

    private float listW() {
        return this.width * 0.42F;
    }

    private float listTop() {
        return Theme.px(56);
    }

    private float listBottom() {
        return this.height - Theme.px(58);
    }

    private float cardH() {
        return Theme.px(44);
    }

    private float cardStride() {
        return cardH() + Theme.px(6);
    }

    private float detailX() {
        return listX() + listW() + Theme.px(16);
    }

    private float detailW() {
        return this.width - detailX() - Theme.px(24);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);

        float pad = Theme.px(24);
        Fonts.drawScaled(g, header, pad, pad, 1.5F, Theme.TEXT_PRIMARY, false);
        Render2D.fillRounded(g, pad, Theme.snap(pad + 9 * 1.5F + Theme.px(5)),
                Fonts.width(header) * 1.5F * 0.6F, Theme.px(3), Theme.px(1.5F), Theme.accent());

        clampScroll();
        g.enableScissor((int) listX() - 2, (int) listTop() - 2,
                (int) (listX() + listW()) + 2, (int) listBottom() + 2);
        float cy = listTop() - scroll;
        for (Entry entry : visible) {
            if (cy + cardH() >= listTop() && cy <= listBottom()) {
                renderCard(g, entry, listX(), cy, listW(), mouseX, mouseY);
            }
            cy += cardStride();
        }
        g.disableScissor();

        renderDetail(g);
    }

    private void renderCard(GuiGraphics g, Entry entry, float x, float y, float w,
                            int mouseX, int mouseY) {
        boolean hover = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + cardH();
        boolean isSelected = entry == selected;
        Render2D.fillRounded(g, x, y, w, cardH(), Theme.px(6),
                hover || isSelected ? Theme.BUTTON : ColorUtil.withAlpha(Theme.BUTTON, 170));
        if (isSelected) {
            Render2D.outlineRounded(g, x, y, w, cardH(), Theme.px(6), 1.2F, Theme.accent());
        }
        float iconSize = Theme.px(30);
        float iconX = x + Theme.px(7);
        float iconY = y + (cardH() - iconSize) / 2;
        Render2D.fillRounded(g, iconX, iconY, iconSize, iconSize, Theme.px(5), 0xFF0E0E10);
        Fonts.drawCentered(g, entry.initial(), iconX + iconSize / 2, iconY + iconSize / 2 - 4.5F,
                1.0F, Theme.accent(), false);
        float textX = iconX + iconSize + Theme.px(8);
        g.drawString(this.font, entry.name(), Math.round(textX), Math.round(y + Theme.px(8)),
                Theme.TEXT_PRIMARY, false);
        Fonts.drawScaled(g, entry.meta(), textX, y + Theme.px(24), 0.8F, Theme.TEXT_SECONDARY, false);
    }

    private void renderDetail(GuiGraphics g) {
        float x = detailX();
        float y = listTop();
        float w = detailW();
        float h = listBottom() - listTop();
        Render2D.fillRounded(g, x, y, w, h, Theme.px(Theme.PANEL_RADIUS), Theme.PANEL);

        if (selected == null) {
            Fonts.drawCentered(g, Fonts.ui("Select a mod"), x + w / 2, y + h / 2 - 4.5F,
                    1.0F, Theme.TEXT_SECONDARY, false);
            return;
        }
        float pad = Theme.px(12);
        g.drawString(this.font, selected.name(), Math.round(x + pad), Math.round(y + pad),
                Theme.TEXT_PRIMARY, false);
        Fonts.drawScaled(g, detailMeta, x + pad, y + pad + Theme.px(16), 0.85F,
                Theme.TEXT_SECONDARY, false);
        Fonts.drawScaled(g, detailAuthors, x + pad, y + pad + Theme.px(30), 0.85F,
                Theme.TEXT_SECONDARY, false);

        float ty = y + pad + Theme.px(52);
        for (FormattedCharSequence line : descriptionLines) {
            if (ty > y + h - pad - 9) {
                break;
            }
            g.drawString(this.font, line, Math.round(x + pad), Math.round(ty),
                    Theme.TEXT_SECONDARY, false);
            ty += 11;
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && mx >= listX() && mx <= listX() + listW()
                && my >= listTop() && my <= listBottom()) {
            float cy = listTop() - scroll;
            for (Entry entry : visible) {
                if (my >= cy && my <= cy + cardH()) {
                    select(entry);
                    setFocused(null);
                    return true;
                }
                cy += cardStride();
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        scroll -= (float) dy * cardStride();
        clampScroll();
        return true;
    }

    private void clampScroll() {
        float max = Math.max(0, visible.size() * cardStride() - (listBottom() - listTop()));
        scroll = Math.max(0, Math.min(scroll, max));
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
