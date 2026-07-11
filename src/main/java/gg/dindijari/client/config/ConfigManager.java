package gg.dindijari.client.config;

import com.google.gson.JsonObject;
import gg.dindijari.client.module.Module;
import gg.dindijari.client.setting.Setting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Persists the state of all registered {@link Module modules} (enabled flag and
 * every {@link Setting}) to JSON profiles on disk, autosaving shortly after any
 * change.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Serialize / deserialize modules to and from the active profile.</li>
 *   <li>Debounced autosave: changes mark the config dirty and a single write is
 *       performed once changes stop for {@code debounceMillis}.</li>
 *   <li>Named profile management delegated to {@link ProfileStore}.</li>
 * </ul>
 *
 * <p>The class is Minecraft-independent: it is given the config root directory
 * and the module collection, so it can be tested against a temporary directory.
 */
public final class ConfigManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("dindijariclient/config");
    private static final long DEFAULT_DEBOUNCE_MILLIS = 750L;

    private final ProfileStore store;
    private final ScheduledExecutorService scheduler;
    private final List<Module> modules = new ArrayList<>();

    private long debounceMillis = DEFAULT_DEBOUNCE_MILLIS;
    private String activeProfile;
    private ScheduledFuture<?> pendingSave;
    private volatile boolean dirty;

    /**
     * Creates a config manager rooted at the given directory.
     *
     * @param root the configuration root directory
     */
    public ConfigManager(Path root) {
        this.store = new ProfileStore(Objects.requireNonNull(root, "root"));
        this.activeProfile = store.getActiveProfile();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "dindijari-config-autosave");
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * Overrides the autosave debounce interval.
     *
     * @param millis the debounce interval in milliseconds
     */
    public void setDebounceMillis(long millis) {
        this.debounceMillis = Math.max(0L, millis);
    }

    /**
     * Binds the manager to the module collection and wires autosave listeners.
     * Call once after all modules have been registered.
     *
     * @param registered the modules to persist
     */
    public void bind(Collection<Module> registered) {
        modules.clear();
        modules.addAll(registered);
        for (Module module : modules) {
            module.onChanged(this::markDirty);
        }
    }

    /**
     * Returns the name of the currently active profile.
     *
     * @return the active profile name
     */
    public String getActiveProfileName() {
        return activeProfile;
    }

    /**
     * Lists all stored profile names.
     *
     * @return the profile names
     */
    public List<String> listProfiles() {
        return store.listProfiles();
    }

    /**
     * Loads the active profile from disk and applies it to the modules. If the
     * profile does not exist yet, it is created from the current module state.
     */
    public void load() {
        JsonObject data = store.readProfile(activeProfile);
        if (data == null) {
            LOGGER.info("No profile '{}' found; creating it from defaults", activeProfile);
            saveNow();
            return;
        }
        applyProfileJson(data);
        LOGGER.info("Loaded config profile '{}' ({} modules)", activeProfile, modules.size());
    }

    /**
     * Marks the config dirty and schedules a debounced save.
     */
    public void markDirty() {
        dirty = true;
        if (scheduler.isShutdown()) {
            return;
        }
        if (pendingSave != null && !pendingSave.isDone()) {
            pendingSave.cancel(false);
        }
        pendingSave = scheduler.schedule(this::saveNow, debounceMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * Writes the current module state to the active profile immediately.
     */
    public synchronized void saveNow() {
        try {
            store.writeProfile(activeProfile, buildProfileJson());
            store.setActiveProfile(activeProfile);
            dirty = false;
        } catch (IOException e) {
            LOGGER.error("Failed to save config profile '{}'", activeProfile, e);
        }
    }

    /**
     * Flushes any pending debounced save synchronously.
     */
    public void flush() {
        if (pendingSave != null && !pendingSave.isDone()) {
            pendingSave.cancel(false);
        }
        if (dirty) {
            saveNow();
        }
    }

    /**
     * Creates a new profile capturing the current module state, without switching
     * to it.
     *
     * @param name the requested profile name
     * @return the sanitized name the profile was stored under
     */
    public String createProfile(String name) {
        String sanitized = ProfileStore.sanitize(name);
        try {
            store.writeProfile(sanitized, buildProfileJson());
        } catch (IOException e) {
            LOGGER.error("Failed to create profile '{}'", sanitized, e);
        }
        return sanitized;
    }

    /**
     * Saves the current profile, then switches to and loads another profile.
     * If the target profile does not exist, it is created from the current state.
     *
     * @param name the profile to switch to
     */
    public void switchProfile(String name) {
        String sanitized = ProfileStore.sanitize(name);
        flush();
        saveNow();
        activeProfile = sanitized;
        store.setActiveProfile(sanitized);
        load();
    }

    /**
     * Deletes a profile. The active profile cannot be deleted.
     *
     * @param name the profile to delete
     * @return {@code true} if a profile file was removed
     */
    public boolean deleteProfile(String name) {
        String sanitized = ProfileStore.sanitize(name);
        if (sanitized.equals(activeProfile)) {
            LOGGER.warn("Refusing to delete the active profile '{}'", sanitized);
            return false;
        }
        return store.deleteProfile(sanitized);
    }

    /**
     * Exports a profile to an external file.
     *
     * @param name the profile name
     * @param dest the destination file
     * @throws IOException if the export fails
     */
    public void exportProfile(String name, Path dest) throws IOException {
        flush();
        store.exportProfile(ProfileStore.sanitize(name), dest);
    }

    /**
     * Imports a profile from an external file.
     *
     * @param source     the source file
     * @param targetName the name to store it under
     * @return the sanitized name the profile was stored as
     * @throws IOException if the import fails
     */
    public String importProfile(Path source, String targetName) throws IOException {
        return store.importProfile(source, targetName);
    }

    /**
     * Flushes pending saves and stops the autosave scheduler. Call on shutdown.
     */
    public void shutdown() {
        flush();
        scheduler.shutdown();
    }

    private JsonObject buildProfileJson() {
        JsonObject root = new JsonObject();
        JsonObject modulesJson = new JsonObject();
        for (Module module : modules) {
            JsonObject moduleJson = new JsonObject();
            moduleJson.addProperty("enabled", module.isEnabled());
            JsonObject settingsJson = new JsonObject();
            for (Setting<?> setting : module.getSettings()) {
                settingsJson.add(setting.getName(), setting.save());
            }
            moduleJson.add("settings", settingsJson);
            modulesJson.add(module.getName(), moduleJson);
        }
        root.add("modules", modulesJson);
        return root;
    }

    private void applyProfileJson(JsonObject root) {
        if (!root.has("modules") || !root.get("modules").isJsonObject()) {
            return;
        }
        JsonObject modulesJson = root.getAsJsonObject("modules");
        for (Module module : modules) {
            if (!modulesJson.has(module.getName())) {
                continue;
            }
            JsonObject moduleJson = modulesJson.getAsJsonObject(module.getName());
            if (moduleJson.has("settings") && moduleJson.get("settings").isJsonObject()) {
                JsonObject settingsJson = moduleJson.getAsJsonObject("settings");
                for (Setting<?> setting : module.getSettings()) {
                    if (settingsJson.has(setting.getName())) {
                        setting.load(settingsJson.get(setting.getName()));
                    }
                }
            }
            if (moduleJson.has("enabled")) {
                module.setEnabled(moduleJson.get("enabled").getAsBoolean());
            }
        }
    }
}
