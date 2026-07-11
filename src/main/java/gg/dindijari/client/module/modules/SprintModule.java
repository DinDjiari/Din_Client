package gg.dindijari.client.module.modules;

import gg.dindijari.client.module.Category;
import gg.dindijari.client.module.Module;
import gg.dindijari.client.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.lwjgl.glfw.GLFW;

/**
 * Automatically keeps the player sprinting while moving forward, so the sprint
 * key does not need to be held.
 *
 * <p>This is a convenience only and grants no advantage beyond vanilla: it
 * respects the same constraints the game itself imposes on sprinting (forward
 * movement, sufficient hunger, not blindly forcing sprint into walls). Vanilla
 * already ships an accessibility "Toggle Sprint" option; this mirrors it.
 *
 * <p>Serves as the Phase&nbsp;1 reference module: it is toggled by a keybind and
 * its enabled state is persisted across restarts.
 */
public class SprintModule extends Module {

    /** Below this food level vanilla disallows sprinting. */
    private static final int MIN_FOOD_TO_SPRINT = 6;

    private final BooleanSetting keepSprintOnHunger = new BooleanSetting(
            "Respect Hunger",
            "Stop auto-sprinting when hunger is too low, exactly like vanilla.",
            true);

    /**
     * Creates the Sprint module with a default toggle keybind of the 'J' key.
     */
    public SprintModule() {
        super("Sprint", "Automatically sprints while you move forward.", Category.UTILITY);
        getKeybind().set(GLFW.GLFW_KEY_J);
        addSetting(keepSprintOnHunger);
    }

    @Override
    protected void onTick() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        // player.zza is the forward movement input: positive means "forwards".
        boolean movingForward = player.zza > 0.0F;
        boolean hungerOk = !keepSprintOnHunger.get()
                || player.getFoodData().getFoodLevel() > MIN_FOOD_TO_SPRINT;
        if (movingForward && hungerOk && !player.isSprinting()
                && !player.isUsingItem() && !player.horizontalCollision) {
            player.setSprinting(true);
        }
    }
}
