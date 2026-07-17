package gg.dindijari.client.gui.screen;

import gg.dindijari.client.render.ColorUtil;
import gg.dindijari.client.render.Render2D;
import gg.dindijari.client.render.Theme;
import gg.dindijari.client.util.animation.Animation;
import gg.dindijari.client.util.animation.Easing;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Base class for all client-owned screens, replacing every vanilla background
 * with the theme's dark treatment:
 *
 * <ul>
 *   <li><b>In a world</b>: the vanilla Gaussian-blur post-process (respecting
 *       the user's "Menu Background Blurriness" accessibility option) below a
 *       {@link Theme#INWORLD_OVERLAY} charcoal tint;</li>
 *   <li><b>Outside a world</b>: a solid {@link Theme#BACKGROUND} fill — never
 *       the vanilla dirt/stone tile or panorama.</li>
 * </ul>
 *
 * <p>Screens additionally get an entrance transition (short fade plus a slight
 * scale-up, wall-clock driven and thus frame-rate independent) and can request
 * a matching exit via {@link #animateClose(Runnable)}. Both are skipped
 * entirely while UI animations are disabled or Performance Mode is on (see
 * {@link Theme#animationsEnabled()}).
 */
public abstract class ThemedScreen extends Screen {

    /** Entrance/exit transition length. */
    private static final long TRANSITION_MS = 180L;

    private final Animation transition = new Animation(0.0, TRANSITION_MS, Easing.CUBIC_OUT);
    private boolean entranceStarted;
    private Runnable closeAction;

    /**
     * Creates a themed screen.
     *
     * @param title the screen title used for narration
     */
    protected ThemedScreen(Component title) {
        super(title);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (this.minecraft.level != null) {
            // Vanilla's blur post-chain (robust in 1.21.1; strength follows the
            // accessibility slider), then the charcoal tint on top. Performance
            // Mode skips the blur pass entirely and deepens the tint instead.
            if (!Theme.reducedEffects()) {
                renderBlurredBackground(partialTick);
                Render2D.fillRect(g, 0, 0, this.width, this.height, Theme.INWORLD_OVERLAY);
            } else {
                Render2D.fillRect(g, 0, 0, this.width, this.height, 0xD00E0E10);
            }
        } else {
            Render2D.fillRect(g, 0, 0, this.width, this.height, Theme.BACKGROUND);
        }
    }

    /** Set while a frame's transition transform is on the pose stack. */
    private boolean transformPushed;

    /**
     * Applies the entrance/exit transform for this frame. Invoked from the
     * screen-render events (see {@code ScreenManager}), which bracket the whole
     * render including tooltips — {@code Screen.renderWithTooltip} is final in
     * 1.21.1, so the transition cannot wrap it from inside the class.
     *
     * @param g the draw context
     */
    public final void beginTransitionFrame(GuiGraphics g) {
        if (!Theme.animationsEnabled()) {
            // Animations off (or toggled off mid-transition): settle instantly.
            transition.snapTo(closeAction != null ? 0.0 : 1.0);
            return;
        }
        if (!entranceStarted) {
            entranceStarted = true;
            transition.snapTo(0.0);
            transition.animateTo(1.0);
        }
        float t = transition.valueF();
        if (t < 1.0F || closeAction != null) {
            float scale = 0.95F + 0.05F * t;
            g.pose().pushPose();
            g.pose().translate(this.width / 2.0F * (1.0F - scale),
                    this.height / 2.0F * (1.0F - scale), 0);
            g.pose().scale(scale, scale, 1.0F);
            transformPushed = true;
        }
    }

    /**
     * Pops the transition transform, draws the fade veil while transitioning
     * and completes a pending exit. Counterpart of
     * {@link #beginTransitionFrame(GuiGraphics)}.
     *
     * @param g the draw context
     */
    public final void endTransitionFrame(GuiGraphics g) {
        if (transformPushed) {
            transformPushed = false;
            g.pose().popPose();
            // Fade: charcoal veil lifting as the screen settles (and returning
            // while it leaves).
            float t = transition.valueF();
            Render2D.fillRect(g, 0, 0, this.width, this.height,
                    ColorUtil.scaleAlpha(Theme.BACKGROUND, 1.0F - t));
        }
        runCloseActionIfDone();
    }

    /**
     * Plays the exit transition (fade + scale back down), then runs the given
     * navigation. With animations disabled the navigation runs immediately.
     * Repeated calls while an exit is already in flight are ignored, so Esc
     * spam cannot double-navigate.
     *
     * @param navigate the actual close/navigation action (e.g. setting the
     *                 parent screen)
     */
    protected final void animateClose(Runnable navigate) {
        if (!Theme.animationsEnabled()) {
            navigate.run();
            return;
        }
        if (closeAction != null) {
            return;
        }
        closeAction = navigate;
        transition.animateTo(0.0);
    }

    /**
     * Indicates whether an exit transition is in flight (input should be
     * ignored meanwhile).
     *
     * @return {@code true} while closing
     */
    protected final boolean isClosing() {
        return closeAction != null;
    }

    private void runCloseActionIfDone() {
        if (closeAction != null && transition.isDone() && transition.target() == 0.0) {
            Runnable action = closeAction;
            closeAction = null;
            action.run();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return isClosing() || super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return isClosing() || super.keyPressed(keyCode, scanCode, modifiers);
    }
}
