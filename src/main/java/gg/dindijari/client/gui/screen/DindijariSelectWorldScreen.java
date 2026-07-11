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
import net.minecraft.world.level.storage.LevelStorageException;
import net.minecraft.world.level.storage.LevelSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The client's Singleplayer screen, replacing the vanilla world-selection
 * screen (swapped in by {@link gg.dindijari.client.gui.ScreenManager}) and
 * matching the design reference: header with accent underline, a search field
 * top-right, worlds as rounded cards (icon square, name, "mode · version ·
 * last played" meta line), the selected card outlined in accent with a
 * chevron, and a bottom row of <em>Play Selected</em> (accent) /
 * <em>Create New World</em> / <em>Back</em>.
 *
 * <p>World data comes from the vanilla level source; playing and world
 * creation delegate to the vanilla open flows, so behaviour is identical —
 * only the presentation is replaced. Double-clicking a card plays it.
 */
public final class DindijariSelectWorldScreen extends ThemedScreen {

    private static final Logger LOGGER = LoggerFactory.getLogger("dindijariclient/worlds");

    private final Screen parent;
    private final Component header = Fonts.ui("Singleplayer");
    private final Component loadingLabel = Fonts.ui("Loading worlds...");
    private final Component emptyLabel = Fonts.ui("No worlds yet — create one!");
    private final Map<String, Component> nameCache = new HashMap<>();
    private final Map<String, Component> metaCache = new HashMap<>();
    private final Map<String, Component> initialCache = new HashMap<>();

    private List<LevelSummary> worlds;
    private final List<LevelSummary> visible = new ArrayList<>();
    private String filter = "";
    private LevelSummary selected;
    private float scroll;
    private long lastClickMs;
    private LevelSummary lastClickTarget;
    private ThemedButton playButton;

    /**
     * Creates the screen.
     *
     * @param parent the screen to return to
     */
    public DindijariSelectWorldScreen(Screen parent) {
        super(Component.translatable("selectWorld.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int pad = Math.round(Theme.px(24));
        int searchW = Math.round(Theme.px(220));
        int searchH = Math.round(Theme.px(26));
        addRenderableWidget(new ThemedTextField(this.width - pad - searchW, pad,
                searchW, searchH, "Search worlds...", q -> {
            filter = q.toLowerCase(Locale.ROOT);
            refilter();
        }));

        int bh = Math.round(Theme.px(34));
        int by = this.height - bh - Math.round(Theme.px(20));
        int bx = pad;
        playButton = new ThemedButton(bx, by, Math.round(Theme.px(150)), bh,
                "Play Selected", true, this::playSelected);
        playButton.active = false;
        addRenderableWidget(playButton);
        bx += Math.round(Theme.px(150)) + Math.round(Theme.px(Theme.GRID));
        addRenderableWidget(new ThemedButton(bx, by, Math.round(Theme.px(170)), bh,
                "Create New World",
                () -> this.minecraft.setScreen(new DindijariCreateWorldScreen(this))));
        bx += Math.round(Theme.px(170)) + Math.round(Theme.px(Theme.GRID));
        addRenderableWidget(new ThemedButton(bx, by, Math.round(Theme.px(100)), bh,
                "Back", this::onClose));

        if (worlds == null) {
            loadWorlds();
        }
    }

    private void loadWorlds() {
        try {
            this.minecraft.getLevelSource()
                    .loadLevelSummaries(this.minecraft.getLevelSource().findLevelCandidates())
                    .thenAccept(list -> this.minecraft.execute(() -> {
                        worlds = list;
                        refilter();
                    }))
                    .exceptionally(e -> {
                        LOGGER.error("Failed to load level summaries", e);
                        this.minecraft.execute(() -> {
                            worlds = List.of();
                            refilter();
                        });
                        return null;
                    });
        } catch (LevelStorageException e) {
            LOGGER.error("Failed to enumerate level candidates", e);
            worlds = List.of();
            refilter();
        }
    }

    private void refilter() {
        visible.clear();
        if (worlds != null) {
            for (LevelSummary summary : worlds) {
                if (filter.isEmpty()
                        || summary.getLevelName().toLowerCase(Locale.ROOT).contains(filter)) {
                    visible.add(summary);
                }
            }
        }
        if (selected != null && !visible.contains(selected)) {
            selected = null;
        }
        if (playButton != null) {
            playButton.active = selected != null;
        }
    }

    // ------------------------------------------------------------------
    // Layout
    // ------------------------------------------------------------------

    private float listX() {
        return Theme.px(24);
    }

    private float listY() {
        return Theme.px(64);
    }

    private float listW() {
        return this.width - 2 * listX();
    }

    private float listH() {
        return this.height - listY() - Theme.px(66);
    }

    private float cardH() {
        return Theme.px(52);
    }

    private float cardStride() {
        return cardH() + Theme.px(Theme.GRID);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);

        float pad = Theme.px(24);
        Fonts.drawScaled(g, header, pad, pad, 1.5F, Theme.TEXT_PRIMARY, false);
        float headerW = Fonts.width(header) * 1.5F;
        Render2D.fillRounded(g, pad, Theme.snap(pad + 9 * 1.5F + Theme.px(5)),
                headerW * 0.6F, Theme.px(3), Theme.px(1.5F), Theme.accent());

        if (worlds == null) {
            Fonts.drawCentered(g, loadingLabel, this.width / 2.0F, this.height / 2.0F,
                    1.0F, Theme.TEXT_SECONDARY, false);
            return;
        }
        if (visible.isEmpty()) {
            Fonts.drawCentered(g, emptyLabel, this.width / 2.0F, this.height / 2.0F,
                    1.0F, Theme.TEXT_SECONDARY, false);
            return;
        }

        clampScroll();
        float x = listX();
        float w = listW();
        float yTop = listY();
        float yBottom = yTop + listH();
        float cy = yTop - scroll;

        g.enableScissor((int) x - 2, (int) yTop - 2, (int) (x + w) + 2, (int) yBottom + 2);
        for (LevelSummary summary : visible) {
            if (cy + cardH() >= yTop && cy <= yBottom) {
                renderCard(g, summary, x, cy, w, mouseX, mouseY);
            }
            cy += cardStride();
        }
        g.disableScissor();
    }

    private void renderCard(GuiGraphics g, LevelSummary summary, float x, float y, float w,
                            int mouseX, int mouseY) {
        boolean hover = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + cardH();
        boolean isSelected = summary == selected;
        float radius = Theme.px(Theme.PANEL_RADIUS);
        int fill = hover || isSelected ? Theme.BUTTON : ColorUtil.withAlpha(Theme.BUTTON, 170);
        Render2D.fillRounded(g, x, y, w, cardH(), radius, fill);
        if (isSelected) {
            Render2D.outlineRounded(g, x, y, w, cardH(), radius, 1.2F, Theme.accent());
        }

        // Icon square with the world's initial.
        float iconSize = Theme.px(36);
        float iconX = x + Theme.px(8);
        float iconY = y + (cardH() - iconSize) / 2;
        Render2D.fillRounded(g, iconX, iconY, iconSize, iconSize, Theme.px(6), 0xFF0E0E10);
        String id = summary.getLevelId();
        Component initial = initialCache.computeIfAbsent(id, k -> Fonts.ui(
                summary.getLevelName().isEmpty() ? "?"
                        : summary.getLevelName().substring(0, 1).toUpperCase(Locale.ROOT)));
        Fonts.drawCentered(g, initial, iconX + iconSize / 2, iconY + iconSize / 2 - 4.5F,
                1.0F, Theme.accent(), false);

        float textX = iconX + iconSize + Theme.px(10);
        Component name = nameCache.computeIfAbsent(id, k -> Fonts.ui(summary.getLevelName()));
        Fonts.draw(g, name, textX, y + Theme.px(10), Theme.TEXT_PRIMARY, false);
        Component meta = metaCache.computeIfAbsent(id, k -> Fonts.ui(metaLine(summary)));
        Fonts.drawScaled(g, meta, textX, y + Theme.px(30), 0.8F, Theme.TEXT_SECONDARY, false);

        if (isSelected) {
            Fonts.draw(g, Fonts.ui("›"), x + w - Theme.px(18), y + cardH() / 2 - 4.5F,
                    Theme.accent(), false);
        }
    }

    private static String metaLine(LevelSummary summary) {
        String mode = summary.getGameMode().getName();
        mode = Character.toUpperCase(mode.charAt(0)) + mode.substring(1);
        String version = summary.levelVersion().minecraftVersionName();
        return mode + " · " + version + " · " + relativeTime(summary.getLastPlayed());
    }

    private static String relativeTime(long epochMs) {
        if (epochMs <= 0) {
            return "never played";
        }
        long minutes = (System.currentTimeMillis() - epochMs) / 60_000L;
        if (minutes < 1) {
            return "just now";
        }
        if (minutes < 60) {
            return minutes + " min ago";
        }
        long hours = minutes / 60;
        if (hours < 24) {
            return hours + (hours == 1 ? " hour ago" : " hours ago");
        }
        long days = hours / 24;
        return days + (days == 1 ? " day ago" : " days ago");
    }

    // ------------------------------------------------------------------
    // Input
    // ------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && worlds != null
                && mx >= listX() && mx <= listX() + listW()
                && my >= listY() && my <= listY() + listH()) {
            float cy = listY() - scroll;
            for (LevelSummary summary : visible) {
                if (my >= cy && my <= cy + cardH()) {
                    boolean doubleClick = summary == lastClickTarget
                            && System.currentTimeMillis() - lastClickMs < 400;
                    lastClickMs = System.currentTimeMillis();
                    lastClickTarget = summary;
                    selected = summary;
                    playButton.active = true;
                    setFocused(null);
                    if (doubleClick) {
                        playSelected();
                    }
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
        float content = visible.size() * cardStride();
        float max = Math.max(0, content - listH());
        scroll = Math.max(0, Math.min(scroll, max));
    }

    private void playSelected() {
        if (selected == null) {
            return;
        }
        LevelSummary target = selected;
        // Vanilla open flow: identical load/error behaviour, themed shell only.
        this.minecraft.createWorldOpenFlows().openWorld(target.getLevelId(),
                () -> this.minecraft.setScreen(this));
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
