package gg.dindijari.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/**
 * A free-text setting (e.g. custom branding text). Values are stored verbatim;
 * {@code null} is normalised to the empty string.
 */
public class StringSetting extends Setting<String> {

    /**
     * Creates a new string setting.
     *
     * @param name         the setting name
     * @param description  a short description
     * @param defaultValue the default text
     */
    public StringSetting(String name, String description, String defaultValue) {
        super(name, description, defaultValue == null ? "" : defaultValue);
    }

    @Override
    public void set(String newValue) {
        super.set(newValue == null ? "" : newValue);
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
