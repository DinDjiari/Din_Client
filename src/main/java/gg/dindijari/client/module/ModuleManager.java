package gg.dindijari.client.module;

import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Owns every registered {@link Module}, provides lookup by name and category,
 * and bridges NeoForge input and tick events to the module lifecycle.
 *
 * <p>Keybind dispatch is handled through {@link InputEvent.Key}: on a key press
 * while no GUI screen is open, every module whose toggle keybind matches the
 * pressed key is toggled. Per-tick behaviour is driven from
 * {@link ClientTickEvent.Post}.
 */
public final class ModuleManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("dindijariclient/modules");

    private final Map<String, Module> byName = new LinkedHashMap<>();

    /**
     * Registers one or more modules. Duplicate names (case-insensitive) are
     * rejected with a warning.
     *
     * @param modules the modules to register
     */
    public void register(Module... modules) {
        for (Module module : modules) {
            String key = module.getName().toLowerCase(Locale.ROOT);
            if (byName.containsKey(key)) {
                LOGGER.warn("Duplicate module name '{}' ignored", module.getName());
                continue;
            }
            byName.put(key, module);
        }
    }

    /**
     * Looks up a module by name, ignoring case.
     *
     * @param name the module name
     * @return the module, or {@code null} if none matches
     */
    public Module getModule(String name) {
        return name == null ? null : byName.get(name.toLowerCase(Locale.ROOT));
    }

    /**
     * Returns all registered modules in registration order.
     *
     * @return an unmodifiable list of modules
     */
    public List<Module> getModules() {
        return Collections.unmodifiableList(new ArrayList<>(byName.values()));
    }

    /**
     * Returns all modules in the given category, in registration order.
     *
     * @param category the category to filter by
     * @return the matching modules
     */
    public List<Module> getByCategory(Category category) {
        List<Module> result = new ArrayList<>();
        for (Module module : byName.values()) {
            if (module.getCategory() == category) {
                result.add(module);
            }
        }
        return result;
    }

    /**
     * Subscribes the manager to the NeoForge game event bus. Call once after all
     * modules have been registered.
     */
    public void registerEvents() {
        IEventBus bus = NeoForge.EVENT_BUS;
        bus.addListener(this::onKey);
        bus.addListener(this::onClientTick);
        LOGGER.info("Module framework initialized with {} modules", byName.size());
    }

    private void onKey(InputEvent.Key event) {
        if (event.getAction() != GLFW.GLFW_PRESS) {
            return;
        }
        // Ignore keybinds while a GUI (chat, inventory, menu) is capturing input.
        if (Minecraft.getInstance().screen != null) {
            return;
        }
        int key = event.getKey();
        for (Module module : byName.values()) {
            if (module.getKeybind().matches(key)) {
                module.toggle();
                LOGGER.debug("Toggled module '{}' -> {}", module.getName(), module.isEnabled());
            }
        }
    }

    private void onClientTick(ClientTickEvent.Post event) {
        // Only tick while a world is loaded so modules can assume a player exists
        // once they check for it.
        for (Module module : byName.values()) {
            module.tick();
        }
    }
}
