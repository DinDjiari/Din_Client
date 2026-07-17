package gg.dindijari.client.util;

import net.minecraft.Util;
import net.neoforged.fml.ModList;

/**
 * Detection of and pointers to <a href="https://modrinth.com/mod/sodium">
 * Sodium</a> (Modrinth project {@code AANobbMI}), the recommended rendering
 * optimization mod. Sodium ships native NeoForge builds for 1.21.1
 * ({@code mc1.21.1-0.8.12-neoforge} at the time of writing) and is declared as
 * an optional dependency in {@code neoforge.mods.toml}.
 *
 * <p>The client never bundles or references Sodium code — detection is purely
 * by mod id, so building and running work identically with and without it.
 * The client's UI renders through the vanilla GUI pipeline ({@code GuiGraphics}
 * with vanilla render types) and ships no mixins, so there is nothing for
 * Sodium's renderer to conflict with.
 */
public final class SodiumIntegration {

    /** The Sodium mod id on NeoForge. */
    public static final String MOD_ID = "sodium";

    /** The Modrinth page offering the NeoForge 1.21.1 builds. */
    public static final String MODRINTH_URL = "https://modrinth.com/mod/sodium";

    private SodiumIntegration() {
    }

    /**
     * Indicates whether Sodium is loaded alongside the client.
     *
     * @return {@code true} if the {@code sodium} mod is present
     */
    public static boolean isLoaded() {
        return ModList.get() != null && ModList.get().isLoaded(MOD_ID);
    }

    /**
     * Opens the Sodium Modrinth page in the system browser.
     */
    public static void openModrinth() {
        Util.getPlatform().openUri(MODRINTH_URL);
    }
}
