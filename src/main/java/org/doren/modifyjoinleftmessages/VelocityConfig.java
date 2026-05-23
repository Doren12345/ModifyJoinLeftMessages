package org.doren.modifyjoinleftmessages;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;

public final class VelocityConfig {

    private final Path configPath;
    private final Path knownPlayersPath;
    private final Yaml yaml = new Yaml();
    private Map<String, Object> values = Collections.emptyMap();

    public VelocityConfig(Path dataDirectory) {
        this.configPath = dataDirectory.resolve("config.yml");
        this.knownPlayersPath = dataDirectory.resolve("known-players.yml");
    }

    public void ensureDefaultConfig(InputStream resource) throws IOException {
        Files.createDirectories(configPath.getParent());
        if (Files.exists(configPath)) {
            return;
        }
        if (resource == null) {
            throw new IOException("Missing bundled config.yml");
        }
        try (InputStream in = resource) {
            Files.copy(in, configPath);
        }
    }

    public void load() throws IOException {
        if (!Files.exists(configPath)) {
            this.values = Collections.emptyMap();
            return;
        }
        try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
            Object loaded = yaml.load(reader);
            if (loaded instanceof Map) {
                //noinspection unchecked
                this.values = (Map<String, Object>) loaded;
            } else {
                this.values = Collections.emptyMap();
            }
        }
    }

    public String getString(String path, String defaultValue) {
        Object value = resolve(path);
        return value == null ? defaultValue : String.valueOf(value);
    }

    public boolean getBoolean(String path, boolean defaultValue) {
        Object value = resolve(path);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return defaultValue;
    }

    public Map<String, Boolean> loadKnownPlayers() throws IOException {
        if (!Files.exists(knownPlayersPath)) {
            return new LinkedHashMap<>();
        }
        try (Reader reader = Files.newBufferedReader(knownPlayersPath, StandardCharsets.UTF_8)) {
            Object loaded = yaml.load(reader);
            if (!(loaded instanceof Map)) {
                return new LinkedHashMap<>();
            }
            Map<String, Boolean> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) loaded).entrySet()) {
                result.put(String.valueOf(entry.getKey()), Boolean.parseBoolean(String.valueOf(entry.getValue())));
            }
            return result;
        }
    }

    public void saveKnownPlayers(Map<String, Boolean> knownPlayers) throws IOException {
        Files.createDirectories(knownPlayersPath.getParent());
        try (Writer writer = Files.newBufferedWriter(knownPlayersPath, StandardCharsets.UTF_8)) {
            yaml.dump(knownPlayers, writer);
        }
    }

    public boolean has(String path) {
        return resolve(path) != null;
    }

    private Object resolve(String path) {
        String[] parts = path.split("\\.");
        Object current = values;
        for (String part : parts) {
            if (!(current instanceof Map)) {
                return null;
            }
            current = ((Map<?, ?>) current).get(part);
        }
        return current;
    }
}
