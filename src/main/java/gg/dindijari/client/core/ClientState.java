package gg.dindijari.client.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Tiny persistent key/value store for client-local UI state that does not
 * belong in a config profile (e.g. "don't show the Sodium recommendation
 * again", the last folder browsed in the icon picker). Stored as
 * {@code config/dindijariclient/client_state.json} and written through
 * immediately on change — the amounts are tiny and losing a flag to a crash
 * would resurface dismissed dialogs.
 */
public final class ClientState {

    private static final Logger LOGGER = LoggerFactory.getLogger("dindijariclient/state");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Path file;
    private static JsonObject data = new JsonObject();

    private ClientState() {
    }

    /**
     * Loads the state file (if any). Called once during mod construction.
     *
     * @param configRoot the client's config root directory
     */
    public static synchronized void init(Path configRoot) {
        file = configRoot.resolve("client_state.json");
        if (!Files.isRegularFile(file)) {
            return;
        }
        try {
            var parsed = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
            if (parsed.isJsonObject()) {
                data = parsed.getAsJsonObject();
            }
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Could not read client state; starting fresh", e);
        }
    }

    /**
     * Reads a boolean flag.
     *
     * @param key      the flag name
     * @param fallback value when the flag is unset
     * @return the stored value or the fallback
     */
    public static synchronized boolean getBool(String key, boolean fallback) {
        return data.has(key) && data.get(key).isJsonPrimitive()
                ? data.get(key).getAsBoolean() : fallback;
    }

    /**
     * Stores a boolean flag and writes the file.
     *
     * @param key   the flag name
     * @param value the value to store
     */
    public static synchronized void setBool(String key, boolean value) {
        data.addProperty(key, value);
        write();
    }

    /**
     * Reads a string value.
     *
     * @param key      the entry name
     * @param fallback value when unset
     * @return the stored value or the fallback
     */
    public static synchronized String getString(String key, String fallback) {
        return data.has(key) && data.get(key).isJsonPrimitive()
                ? data.get(key).getAsString() : fallback;
    }

    /**
     * Stores a string value and writes the file.
     *
     * @param key   the entry name
     * @param value the value to store
     */
    public static synchronized void setString(String key, String value) {
        data.addProperty(key, value);
        write();
    }

    private static void write() {
        if (file == null) {
            return;
        }
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(data), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.warn("Could not write client state", e);
        }
    }
}
