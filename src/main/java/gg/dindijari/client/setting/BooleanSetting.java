package gg.dindijari.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/**
 * A boolean on/off setting.
 */
public class BooleanSetting extends Setting<Boolean> {

    /**
     * Creates a new boolean setting.
     *
     * @param name         the setting name
     * @param description  a short description
     * @param defaultValue the default state
     */
    public BooleanSetting(String name, String description, boolean defaultValue) {
        super(name, description, defaultValue);
    }

    /**
     * Flips the current value.
     */
    public void toggle() {
        set(!get());
    }

    @Override
    public JsonElement save() {
        return new JsonPrimitive(get());
    }

    @Override
    public void load(JsonElement json) {
        if (json != null && json.isJsonPrimitive()) {
            set(json.getAsBoolean());
        }
    }
}
