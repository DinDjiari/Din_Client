package gg.dindijari.client.gui.clickgui;

import gg.dindijari.client.render.Fonts;
import gg.dindijari.client.render.Render2D;
import gg.dindijari.client.render.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Base class for the Click GUI's floating panels: a rounded {@link Theme#PANEL}
 * card with the animated RGB gradient line along its top edge and a bold
 * header, draggable by that header. Subclasses render their body content and
 * report its height.
 */
public abstract class Panel {

    /** Current position, in GUI units (persisted per session by the screen). */
    protected float x;
    /** Current position, in GUI units (persisted per session by the screen). */
    protected float y;

    private final Component title;
    private final String key;
    private boolean dragging;
    private float dragOffX;
    private float dragOffY;

    /**
     * Creates a panel.
     *
     * @param title the header title
     * @param x     initial left edge in GUI units
     * @param y     initial top edge in GUI units
     */
    protected Panel(String title, float x, float y) {
        this.title = Fonts.ui(title);
        this.key = title;
        this.x = x;
        this.y = y;
    }

    /**
     * Stable identity for persisting this panel's position across openings.
     *
     * @return the panel's title key
     */
    public final String key() {
        return key;
    }

    /** @return the panel width in GUI units */
    public float width() {
        return Theme.px(212);
    }

    /** @return the header height in GUI units */
    protected final float headerHeight() {
        return Theme.px(34);
    }

    /** @return the total panel height (header + body) in GUI units */
    public final float height() {
        return headerHeight() + bodyHeight() + Theme.px(8);
    }

    /** @return the height of the body content in GUI units */
    protected abstract float bodyHeight();

    /**
     * Renders the panel frame and delegates to {@link #renderBody}.
     *
     * @param g      the draw context
     * @param mouseX mouse x in GUI units
     * @param mouseY mouse y in GUI units
     */
    public final void render(GuiGraphics g, int mouseX, int mouseY) {
        float w = width();
        float h = height();
        float radius = Theme.px(Theme.PANEL_RADIUS);

        Render2D.dropShadow(g, x, y, w, h, radius, Theme.px(10), Theme.SHADOW);
        Render2D.fillRounded(g, x, y, w, h, radius, Theme.PANEL);
        Render2D.rgbLine(g, x + radius, y, w - 2 * radius, Theme.px(3));

        Fonts.draw(g, title, x + Theme.px(14), y + Theme.px(12), Theme.TEXT_PRIMARY, false);

        renderBody(g, x + Theme.px(10), y + headerHeight(), mouseX, mouseY);
    }

    /**
     * Renders the panel body.
     *
     * @param g      the draw context
     * @param bx     body left edge (already padded)
     * @param by     body top edge
     * @param mouseX mouse x in GUI units
     * @param mouseY mouse y in GUI units
     */
    protected abstract void renderBody(GuiGraphics g, float bx, float by, int mouseX, int mouseY);

    /**
     * Handles a mouse press. The header starts a drag; body clicks are
     * delegated to {@link #bodyClicked}.
     *
     * @param mx     mouse x
     * @param my     mouse y
     * @param button GLFW mouse button
     * @return {@code true} if the click was consumed
     */
    public boolean mouseClicked(double mx, double my, int button) {
        if (mx >= x && mx <= x + width()) {
            if (my >= y && my <= y + headerHeight()) {
                dragging = true;
                dragOffX = (float) (mx - x);
                dragOffY = (float) (my - y);
                return true;
            }
            if (my <= y + height()) {
                return bodyClicked(mx, my, button);
            }
        }
        return false;
    }

    /**
     * Handles a click inside the body area.
     *
     * @param mx     mouse x
     * @param my     mouse y
     * @param button GLFW mouse button
     * @return {@code true} if consumed
     */
    protected boolean bodyClicked(double mx, double my, int button) {
        return false;
    }

    /**
     * Handles mouse drag, moving the panel while its header is held.
     *
     * @param mx        mouse x
     * @param my        mouse y
     * @param maxWidth  screen width, for clamping
     * @param maxHeight screen height, for clamping
     * @return {@code true} if the drag was consumed
     */
    public boolean mouseDragged(double mx, double my, float maxWidth, float maxHeight) {
        if (!dragging) {
            return false;
        }
        x = Math.max(0, Math.min((float) mx - dragOffX, maxWidth - width()));
        y = Math.max(0, Math.min((float) my - dragOffY, maxHeight - headerHeight()));
        return true;
    }

    /**
     * Ends any in-progress drag.
     */
    public void mouseReleased() {
        dragging = false;
    }

    /** @return current left edge */
    public float getX() {
        return x;
    }

    /** @return current top edge */
    public float getY() {
        return y;
    }

    /**
     * Moves the panel.
     *
     * @param x new left edge
     * @param y new top edge
     */
    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }
}
