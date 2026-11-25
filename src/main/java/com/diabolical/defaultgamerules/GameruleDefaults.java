package com.diabolical.defaultgamerules;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.fml.ModList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public final class GameruleDefaults {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(GameruleDefaults.class);

    private static final Path CONFIG =
            FMLPaths.CONFIGDIR.get().resolve("gamerule_defaults.toml");

    private static boolean syncDefaultsInOldWorld = false;

    private GameruleDefaults() {}

    public static boolean shouldPromptOldWorlds() { return syncDefaultsInOldWorld; }

    public static void initialize() {
        try {
            if (!Files.exists(CONFIG)) {
                save(Map.of());
            }
            load();
        } catch (Exception exception) {
            LOGGER.error("Could not initialize gamerule defaults!", exception);
        }
    }

    /**
     * Reads grouped sections such as [gamerules.minecraft] and
     * [gamerules.some_mod]. The former [gamerules] format is also accepted
     * so existing installations retain their configured defaults.
     */
    private static Map<String, String> load() throws IOException {
        Map<String, String> result = new TreeMap<>();
        String currentNamespace = null;
        syncDefaultsInOldWorld = false;

        for (String original : Files.readAllLines(CONFIG)) {
            String line = original.trim();

            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            if (currentNamespace == null && line.startsWith("syncDefaultsInOldWorld")) {
                int optionSeparator = line.indexOf("=");
                if (optionSeparator >= 0) syncDefaultsInOldWorld = Boolean.parseBoolean(line.substring(optionSeparator + 1).trim());
                continue;
            }
            if (line.startsWith("[") && line.endsWith("]")) {
                String section = line.substring(1, line.length() - 1).trim();
                if (section.equals("gamerules")) {
                    currentNamespace = "minecraft";
                } else if (section.startsWith("gamerules.")) {
                    currentNamespace = section.substring("gamerules.".length());
                } else {
                    currentNamespace = null;
                }
                continue;
            }

            if (currentNamespace == null) {
                continue;
            }

            int separator = line.indexOf('=');
            if (separator < 0) {
                continue;
            }

            String key = unquote(line.substring(0, separator).trim());
            String value = unquote(line.substring(separator + 1).trim());

            if (value.equalsIgnoreCase("true")
                    || value.equalsIgnoreCase("false")
                    || value.matches("-?\\d+")) {
                String id = key.contains(":") ? key : currentNamespace + ":" + key;
                result.put(id, value);
            } else {
                LOGGER.warn("Invalid value for gamerule {}: {}", key, value);
            }
        }

        return result;
    }

    private static String unquote(String value) {
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    /**
     * GameRules.Key#getId() in Minecraft 1.21.1 exposes only the rule name,
     * not its registering mod namespace. For the common convention where a
     * mod prefixes its rule names with its mod id in camelCase, recover that
     * namespace from the loaded mod list. Rules without a recognizable
     * prefix remain in minecraft because the API provides no owner metadata.
     */
    private static String qualifiedId(String ruleName) {
        String owner = ModList.get().getMods().stream()
                .map(mod -> mod.getModId())
                .filter(modId -> ruleName.startsWith(toCamelCase(modId)))
                .max(java.util.Comparator.comparingInt(String::length))
                .orElse("minecraft");
        return owner + ":" + ruleName;
    }

    private static String toCamelCase(String modId) {
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = false;
        for (char character : modId.toCharArray()) {
            if (character == '_' || character == '-') {
                capitalizeNext = true;
            } else {
                result.append(capitalizeNext ? Character.toUpperCase(character) : character);
                capitalizeNext = false;
            }
        }
        return result.toString();
    }

    private static String namespace(String id) {
        int separator = id.indexOf(':');
        return separator >= 0 ? id.substring(0, separator) : "minecraft";
    }

    private static String path(String id) {
        int separator = id.indexOf(':');
        return separator >= 0 ? id.substring(separator + 1) : id;
    }

    private static void save(Map<String, String> values) throws IOException {
        Map<String, Map<String, String>> grouped = new TreeMap<>();
        values.forEach((id, value) ->
                grouped.computeIfAbsent(namespace(id), ignored -> new TreeMap<>())
                        .put(path(id), value)
        );

        StringBuilder output = new StringBuilder();
        output.append("# Diabolical Default Gamerules\n");
        output.append("# Ask before applying configured gamerules to existing worlds.\n");
        output.append("syncDefaultsInOldWorld = ").append(syncDefaultsInOldWorld).append("\n");
        output.append("# Only gamerules differing from their registered startup defaults are stored.\n");
        output.append("# Gamerules are grouped by the mod that registered them.\n");

        if (grouped.isEmpty()) {
            output.append("\n# No gamerule overrides are currently saved.\n");
        }

        grouped.forEach((modId, rules) -> {
            output.append("\n[gamerules.").append(modId).append("]\n");
            rules.forEach((key, value) ->
                    output.append("\"").append(key).append("\" = ")
                            .append(value).append("\n")
            );
        });

        Path temporary = CONFIG.resolveSibling(CONFIG.getFileName() + ".tmp");
        Files.writeString(temporary, output.toString());

        try {
            Files.move(temporary, CONFIG,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException exception) {
            Files.move(temporary, CONFIG, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Applies configured overrides to the supplied world only. Returns the
     * number of gamerules whose values actually changed.
     */
    public static int apply(ServerLevel level) {
        Map<String, String> configured;
        try {
            configured = load();
        } catch (IOException exception) {
            LOGGER.error("Failed to read gamerule config!", exception);
            return 0;
        }

        if (configured.isEmpty()) {
            return 0;
        }

        Set<String> found = new HashSet<>();
        int[] changed = {0};
        GameRules rules = level.getGameRules();
        MinecraftServer server = level.getServer();

        GameRules.visitGameRuleTypes(new GameRules.GameRuleTypeVisitor() {
            @Override
            public void visitBoolean(GameRules.Key<GameRules.BooleanValue> key,
                                     GameRules.Type<GameRules.BooleanValue> type) {
                String id = qualifiedId(key.getId());
                found.add(id);
                String configuredValue = configured.get(id);
                if (configuredValue == null) {
                    return;
                }

                if (!configuredValue.equalsIgnoreCase("true")
                        && !configuredValue.equalsIgnoreCase("false")) {
                    LOGGER.warn("Invalid boolean value for gamerule {}", id);
                    return;
                }

                boolean desired = Boolean.parseBoolean(configuredValue);
                GameRules.BooleanValue current = rules.getRule(key);
                if (current.get() != desired) {
                    current.set(desired, server);
                    changed[0]++;
                }
            }

            @Override
            public void visitInteger(GameRules.Key<GameRules.IntegerValue> key,
                                     GameRules.Type<GameRules.IntegerValue> type) {
                String id = qualifiedId(key.getId());
                found.add(id);
                String configuredValue = configured.get(id);
                if (configuredValue == null) {
                    return;
                }

                try {
                    int desired = Integer.parseInt(configuredValue);
                    GameRules.IntegerValue current = rules.getRule(key);
                    if (current.get() != desired) {
                        current.set(desired, server);
                        changed[0]++;
                    }
                } catch (NumberFormatException exception) {
                    LOGGER.warn("Invalid integer gamerule value for {}", id);
                }
            }
        });

        configured.keySet().stream()
                .filter(key -> !found.contains(key))
                .forEach(key -> LOGGER.warn("Unknown gamerule in config: {}", key));

        return changed[0];
    }

    /**
     * Saves gamerules that differ from their registered startup defaults.
     */
    public static int saveDefaults(MinecraftServer server) {
        Map<String, String> changed = new TreeMap<>();
        GameRules defaults = new GameRules();
        GameRules current = server.overworld().getGameRules();

        GameRules.visitGameRuleTypes(new GameRules.GameRuleTypeVisitor() {
            @Override
            public void visitBoolean(GameRules.Key<GameRules.BooleanValue> key,
                                     GameRules.Type<GameRules.BooleanValue> type) {
                compare(key);
            }

            @Override
            public void visitInteger(GameRules.Key<GameRules.IntegerValue> key,
                                     GameRules.Type<GameRules.IntegerValue> type) {
                compare(key);
            }

            private void compare(GameRules.Key<?> key) {
                String startup = defaults.getRule(key).serialize();
                String actual = current.getRule(key).serialize();
                if (!startup.equals(actual)) {
                    changed.put(qualifiedId(key.getId()), actual);
                }
            }
        });

        try {
            save(changed);
            return changed.size();
        } catch (IOException exception) {
            LOGGER.error("Failed to save gamerule defaults!", exception);
            return 0;
        }
    }
}