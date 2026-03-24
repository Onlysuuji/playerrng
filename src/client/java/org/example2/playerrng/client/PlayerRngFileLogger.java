package org.example2.playerrng.client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import net.fabricmc.loader.api.FabricLoader;

public final class PlayerRngFileLogger {
    private static final Object LOCK = new Object();
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final Path LOG_PATH = FabricLoader.getInstance().getGameDir().resolve("logs").resolve("playerrng.log");
    private static boolean initialized = false;
    private static boolean failed = false;

    private PlayerRngFileLogger() {}

    public static void init() {
        synchronized (LOCK) {
            if (initialized) {
                return;
            }

            try {
                Files.createDirectories(LOG_PATH.getParent());
                writeLineLocked("===== playerrng session start =====");
                writeLineLocked("log_path=" + LOG_PATH.toAbsolutePath());
                initialized = true;
            } catch (IOException e) {
                fail(e);
            }
        }
    }

    public static void log(String message) {
        synchronized (LOCK) {
            if (!initialized && !failed) {
                init();
            }

            if (failed) {
                return;
            }

            try {
                writeLineLocked("[" + LocalDateTime.now().format(TIMESTAMP_FORMATTER) + "] " + message);
            } catch (IOException e) {
                fail(e);
            }
        }
    }

    public static Path getLogPath() {
        return LOG_PATH;
    }

    private static void writeLineLocked(String line) throws IOException {
        Files.writeString(
                LOG_PATH,
                line + System.lineSeparator(),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
        );
    }

    private static void fail(IOException e) {
        if (failed) {
            return;
        }

        failed = true;
        System.err.println("[playerrng] failed to write log file: " + e.getMessage());
    }
}
