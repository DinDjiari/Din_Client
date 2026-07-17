package gg.dindijari.client.gui.screen;

import gg.dindijari.client.core.ClientSounds;
import gg.dindijari.client.core.Services;
import gg.dindijari.client.crash.CrashRecord;
import gg.dindijari.client.crash.CrashWatcher;
import gg.dindijari.client.crash.OllamaClient;
import gg.dindijari.client.gui.notify.Notifications;
import gg.dindijari.client.gui.widget.ThemedButton;
import gg.dindijari.client.module.modules.client.CrashAssistantModule;
import gg.dindijari.client.render.Fonts;
import gg.dindijari.client.render.Render2D;
import gg.dindijari.client.render.Theme;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * The themed crash-report screen, shown on the first main menu after a crash
 * was recorded by the previous session (a hard crash kills the render
 * pipeline, so nothing can be shown at crash time): a dark panel with an
 * accent header, the scrollable monospace report/stack trace, and actions to
 * copy the report, open the {@code crash-reports} folder, run the local AI
 * analysis, or dismiss.
 *
 * <p>The analysis talks to the user's own Ollama install (URL/model from the
 * Crash Assistant settings) via {@link OllamaClient}: fully asynchronous with
 * a loading state, and honest errors — if Ollama is unreachable the screen
 * says so instead of faking an answer. Nothing leaves the machine.
 */
public final class CrashReportScreen extends ThemedScreen {

    private static final Logger LOGGER = LoggerFactory.getLogger("dindijariclient/crash");
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Screen parentScreen;
    private final CrashRecord record;
    private final String reportText;
    private final Component heading = Fonts.ui("Crash Report");
    private final Component subtitle;

    /** Wrapped monospace report lines, rebuilt in init() for the current width. */
    private final List<FormattedCharSequence> reportLines = new ArrayList<>();
    /** Wrapped analysis lines (UI font), rebuilt when the answer arrives. */
    private final List<FormattedCharSequence> analysisLines = new ArrayList<>();

    private ThemedButton analyzeButton;
    private boolean analyzing;
    private boolean analysisFailed;
    private String analysisText;
    private boolean showAnalysis;
    private float scroll;

    /**
     * Creates the crash-report screen.
     *
     * @param parent the screen to return to on dismiss
     * @param record the crash recorded by the previous session
     */
    public CrashReportScreen(Screen parent, CrashRecord record) {
        super(Component.literal("Crash Report"));
        this.parentScreen = parent;
        this.record = record;
        this.reportText = CrashWatcher.loadReportText(record);

        String when = TIME_FORMAT.format(Instant.ofEpochMilli(record.timestamp)
                .atZone(ZoneId.systemDefault()));
        String source = record.reportFile != null
                ? Path.of(record.reportFile).getFileName().toString()
                : (record.summary == null ? "uncaught exception" : record.summary);
        this.subtitle = Fonts.ui("The game crashed on " + when + " · " + shorten(source, 44));
        ClientSounds.dialogOpen();
    }

    private static String shorten(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max - 1) + "…";
    }

    @Override
    protected void init() {
        reportLines.clear();
        int wrapWidth = Math.round(contentWidth() - Theme.px(16));
        for (String line : reportText.split("\n", -1)) {
            if (line.isEmpty()) {
                reportLines.add(FormattedCharSequence.EMPTY);
            } else {
                reportLines.addAll(this.font.split(Fonts.mono(line), wrapWidth));
            }
        }
        if (analysisText != null) {
            rebuildAnalysisLines();
        }

        int bw = Math.round(Theme.px(150));
        int bh = Math.round(Theme.px(28));
        int gap = Math.round(Theme.px(8));
        int total = 4 * bw + 3 * gap;
        int x = (this.width - total) / 2;
        int by = this.height - bh - Math.round(Theme.px(14));

        addRenderableWidget(new ThemedButton(x, by, bw, bh, "Copy report", () -> {
            this.minecraft.keyboardHandler.setClipboard(reportText);
            Notifications.info("Crash report copied to clipboard.");
        }));
        addRenderableWidget(new ThemedButton(x + bw + gap, by, bw, bh, "Open folder", () -> {
            Path dir = CrashWatcher.crashReportsDir();
            if (dir != null && Files.isDirectory(dir)) {
                Util.getPlatform().openFile(dir.toFile());
            } else {
                Notifications.error("No crash-reports folder found.");
            }
        }));
        analyzeButton = new ThemedButton(x + 2 * (bw + gap), by, bw, bh,
                analyzeLabel(), true, this::onAnalyzePressed);
        addRenderableWidget(analyzeButton);
        addRenderableWidget(new ThemedButton(x + 3 * (bw + gap), by, bw, bh, "Dismiss",
                this::onClose));
    }

    private String analyzeLabel() {
        if (analyzing) {
            return "Analyzing…";
        }
        if (analysisText != null) {
            return showAnalysis ? "Show report" : "Show analysis";
        }
        return "Analyze with AI";
    }

    private void onAnalyzePressed() {
        if (analyzing) {
            return;
        }
        if (analysisText != null) {
            // Toggle between the report and the (already fetched) analysis.
            showAnalysis = !showAnalysis;
            scroll = 0;
            analyzeButton.setLabel(analyzeLabel());
            return;
        }
        CrashAssistantModule assistant =
                (CrashAssistantModule) Services.modules().getModule("Crash Assistant");
        String url = assistant.effectiveUrl();
        String model = assistant.effectiveModel();
        analyzing = true;
        showAnalysis = true;
        scroll = 0;
        analyzeButton.setLabel(analyzeLabel());

        new OllamaClient().analyze(url, model, reportText).whenComplete((answer, error) ->
                this.minecraft.execute(() -> {
                    analyzing = false;
                    if (error != null) {
                        Throwable cause = error instanceof java.util.concurrent.CompletionException
                                && error.getCause() != null ? error.getCause() : error;
                        String message = cause instanceof OllamaClient.OllamaException
                                ? cause.getMessage()
                                : "Analyse fehlgeschlagen: " + cause.getClass().getSimpleName();
                        LOGGER.info("Ollama analysis failed: {}", message);
                        analysisFailed = true;
                        analysisText = message + "\n\nDie Analyse läuft vollständig lokal über "
                                + "deine eigene Ollama-Installation (" + url + ", Modell \""
                                + model + "\"). URL und Modell lassen sich in den Client "
                                + "Settings unter \"Crash Assistant\" ändern.";
                        Notifications.error("AI analysis failed — see the crash screen.");
                    } else {
                        analysisFailed = false;
                        analysisText = answer;
                        Notifications.info("AI analysis ready.");
                    }
                    rebuildAnalysisLines();
                    analyzeButton.setLabel(analyzeLabel());
                }));
    }

    private void rebuildAnalysisLines() {
        analysisLines.clear();
        if (analysisText == null) {
            return;
        }
        int wrapWidth = Math.round(contentWidth() - Theme.px(16));
        for (String line : analysisText.split("\n", -1)) {
            if (line.isEmpty()) {
                analysisLines.add(FormattedCharSequence.EMPTY);
            } else {
                analysisLines.addAll(this.font.split(Fonts.ui(line), wrapWidth));
            }
        }
    }

    private float contentX() {
        return Math.max(Theme.px(24), (this.width - Theme.px(640)) / 2.0F);
    }

    private float contentWidth() {
        return this.width - 2 * contentX();
    }

    private float contentTop() {
        return Theme.px(66);
    }

    private float contentBottom() {
        return this.height - Theme.px(52);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);

        // Accent header.
        Fonts.drawCentered(g, heading, this.width / 2.0F, Theme.px(18), 1.2F,
                Theme.accent(), false);
        Fonts.drawCentered(g, subtitle, this.width / 2.0F, Theme.px(38), 0.85F,
                Theme.TEXT_SECONDARY, false);

        float x = contentX();
        float w = contentWidth();
        float top = contentTop();
        float bottom = contentBottom();
        float radius = Theme.px(Theme.PANEL_RADIUS);
        Render2D.dropShadow(g, x, top, w, bottom - top, radius, Theme.px(10), Theme.SHADOW);
        Render2D.fillRounded(g, x, top, w, bottom - top, radius, Theme.PANEL);
        Render2D.rgbLine(g, x + radius, top, w - 2 * radius, Theme.px(3));

        List<FormattedCharSequence> lines = showAnalysis ? analysisLines : reportLines;
        float lineHeight = 10;
        float textTop = top + Theme.px(12);
        float textBottom = bottom - Theme.px(8);

        if (showAnalysis && analyzing) {
            int dots = (int) ((System.currentTimeMillis() / 400) % 4);
            Fonts.drawCentered(g, Fonts.ui("Analysiere lokal mit Ollama" + ".".repeat(dots)),
                    this.width / 2.0F, (top + bottom) / 2.0F - 5, 0.95F, Theme.TEXT_SECONDARY,
                    false);
        } else {
            g.enableScissor((int) Math.floor(x), (int) Math.floor(textTop),
                    (int) Math.ceil(x + w), (int) Math.ceil(textBottom));
            float cy = textTop - scroll;
            int color = showAnalysis && analysisFailed ? 0xFFFFAA66 : Theme.TEXT_PRIMARY;
            for (FormattedCharSequence line : lines) {
                if (cy + lineHeight >= textTop && cy <= textBottom) {
                    g.drawString(this.font, line, Math.round(x + Theme.px(8)), Math.round(cy),
                            color, false);
                }
                cy += lineHeight;
            }
            g.disableScissor();
        }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        List<FormattedCharSequence> lines = showAnalysis ? analysisLines : reportLines;
        float visible = contentBottom() - Theme.px(8) - (contentTop() + Theme.px(12));
        float maxScroll = Math.max(0, lines.size() * 10 - visible);
        scroll = Math.max(0, Math.min(maxScroll, scroll - (float) scrollY * 30));
        return true;
    }

    @Override
    public void onClose() {
        if (isClosing()) {
            return;
        }
        animateClose(() -> this.minecraft.setScreen(parentScreen));
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}
