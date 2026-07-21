package gg.dindijari.client.module;

import gg.dindijari.client.setting.KeybindSetting;
import gg.dindijari.client.setting.Setting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Base class for every client module.
 *
 * <p>A module has a unique {@link #getName() name}, a {@link #getDescription()
 * description}, a {@link Category category}, an enabled/disabled state, a toggle
 * {@link #getKeybind() keybind} and a list of typed {@link Setting settings}.
 * Subclasses override the {@link #onEnable()}, {@link #onDisable()} and
 * {@link #onTick()} lifecycle hooks.
 *
 * <p>This class carries no Minecraft dependency; the game loop drives it through
 * {@link #tick()}, and the {@code ModuleManager} bridges NeoForge events to
 * these hooks.
 */
public abstract class Module {

    private final String name;
    private final String description;
    private final Category category;
    private final KeybindSetting keybind;
    private final List<Setting<?>> settings = new ArrayList<>();
    private final List<Runnable> changeListeners = new ArrayList<>();

    private boolean enabled;
    private boolean toggleable = true;
    private boolean favorite;

    /**
     * Creates a new module with an unbound toggle keybind.
     *
     * @param name        the unique module name
     * @param description a short description shown as a tooltip
     * @param category    the category this module belongs to
     */
    protected Module(String name, String description, Category category) {
        this.name = Objects.requireNonNull(name, "name");
        this.description = description == null ? "" : description;
        this.category = Objects.requireNonNull(category, "category");
        this.keybind = new KeybindSetting("Keybind", "Key that toggles this module.",
                KeybindSetting.UNBOUND);
        addSetting(this.keybind);
    }

    /**
     * Returns the unique module name.
     *
     * @return the name
     */
    public final String getName() {
        return name;
    }

    /**
     * Returns the module description.
     *
     * @return the description
     */
    public final String getDescription() {
        return description;
    }

    /**
     * Returns the module category.
     *
     * @return the category
     */
    public final Category getCategory() {
        return category;
    }

    /**
     * Returns the toggle keybind setting.
     *
     * @return the keybind setting
     */
    public final KeybindSetting getKeybind() {
        return keybind;
    }

    /**
     * Returns an unmodifiable view of this module's settings, including the
     * toggle keybind.
     *
     * @return the settings list
     */
    public final List<Setting<?>> getSettings() {
        return Collections.unmodifiableList(settings);
    }

    /**
     * Indicates whether the module is currently enabled.
     *
     * @return {@code true} if enabled
     */
    public final boolean isEnabled() {
        return enabled;
    }

    /**
     * Enables or disables the module, invoking the matching lifecycle hook and
     * notifying change listeners when the state actually changes.
     *
     * @param enabled the desired state
     */
    public final void setEnabled(boolean enabled) {
        if (this.enabled == enabled) {
            return;
        }
        this.enabled = enabled;
        if (enabled) {
            onEnable();
        } else {
            onDisable();
        }
        fireChanged();
    }

    /**
     * Toggles the enabled state.
     */
    public final void toggle() {
        setEnabled(!enabled);
    }

    /**
     * Indicates whether the user has starred this module as a favorite.
     *
     * @return {@code true} if favorited
     */
    public final boolean isFavorite() {
        return favorite;
    }

    /**
     * Sets the favorite (starred) state, notifying change listeners so the
     * config autosaves.
     *
     * @param favorite the desired state
     */
    public final void setFavorite(boolean favorite) {
        if (this.favorite != favorite) {
            this.favorite = favorite;
            fireChanged();
        }
    }

    /**
     * Indicates whether this module has a meaningful on/off state. Settings-only
     * modules (e.g. the theme) return {@code false} and render without a toggle
     * in the GUI.
     *
     * @return {@code true} if the module can be toggled
     */
    public final boolean isToggleable() {
        return toggleable;
    }

    /**
     * Marks this module as settings-only (no on/off toggle shown in the GUI).
     * Intended to be called from a subclass constructor.
     */
    protected final void setToggleable(boolean toggleable) {
        this.toggleable = toggleable;
    }

    /**
     * Registers a listener notified whenever this module's enabled state or any
     * of its settings change. Intended for the config layer's autosave.
     *
     * @param listener the change listener
     */
    public final void onChanged(Runnable listener) {
        Objects.requireNonNull(listener, "listener");
        changeListeners.add(listener);
        for (Setting<?> setting : settings) {
            setting.onChange(value -> listener.run());
        }
    }

    /**
     * Registers additional settings for this module and wires already-registered
     * change listeners to them.
     *
     * @param toAdd the settings to add
     */
    protected final void addSetting(Setting<?>... toAdd) {
        for (Setting<?> setting : toAdd) {
            settings.add(setting);
            for (Runnable listener : changeListeners) {
                setting.onChange(value -> listener.run());
            }
        }
    }

    private void fireChanged() {
        for (Runnable listener : changeListeners) {
            listener.run();
        }
    }

    /**
     * Called by the client tick when the module is enabled. Subclasses that need
     * per-tick behaviour override {@link #onTick()}.
     */
    public final void tick() {
        if (enabled) {
            onTick();
        }
    }

    /**
     * Invoked once when the module becomes enabled. Default implementation is
     * empty.
     */
    protected void onEnable() {
        // no-op by default
    }

    /**
     * Invoked once when the module becomes disabled. Default implementation is
     * empty.
     */
    protected void onDisable() {
        // no-op by default
    }

    /**
     * Invoked every client tick while the module is enabled. Default
     * implementation is empty.
     */
    protected void onTick() {
        // no-op by default
    }
}
