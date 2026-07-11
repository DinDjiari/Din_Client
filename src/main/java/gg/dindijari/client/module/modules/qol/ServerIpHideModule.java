package gg.dindijari.client.module.modules.qol;

import gg.dindijari.client.module.Category;
import gg.dindijari.client.module.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.List;

/**
 * Streaming privacy: while enabled, the current server address is masked in
 * the F3 debug overlay and in the client's own multiplayer server list. The
 * mask is display-only; connections and saved data are untouched.
 */
public final class ServerIpHideModule extends Module {

    /** Shared instance so screens can consult the toggle cheaply. */
    private static ServerIpHideModule instance;

    /**
     * Creates the module and registers its (enabled-gated) debug-text filter.
     */
    public ServerIpHideModule() {
        super("Server IP Hide", "Masks the server address in F3 and the server list.",
                Category.UTILITY);
        instance = this;
        NeoForge.EVENT_BUS.addListener(this::onDebugText);
    }

    /**
     * Whether addresses should currently be masked.
     *
     * @return {@code true} while the module is enabled
     */
    public static boolean active() {
        return instance != null && instance.isEnabled();
    }

    private void onDebugText(CustomizeGuiOverlayEvent.DebugText event) {
        if (!isEnabled()) {
            return;
        }
        ServerData server = Minecraft.getInstance().getCurrentServer();
        if (server == null || server.ip == null || server.ip.isEmpty()) {
            return;
        }
        List<String> left = event.getLeft();
        for (int i = 0; i < left.size(); i++) {
            String line = left.get(i);
            if (line != null && line.contains(server.ip)) {
                left.set(i, line.replace(server.ip, "\u2022\u2022\u2022 hidden \u2022\u2022\u2022"));
            }
        }
    }
}
