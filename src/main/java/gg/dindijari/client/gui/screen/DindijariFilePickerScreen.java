package gg.dindijari.client.gui.screen;

import gg.dindijari.client.core.ClientSounds;
import gg.dindijari.client.core.ClientState;
import gg.dindijari.client.gui.notify.Notifications;
import gg.dindijari.client.gui.widget.ThemedButton;
import gg.dindijari.client.render.ColorUtil;
import gg.dindijari.client.render.Fonts;
import gg.dindijari.client.render.Render2D;
import gg.dindijari.client.render.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * The client's own file picker: a themed screen listing the current folder's
 * subfolders and image files (the formats the icon converter can decode),
 * with hover-highlighted rows, mouse-wheel scrolling, an "Up" navigation
 * button and Select / Cancel actions. Clicking a folder enters it; clicking a
 * file selects it. The last browsed folder is remembered across sessions.
 *
 * <p>Built entirely from the client's widgets and render library — no AWT/
 * Swing dialogs (unsafe on macOS under {@code -XstartOnFirstThread}) and no
 * vanilla list widgets.
 */
public final class DindijariFilePickerScreen extends ThemedScreen {

    /** ClientState key remembering the last browsed folder. */
    private static final String LAST_DIR_KEY = "iconPickerDir";

    /** Extensions the icon converter can decode (STB-based). */
    private static final List<String> IMAGE_EXTENSIONS =
            List.of(".png", ".jpg", ".jpeg", ".bmp", ".tga", ".gif");

    private final Screen parentScreen;
    private final Consumer<Path> onPick;
    private final Component heading;

    private Path directory;
    private final List<Entry> entries = new ArrayList<>();
    private Component pathLabel = Component.empty();
    private Path selected;
    private float scroll;
    private ThemedButton selectButton;

    private record Entry(Path path, Component label, boolean isDirectory) {
    }

    /**
     * Creates the picker.
     *
     * @param parent the screen to return to
     * @param title  the heading (e.g. "Choose Window Icon")
     * @param onPick receives the picked image file
     */
    public DindijariFilePickerScreen(Screen parent, String title, Consumer<Path> onPick) {
        super(Component.literal(title));
        this.parentScreen = parent;
        this.onPick = onPick;
        this.heading = Fonts.ui(title);

        Path start = Path.of(ClientState.getString(LAST_DIR_KEY,
                System.getProperty("user.home", ".")));
        if (!Files.isDirectory(start)) {
            start = Path.of(System.getProperty("user.home", "."));
        }
        navigateTo(start);
    }

    private void navigateTo(Path dir) {
        try {
            directory = dir.toAbsolutePath().normalize();
            entries.clear();
            selected = null;
            scroll = 0;
            List<Entry> dirs = new ArrayList<>();
            List<Entry> files = new ArrayList<>();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
                for (Path path : stream) {
                    String name = path.getFileName().toString();
                    if (name.startsWith(".")) {
                        continue;
                    }
                    if (Files.isDirectory(path)) {
                        dirs.add(new Entry(path, Fonts.ui(name + "/"), true));
                    } else if (isImage(name)) {
                        files.add(new Entry(path, Fonts.ui(name), false));
                    }
                }
            }
            Comparator<Entry> byName = Comparator.comparing(
                    e -> e.path.getFileName().toString().toLowerCase(Locale.ROOT));
            dirs.sort(byName);
            files.sort(byName);
            entries.addAll(dirs);
            entries.addAll(files);
            pathLabel = Fonts.ui(shorten(directory.toString(), 64));
            ClientState.setString(LAST_DIR_KEY, directory.toString());
            if (selectButton != null) {
                selectButton.active = false;
            }
        } catch (IOException | SecurityException e) {
            Notifications.error("Cannot open folder: " + dir.getFileName());
        }
    }

    private static boolean isImage(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        return IMAGE_EXTENSIONS.stream().anyMatch(lower::endsWith);
    }

    private static String shorten(String text, int max) {
        return text.length() <= max ? text : "…" + text.substring(text.length() - max + 1);
    }

    @Override
    protected void init() {
        int bw = Math.round(Theme.px(110));
        int bh = Math.round(Theme.px(28));
        int gap = Math.round(Theme.px(8));
        int by = this.height - bh - Math.round(Theme.px(16));
        int cx = this.width / 2;

        addRenderableWidget(new ThemedButton(cx - bw - bw / 2 - gap, by, bw, bh, "Up", () -> {
            Path parent = directory.getParent();
            if (parent != null) {
                navigateTo(parent);
            }
        }));
        selectButton = new ThemedButton(cx - bw / 2, by, bw, bh, "Select", true, () -> {
            if (selected != null) {
                Path pick = selected;
                this.minecraft.setScreen(parentScreen);
                onPick.accept(pick);
            }
        });
        selectButton.active = selected != null;
        addRenderableWidget(selectButton);
        addRenderableWidget(new ThemedButton(cx + bw / 2 + gap, by, bw, bh, "Cancel",
                this::onClose));
    }

    private float listTop() {
        return Theme.px(64);
    }

    private float listBottom() {
        return this.height - Theme.px(56);
    }

    private float rowHeight() {
        return Theme.px(22);
    }

    private float listX() {
        return Math.max(Theme.px(24), (this.width - Theme.px(520)) / 2.0F);
    }

    private float listWidth() {
        return this.width - 2 * listX();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);

        Fonts.drawCentered(g, heading, this.width / 2.0F, Theme.px(20), 1.0F,
                Theme.TEXT_PRIMARY, false);
        Fonts.drawCentered(g, pathLabel, this.width / 2.0F, Theme.px(38), 0.85F,
                Theme.TEXT_SECONDARY, false);

        float x = listX();
        float w = listWidth();
        float top = listTop();
        float bottom = listBottom();
        Render2D.fillRounded(g, x - Theme.px(8), top - Theme.px(6), w + Theme.px(16),
                bottom - top + Theme.px(12), Theme.px(Theme.PANEL_RADIUS), Theme.PANEL);

        g.enableScissor((int) Math.floor(x - Theme.px(8)), (int) Math.floor(top),
                (int) Math.ceil(x + w + Theme.px(8)), (int) Math.ceil(bottom));
        float rh = rowHeight();
        float cy = top - scroll;
        for (Entry entry : entries) {
            if (cy + rh >= top && cy <= bottom) {
                boolean hover = mouseX >= x && mouseX <= x + w && mouseY >= cy && mouseY < cy + rh
                        && mouseY >= top && mouseY <= bottom;
                boolean isSelected = !entry.isDirectory() && entry.path().equals(selected);
                if (isSelected) {
                    Render2D.fillRounded(g, x, cy, w, rh - Theme.px(2), Theme.px(4),
                            ColorUtil.withAlpha(Theme.accent(), 60));
                } else if (hover) {
                    Render2D.fillRounded(g, x, cy, w, rh - Theme.px(2), Theme.px(4),
                            ColorUtil.withAlpha(Theme.BUTTON_HOVER, 200));
                }
                Fonts.drawScaled(g, entry.label(), x + Theme.px(8), cy + Theme.px(7), 0.9F,
                        entry.isDirectory() ? Theme.TEXT_SECONDARY : Theme.TEXT_PRIMARY, false);
            }
            cy += rh;
        }
        if (entries.isEmpty()) {
            Fonts.drawCentered(g, Fonts.ui("No folders or images here"), this.width / 2.0F,
                    top + Theme.px(16), 0.9F, Theme.TEXT_SECONDARY, false);
        }
        g.disableScissor();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) {
            return true;
        }
        float x = listX();
        float w = listWidth();
        float top = listTop();
        float bottom = listBottom();
        if (mx >= x && mx <= x + w && my >= top && my <= bottom) {
            int index = (int) Math.floor((my - top + scroll) / rowHeight());
            if (index >= 0 && index < entries.size()) {
                Entry entry = entries.get(index);
                ClientSounds.click();
                if (entry.isDirectory()) {
                    navigateTo(entry.path());
                } else {
                    selected = entry.path();
                    if (selectButton != null) {
                        selectButton.active = true;
                    }
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        float maxScroll = Math.max(0, entries.size() * rowHeight() - (listBottom() - listTop()));
        scroll = Math.max(0, Math.min(maxScroll, scroll - (float) scrollY * rowHeight() * 2));
        return true;
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parentScreen);
    }
}
