package gg.dindijari.client.module.modules.client;

import gg.dindijari.client.module.Category;
import gg.dindijari.client.module.Module;
import gg.dindijari.client.setting.BooleanSetting;
import gg.dindijari.client.setting.StringSetting;

/**
 * Settings-only module configuring the crash assistant: whether the themed
 * crash-report screen appears after a crash, and how the optional AI analysis
 * reaches the user's <em>local</em> Ollama install.
 *
 * <p>Analysis never leaves the machine: the client only ever talks to the
 * configured base URL (default {@code http://localhost:11434}) with the
 * configured model (default {@code llama3.2}). No API key, no cloud service.
 */
public final class CrashAssistantModule extends Module {

    private final BooleanSetting crashScreen = new BooleanSetting(
            "Crash Screen", "Show the themed crash-report screen on the next start after a crash.",
            true);
    private final StringSetting ollamaUrl = new StringSetting(
            "Ollama URL", "Base URL of the local Ollama server used for crash analysis.",
            "http://localhost:11434");
    private final StringSetting ollamaModel = new StringSetting(
            "Ollama Model", "Ollama model used for crash analysis (e.g. llama3.2).",
            "llama3.2");

    /**
     * Creates the crash assistant module.
     */
    public CrashAssistantModule() {
        super("Crash Assistant",
                "Crash-report screen with optional AI analysis via your local Ollama.",
                Category.CLIENT);
        setToggleable(false);
        addSetting(crashScreen, ollamaUrl, ollamaModel);
    }

    /**
     * Returns the crash-screen toggle.
     *
     * @return the crash screen setting
     */
    public BooleanSetting crashScreen() {
        return crashScreen;
    }

    /**
     * Returns the Ollama base-URL setting.
     *
     * @return the URL setting
     */
    public StringSetting ollamaUrl() {
        return ollamaUrl;
    }

    /**
     * Returns the Ollama model-name setting.
     *
     * @return the model setting
     */
    public StringSetting ollamaModel() {
        return ollamaModel;
    }

    /**
     * The base URL to contact, trimmed of trailing slashes; falls back to the
     * default when blank.
     *
     * @return the effective Ollama base URL
     */
    public String effectiveUrl() {
        String value = ollamaUrl.get().trim();
        if (value.isEmpty()) {
            value = ollamaUrl.getDefault();
        }
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    /**
     * The model name to request; falls back to the default when blank.
     *
     * @return the effective model name
     */
    public String effectiveModel() {
        String value = ollamaModel.get().trim();
        return value.isEmpty() ? ollamaModel.getDefault() : value;
    }
}
