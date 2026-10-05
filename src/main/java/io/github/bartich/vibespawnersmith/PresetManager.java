package io.github.bartich.vibespawnersmith;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.neoforged.fml.loading.FMLPaths;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class PresetManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE_PATH = FMLPaths.CONFIGDIR.get().resolve("vibespawnersmith_presets.json");

    public static final String LAST_USED_KEY = "_last_used";

    public record SpawnerPreset(
            String name,
            List<SavedMob> mobs,
            int totalMobs,
            int simultaneousMobs,
            int ticksBetween,
            int cooldownSeconds,
            String lootTable,
            boolean oneShot
    ) {}

    public record SavedMob(
            String id,
            int weight,
            String mainhand, float mainDropChance,
            String offhand, float offDropChance,
            String helmet, float helmDropChance,
            String chestplate, float chestDropChance,
            String leggings, float legsDropChance,
            String boots, float bootsDropChance,
            String customDrops,
            String effects
    ) {}

    private static final Map<String, SpawnerPreset> PRESETS = new LinkedHashMap<>();

    public static void load() {
        if (!Files.exists(FILE_PATH)) {
            createDefaultPresets();
            save();
            return;
        }

        try (Reader reader = Files.newBufferedReader(FILE_PATH)) {
            Type type = new TypeToken<Map<String, SpawnerPreset>>() {}.getType();
            Map<String, SpawnerPreset> loaded = GSON.fromJson(reader, type);
            if (loaded != null) {
                PRESETS.clear();
                PRESETS.putAll(loaded);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void save() {
        try {
            if (FILE_PATH.getParent() != null) {
                Files.createDirectories(FILE_PATH.getParent());
            }
            try (Writer writer = Files.newBufferedWriter(FILE_PATH)) {
                GSON.toJson(PRESETS, writer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void putPreset(String name, SpawnerPreset preset) {
        PRESETS.put(name, preset);
        save();
    }

    public static SpawnerPreset getPreset(String name) {
        return PRESETS.get(name);
    }

    public static void removePreset(String name) {
        PRESETS.remove(name);
        save();
    }

    // Метод для PresetSelectionScreen
    public static Map<String, SpawnerPreset> getAllPresets() {
        return Collections.unmodifiableMap(PRESETS);
    }

    public static List<String> getPresetNames() {
        List<String> list = new ArrayList<>();
        for (String key : PRESETS.keySet()) {
            if (!key.equals(LAST_USED_KEY)) {
                list.add(key);
            }
        }
        return list;
    }

    public static SpawnerPreset getLastUsed() {
        return PRESETS.get(LAST_USED_KEY);
    }

    private static void createDefaultPresets() {
        PRESETS.put("Бриз-испытание", new SpawnerPreset(
                "Бриз-испытание",
                List.of(new SavedMob("minecraft:breeze", 1, "", 0, "", 0, "", 0, "", 0, "", 0, "", 0, "", "")),
                4, 1, 60, 1800, "minecraft:chests/trial_chambers/reward", false
        ));
        PRESETS.put("Черепной легион (Босс)", new SpawnerPreset(
                "Черепной легион (Босс)",
                List.of(
                        new SavedMob("minecraft:wither_skeleton", 1, "minecraft:netherite_sword", 100.0f, "", 0, "minecraft:netherite_helmet", 50.0f, "", 0, "", 0, "", 0, "pool(1,false)[minecraft:nether_star:1-1:100.0]", "speed:2"),
                        new SavedMob("minecraft:skeleton", 3, "minecraft:bow", 8.5f, "", 0, "minecraft:iron_helmet", 0, "", 0, "", 0, "", 0, "pool(1,false)[minecraft:arrow:2-5:100.0]", "")
                ),
                8, 3, 30, 0, "minecraft:chests/trial_chambers/reward_ominous", true
        ));
    }
}