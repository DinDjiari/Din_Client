package gg.dindijari.client.util.animation;

/**
 * A frame-rate-independent scalar animation.
 *
 * <p>The animation interpolates from a captured start value towards a target
 * over a fixed wall-clock duration, shaped by an {@link Easing}. Progress is
 * derived from {@link System#nanoTime()} on every {@link #value()} call, so the
 * result is identical at 30 FPS and 300 FPS — only smoothness differs.
 *
 * <p>Instances are intended to be created once (per widget, per effect) and
 * retargeted with {@link #animateTo(double)}; querying the value allocates
 * nothing, which keeps render loops garbage-free.
 *
 * <p>Not thread-safe; use from the render thread only.
 */
public final class Animation {

    private final long durationNanos;
    private Easing easing;

    private double from;
    private double to;
    private long startNanos;

    /**
     * Creates an animation resting at an initial value.
     *
     * @param initialValue the starting (and current) value
     * @param durationMs   the duration of one full transition in milliseconds
     * @param easing       the easing curve applied to progress
     */
    public Animation(double initialValue, long durationMs, Easing easing) {
        if (durationMs <= 0) {
            throw new IllegalArgumentException("durationMs must be > 0");
        }
        this.durationNanos = durationMs * 1_000_000L;
        this.easing = easing == null ? Easing.LINEAR : easing;
        this.from = initialValue;
        this.to = initialValue;
        this.startNanos = System.nanoTime() - this.durationNanos; // already settled
    }

    /**
     * Starts animating from the <em>current</em> value towards a new target.
     * Retargeting mid-flight is smooth: the in-progress value becomes the new
     * start point. Retargeting to the current target is a no-op.
     *
     * @param target the value to animate towards
     */
    public void animateTo(double target) {
        if (target == this.to) {
            return;
        }
        this.from = value();
        this.to = target;
        this.startNanos = System.nanoTime();
    }

    /**
     * Jumps immediately to a value with no transition.
     *
     * @param value the value to rest at
     */
    public void snapTo(double value) {
        this.from = value;
        this.to = value;
        this.startNanos = System.nanoTime() - this.durationNanos;
    }

    /**
     * Returns the current animated value, computed from wall-clock time.
     *
     * @return the eased value between the start point and the target
     */
    public double value() {
        long elapsed = System.nanoTime() - this.startNanos;
        if (elapsed >= this.durationNanos) {
            return this.to;
        }
        double t = (double) elapsed / (double) this.durationNanos;
        return this.from + (this.to - this.from) * this.easing.apply(t);
    }

    /**
     * Returns the current value as a float, for render code.
     *
     * @return {@link #value()} narrowed to float
     */
    public float valueF() {
        return (float) value();
    }

    /**
     * Returns the value this animation is heading towards (or resting at).
     *
     * @return the target value
     */
    public double target() {
        return this.to;
    }

    /**
     * Indicates whether the transition has finished.
     *
     * @return {@code true} once the full duration has elapsed
     */
    public boolean isDone() {
        return System.nanoTime() - this.startNanos >= this.durationNanos;
    }

    /**
     * Swaps the easing curve used for subsequent progress evaluation.
     *
     * @param easing the new easing; ignored if {@code null}
     */
    public void setEasing(Easing easing) {
        if (easing != null) {
            this.easing = easing;
        }
    }
}
