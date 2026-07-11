package gg.dindijari.client.gui.widget;

import gg.dindijari.client.render.ColorUtil;
import gg.dindijari.client.render.Render2D;
import gg.dindijari.client.render.Theme;
import gg.dindijari.client.util.animation.Animation;
import gg.dindijari.client.util.animation.Easing;
import net.minecraft.client.gui.GuiGraphics;

/**
 * A reusable vertical scroll container for immediate-mode content. A caller:
 *
 * <pre>
 *   float top = panel.begin(g, x, y, w, h, contentHeight);
 *   // draw content starting at world-Y = top (already offset by the scroll)
 *   panel.end(g);           // ends the clip, draws the scrollbar
 * </pre>
 *
 * <p>Content is clipped to the viewport with a scissor rectangle so nothing
 * bleeds outside. The scroll offset animates smoothly (eased) towards the wheel
 * target, is clamped to {@code [0, contentHeight - viewportHeight]} (no
 * over-scroll), and the thin rounded scrollbar (a {@link Theme#BUTTON_HOVER}
 * track with an accent thumb) auto-hides when the content fits. Reset the
 * position with {@link #reset()} when reopening a view.
 */
public final class ScrollPanel {

    private static final float WHEEL_STEP = 28.0F;

    private final Animation offset = new Animation(0.0, 200, Easing.CUBIC_OUT);
    private float target;

    private float vpX;
    private float vpY;
    private float vpW;
    private float vpH;
    private float contentH;

    /**
     * Begins a scrolled, clipped region and returns the world-space Y at which
     * content should start drawing (already offset by the current scroll).
     *
     * @param g             the draw context
     * @param x             viewport left edge (GUI units)
     * @param y             viewport top edge
     * @param w             viewport width
     * @param h             viewport height
     * @param contentHeight total height of the content to be drawn
     * @return the world-space top Y for content (i.e. {@code y - scroll})
     */
    public float begin(GuiGraphics g, float x, float y, float w, float h, float contentHeight) {
        this.vpX = x;
        this.vpY = y;
        this.vpW = w;
        this.vpH = h;
        this.contentH = contentHeight;
        clampTarget();
        offset.animateTo(target);
        g.enableScissor((int) Math.floor(x), (int) Math.floor(y),
                (int) Math.ceil(x + w), (int) Math.ceil(y + h));
        return y - offset.valueF();
    }

    /**
     * Ends the clipped region and draws the scrollbar (when needed).
     *
     * @param g the draw context
     */
    public void end(GuiGraphics g) {
        g.disableScissor();
        if (contentH <= vpH) {
            return;
        }
        float trackW = Theme.px(4);
        float trackX = vpX + vpW - trackW;
        Render2D.fillRounded(g, trackX, vpY, trackW, vpH, trackW / 2, Theme.BUTTON_HOVER);
        float thumbH = Math.max(Theme.px(16), vpH * (vpH / contentH));
        float travel = vpH - thumbH;
        float t = maxScroll() <= 0 ? 0 : offset.valueF() / maxScroll();
        float thumbY = vpY + travel * t;
        Render2D.fillRounded(g, trackX, thumbY, trackW, thumbH, trackW / 2, Theme.accent());
    }

    /**
     * Handles a mouse-wheel event, scrolling only when the cursor is over this
     * viewport.
     *
     * @param mx    mouse x
     * @param my    mouse y
     * @param delta wheel delta (positive = up)
     * @return {@code true} if the event was consumed
     */
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (mx < vpX || mx > vpX + vpW || my < vpY || my > vpY + vpH || contentH <= vpH) {
            return false;
        }
        target -= (float) delta * WHEEL_STEP;
        clampTarget();
        return true;
    }

    /**
     * The current animated scroll offset (subtract from content Y).
     *
     * @return the scroll offset in GUI units
     */
    public float scroll() {
        return offset.valueF();
    }

    /**
     * Resets the scroll position to the top (used when reopening a view).
     */
    public void reset() {
        target = 0;
        offset.snapTo(0);
    }

    private float maxScroll() {
        return Math.max(0, contentH - vpH);
    }

    private void clampTarget() {
        target = Math.max(0, Math.min(target, maxScroll()));
    }
}
