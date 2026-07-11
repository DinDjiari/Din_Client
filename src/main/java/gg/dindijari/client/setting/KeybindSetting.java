package gg.dindijari.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.HashMap;
import java.util.Map;

/**
 * A setting storing a single GLFW key code. The value {@link #UNBOUND} (-1)
 * represents "no key bound".
 *
 * <p>The key code uses the GLFW numbering scheme (the same one Minecraft's
 * {@code InputConstants} exposes) so it can be compared directly against the
 * codes delivered by NeoForge input events, without this class depending on any
 * Minecraft type. A best-effort human readable name is provided by
 * {@link #getKeyName()}.
 */
public class KeybindSetting extends Setting<Integer> {

    /** Sentinel value indicating that no key is bound. */
    public static final int UNBOUND = -1;

    private static final Map<Integer, String> SPECIAL_NAMES = new HashMap<>();

    static {
        SPECIAL_NAMES.put(32, "Space");
        SPECIAL_NAMES.put(256, "Escape");
        SPECIAL_NAMES.put(257, "Enter");
        SPECIAL_NAMES.put(258, "Tab");
        SPECIAL_NAMES.put(259, "Backspace");
        SPECIAL_NAMES.put(260, "Insert");
        SPECIAL_NAMES.put(261, "Delete");
        SPECIAL_NAMES.put(262, "Right");
        SPECIAL_NAMES.put(263, "Left");
        SPECIAL_NAMES.put(264, "Down");
        SPECIAL_NAMES.put(265, "Up");
        SPECIAL_NAMES.put(340, "Left Shift");
        SPECIAL_NAMES.put(341, "Left Ctrl");
        SPECIAL_NAMES.put(342, "Left Alt");
        SPECIAL_NAMES.put(344, "Right Shift");
        SPECIAL_NAMES.put(345, "Right Ctrl");
        SPECIAL_NAMES.put(346, "Right Alt");
    }

    /**
     * Creates a new keybind setting.
     *
     * @param name         the setting name
     * @param description  a short description
     * @param defaultKey   the default GLFW key code, or {@link #UNBOUND}
     */
    public KeybindSetting(String name, String description, int defaultKey) {
        super(name, description, defaultKey);
    }

    /**
     * Indicates whether a key is currently bound.
     *
     * @return {@code true} if a key is bound
     */
    public boolean isBound() {
        return get() != UNBOUND;
    }

    /**
     * Tests whether the given GLFW key code matches this binding.
     *
     * @param keyCode the GLFW key code from an input event
     * @return {@code true} if bound and equal to {@code keyCode}
     */
    public boolean matches(int keyCode) {
        return isBound() && get() == keyCode;
    }

    /**
     * Clears the binding.
     */
    public void unbind() {
        set(UNBOUND);
    }

    /**
     * Returns a best-effort human readable name for the bound key.
     *
     * @return the key name, or "None" when unbound
     */
    public String getKeyName() {
        int code = get();
        if (code == UNBOUND) {
            return "None";
        }
        if (code >= 65 && code <= 90) {
            return String.valueOf((char) code); // A-Z
        }
        if (code >= 48 && code <= 57) {
            return String.valueOf((char) code); // 0-9
        }
        if (code >= 290 && code <= 314) {
            return "F" + (code - 289); // F1-F25
        }
        return SPECIAL_NAMES.getOrDefault(code, "Key " + code);
    }

    @Override
    public JsonElement save() {
        return new JsonPrimitive(get());
    }

    @Override
    public void load(JsonElement json) {
        if (json != null && json.isJsonPrimitive()) {
            try {
                set(json.getAsInt());
            } catch (NumberFormatException ignored) {
                // Keep the current binding on malformed input.
            }
        }
    }
}
