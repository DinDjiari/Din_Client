package gg.dindijari.client.gui.options;

import gg.dindijari.client.gui.screen.ThemedScreen;
import gg.dindijari.client.gui.widget.ThemedButton;
import gg.dindijari.client.render.ColorUtil;
import gg.dindijari.client.render.Fonts;
import gg.dindijari.client.render.Render2D;
import gg.dindijari.client.render.Theme;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Generic themed options screen: a scrollable single-column list of
 * {@link OptionRow}s over the client's dark background, with a header (accent
 * underline) and a Done button. Vanilla {@link OptionInstance}s keep complete
 * ownership of behaviour — this screen only reads {@code get()} and calls
 * {@code set()}, so every callback (GUI scale resize, vsync, fullscreen, ...)
 * and all persistence work exactly as vanilla. Options are saved on close,
 * like the vanilla options screens.
 *
 * <p>Rows the wrapper cannot express with themed widgets embed the option's
 * own vanilla widget instead, so no functionality is lost (documented in
 * LIMITATIONS.md).
 */
public class ThemedOptionsScreen extends ThemedScreen {

    private final Screen parent;
    private final Component header;
    private final List<OptionRow> rows;
    private final Map<OptionRow, Component> labels = new HashMap<>();
    private final Map<OptionRow, Component> values = new HashMap<>();
    private final Map<OptionRow, String> valueTexts = new HashMap<>();
    private final Map<OptionRow, AbstractWidget> embedded = new HashMap<>();
    private final Map<OptionRow, ThemedButton> navButtons = new HashMap<>();

    private float scroll;
    private OptionRow draggingSlider;

    /**
     * Creates a themed options screen.
     *
     * @param parent the screen to return to
     * @param title  the header title
     * @param rows   the rows to show, in order
     */
    public ThemedOptionsScreen(Screen parent, String title, List<OptionRow> rows) {
        super(Component.literal(title));
        this.parent = parent;
        this.header = Fonts.ui(title);
        this.rows = rows;
    }

    @Override
    protected void init() {
        embedded.clear();
        navButtons.clear();

        int bh = Math.round(Theme.px(34));
        addRenderableWidget(new ThemedButton((this.width - Math.round(Theme.px(160))) / 2,
                this.height - bh - Math.round(Theme.px(16)),
                Math.round(Theme.px(160)), bh, "Done", this::onClose));

        // Vanilla-fallback and nav rows are real widgets; positioned every frame.
        for (OptionRow row : rows) {
            if (row.kind == OptionRow.Kind.VANILLA) {
                AbstractWidget widget = row.option.createButton(this.minecraft.options,
                        0, 0, Math.round(rowWidth() * 0.45F));
                embedded.put(row, widget);
                addRenderableWidget(widget);
            } else if (row.kind == OptionRow.Kind.NAV) {
                ThemedButton button = new ThemedButton(0, 0, Math.round(rowWidth()),
                        Math.round(Theme.px(30)), row.label,
                        () -> this.minecraft.setScreen(row.navTarget.get()));
                navButtons.put(row, button);
                addRenderableWidget(button);
            }
        }
    }

    // ------------------------------------------------------------------
    // Layout
    // ------------------------------------------------------------------

    private float rowWidth() {
        return Math.min(Theme.px(560), this.width - Theme.px(64));
    }

    private float rowX() {
        return (this.width - rowWidth()) / 2;
    }

    private float listTop() {
        return Theme.px(56);
    }

    private float listBottom() {
        return this.height - Theme.px(60);
    }

    private float rowHeight(OptionRow row) {
        return Theme.px(row.kind == OptionRow.Kind.NAV ? 38 : 32);
    }

    private float contentHeight() {
        float h = 0;
        for (OptionRow row : rows) {
            h += rowHeight(row);
        }
        return h;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        clampScroll();

        // Position the embedded/nav widgets before super renders them.
        float cy = listTop() - scroll;
        for (OptionRow row : rows) {
            AbstractWidget widget = embedded.get(row);
            if (widget != null) {
                widget.setX(Math.round(rowX() + rowWidth() * 0.55F));
                widget.setY(Math.round(cy + Theme.px(2)));
                widget.visible = cy + rowHeight(row) > listTop() && cy < listBottom();
            }
            ThemedButton nav = navButtons.get(row);
            if (nav != null) {
                nav.setX(Math.round(rowX()));
                nav.setY(Math.round(cy + Theme.px(2)));
                nav.visible = cy + rowHeight(row) > listTop() && cy < listBottom();
            }
            cy += rowHeight(row);
        }

        super.render(g, mouseX, mouseY, partialTick);

        float pad = Theme.px(24);
        Fonts.drawScaled(g, header, pad, pad, 1.5F, Theme.TEXT_PRIMARY, false);
        Render2D.fillRounded(g, pad, Theme.snap(pad + 9 * 1.5F + Theme.px(5)),
                Fonts.width(header) * 1.5F * 0.6F, Theme.px(3), Theme.px(1.5F), Theme.accent());

        g.enableScissor((int) rowX() - 4, (int) listTop() - 2,
                (int) (rowX() + rowWidth()) + 4, (int) listBottom() + 2);
        cy = listTop() - scroll;
        for (OptionRow row : rows) {
            if (cy + rowHeight(row) >= listTop() && cy <= listBottom()) {
                renderRow(g, row, rowX(), cy, rowWidth(), mouseX, mouseY);
            }
            cy += rowHeight(row);
        }
        g.disableScissor();
    }

    private void renderRow(GuiGraphics g, OptionRow row, float x, float y, float w,
                           int mouseX, int mouseY) {
        if (row.kind == OptionRow.Kind.NAV) {
            return; // real widget
        }
        boolean hover = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY < y + rowHeight(row);
        if (hover) {
            Render2D.fillRounded(g, x - Theme.px(6), y, w + Theme.px(12), rowHeight(row) - Theme.px(3),
                    Theme.px(4), ColorUtil.withAlpha(Theme.BUTTON_HOVER, 120));
        }
        Component label = labels.computeIfAbsent(row, this::buildLabel);
        Fonts.drawScaled(g, label, x, y + Theme.px(10), 0.95F, Theme.TEXT_PRIMARY, false);

        switch (row.kind) {
            case TOGGLE -> drawToggleValue(g, x, y, w, booleanValue(row));
            case CUSTOM_TOGGLE -> drawToggleValue(g, x, y, w, row.customGetter.getAsBoolean());
            case SLIDER -> drawSlider(g, row, x, y, w);
            case CYCLE -> drawCycleChip(g, row, x, y, w);
            default -> {
            }
        }
    }

    private Component buildLabel(OptionRow row) {
        if (row.option != null) {
            return row.option.caption.copy().withStyle(Fonts.ui("").getStyle());
        }
        return Fonts.ui(row.label);
    }

    @SuppressWarnings("unchecked")
    private boolean booleanValue(OptionRow row) {
        return (Boolean) ((OptionInstance<Object>) row.option).get();
    }

    private void drawToggleValue(GuiGraphics g, float x, float y, float w, boolean on) {
        float th = Theme.px(18);
        float tw = Theme.px(32);
        float ty = y + (Theme.px(32) - Theme.px(3) - th) / 2;
        float r = th / 2;
        Render2D.fillRounded(g, x + w - tw, ty, tw, th, r, on ? Theme.accent() : Theme.BUTTON_HOVER);
        float knobX = on ? x + w - r : x + w - tw + r;
        Render2D.fillCircle(g, knobX, ty + r, r - Theme.px(2.5F),
                on ? 0xFF0E0E10 : Theme.TEXT_SECONDARY);
    }

    private void drawSlider(GuiGraphics g, OptionRow row, float x, float y, float w) {
        float valueZone = Theme.px(92);
        float trackW = w * 0.30F;
        float trackX = x + w - trackW - valueZone;
        float trackY = y + Theme.px(13);
        float t = sliderPosition(row);
        Render2D.fillRounded(g, trackX, trackY, trackW, Theme.px(4), Theme.px(2), Theme.BUTTON_HOVER);
        if (trackW * t >= Theme.px(2)) {
            Render2D.fillRounded(g, trackX, trackY, trackW * t, Theme.px(4), Theme.px(2), Theme.accent());
        }
        Render2D.fillCircle(g, trackX + trackW * t, trackY + Theme.px(2), Theme.px(6), Theme.accent());
        Component value = valueComponent(row);
        // Right-aligned so long value texts ("12 chunks") never clip.
        Fonts.drawScaled(g, value, x + w - Fonts.width(value) * 0.85F, y + Theme.px(10), 0.85F,
                Theme.TEXT_SECONDARY, false);
    }

    private void drawCycleChip(GuiGraphics g, OptionRow row, float x, float y, float w) {
        Component value = valueComponent(row);
        float chipW = Fonts.width(value) * 0.9F + Theme.px(18);
        float chipX = x + w - chipW;
        Render2D.fillRounded(g, chipX, y + Theme.px(5), chipW, Theme.px(20), Theme.px(4), Theme.BUTTON);
        Fonts.drawScaled(g, value, chipX + Theme.px(9), y + Theme.px(11), 0.9F, Theme.accent(), false);
    }

    /**
     * The option's current display text, rebuilt only when it changes. Vanilla
     * formats values as "Caption: Value"; the caption prefix is stripped so
     * chips show just the value.
     */
    @SuppressWarnings("unchecked")
    private Component valueComponent(OptionRow row) {
        OptionInstance<Object> option = (OptionInstance<Object>) row.option;
        String full = option.toString.apply(option.get()).getString();
        String caption = option.caption.getString();
        String text = full.startsWith(caption) ? full.substring(caption.length()).replaceFirst("^: ", "") : full;
        if (text.isBlank()) {
            text = full;
        }
        String previous = valueTexts.get(row);
        if (!text.equals(previous)) {
            valueTexts.put(row, text);
            values.put(row, Fonts.ui(text));
        }
        return values.get(row);
    }

    // ------------------------------------------------------------------
    // Slider math (mirrors the public range records)
    // ------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private float sliderPosition(OptionRow row) {
        OptionInstance<Object> option = (OptionInstance<Object>) row.option;
        OptionInstance.SliderableValueSet<Object> valueSet =
                (OptionInstance.SliderableValueSet<Object>) option.values();
        // Clamp: modules (e.g. Fullbright) may hold values beyond slider range.
        return (float) Math.max(0, Math.min(1, valueSet.toSliderValue(option.get())));
    }

    @SuppressWarnings("unchecked")
    private void applySlider(OptionRow row, double mouseX, float trackX, float trackW) {
        float t = (float) Math.max(0, Math.min(1, (mouseX - trackX) / trackW));
        OptionInstance<Object> option = (OptionInstance<Object>) row.option;
        OptionInstance.SliderableValueSet<Object> valueSet =
                (OptionInstance.SliderableValueSet<Object>) option.values();
        option.set(valueSet.fromSliderValue(t));
        valueTexts.remove(row);
    }

    // ------------------------------------------------------------------
    // Input
    // ------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (my >= listTop() && my <= listBottom()) {
            float cy = listTop() - scroll;
            for (OptionRow row : rows) {
                float rh = rowHeight(row);
                if (my >= cy && my < cy + rh && mx >= rowX() - Theme.px(6)
                        && mx <= rowX() + rowWidth() + Theme.px(6)) {
                    if (rowClicked(row, mx, cy, button)) {
                        setFocused(null);
                        return true;
                    }
                    break;
                }
                cy += rh;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @SuppressWarnings("unchecked")
    private boolean rowClicked(OptionRow row, double mx, float rowY, int button) {
        float w = rowWidth();
        float x = rowX();
        switch (row.kind) {
            case TOGGLE -> {
                OptionInstance<Boolean> option = (OptionInstance<Boolean>) row.option;
                option.set(!option.get());
                return true;
            }
            case CUSTOM_TOGGLE -> {
                row.customSetter.accept(!row.customGetter.getAsBoolean());
                return true;
            }
            case SLIDER -> {
                float trackW = w * 0.30F;
                float trackX = x + w - trackW - Theme.px(92);
                if (mx >= trackX - Theme.px(8) && mx <= trackX + trackW + Theme.px(8)) {
                    draggingSlider = row;
                    applySlider(row, mx, trackX, trackW);
                    return true;
                }
                return false;
            }
            case CYCLE -> {
                List<Object> candidates = row.cycleValues();
                if (candidates.isEmpty()) {
                    return false;
                }
                OptionInstance<Object> option = (OptionInstance<Object>) row.option;
                int index = candidates.indexOf(option.get());
                int step = button == GLFW.GLFW_MOUSE_BUTTON_RIGHT ? -1 : 1;
                option.set(candidates.get(Math.floorMod(index + step, candidates.size())));
                valueTexts.remove(row);
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (draggingSlider != null) {
            float w = rowWidth();
            float trackW = w * 0.30F;
            float trackX = rowX() + w - trackW - Theme.px(92);
            applySlider(draggingSlider, mx, trackX, trackW);
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        draggingSlider = null;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        scroll -= (float) dy * Theme.px(32);
        clampScroll();
        return true;
    }

    private void clampScroll() {
        float max = Math.max(0, contentHeight() - (listBottom() - listTop()));
        scroll = Math.max(0, Math.min(scroll, max));
    }

    @Override
    public void removed() {
        super.removed();
        // Same guarantee as the vanilla options screens.
        this.minecraft.options.save();
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
