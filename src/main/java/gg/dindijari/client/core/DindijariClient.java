package gg.dindijari.client.core;

import gg.dindijari.client.config.ConfigManager;
import gg.dindijari.client.gui.ScreenManager;
import gg.dindijari.client.module.ModuleManager;
import gg.dindijari.client.module.modules.SprintModule;
import gg.dindijari.client.module.modules.ThemeModule;
import gg.dindijari.client.module.modules.performance.FpsLimiterModule;
import gg.dindijari.client.module.modules.performance.FpsPresetsModule;
import gg.dindijari.client.module.modules.performance.LowParticlesModule;
import gg.dindijari.client.module.modules.performance.NoEntityShadowsModule;
import gg.dindijari.client.module.modules.performance.NoFogModule;
import gg.dindijari.client.module.modules.performance.PerformanceModeModule;
import gg.dindijari.client.module.modules.qol.AutoReconnectModule;
import gg.dindijari.client.module.modules.qol.ChatTimestampsModule;
import gg.dindijari.client.module.modules.qol.CoordsCopyModule;
import gg.dindijari.client.module.modules.qol.FullbrightModule;
import gg.dindijari.client.module.modules.qol.ServerIpHideModule;
import gg.dindijari.client.module.modules.qol.ZoomModule;
import gg.dindijari.client.render.Theme;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/**
 * Mod entrypoint for the Dindijari Client.
 *
 * <p>The mod is client-only ({@link Dist#CLIENT}); it registers no packets and
 * makes no protocol changes, so it never runs on a dedicated server and remains
 * safe to use on vanilla servers.
 *
 * <p>Start-up wiring:
 * <ol>
 *   <li>build the {@link ModuleManager} and register modules;</li>
 *   <li>build the {@link ConfigManager} rooted at
 *       {@code config/dindijariclient/} and load the active profile;</li>
 *   <li>subscribe module input/tick handlers to the NeoForge event bus;</li>
 *   <li>publish both managers through {@link Services}.</li>
 * </ol>
 */
@Mod(value = DindijariClient.MOD_ID, dist = Dist.CLIENT)
public final class DindijariClient {

    /** The mod id, matching {@code neoforge.mods.toml}. */
    public static final String MOD_ID = "dindijariclient";

    /** The human readable mod name. */
    public static final String MOD_NAME = "Dindijari Client";

    /** The mod version shown in UI footers; kept in sync with gradle.properties. */
    public static final String MOD_VERSION = "0.1.0";

    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    /**
     * Constructs and initializes the client. Invoked by NeoForge during mod
     * loading with the mod-specific event bus and container injected.
     *
     * @param modEventBus  the mod-specific event bus
     * @param modContainer this mod's container
     */
    public DindijariClient(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Initializing {}", MOD_NAME);

        ModuleManager moduleManager = new ModuleManager();
        moduleManager.register(new SprintModule());
        ThemeModule themeModule = new ThemeModule();
        moduleManager.register(themeModule);
        Theme.install(themeModule);

        PerformanceModeModule performanceMode = new PerformanceModeModule();
        Theme.installPerformanceMode(performanceMode);
        moduleManager.register(
                performanceMode,
                new LowParticlesModule(),
                new NoEntityShadowsModule(),
                new NoFogModule(),
                new FpsPresetsModule(),
                new FpsLimiterModule(),
                new ZoomModule(),
                new FullbrightModule(),
                new ChatTimestampsModule(),
                new CoordsCopyModule(),
                new ServerIpHideModule(),
                new AutoReconnectModule());

        Path configRoot = FMLPaths.CONFIGDIR.get().resolve(MOD_ID);
        ConfigManager configManager = new ConfigManager(configRoot);
        configManager.bind(moduleManager.getModules());
        configManager.load();

        moduleManager.registerEvents();
        new ScreenManager().registerEvents();
        Services.install(moduleManager, configManager);

        Runtime.getRuntime().addShutdownHook(
                new Thread(configManager::shutdown, "dindijari-config-flush"));

        LOGGER.info("{} ready", MOD_NAME);
    }
}
