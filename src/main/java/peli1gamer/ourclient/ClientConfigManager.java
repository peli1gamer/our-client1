package peli1gamer.ourclient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/** Named, user-managed configuration profiles. */
public final class ClientConfigManager {
    private static final String DEFAULT_PROFILE = "default";
    private static final int MAX_NAME_LENGTH = 32;
    private static final int MAX_PROFILES = 64;
    private final Path directory;

    public ClientConfigManager(Path gameDirectory) {
        this.directory = gameDirectory.resolve("config/our-client1/profiles");
    }

    public List<String> list() {
        try {
            if (!Files.isDirectory(directory)) return List.of();
            try (Stream<Path> files = Files.list(directory)) {
                return files.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().endsWith(".json"))
                        .map(path -> path.getFileName().toString().substring(0, path.getFileName().toString().length() - 5))
                        .sorted(String.CASE_INSENSITIVE_ORDER)
                        .limit(MAX_PROFILES)
                        .toList();
            }
        } catch (IOException | RuntimeException exception) {
            OurClient.LOGGER.warn("Could not list Arson config profiles", exception);
            return List.of();
        }
    }

    public boolean save(String name, ClientConfig config) {
        if (config == null) return false;
        String safe = sanitizeName(name);
        if (safe == null) return false;
        try {
            Files.createDirectories(directory);
            config.save(path(safe));
            return Files.isRegularFile(path(safe));
        } catch (RuntimeException | IOException exception) {
            OurClient.LOGGER.warn("Could not save Arson config profile '{}'", safe, exception);
            return false;
        }
    }

    public ClientConfig load(String name) {
        String safe = sanitizeName(name);
        if (safe == null) return null;
        Path profile = path(safe);
        if (!Files.isRegularFile(profile)) return null;
        return ClientConfig.load(profile);
    }

    public boolean delete(String name) {
        String safe = sanitizeName(name);
        if (safe == null || DEFAULT_PROFILE.equals(safe)) return false;
        try { return Files.deleteIfExists(path(safe)); }
        catch (IOException | RuntimeException exception) {
            OurClient.LOGGER.warn("Could not delete Arson config profile '{}'", safe, exception);
            return false;
        }
    }

    public boolean ensureDefault(ClientConfig current) {
        if (current == null) return false;
        if (Files.isRegularFile(path(DEFAULT_PROFILE))) return true;
        return save(DEFAULT_PROFILE, current);
    }

    private Path path(String safeName) { return directory.resolve(safeName + ".json"); }

    private static String sanitizeName(String name) {
        if (name == null) return null;
        String value = name.trim().toLowerCase(Locale.ROOT);
        if (value.isEmpty() || value.length() > MAX_NAME_LENGTH) return null;
        if (!value.matches("[a-z0-9][a-z0-9._-]*")) return null;
        if (value.equals(".") || value.equals("..")) return null;
        return value;
    }
}
