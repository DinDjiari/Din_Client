package gg.dindijari.client.crash;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

/**
 * Minimal client for a <em>local</em> Ollama server ({@code /api/generate},
 * JSON via Gson). Used by the crash-report screen to produce a plain-language
 * explanation of a crash.
 *
 * <p>Privacy: the crash text is only ever sent to the user-configured base URL
 * (default {@code http://localhost:11434}) — the user's own Ollama install.
 * Nothing leaves the machine unless the user deliberately points the URL
 * elsewhere. There is no API key and no cost.
 *
 * <p>All work runs off-thread on the JDK HTTP client's async pipeline; the
 * returned future completes (or fails with an {@link OllamaException} carrying
 * a user-presentable German hint) and never blocks the render thread.
 */
public final class OllamaClient {

    /** How long to wait for the full completion (local LLMs can be slow). */
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(120);
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);

    /** Crash text beyond this many characters is truncated to fit the context window. */
    private static final int MAX_CRASH_CHARS = 12_000;

    private static final Gson GSON = new Gson();

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .build();

    /** Failure with a message suitable for direct display. */
    public static final class OllamaException extends RuntimeException {
        OllamaException(String userMessage, Throwable cause) {
            super(userMessage, cause);
        }
    }

    /**
     * Asks the local Ollama model to explain a crash.
     *
     * @param baseUrl   the Ollama base URL, e.g. {@code http://localhost:11434}
     * @param model     the model name, e.g. {@code llama3.2}
     * @param crashText the crash report / stack trace text
     * @return a future completing with the model's answer, or failing with an
     *         {@link OllamaException} whose message is user-presentable
     */
    public CompletableFuture<String> analyze(String baseUrl, String model, String crashText) {
        String truncated = crashText.length() > MAX_CRASH_CHARS
                ? crashText.substring(0, MAX_CRASH_CHARS) + "\n[... gekürzt ...]"
                : crashText;

        JsonObject payload = new JsonObject();
        payload.addProperty("model", model);
        payload.addProperty("prompt", buildPrompt(truncated));
        payload.addProperty("stream", false);

        HttpRequest request;
        try {
            request = HttpRequest.newBuilder(URI.create(baseUrl + "/api/generate"))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(payload),
                            StandardCharsets.UTF_8))
                    .build();
        } catch (IllegalArgumentException e) {
            return CompletableFuture.failedFuture(new OllamaException(
                    "Ungültige Ollama-URL: \"" + baseUrl + "\". Bitte in den Client Settings "
                            + "korrigieren (Standard: http://localhost:11434).", e));
        }

        return http.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .handle((response, error) -> {
                    if (error != null) {
                        throw mapTransportError(error, baseUrl, model);
                    }
                    return parseResponse(response, model);
                });
    }

    private static String buildPrompt(String crashText) {
        return """
                Du bist ein erfahrener Minecraft-Modding-Experte. Analysiere den folgenden \
                Minecraft-Crash-Report (NeoForge 1.21.1) und antworte auf Deutsch, kurz und klar:

                1. Was ist passiert? (einfache Sprache, keine Fachbegriffe ohne Erklärung)
                2. Was ist die wahrscheinliche Ursache? (welcher Mod oder welche Komponente)
                3. Was sollte der Spieler konkret tun, um das Problem zu beheben?

                Crash-Report:
                ---
                """ + crashText + "\n---";
    }

    private String parseResponse(HttpResponse<String> response, String model) {
        if (response.statusCode() == 404) {
            String body = response.body() == null ? "" : response.body().toLowerCase(Locale.ROOT);
            if (body.contains("model") || body.contains("not found")) {
                throw new OllamaException("Modell \"" + model + "\" wurde nicht gefunden. "
                        + "Installiere es mit `ollama pull " + model + "`.", null);
            }
            throw new OllamaException("Ollama hat mit 404 geantwortet — ist die URL korrekt? "
                    + "(Standard: http://localhost:11434)", null);
        }
        if (response.statusCode() != 200) {
            String detail = extractError(response.body());
            throw new OllamaException("Ollama-Fehler (HTTP " + response.statusCode() + ")"
                    + (detail.isEmpty() ? "" : ": " + detail), null);
        }
        try {
            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            if (json.has("error")) {
                throw new OllamaException("Ollama-Fehler: " + json.get("error").getAsString(), null);
            }
            String answer = json.has("response") ? json.get("response").getAsString().trim() : "";
            if (answer.isEmpty()) {
                throw new OllamaException("Ollama hat eine leere Antwort geliefert.", null);
            }
            return answer;
        } catch (OllamaException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new OllamaException("Antwort von Ollama konnte nicht gelesen werden.", e);
        }
    }

    private static String extractError(String body) {
        if (body == null || body.isBlank()) {
            return "";
        }
        try {
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            return json.has("error") ? json.get("error").getAsString() : "";
        } catch (RuntimeException e) {
            return "";
        }
    }

    private static OllamaException mapTransportError(Throwable error, String baseUrl, String model) {
        Throwable cause = error;
        while (cause.getCause() != null && !(cause instanceof HttpTimeoutException)
                && !(cause instanceof ConnectException)) {
            cause = cause.getCause();
        }
        if (cause instanceof HttpTimeoutException) {
            return new OllamaException("Zeitüberschreitung — das Modell hat zu lange gebraucht. "
                    + "Versuche es erneut oder nutze ein kleineres Modell.", error);
        }
        if (cause instanceof ConnectException) {
            return new OllamaException("Ollama ist unter " + baseUrl + " nicht erreichbar. "
                    + "Läuft `ollama serve`? Modell installiert mit `ollama pull " + model + "`?",
                    error);
        }
        return new OllamaException("Verbindung zu Ollama fehlgeschlagen: "
                + cause.getClass().getSimpleName()
                + (cause.getMessage() == null ? "" : " (" + cause.getMessage() + ")"), error);
    }
}
