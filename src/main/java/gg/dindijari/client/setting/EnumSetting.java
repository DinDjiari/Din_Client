package gg.dindijari.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/**
 * A setting whose value is one of the constants of an {@link Enum}. Useful for
 * mode selectors (e.g. rendering style, anchor position).
 *
 * @param <E> the enum type
 */
public class EnumSetting<E extends Enum<E>> extends Setting<E> {

    private final Class<E> enumClass;

    /**
     * Creates a new enum setting. The enum type is inferred from the default
     * value's declaring class.
     *
     * @param name         the setting name
     * @param description  a short description
     * @param defaultValue the default constant; must not be {@code null}
     */
    @SuppressWarnings("unchecked")
    public EnumSetting(String name, String description, E defaultValue) {
        super(name, description, defaultValue);
        this.enumClass = (Class<E>) defaultValue.getDeclaringClass();
    }

    /**
     * Returns all constants of this setting's enum type, in declaration order.
     *
     * @return the enum constants
     */
    public E[] getValues() {
        return enumClass.getEnumConstants();
    }

    /**
     * Advances to the next constant, wrapping around to the first.
     */
    public void cycle() {
        E[] values = getValues();
        int next = (get().ordinal() + 1) % values.length;
        set(values[next]);
    }

    /**
     * Moves to the previous constant, wrapping around to the last.
     */
    public void cycleBackward() {
        E[] values = getValues();
        int prev = (get().ordinal() - 1 + values.length) % values.length;
        set(values[prev]);
    }

    @Override
    public JsonElement save() {
        return new JsonPrimitive(get().name());
    }

    @Override
    public void load(JsonElement json) {
        if (json != null && json.isJsonPrimitive()) {
            try {
                set(Enum.valueOf(enumClass, json.getAsString()));
            } catch (IllegalArgumentException ignored) {
                // Unknown constant (e.g. renamed enum): keep the current value.
            }
        }
    }
}
