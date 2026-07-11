package gg.dindijari.client.module;

/**
 * The categories under which modules are grouped in the Click GUI.
 *
 * <p>Note that this is deliberately <em>not</em> a cheat client: there is no
 * "Combat" category. Categories reflect quality-of-life, cosmetic and
 * client-management concerns only.
 */
public enum Category {

    /** Heads-up display elements (FPS, coordinates, etc.). */
    HUD("HUD"),
    /** Visual and rendering tweaks. */
    VISUALS("Visuals"),
    /** Performance related toggles. */
    PERFORMANCE("Performance"),
    /** General utilities. */
    UTILITY("Utility"),
    /** Client configuration and meta features. */
    CLIENT("Client");

    private final String displayName;

    Category(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Returns the human readable name shown in the UI.
     *
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }
}
