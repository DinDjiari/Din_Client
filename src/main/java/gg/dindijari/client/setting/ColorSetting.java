package gg.dindijari.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * A colour setting storing a packed 32-bit ARGB value plus an optional
 * "RGB cycle" mode. When cycling is enabled the render layer (Phase 2) is
 * expected to derive an animated hue from {@link #getCycleSpeed()}; the base
 * ARGB value stored here is used for the alpha channel and as the value shown
 * while cycling is disabled.
 */
public class ColorSetting extends Setting<Integer> {

    private boolean rgbCycle;
    private double cycleSpeed;

    /**
     * Creates a new colour setting with cycling disabled.
     *
     * @param name         the setting name
     * @param description  a short description
     * @param defaultArgb  the default packed ARGB colour
     */
    public ColorSetting(String name, String description, int defaultArgb) {
        this(name, description, defaultArgb, false, 1.0D);
    }

    /**
     * Creates a new colour setting.
     *
     * @param name         the setting name
     * @param description  a short description
     * @param defaultArgb  the default packed ARGB colour
     * @param rgbCycle     whether RGB cycling is enabled by default
     * @param cycleSpeed   the default cycle speed multiplier
     */
    public ColorSetting(String name, String description, int defaultArgb,
                        boolean rgbCycle, double cycleSpeed) {
        super(name, description, defaultArgb);
        this.rgbCycle = rgbCycle;
        this.cycleSpeed = cycleSpeed;
    }

    /**
     * Packs the given channels into an ARGB integer.
     *
     * @param a alpha channel (0-255)
     * @param r red channel (0-255)
     * @param g green channel (0-255)
     * @param b blue channel (0-255)
     * @return the packed ARGB value
     */
    public static int argb(int a, int r, int g, int b) {
        return ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    /**
     * Returns the alpha channel of the base colour.
     *
     * @return alpha 0-255
     */
    public int getAlpha() {
        return (get() >> 24) & 0xFF;
    }

    /**
     * Returns the red channel of the base colour.
     *
     * @return red 0-255
     */
    public int getRed() {
        return (get() >> 16) & 0xFF;
    }

    /**
     * Returns the green channel of the base colour.
     *
     * @return green 0-255
     */
    public int getGreen() {
        return (get() >> 8) & 0xFF;
    }

    /**
     * Returns the blue channel of the base colour.
     *
     * @return blue 0-255
     */
    public int getBlue() {
        return get() & 0xFF;
    }

    /**
     * Indicates whether RGB cycling is enabled.
     *
     * @return {@code true} if cycling is enabled
     */
    public boolean isRgbCycle() {
        return rgbCycle;
    }

    /**
     * Enables or disables RGB cycling. Change listeners are notified.
     *
     * @param rgbCycle the new cycle state
     */
    public void setRgbCycle(boolean rgbCycle) {
        if (this.rgbCycle != rgbCycle) {
            this.rgbCycle = rgbCycle;
            notifyListeners();
        }
    }

    /**
     * Returns the cycle speed multiplier.
     *
     * @return the cycle speed
     */
    public double getCycleSpeed() {
        return cycleSpeed;
    }

    /**
     * Sets the cycle speed multiplier. Change listeners are notified.
     *
     * @param cycleSpeed the new cycle speed
     */
    public void setCycleSpeed(double cycleSpeed) {
        if (this.cycleSpeed != cycleSpeed) {
            this.cycleSpeed = cycleSpeed;
            notifyListeners();
        }
    }

    @Override
    public JsonElement save() {
        JsonObject object = new JsonObject();
        object.addProperty("argb", get());
        object.addProperty("rgbCycle", rgbCycle);
        object.addProperty("cycleSpeed", cycleSpeed);
        return object;
    }

    @Override
    public void load(JsonElement json) {
        if (json == null || !json.isJsonObject()) {
            return;
        }
        JsonObject object = json.getAsJsonObject();
        if (object.has("argb")) {
            set(object.get("argb").getAsInt());
        }
        if (object.has("rgbCycle")) {
            setRgbCycle(object.get("rgbCycle").getAsBoolean());
        }
        if (object.has("cycleSpeed")) {
            setCycleSpeed(object.get("cycleSpeed").getAsDouble());
        }
    }
}
