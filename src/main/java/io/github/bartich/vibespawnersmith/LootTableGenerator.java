package io.github.bartich.vibespawnersmith;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.server.MinecraftServer;

import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class LootTableGenerator {

    public record CustomDrop(String itemId, int min, int max, float chancePercent) {}

    public static String generateMobLootTable(MinecraftServer server, String mobName, List<CustomDrop> drops) {
        if (drops.isEmpty()) return "";

        String cleanName = mobName.replace(":", "_").replace(" ", "_").toLowerCase();
        String fileName = "drop_" + cleanName;

        JsonObject root = new JsonObject();
        root.addProperty("type", "minecraft:entity");

        JsonArray pools = new JsonArray();
        JsonObject pool = new JsonObject();
        pool.addProperty("rolls", 1);

        JsonArray entries = new JsonArray();

        for (CustomDrop drop : drops) {
            String id = drop.itemId().trim();
            if (id.isEmpty()) continue;
            if (!id.contains(":")) id = "minecraft:" + id;

            JsonObject entry = new JsonObject();
            entry.addProperty("type", "minecraft:item");
            entry.addProperty("name", id);

            JsonArray functions = new JsonArray();
            JsonObject countFunc = new JsonObject();
            countFunc.addProperty("function", "minecraft:set_count");

            JsonObject countObj = new JsonObject();
            countObj.addProperty("type", "minecraft:uniform");
            int minCount = Math.max(1, drop.min());
            int maxCount = Math.max(minCount, drop.max());
            countObj.addProperty("min", minCount);
            countObj.addProperty("max", maxCount);
            countFunc.add("count", countObj);
            functions.add(countFunc);

            entry.add("functions", functions);

            if (drop.chancePercent() < 100.0f) {
                JsonArray conditions = new JsonArray();
                JsonObject chanceCond = new JsonObject();
                chanceCond.addProperty("condition", "minecraft:random_chance");
                chanceCond.addProperty("chance", Math.max(0.01f, drop.chancePercent() / 100.0f));
                conditions.add(chanceCond);
                entry.add("conditions", conditions);
            }

            entries.add(entry);
        }

        pool.add("entries", entries);
        pools.add(pool);
        root.add("pools", pools);

        try {
            // Сохраняем прямо в datapacks активного мира!
            Path worldDir = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT);
            Path datapackDir = worldDir.resolve("datapacks/vibe_drops/data/vibespawnersmith/loot_table");
            Files.createDirectories(datapackDir);

            // Создаем pack.mcmeta если его нет
            Path mcmeta = worldDir.resolve("datapacks/vibe_drops/pack.mcmeta");
            if (!Files.exists(mcmeta)) {
                String metaContent = "{\"pack\":{\"pack_format\":48,\"description\":\"Vibe Spawner Drops\"}}";
                Files.writeString(mcmeta, metaContent);
            }

            Path targetFile = datapackDir.resolve(fileName + ".json");
            try (Writer writer = Files.newBufferedWriter(targetFile)) {
                writer.write(root.toString());
            }

            // Мягко обновляем лут-таблицы на сервере
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withPermission(4).withSuppressedOutput(), "datapack enable \"file/vibe_drops\"");
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "vibespawnersmith:" + fileName;
    }
}