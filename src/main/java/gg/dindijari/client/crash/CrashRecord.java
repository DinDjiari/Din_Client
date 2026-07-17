package gg.dindijari.client.crash;

/**
 * The crash information persisted between sessions: written when a crash is
 * detected (uncaught exception or a new file in {@code crash-reports/}) and
 * read back on the next client start to drive the crash-report screen.
 *
 * <p>Serialized with Gson to {@code config/dindijariclient/last-crash.json}.
 */
public final class CrashRecord {

    /** Epoch millis of the crash. */
    public long timestamp;

    /** Absolute path of the vanilla crash-report file, if one was written. */
    public String reportFile;

    /** One-line summary (exception class + message), if captured. */
    public String summary;

    /** Full stack trace captured by the uncaught-exception hook, if any. */
    public String stackTrace;

    /**
     * Creates an empty record (Gson).
     */
    public CrashRecord() {
    }
}
