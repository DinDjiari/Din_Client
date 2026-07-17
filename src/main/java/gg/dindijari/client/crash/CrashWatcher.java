package gg.dindijari.client.crash;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Records crashes so they can be presented on the <em>next</em> client start.
 *
 * <p>When Minecraft hard-crashes the render pipeline is usually gone, so no
 * overlay can be shown at crash time. Instead this watcher captures the crash
 * through two independent hooks and persists a {@link CrashRecord} to
 * {@code config/dindijariclient/last-crash.json}:
 *
 * <ul>
 *   <li>a <b>default uncaught-exception handler</b> (chained in front of any
 *       previously installed handler) that writes the throwable's stack trace
 *       immediately;</li>
 *   <li>a <b>shutdown hook</b> that scans {@code crash-reports/} for report
 *       files created during this session — this also catches render-thread
 *       crashes, which Minecraft handles itself (they never reach an uncaught
 *       handler) but which always produce a report file before the forced
 *       exit.</li>
 * </ul>
 *
 * <p>On the next start {@link #install} reads and deletes the record; the
 * screen manager then shows the themed crash-report screen once the title
 * screen is up.
 */
public final class CrashWatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger("dindijariclient/crash");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String RECORD_FILE = "last-crash.json";

    private static Path recordFile;
    private static Path crashReportsDir;
    private static long sessionStartMillis;
    private static volatile boolean recorded;
    private static CrashRecord previousCrash;

    private CrashWatcher() {
    }

    /**
     * Installs the crash hooks and loads (then clears) any crash recorded by
     * the previous session. Called once during mod construction.
     *
     * @param gameDir    the game directory (containing {@code crash-reports/})
     * @param configRoot the client's config root directory
     */
    public static void install(Path gameDir, Path configRoot) {
        recordFile = configRoot.resolve(RECORD_FILE);
        crashReportsDir = gameDir.resolve("crash-reports");
        sessionStartMillis = System.currentTimeMillis();

        previousCrash = loadAndClearRecord();

        Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            try {
                recordThrowable(thread, throwable);
            } catch (Throwable t) {
                // Never make a crash worse.
            }
            if (previous != null) {
                previous.uncaughtException(thread, throwable);
            } else {
                throwable.printStackTrace();
            }
        });

        Runtime.getRuntime().addShutdownHook(
                new Thread(CrashWatcher::scanCrashReportsOnExit, "dindijari-crash-scan"));
        LOGGER.info("Crash watcher installed (session start {})", sessionStartMillis);
    }

    /**
     * The crash recorded by the previous session, if any. The on-disk record
     * is already cleared, so it is shown at most once.
     *
     * @return the previous session's crash, or {@code null}
     */
    public static CrashRecord previousCrash() {
        return previousCrash;
    }

    /**
     * The directory vanilla writes crash reports to.
     *
     * @return the {@code crash-reports} directory
     */
    public static Path crashReportsDir() {
        return crashReportsDir;
    }

    private static void recordThrowable(Thread thread, Throwable throwable) {
        CrashRecord record = new CrashRecord();
        record.timestamp = System.currentTimeMillis();
        record.summary = throwable.getClass().getName()
                + (throwable.getMessage() == null ? "" : ": " + throwable.getMessage())
                + " (thread " + thread.getName() + ")";
        StringWriter writer = new StringWriter();
        throwable.printStackTrace(new PrintWriter(writer));
        record.stackTrace = writer.toString();
        Path report = findNewCrashReport();
        record.reportFile = report == null ? null : report.toAbsolutePath().toString();
        writeRecord(record);
    }

    private static void scanCrashReportsOnExit() {
        try {
            if (recorded) {
                // The uncaught handler already wrote a record; enrich it with
                // the report file if one appeared afterwards.
                return;
            }
            Path report = findNewCrashReport();
            if (report == null) {
                return; // clean shutdown
            }
            CrashRecord record = new CrashRecord();
            record.timestamp = System.currentTimeMillis();
            record.reportFile = report.toAbsolutePath().toString();
            record.summary = report.getFileName().toString();
            writeRecord(record);
        } catch (Throwable t) {
            // Shutdown hooks must never throw.
        }
    }

    /** Newest crash-report file created during this session, or null. */
    private static Path findNewCrashReport() {
        if (crashReportsDir == null || !Files.isDirectory(crashReportsDir)) {
            return null;
        }
        try (Stream<Path> stream = Files.list(crashReportsDir)) {
            return stream
                    .filter(p -> p.getFileName().toString().endsWith(".txt"))
                    .filter(p -> {
                        try {
                            return Files.getLastModifiedTime(p).toMillis()
                                    >= sessionStartMillis - 5000L;
                        } catch (IOException e) {
                            return false;
                        }
                    })
                    .max(Comparator.comparing(p -> {
                        try {
                            return Files.getLastModifiedTime(p).toMillis();
                        } catch (IOException e) {
                            return 0L;
                        }
                    }))
                    .orElse(null);
        } catch (IOException e) {
            return null;
        }
    }

    private static synchronized void writeRecord(CrashRecord record) {
        try {
            Files.createDirectories(recordFile.getParent());
            Files.writeString(recordFile, GSON.toJson(record), StandardCharsets.UTF_8);
            recorded = true;
        } catch (IOException e) {
            // Nothing sensible to do while crashing.
        }
    }

    private static CrashRecord loadAndClearRecord() {
        if (!Files.isRegularFile(recordFile)) {
            return null;
        }
        try {
            String json = Files.readString(recordFile, StandardCharsets.UTF_8);
            CrashRecord record = GSON.fromJson(json, CrashRecord.class);
            Files.deleteIfExists(recordFile);
            return record;
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Could not read the previous crash record", e);
            try {
                Files.deleteIfExists(recordFile);
            } catch (IOException ignored) {
                // best effort
            }
            return null;
        }
    }

    /**
     * Loads the full report text for a record: the vanilla report file when
     * available (capped at 256&nbsp;KB), otherwise the captured stack trace.
     *
     * @param record the crash record
     * @return the text to display; never {@code null}
     */
    public static String loadReportText(CrashRecord record) {
        if (record.reportFile != null) {
            try {
                Path path = Path.of(record.reportFile);
                if (Files.isRegularFile(path)) {
                    String text = Files.readString(path, StandardCharsets.UTF_8);
                    return text.length() > 256 * 1024 ? text.substring(0, 256 * 1024) : text;
                }
            } catch (IOException | RuntimeException e) {
                LOGGER.warn("Could not read crash report {}", record.reportFile, e);
            }
        }
        if (record.stackTrace != null && !record.stackTrace.isBlank()) {
            return (record.summary == null ? "" : record.summary + "\n\n") + record.stackTrace;
        }
        return record.summary == null ? "No crash details captured." : record.summary;
    }
}
