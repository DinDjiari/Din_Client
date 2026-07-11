package gg.dindijari.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Low-level filesystem storage for configuration profiles.
 *
 * <p>Each profile is a JSON file under {@code <root>/profiles/}. The name of the
 * active profile is persisted in {@code <root>/meta.json}. This class only deals
 * in raw {@link JsonObject}s and file IO; mapping modules to and from JSON is the
 * job of {@link ConfigManager}.
 *
 * <p>The class has no Minecraft dependency: the root directory is injected, so it
 * can be exercised against any temporary directory in tests.
 */
public final class ProfileStore {

    /** Name of the profile created when none exists yet. */
    public static final String DEFAULT_PROFILE = "default";

    private static final Logger LOGGER = LoggerFactory.getLogger("dindijariclient/config");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path root;
    private final Path profilesDir;
    private final Path metaFile;

    /**
     * Creates a store rooted at the given directory.
     *
     * @param root the configuration root directory (e.g. {@code config/dindijariclient})
     */
    public ProfileStore(Path root) {
        this.root = Objects.requireNonNull(root, "root");
        this.profilesDir = root.resolve("profiles");
        this.metaFile = root.resolve("meta.json");
    }

    /**
     * Creates the root and profiles directories if they do not yet exist.
     *
     * @throws IOException if the directories cannot be created
     */
    public void ensureDirectories() throws IOException {
        Files.createDirectories(profilesDir);
    }

    /**
     * Converts an arbitrary profile name into a safe file base name, keeping only
     * alphanumeric characters, spaces, hyphens and underscores.
     *
     * @param name the requested profile name
     * @return a sanitized name, never empty
     */
    public static String sanitize(String name) {
        if (name == null) {
            return DEFAULT_PROFILE;
        }
        String cleaned = name.trim().replaceAll("[^A-Za-z0-9 _-]", "").trim();
        return cleaned.isEmpty() ? DEFAULT_PROFILE : cleaned;
    }

    /**
     * Returns the absolute path of the file backing the named profile.
     *
     * @param name the profile name
     * @return the profile file path
     */
    public Path profilePath(String name) {
        return profilesDir.resolve(sanitize(name) + ".json");
    }

    /**
     * Lists the names of all stored profiles, sorted alphabetically.
     *
     * @return the profile names
     */
    public List<String> listProfiles() {
        List<String> names = new ArrayList<>();
        if (!Files.isDirectory(profilesDir)) {
            return names;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(profilesDir, "*.json")) {
            for (Path path : stream) {
                String fileName = path.getFileName().toString();
                names.add(fileName.substring(0, fileName.length() - ".json".length()));
            }
        } catch (IOException e) {
            LOGGER.error("Failed to list profiles in {}", profilesDir, e);
        }
        Collections.sort(names);
        return names;
    }

    /**
     * Indicates whether a profile with the given name exists.
     *
     * @param name the profile name
     * @return {@code true} if the profile file exists
     */
    public boolean exists(String name) {
        return Files.isRegularFile(profilePath(name));
    }

    /**
     * Writes a profile's JSON to disk, overwriting any existing file.
     *
     * @param name the profile name
     * @param data the profile data
     * @throws IOException if writing fails
     */
    public void writeProfile(String name, JsonObject data) throws IOException {
        ensureDirectories();
        Path path = profilePath(name);
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(data, writer);
        }
    }

    /**
     * Reads a profile's JSON from disk.
     *
     * @param name the profile name
     * @return the parsed object, or {@code null} if missing or invalid
     */
    public JsonObject readProfile(String name) {
        Path path = profilePath(name);
        if (!Files.isRegularFile(path)) {
            return null;
        }
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException | JsonSyntaxException | IllegalStateException e) {
            LOGGER.error("Failed to read profile '{}' from {}", name, path, e);
            return null;
        }
    }

    /**
     * Deletes the named profile, if present.
     *
     * @param name the profile name
     * @return {@code true} if a file was deleted
     */
    public boolean deleteProfile(String name) {
        try {
            return Files.deleteIfExists(profilePath(name));
        } catch (IOException e) {
            LOGGER.error("Failed to delete profile '{}'", name, e);
            return false;
        }
    }

    /**
     * Exports a profile to an external file path.
     *
     * @param name the profile name
     * @param dest the destination file
     * @throws IOException if the profile is missing or the copy fails
     */
    public void exportProfile(String name, Path dest) throws IOException {
        Path source = profilePath(name);
        if (!Files.isRegularFile(source)) {
            throw new IOException("Profile '" + name + "' does not exist");
        }
        Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Imports a profile from an external file, validating that it is JSON.
     *
     * @param source     the file to import
     * @param targetName the name to store it under
     * @return the sanitized name the profile was stored as
     * @throws IOException if the source is unreadable or not valid JSON
     */
    public String importProfile(Path source, String targetName) throws IOException {
        try (Reader reader = Files.newBufferedReader(source, StandardCharsets.UTF_8)) {
            JsonObject parsed = JsonParser.parseReader(reader).getAsJsonObject();
            String name = sanitize(targetName);
            writeProfile(name, parsed);
            return name;
        } catch (JsonSyntaxException | IllegalStateException e) {
            throw new IOException("File is not a valid profile: " + source, e);
        }
    }

    /**
     * Returns the name of the active profile, defaulting to {@link #DEFAULT_PROFILE}.
     *
     * @return the active profile name
     */
    public String getActiveProfile() {
        if (Files.isRegularFile(metaFile)) {
            try (Reader reader = Files.newBufferedReader(metaFile, StandardCharsets.UTF_8)) {
                JsonObject meta = JsonParser.parseReader(reader).getAsJsonObject();
                if (meta.has("activeProfile")) {
                    return meta.get("activeProfile").getAsString();
                }
            } catch (IOException | JsonSyntaxException | IllegalStateException e) {
                LOGGER.error("Failed to read meta file {}", metaFile, e);
            }
        }
        return DEFAULT_PROFILE;
    }

    /**
     * Persists the name of the active profile.
     *
     * @param name the profile name to record as active
     */
    public void setActiveProfile(String name) {
        JsonObject meta = new JsonObject();
        meta.addProperty("activeProfile", sanitize(name));
        try {
            ensureDirectories();
            try (Writer writer = Files.newBufferedWriter(metaFile, StandardCharsets.UTF_8)) {
                GSON.toJson(meta, writer);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to write meta file {}", metaFile, e);
        }
    }
}
