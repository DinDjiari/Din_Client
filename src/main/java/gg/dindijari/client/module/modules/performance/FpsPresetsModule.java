package gg.dindijari.client.module.modules.performance;

import gg.dindijari.client.gui.clickgui.ClickGuiScreen;
import gg.dindijari.client.module.Category;
import gg.dindijari.client.module.Module;
import gg.dindijari.client.setting.EnumSetting;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.ParticleStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * One-click vanilla video-settings presets (settings-only module). Cycling the
 * Preset setting in the Click GUI batch-applies these exact vanilla options:
 *
 * <ul>
 *   <li><b>Max FPS</b>: render distance 8, simulation distance 8, graphics
 *       Fast, clouds off, smooth lighting off, mipmap levels 0, particles
 *       Minimal, entity shadows off;</li>
 *   <li><b>Balanced</b>: render distance 12, simulation 10, graphics Fancy,
 *       clouds Fast, smooth lighting on, mipmaps 2, particles Decreased,
 *       entity shadows on;</li>
 *   <li><b>Quality</b>: render distance 16, simulation 12, graphics Fancy,
 *       clouds Fancy, smooth lighting on, mipmaps 4, particles All, entity
 *       shadows on.</li>
 * </ul>
 *
 * <p>Presets apply only when changed by the user in the Click GUI — loading
 * the saved config at startup never overwrites your vanilla settings. No FPS
 * numbers are promised; the preset just batch-sets the options above.
 */
public final class FpsPresetsModule extends Module {

    private static final Logger LOGGER = LoggerFactory.getLogger("dindijariclient/presets");

    /** The available presets. */
    public enum Preset {
        MAX_FPS, BALANCED, QUALITY
    }

    private final EnumSetting<Preset> preset = new EnumSetting<>(
            "Preset", "Batch-applies vanilla video settings.", Preset.BALANCED);

    /**
     * Creates the module.
     */
    public FpsPresetsModule() {
        super("FPS Presets",
                "One-click vanilla video-settings presets.",
                Category.PERFORMANCE);
        setToggleable(false);
        addSetting(preset);
        preset.onChange(this::applyIfUserContext);
    }

    /**
     * Applies only for user-driven changes (the Click GUI is open); config
     * loading at startup must never stomp the player's vanilla settings.
     */
    private void applyIfUserContext(Preset chosen) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.options == null
                || !(minecraft.screen instanceof ClickGuiScreen)) {
            return;
        }
        Options options = minecraft.options;
        switch (chosen) {
            case MAX_FPS -> apply(options, 8, 8, GraphicsStatus.FAST, CloudStatus.OFF,
                    false, 0, ParticleStatus.MINIMAL, false);
            case BALANCED -> apply(options, 12, 10, GraphicsStatus.FANCY, CloudStatus.FAST,
                    true, 2, ParticleStatus.DECREASED, true);
            case QUALITY -> apply(options, 16, 12, GraphicsStatus.FANCY, CloudStatus.FANCY,
                    true, 4, ParticleStatus.ALL, true);
        }
        options.save();
        LOGGER.info("Applied video preset {}", chosen);
    }

    private static void apply(Options options, int renderDistance, int simulationDistance,
                              GraphicsStatus graphics, CloudStatus clouds, boolean smoothLighting,
                              int mipmaps, ParticleStatus particles, boolean entityShadows) {
        options.renderDistance().set(renderDistance);
        options.simulationDistance().set(simulationDistance);
        options.graphicsMode().set(graphics);
        options.cloudStatus().set(clouds);
        options.ambientOcclusion().set(smoothLighting);
        options.mipmapLevels().set(mipmaps);
        options.particles().set(particles);
        options.entityShadows().set(entityShadows);
        Minecraft.getInstance().levelRenderer.allChanged();
        Minecraft.getInstance().updateMaxMipLevel(mipmaps);
    }
}
