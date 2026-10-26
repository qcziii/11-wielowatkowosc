package pl.kurs.threads;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Application settings. Many threads read them at the same time (every HTTP request), and from time to time
 * one thread reloads everything from a file.
 * <ul>
 *     <li>Readers do not block each other: many threads can read at the same time.</li>
 *     <li>Reloading is exclusive: during {@link #reload(Map)} nobody reads,
 *         so nobody sees a half-old, half-new configuration.</li>
 *     <li>An exception while reading must not lock up the configuration.</li>
 * </ul>
 * Implement it with {@link java.util.concurrent.locks.ReentrantReadWriteLock}.
 */
public class Configuration {

    private final Map<String, String> settings = new HashMap<>();

    public Configuration(Map<String, String> initial) {
        settings.putAll(initial);
    }

    /** Value of one setting, or null. */
    public String get(String key) {
        // TODO
        throw new UnsupportedOperationException("TODO: implement get");
    }

    /**
     * Runs the function on a consistent view of all settings (e.g. to read several related values at once).
     * The function must not modify the map.
     */
    public <T> T read(Function<Map<String, String>, T> function) {
        // TODO
        throw new UnsupportedOperationException("TODO: implement read");
    }

    /** Replaces all settings with new ones. */
    public void reload(Map<String, String> newSettings) {
        // TODO: clear the settings and put the new ones so that nobody sees an intermediate state
        throw new UnsupportedOperationException("TODO: implement reload");
    }
}
