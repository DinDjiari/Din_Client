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
import gg.dindijari.client.module.modules.qol.MusicModule;
import gg.dindijari.client.module.modules.qol.NotificationsModule;
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
    public static final String MOD_VERSION = "1.3.0";

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
        moduleManager.register(new gg.dindijari.client.module.modules.BrandingModule());

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
                new AutoReconnectModule(),
                new MusicModule(),
                new NotificationsModule());

        Path configRoot = FMLPaths.CONFIGDIR.get().resolve(MOD_ID);
        // First launch = no config profile has ever been written.
        boolean firstLaunch = !java.nio.file.Files.exists(
                configRoot.resolve("profiles").resolve("default.json"));
        ConfigManager configManager = new ConfigManager(configRoot);
        configManager.bind(moduleManager.getModules());
        configManager.load();
        if (firstLaunch) {
            // Options are not available yet during mod construction; apply on
            // the first client tick instead (exactly once).
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                    DindijariClient::muteMusicOnFirstTick);
        }

        moduleManager.registerEvents();
        new ScreenManager().registerEvents();
        Services.install(moduleManager, configManager);

        Runtime.getRuntime().addShutdownHook(
                new Thread(configManager::shutdown, "dindijari-config-flush"));

        LOGGER.info("{} ready", MOD_NAME);
    }

    /** One-shot guard for the first-launch music mute. */
    private static boolean musicMuteApplied;

    /**
     * Sets the vanilla Music volume to 0 exactly once — on the very first
     * launch, before any config existed. Later launches never touch it; the
     * "Music" module in the Click GUI (or the vanilla slider) turns it back
     * on. Documented in the README. Runs on the first client tick because
     * options are not yet constructed during mod loading.
     *
     * @param event the client tick
     */
    private static void muteMusicOnFirstTick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {
        if (musicMuteApplied) {
            return;
        }
        net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
        if (minecraft == null || minecraft.options == null) {
            return;
        }
        musicMuteApplied = true;
        minecraft.options.getSoundSourceOptionInstance(
                net.minecraft.sounds.SoundSource.MUSIC).set(0.0);
        minecraft.options.save();
        LOGGER.info("First launch: vanilla music volume set to 0 (re-enable via the Music module)");
    }
}
