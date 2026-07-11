package gg.dindijari.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/**
 * A numeric setting bounded by a minimum and maximum and snapped to a step
 * increment. Values are stored as {@code double}; convenience accessors expose
 * {@code int} and {@code float} views.
 */
public class NumberSetting extends Setting<Double> {

    private final double min;
    private final double max;
    private final double step;

    /**
     * Creates a new numeric setting.
     *
     * @param name         the setting name
     * @param description  a short description
     * @param defaultValue the default value (clamped and snapped on construction)
     * @param min          the inclusive minimum
     * @param max          the inclusive maximum
     * @param step         the snap increment; must be greater than zero
     */
    public NumberSetting(String name, String description, double defaultValue,
                         double min, double max, double step) {
        super(name, description, sanitize(defaultValue, min, max, step));
        if (min > max) {
            throw new IllegalArgumentException("min > max for setting " + name);
        }
        if (step <= 0) {
            throw new IllegalArgumentException("step must be > 0 for setting " + name);
        }
        this.min = min;
        this.max = max;
        this.step = step;
    }

    private static double sanitize(double value, double min, double max, double step) {
        double clamped = Math.max(min, Math.min(max, value));
        if (step <= 0) {
            return clamped;
        }
        double snapped = min + Math.round((clamped - min) / step) * step;
        return Math.max(min, Math.min(max, snapped));
    }

    @Override
    public void set(Double newValue) {
        super.set(sanitize(newValue, min, max, step));
    }

    /**
     * Returns the inclusive minimum.
     *
     * @return the minimum value
     */
    public double getMin() {
        return min;
    }

    /**
     * Returns the inclusive maximum.
     *
     * @return the maximum value
     */
    public double getMax() {
        return max;
    }

    /**
     * Returns the snap increment.
     *
     * @return the step value
     */
    public double getStep() {
        return step;
    }

    /**
     * Returns the current value rounded to the nearest integer.
     *
     * @return the value as an {@code int}
     */
    public int getAsInt() {
        return (int) Math.round(get());
    }

    /**
     * Returns the current value as a {@code float}.
     *
     * @return the value as a {@code float}
     */
    public float getAsFloat() {
        return get().floatValue();
    }

    @Override
    public JsonElement save() {
        return new JsonPrimitive(get());
    }

    @Override
    public void load(JsonElement json) {
        if (json != null && json.isJsonPrimitive()) {
            try {
                set(json.getAsDouble());
            } catch (NumberFormatException ignored) {
                // Leave the current value untouched on malformed input.
            }
        }
    }
}
