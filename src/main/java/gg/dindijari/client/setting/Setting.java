package gg.dindijari.client.setting;

import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Base class for a single, typed, persistable configuration value owned by a
 * {@code Module} (or any other subsystem).
 *
 * <p>A setting has a stable {@link #getName() name} (used as its JSON key), a
 * human readable {@link #getDescription() description}, a default value and a
 * current value. Interested parties may register change listeners via
 * {@link #onChange(Consumer)}; they are invoked whenever the value actually
 * changes (as determined by {@link Object#equals(Object)}).
 *
 * <p>This class and all of its subclasses are intentionally free of any
 * Minecraft or NeoForge dependency so that the setting/config core can be unit
 * tested and reasoned about in isolation.
 *
 * @param <T> the type of value held by this setting
 */
public abstract class Setting<T> {

    private final String name;
    private final String description;
    private final T defaultValue;
    private T value;
    private final List<Consumer<T>> listeners = new ArrayList<>();

    /**
     * Creates a new setting.
     *
     * @param name         the unique (within its owner) name, used as the JSON key
     * @param description  a short human readable description
     * @param defaultValue the initial and reset value; must not be {@code null}
     */
    protected Setting(String name, String description, T defaultValue) {
        this.name = Objects.requireNonNull(name, "name");
        this.description = description == null ? "" : description;
        this.defaultValue = Objects.requireNonNull(defaultValue, "defaultValue");
        this.value = defaultValue;
    }

    /**
     * Returns this setting's name, which doubles as its serialization key.
     *
     * @return the setting name
     */
    public final String getName() {
        return name;
    }

    /**
     * Returns this setting's human readable description.
     *
     * @return the description, never {@code null}
     */
    public final String getDescription() {
        return description;
    }

    /**
     * Returns the current value.
     *
     * @return the current value
     */
    public final T get() {
        return value;
    }

    /**
     * Returns the value this setting was constructed with.
     *
     * @return the default value
     */
    public final T getDefault() {
        return defaultValue;
    }

    /**
     * Sets a new value. If the new value differs from the current value, all
     * registered change listeners are notified.
     *
     * @param newValue the value to apply; must not be {@code null}
     */
    public void set(T newValue) {
        Objects.requireNonNull(newValue, "newValue");
        if (Objects.equals(this.value, newValue)) {
            return;
        }
        this.value = newValue;
        notifyListeners();
    }

    /**
     * Resets this setting back to its default value.
     */
    public final void reset() {
        set(defaultValue);
    }

    /**
     * Indicates whether the current value equals the default value.
     *
     * @return {@code true} if the current value is the default
     */
    public final boolean isDefault() {
        return Objects.equals(value, defaultValue);
    }

    /**
     * Registers a listener invoked whenever the value changes.
     *
     * @param listener the change listener, receiving the new value
     * @return this setting, for chaining
     */
    public final Setting<T> onChange(Consumer<T> listener) {
        listeners.add(Objects.requireNonNull(listener, "listener"));
        return this;
    }

    /**
     * Notifies all registered listeners of the current value. Subclasses that
     * mutate extra state (e.g. a colour's RGB-cycle flag) may call this to
     * broadcast a change.
     */
    protected final void notifyListeners() {
        for (Consumer<T> listener : listeners) {
            listener.accept(value);
        }
    }

    /**
     * Serializes the full state of this setting to a JSON element.
     *
     * @return a JSON representation suitable for {@link #load(JsonElement)}
     */
    public abstract JsonElement save();

    /**
     * Restores this setting's state from a previously {@link #save() saved}
     * JSON element. Implementations must tolerate malformed or missing data by
     * leaving the current value untouched rather than throwing.
     *
     * @param json the JSON element to read from
     */
    public abstract void load(JsonElement json);

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" + name + "=" + value + "}";
    }
}
