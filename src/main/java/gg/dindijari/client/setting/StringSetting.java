package gg.dindijari.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/**
 * A free-text setting (e.g. the window title or the Ollama endpoint). Values
 * are plain strings; the empty string is a valid value whose meaning ("use the
 * default") is up to the consumer. Length is capped to keep config files and
 * UI rendering sane.
 */
public class StringSetting extends Setting<String> {

    /** Hard cap applied to every value. */
    public static final int MAX_LENGTH = 256;

    /**
     * Creates a new string setting.
     *
     * @param name         the setting name
     * @param description  a short description
     * @param defaultValue the default text
     */
    public StringSetting(String name, String description, String defaultValue) {
        super(name, description, truncate(defaultValue));
    }

    private static String truncate(String value) {
        return value.length() > MAX_LENGTH ? value.substring(0, MAX_LENGTH) : value;
    }

    @Override
    public void set(String newValue) {
        super.set(truncate(newValue));
    }

    @Override
    public JsonElement save() {
        return new JsonPrimitive(get());
    }

    @Override
    public void load(JsonElement json) {
        if (json != null && json.isJsonPrimitive()) {
            set(json.getAsString());
        }
    }
}
