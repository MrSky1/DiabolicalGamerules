package com.diabolical.defaultgamerules;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.HashSet;

import net.neoforged.fml.loading.FMLPaths;

public final class GameruleDefaults {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(GameruleDefaults.class);

    private static final Path CONFIG =
            FMLPaths.CONFIGDIR.get()
                    .resolve("gamerule_defaults.toml");

    private GameruleDefaults() {}

    public static void initialize() {
        try {
            if (!Files.exists(CONFIG)) {
                save(Map.of());
            }

            load();

        } catch (Exception exception) {
            LOGGER.error(
                    "Could not initialize gamerule defaults!",
                    exception
            );
        }
    }

    private static Map<String, String> load()
            throws IOException {

        Map<String, String> result = new TreeMap<>();

        boolean inGamerules = false;

        for (String original :
                Files.readAllLines(CONFIG)) {

            String line = original.trim();

            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            if (line.startsWith("[")) {
                inGamerules =
                        line.equals("[gamerules]");
                continue;
            }

            if (!inGamerules) {
                continue;
            }

            int separator = line.indexOf('=');

            if (separator < 0) {
                continue;
            }

            String key =
                    line.substring(0, separator).trim();

            String value =
                    line.substring(separator + 1).trim();

            if (key.startsWith("\"")
                    && key.endsWith("\"")) {
                key = key.substring(1, key.length() - 1);
            }

            if (value.startsWith("\"")
                    && value.endsWith("\"")) {
                value = value.substring(1, value.length() - 1);
            }

            if (value.equalsIgnoreCase("true")
                    || value.equalsIgnoreCase("false")
                    || value.matches("-?\\d+")) {

                result.put(key, value);
            } else {
                LOGGER.warn(
                        "Invalid value for gamerule {}: {}",
                        key, value
                );
            }
        }

        return result;
    }

    private static void save(
            Map<String, String> values
    ) throws IOException {

        StringBuilder output = new StringBuilder();

        output.append("# Diabolical Default Gamerules\n");
        output.append("# Only gamerules differing from their\n");
        output.append("# registered startup defaults are stored.\n\n");
        output.append("[gamerules]\n");

        values.forEach((key, value) ->
                output.append("\"")
                        .append(key)
                        .append("\" = ")
                        .append(value)
                        .append("\n")
        );

        Path temporary = CONFIG.resolveSibling(
                CONFIG.getFileName() + ".tmp"
        );

        Files.writeString(temporary, output.toString());

        try {
            Files.move(
                    temporary,
                    CONFIG,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
            );
        } catch (IOException exception) {
            Files.move(
                    temporary,
                    CONFIG,
                    StandardCopyOption.REPLACE_EXISTING
            );
        }
    }

    public static void apply(ServerLevel level) {

        Map<String, String> configured;

        try {
            configured = load();
        } catch (IOException exception) {
            LOGGER.error("Failed to read gamerule config!", exception);
            return;
        }

        Set<String> found = new HashSet<>();

        GameRules rules = level.getGameRules();
        MinecraftServer server = level.getServer();

        GameRules.visitGameRuleTypes(
                new GameRules.GameRuleTypeVisitor() {

            @Override
            public void visitBoolean(
                    GameRules.Key<GameRules.BooleanValue> key,
                    GameRules.Type<GameRules.BooleanValue> type
            ) {
                String id = key.getId();
                found.add(id);

                String configuredValue = configured.get(id);

                if (configuredValue == null) {
                    return;
                }

                if (!configuredValue.equals("true")
                        && !configuredValue.equals("false")) {
                    LOGGER.warn(
                            "Invalid boolean value for {}",
                            id
                    );
                    return;
                }

                boolean desired =
                        Boolean.parseBoolean(configuredValue);

                GameRules.BooleanValue current =
                        rules.getRule(key);

                if (current.get() != desired) {
                    current.set(desired, server);
                }
            }

            @Override
            public void visitInteger(
                    GameRules.Key<GameRules.IntegerValue> key,
                    GameRules.Type<GameRules.IntegerValue> type
            ) {
                String id = key.getId();
                found.add(id);

                String configuredValue = configured.get(id);

                if (configuredValue == null) {
                    return;
                }

                try {
                    int desired =
                            Integer.parseInt(configuredValue);

                    GameRules.IntegerValue current =
                            rules.getRule(key);

                    if (current.get() != desired) {
                        current.set(desired, server);
                    }

                } catch (NumberFormatException exception) {
                    LOGGER.warn(
                            "Invalid integer gamerule value for {}",
                            id
                    );
                }
            }
        });

        configured.keySet().stream()
                .filter(key -> !found.contains(key))
                .forEach(key -> LOGGER.warn(
                        "Unknown gamerule in config: {}", key
                ));
    }

    public static int sync(MinecraftServer server) {

        Map<String, String> changed = new TreeMap<>();

        GameRules defaults = new GameRules();

        GameRules current =
                server.overworld().getGameRules();

        GameRules.visitGameRuleTypes(
                new GameRules.GameRuleTypeVisitor() {

            @Override
            public void visitBoolean(
                    GameRules.Key<GameRules.BooleanValue> key,
                    GameRules.Type<GameRules.BooleanValue> type
            ) {
                compare(key);
            }

            @Override
            public void visitInteger(
                    GameRules.Key<GameRules.IntegerValue> key,
                    GameRules.Type<GameRules.IntegerValue> type
            ) {
                compare(key);
            }

            private void compare(GameRules.Key<?> key) {

                String startup =
                        defaults.getRule(key).serialize();

                String actual =
                        current.getRule(key).serialize();

                if (!startup.equals(actual)) {
                    changed.put(key.getId(), actual);
                }
            }
        });

        try {
            save(changed);
            return changed.size();

        } catch (IOException exception) {
            LOGGER.error(
                    "Failed to synchronize gamerule config!",
                    exception
            );

            return 0;
        }
    }
}