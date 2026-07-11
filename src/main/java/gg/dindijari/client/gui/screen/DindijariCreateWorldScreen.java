package gg.dindijari.client.gui.screen;

import gg.dindijari.client.gui.widget.ThemedButton;
import gg.dindijari.client.gui.widget.ThemedTextField;
import gg.dindijari.client.gui.widget.ThemedToggle;
import gg.dindijari.client.render.Fonts;
import gg.dindijari.client.render.Render2D;
import gg.dindijari.client.render.Theme;
import net.minecraft.FileUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Locale;
import java.util.OptionalLong;

/**
 * The client's Create New World screen, replacing the vanilla one (swapped in
 * by {@link gg.dindijari.client.gui.ScreenManager}) in the client's design
 * language: a centred dark panel with a world-name field, game-mode and
 * difficulty cyclers, a collapsible <em>Advanced</em> section (seed input,
 * Generate Structures and Bonus Chest toggles), the accent
 * <em>Create World</em> primary button and a flat <em>Back</em>.
 *
 * <p>World creation goes through the vanilla
 * {@code WorldOpenFlows.createFreshLevel} pipeline with a normal-preset
 * overworld, so the result is exactly a vanilla default world. Advanced
 * vanilla options not in the design (world type presets, gamerules, data
 * packs, experiments) are intentionally not exposed here — see
 * docs/LIMITATIONS.md.
 */
public final class DindijariCreateWorldScreen extends ThemedScreen {

    private static final Logger LOGGER = LoggerFactory.getLogger("dindijariclient/createworld");

    /** The game-mode choices offered, mirroring the vanilla create-world cycler. */
    private enum Mode {
        SURVIVAL("Survival", GameType.SURVIVAL, false),
        CREATIVE("Creative", GameType.CREATIVE, false),
        HARDCORE("Hardcore", GameType.SURVIVAL, true);

        final String label;
        final GameType gameType;
        final boolean hardcore;

        Mode(String label, GameType gameType, boolean hardcore) {
            this.label = label;
            this.gameType = gameType;
            this.hardcore = hardcore;
        }
    }

    private final Screen parent;
    private final Component header = Fonts.ui("Create New World");
    private final Component nameLabel = Fonts.ui("World Name");
    private final Component seedLabel = Fonts.ui("Seed (leave blank for random)");
    private final Component structuresLabel = Fonts.ui("Generate Structures");
    private final Component bonusChestLabel = Fonts.ui("Bonus Chest");

    // Selections survive the Advanced-section re-init.
    private Mode mode = Mode.SURVIVAL;
    private Difficulty difficulty = Difficulty.NORMAL;
    private boolean advancedOpen;
    private boolean generateStructures = true;
    private boolean bonusChest;
    private String nameText = "";
    private String seedText = "";

    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;

    /**
     * Creates the screen.
     *
     * @param parent the screen to return to
     */
    public DindijariCreateWorldScreen(Screen parent) {
        super(Component.translatable("selectWorld.create"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int pad = Math.round(Theme.px(Theme.PANEL_PADDING));
        int grid = Math.round(Theme.px(Theme.GRID));
        int fieldH = Math.round(Theme.px(30));
        int rowH = fieldH + grid;

        panelW = Math.min(Math.round(Theme.px(420)), this.width - 4 * grid);
        int innerW = panelW - 2 * pad;

        // Height: header + name label/field + two cyclers + advanced toggle
        // (+ advanced rows) + buttons.
        int rows = 4;
        int advancedH = advancedOpen ? Math.round(Theme.px(14)) + rowH + 2 * rowH : 0;
        panelH = Math.round(Theme.px(48)) + Math.round(Theme.px(14)) + rows * rowH
                + advancedH + Math.round(Theme.px(44)) + pad;
        panelX = (this.width - panelW) / 2;
        panelY = Math.max(Math.round(Theme.px(16)), (this.height - panelH) / 2);

        int x = panelX + pad;
        int y = panelY + Math.round(Theme.px(48));

        y += Math.round(Theme.px(14)); // name label drawn in render
        ThemedTextField nameField = new ThemedTextField(x, y, innerW, fieldH,
                "New World", value -> nameText = value);
        nameField.setValue(nameText);
        addRenderableWidget(nameField);
        y += rowH;

        // Cycler buttons update their own label via a one-element array so the
        // press action can reference the button being constructed.
        ThemedButton[] modeRef = new ThemedButton[1];
        modeRef[0] = new ThemedButton(x, y, innerW, fieldH, "Game Mode: " + mode.label, () -> {
            mode = Mode.values()[(mode.ordinal() + 1) % Mode.values().length];
            modeRef[0].setLabel("Game Mode: " + mode.label);
        });
        addRenderableWidget(modeRef[0]);
        y += rowH;

        ThemedButton[] diffRef = new ThemedButton[1];
        diffRef[0] = new ThemedButton(x, y, innerW, fieldH,
                "Difficulty: " + prettyName(difficulty.name()), () -> {
            difficulty = Difficulty.values()[(difficulty.ordinal() + 1) % Difficulty.values().length];
            diffRef[0].setLabel("Difficulty: " + prettyName(difficulty.name()));
        });
        addRenderableWidget(diffRef[0]);
        y += rowH;

        addRenderableWidget(new ThemedButton(x, y, innerW, fieldH,
                advancedOpen ? "Hide Advanced" : "Show Advanced", () -> {
            advancedOpen = !advancedOpen;
            rebuildWidgets();
        }));
        y += rowH;

        if (advancedOpen) {
            y += Math.round(Theme.px(14)); // seed label drawn in render
            ThemedTextField seedField = new ThemedTextField(x, y, innerW, fieldH,
                    "Random", value -> seedText = value);
            seedField.setValue(seedText);
            addRenderableWidget(seedField);
            y += rowH;

            int toggleW = Math.round(Theme.px(40));
            int toggleH = Math.round(Theme.px(22));
            addRenderableWidget(new ThemedToggle(x + innerW - toggleW, y + (fieldH - toggleH) / 2,
                    toggleW, toggleH, Fonts.ui("Generate Structures"),
                    () -> generateStructures, v -> generateStructures = v));
            y += rowH;
            addRenderableWidget(new ThemedToggle(x + innerW - toggleW, y + (fieldH - toggleH) / 2,
                    toggleW, toggleH, Fonts.ui("Bonus Chest"),
                    () -> bonusChest, v -> bonusChest = v));
            y += rowH;
        }

        int btnH = Math.round(Theme.px(34));
        int btnY = panelY + panelH - btnH - pad / 2;
        int createW = Math.round(Theme.px(180));
        int backW = Math.round(Theme.px(100));
        int total = createW + grid + backW;
        addRenderableWidget(new ThemedButton(panelX + (panelW - total) / 2, btnY,
                createW, btnH, "Create World", true, this::createWorld));
        addRenderableWidget(new ThemedButton(panelX + (panelW - total) / 2 + createW + grid, btnY,
                backW, btnH, "Back", this::onClose));
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);

        float radius = Theme.px(Theme.PANEL_RADIUS);
        float pad = Theme.px(Theme.PANEL_PADDING);
        Render2D.dropShadow(g, panelX, panelY, panelW, panelH, radius, Theme.px(12), Theme.SHADOW);
        Render2D.fillRounded(g, panelX, panelY, panelW, panelH, radius, Theme.PANEL);

        float headerX = panelX + pad;
        float headerY = panelY + pad * 0.75F;
        Fonts.drawScaled(g, header, headerX, headerY, 1.4F, Theme.TEXT_PRIMARY, false);
        Render2D.fillRounded(g, headerX, Theme.snap(headerY + 9 * 1.4F + Theme.px(4)),
                Fonts.width(header) * 1.4F * 0.6F, Theme.px(3), Theme.px(1.5F), Theme.accent());

        // Field labels (small, secondary) above their inputs.
        float labelY = panelY + Theme.px(48) + Theme.px(2);
        Fonts.drawScaled(g, nameLabel, headerX, labelY, 0.8F, Theme.TEXT_SECONDARY, false);
        if (advancedOpen) {
            float rowH = Theme.px(30) + Theme.px(Theme.GRID);
            float seedLabelY = panelY + Theme.px(48) + Theme.px(14) + 4 * rowH + Theme.px(2);
            Fonts.drawScaled(g, seedLabel, headerX, seedLabelY, 0.8F, Theme.TEXT_SECONDARY, false);
            float toggleRow1 = seedLabelY + Theme.px(12) + rowH;
            Fonts.drawScaled(g, structuresLabel, headerX,
                    toggleRow1 + Theme.px(9), 0.9F, Theme.TEXT_PRIMARY, false);
            Fonts.drawScaled(g, bonusChestLabel, headerX,
                    toggleRow1 + rowH + Theme.px(9), 0.9F, Theme.TEXT_PRIMARY, false);
        }
    }

    /**
     * Creates the world through the vanilla open flow: normal world preset,
     * the selected mode/difficulty, and the parsed (or random) seed.
     */
    private void createWorld() {
        String name = nameText.isBlank() ? "New World" : nameText.trim();
        String levelId;
        try {
            levelId = FileUtil.findAvailableName(this.minecraft.getLevelSource().getBaseDir(), name, "");
        } catch (IOException e) {
            LOGGER.error("Could not derive a folder name for '{}'", name, e);
            levelId = "World-" + System.currentTimeMillis();
        }

        LevelSettings settings = new LevelSettings(name, mode.gameType, mode.hardcore,
                difficulty, mode == Mode.CREATIVE, new GameRules(), WorldDataConfiguration.DEFAULT);
        OptionalLong parsed = WorldOptions.parseSeed(seedText.trim());
        WorldOptions options = new WorldOptions(parsed.orElse(WorldOptions.randomSeed()),
                generateStructures, bonusChest);

        this.minecraft.createWorldOpenFlows().createFreshLevel(levelId, settings, options,
                WorldPresets::createNormalWorldDimensions, this);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }

    private static String prettyName(String constant) {
        String lower = constant.toLowerCase(Locale.ROOT);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
