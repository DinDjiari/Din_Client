package gg.dindijari.client.core;

import gg.dindijari.client.config.ConfigManager;
import gg.dindijari.client.module.ModuleManager;

/**
 * Tiny service registry giving the rest of the client static access to the
 * long-lived singletons created during mod construction.
 *
 * <p>Kept deliberately minimal: it holds references only, performs no logic, and
 * is populated exactly once by {@link DindijariClient} at start-up.
 */
public final class Services {

    private static ModuleManager moduleManager;
    private static ConfigManager configManager;

    private Services() {
    }

    /**
     * Stores the shared service instances. Called once during start-up.
     *
     * @param modules the module manager
     * @param config  the config manager
     */
    static void install(ModuleManager modules, ConfigManager config) {
        moduleManager = modules;
        configManager = config;
    }

    /**
     * Returns the shared {@link ModuleManager}.
     *
     * @return the module manager
     * @throws IllegalStateException if accessed before start-up
     */
    public static ModuleManager modules() {
        return require(moduleManager);
    }

    /**
     * Returns the shared {@link ConfigManager}.
     *
     * @return the config manager
     * @throws IllegalStateException if accessed before start-up
     */
    public static ConfigManager config() {
        return require(configManager);
    }

    private static <T> T require(T service) {
        if (service == null) {
            throw new IllegalStateException("Dindijari services accessed before initialization");
        }
        return service;
    }
}
