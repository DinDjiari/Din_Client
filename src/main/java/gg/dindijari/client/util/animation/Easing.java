package gg.dindijari.client.util.animation;

/**
 * Standard easing functions used to shape animation progress curves.
 *
 * <p>Every easing maps a linear progress value {@code t} in {@code [0, 1]} to an
 * eased value, also nominally in {@code [0, 1]} (the {@code BACK} and
 * {@code ELASTIC} families intentionally overshoot). Each family is provided in
 * the conventional {@code IN}, {@code OUT} and {@code IN_OUT} variants.
 *
 * <p>All implementations are pure functions and allocate nothing, so they are
 * safe to call every frame from render code.
 */
public enum Easing {

    /** Constant-speed interpolation; the identity easing. */
    LINEAR {
        @Override
        public double apply(double t) {
            return t;
        }
    },

    /** Quadratic acceleration from zero velocity. */
    QUAD_IN {
        @Override
        public double apply(double t) {
            return t * t;
        }
    },
    /** Quadratic deceleration to zero velocity. */
    QUAD_OUT {
        @Override
        public double apply(double t) {
            return 1.0 - (1.0 - t) * (1.0 - t);
        }
    },
    /** Quadratic acceleration then deceleration. */
    QUAD_IN_OUT {
        @Override
        public double apply(double t) {
            return t < 0.5
                    ? 2.0 * t * t
                    : 1.0 - Math.pow(-2.0 * t + 2.0, 2) / 2.0;
        }
    },

    /** Cubic acceleration from zero velocity. */
    CUBIC_IN {
        @Override
        public double apply(double t) {
            return t * t * t;
        }
    },
    /** Cubic deceleration to zero velocity. */
    CUBIC_OUT {
        @Override
        public double apply(double t) {
            return 1.0 - Math.pow(1.0 - t, 3);
        }
    },
    /** Cubic acceleration then deceleration. */
    CUBIC_IN_OUT {
        @Override
        public double apply(double t) {
            return t < 0.5
                    ? 4.0 * t * t * t
                    : 1.0 - Math.pow(-2.0 * t + 2.0, 3) / 2.0;
        }
    },

    /** Exponential acceleration from rest. */
    EXPO_IN {
        @Override
        public double apply(double t) {
            return t <= 0.0 ? 0.0 : Math.pow(2.0, 10.0 * t - 10.0);
        }
    },
    /** Exponential deceleration to rest. */
    EXPO_OUT {
        @Override
        public double apply(double t) {
            return t >= 1.0 ? 1.0 : 1.0 - Math.pow(2.0, -10.0 * t);
        }
    },
    /** Exponential acceleration then deceleration. */
    EXPO_IN_OUT {
        @Override
        public double apply(double t) {
            if (t <= 0.0) {
                return 0.0;
            }
            if (t >= 1.0) {
                return 1.0;
            }
            return t < 0.5
                    ? Math.pow(2.0, 20.0 * t - 10.0) / 2.0
                    : (2.0 - Math.pow(2.0, -20.0 * t + 10.0)) / 2.0;
        }
    },

    /** Pulls slightly backwards before accelerating forwards (overshoots below 0). */
    BACK_IN {
        @Override
        public double apply(double t) {
            return (BACK_C1 + 1.0) * t * t * t - BACK_C1 * t * t;
        }
    },
    /** Overshoots past the target before settling (overshoots above 1). */
    BACK_OUT {
        @Override
        public double apply(double t) {
            double u = t - 1.0;
            return 1.0 + (BACK_C1 + 1.0) * u * u * u + BACK_C1 * u * u;
        }
    },
    /** Overshoots on both ends. */
    BACK_IN_OUT {
        @Override
        public double apply(double t) {
            if (t < 0.5) {
                double u = 2.0 * t;
                return (u * u * ((BACK_C2 + 1.0) * u - BACK_C2)) / 2.0;
            }
            double u = 2.0 * t - 2.0;
            return (u * u * ((BACK_C2 + 1.0) * u + BACK_C2) + 2.0) / 2.0;
        }
    },

    /** Winds up like a spring before releasing towards the target. */
    ELASTIC_IN {
        @Override
        public double apply(double t) {
            if (t <= 0.0) {
                return 0.0;
            }
            if (t >= 1.0) {
                return 1.0;
            }
            return -Math.pow(2.0, 10.0 * t - 10.0) * Math.sin((t * 10.0 - 10.75) * ELASTIC_C4);
        }
    },
    /** Springs past the target and oscillates into place. */
    ELASTIC_OUT {
        @Override
        public double apply(double t) {
            if (t <= 0.0) {
                return 0.0;
            }
            if (t >= 1.0) {
                return 1.0;
            }
            return Math.pow(2.0, -10.0 * t) * Math.sin((t * 10.0 - 0.75) * ELASTIC_C4) + 1.0;
        }
    },
    /** Spring behaviour on both ends. */
    ELASTIC_IN_OUT {
        @Override
        public double apply(double t) {
            if (t <= 0.0) {
                return 0.0;
            }
            if (t >= 1.0) {
                return 1.0;
            }
            return t < 0.5
                    ? -(Math.pow(2.0, 20.0 * t - 10.0) * Math.sin((20.0 * t - 11.125) * ELASTIC_C5)) / 2.0
                    : (Math.pow(2.0, -20.0 * t + 10.0) * Math.sin((20.0 * t - 11.125) * ELASTIC_C5)) / 2.0 + 1.0;
        }
    };

    /** Overshoot constant for the {@code BACK} family (standard value). */
    private static final double BACK_C1 = 1.70158;
    /** Overshoot constant for {@code BACK_IN_OUT} (scaled by 1.525). */
    private static final double BACK_C2 = BACK_C1 * 1.525;
    /** Period constant for single-sided {@code ELASTIC} easings. */
    private static final double ELASTIC_C4 = (2.0 * Math.PI) / 3.0;
    /** Period constant for {@code ELASTIC_IN_OUT}. */
    private static final double ELASTIC_C5 = (2.0 * Math.PI) / 4.5;

    /**
     * Applies this easing to a linear progress value.
     *
     * @param t linear progress, expected in {@code [0, 1]}
     * @return the eased progress; {@code BACK}/{@code ELASTIC} variants may
     *         return values slightly outside {@code [0, 1]} by design
     */
    public abstract double apply(double t);

    /**
     * Applies this easing after clamping the input into {@code [0, 1]}.
     *
     * @param t linear progress; values outside {@code [0, 1]} are clamped
     * @return the eased progress
     */
    public final double applyClamped(double t) {
        return apply(t < 0.0 ? 0.0 : (t > 1.0 ? 1.0 : t));
    }
}
