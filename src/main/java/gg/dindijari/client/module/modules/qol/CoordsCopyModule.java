package gg.dindijari.client.module.modules.qol;

import gg.dindijari.client.module.Category;
import gg.dindijari.client.module.Module;
import gg.dindijari.client.setting.KeybindSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

/**
 * Copies your current block coordinates ("x y z") to the clipboard when the
 * copy key (default F8) is pressed in-world, and confirms with an action-bar
 * message.
 */
public final class CoordsCopyModule extends Module {

    private final KeybindSetting copyKey = new KeybindSetting(
            "Copy Key", "Copies your coordinates to the clipboard.", GLFW.GLFW_KEY_F8);

    /**
     * Creates the module and registers its (enabled-gated) key listener.
     */
    public CoordsCopyModule() {
        super("Coords Copy", "Hotkey that copies your coordinates to the clipboard.",
                Category.UTILITY);
        addSetting(copyKey);
        NeoForge.EVENT_BUS.addListener(this::onKey);
    }

    private void onKey(InputEvent.Key event) {
        if (!isEnabled() || event.getAction() != GLFW.GLFW_PRESS || !copyKey.matches(event.getKey())) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.screen != null) {
            return;
        }
        String coords = String.format(Locale.ROOT, "%d %d %d",
                player.blockPosition().getX(), player.blockPosition().getY(),
                player.blockPosition().getZ());
        minecraft.keyboardHandler.setClipboard(coords);
        player.displayClientMessage(Component.literal("Copied " + coords + " to clipboard"), true);
    }
}
