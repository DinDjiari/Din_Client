package gg.dindijari.client.gui.notify;

import gg.dindijari.client.core.ClientSounds;
import gg.dindijari.client.render.Render2D;
import gg.dindijari.client.render.Theme;
import gg.dindijari.client.util.animation.Animation;
import gg.dindijari.client.util.animation.Easing;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * The client's toast notifications: small themed cards in the top-right corner
 * that slide in, hold for a few seconds and slide back out (sliding is skipped
 * when UI animations are off). Info toasts carry the accent edge and the pop
 * sound; error toasts a red edge and the error buzz.
 *
 * <p>Toasts render both over the in-game HUD and over any open screen, and are
 * purely visual — they never consume input. Thread-safe to post from worker
 * threads.
 */
public final class Notifications {

    /** How long a toast stays fully visible. */
    private static final long HOLD_MS = 4000L;
    /** Slide in/out duration. */
    private static final long SLIDE_MS = 220L;
    /** Red edge/tint for error toasts. */
    private static final int ERROR_COLOR = 0xFFFF5555;

    private static final List<Toast> TOASTS = new ArrayList<>();

    private Notifications() {
    }

    /**
     * Subscribes the render listeners. Called once during mod construction.
     */
    public static void registerEvents() {
        NeoForge.EVENT_BUS.addListener((RenderGuiEvent.Post event) -> {
            if (Minecraft.getInstance().screen == null) {
                render(event.getGuiGraphics());
            }
        });
        NeoForge.EVENT_BUS.addListener((ScreenEvent.Render.Post event) ->
                render(event.getGuiGraphics()));
    }

    /**
     * Shows an informational toast (accent edge, pop sound).
     *
     * @param message the message text; wrapped to the toast width
     */
    public static void info(String message) {
        post(message, false);
    }

    /**
     * Shows an error toast (red edge, error buzz).
     *
     * @param message the message text; wrapped to the toast width
     */
    public static void error(String message) {
        post(message, true);
    }

    private static void post(String message, boolean error) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            synchronized (TOASTS) {
                TOASTS.add(new Toast(message, error));
                while (TOASTS.size() > 5) {
                    TOASTS.remove(0);
                }
            }
            if (error) {
                ClientSounds.error();
            } else {
                ClientSounds.notifyPop();
            }
        });
    }

    private static void render(GuiGraphics g) {
        synchronized (TOASTS) {
            if (TOASTS.isEmpty()) {
                return;
            }
            int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
            float margin = Theme.px(10);
            float y = margin;
            long now = System.currentTimeMillis();
            for (Iterator<Toast> it = TOASTS.iterator(); it.hasNext(); ) {
                Toast toast = it.next();
                if (toast.expired(now)) {
                    it.remove();
                    continue;
                }
                y += toast.render(g, screenWidth, y, now) + Theme.px(6);
            }
        }
    }

    /** One live toast card. */
    private static final class Toast {

        private final List<FormattedCharSequence> lines;
        private final boolean error;
        private final long createdAt;
        private final Animation slide;
        private boolean leaving;

        Toast(String message, boolean error) {
            this.error = error;
            this.createdAt = System.currentTimeMillis();
            float textWidth = width() - Theme.px(24);
            this.lines = Minecraft.getInstance().font.split(
                    gg.dindijari.client.render.Fonts.ui(message), Math.max(40, Math.round(textWidth)));
            this.slide = new Animation(0.0, SLIDE_MS, Easing.CUBIC_OUT);
            if (Theme.animationsEnabled()) {
                this.slide.animateTo(1.0);
            } else {
                this.slide.snapTo(1.0);
            }
        }

        private float width() {
            return Theme.px(230);
        }

        private float height() {
            return Theme.px(20) + lines.size() * 10;
        }

        boolean expired(long now) {
            return leaving && slide.isDone() && slide.target() == 0.0;
        }

        float render(GuiGraphics g, int screenWidth, float y, long now) {
            if (!leaving && now - createdAt > HOLD_MS) {
                leaving = true;
                if (Theme.animationsEnabled()) {
                    slide.animateTo(0.0);
                } else {
                    slide.snapTo(0.0);
                }
            }
            float t = slide.valueF();
            float w = width();
            float h = height();
            float margin = Theme.px(10);
            // Slide in from beyond the right edge.
            float x = screenWidth - margin - w + (1.0F - t) * (w + margin * 2);

            int edge = error ? ERROR_COLOR : Theme.accent();
            float radius = Theme.px(6);
            Render2D.dropShadow(g, x, y, w, h, radius, Theme.px(8), Theme.SHADOW);
            Render2D.fillRounded(g, x, y, w, h, radius, Theme.PANEL);
            // Accent/error edge on the left.
            Render2D.fillRounded(g, x, y, Theme.px(3), h, radius, 0, 0, radius, edge);

            float textX = x + Theme.px(12);
            float textY = y + Theme.px(10) - 1;
            for (FormattedCharSequence line : lines) {
                g.drawString(Minecraft.getInstance().font, line,
                        Math.round(textX), Math.round(textY), Theme.TEXT_PRIMARY, false);
                textY += 10;
            }
            return h;
        }
    }
}
