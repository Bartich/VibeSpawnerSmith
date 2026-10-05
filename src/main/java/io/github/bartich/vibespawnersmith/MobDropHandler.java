package io.github.bartich.vibespawnersmith;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = VibeSpawnerSmith.MODID)
public class MobDropHandler {

    private record DropEntry(String itemId, int min, int max, float val) {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDrops(LivingDropsEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) return;

        String dropData = "";
        CompoundTag persistent = entity.getPersistentData();
        if (persistent.contains("vibe_drops")) {
            dropData = persistent.getString("vibe_drops");
        } else {
            CompoundTag fullTag = new CompoundTag();
            entity.saveWithoutId(fullTag);
            if (fullTag.contains("vibe_drops")) {
                dropData = fullTag.getString("vibe_drops");
            }
        }

        dropData = dropData.trim();
        if (dropData.isEmpty()) return;

        // Полная отмена ванильного дропа
        event.getDrops().clear();
        if (dropData.equalsIgnoreCase("none") || dropData.equalsIgnoreCase("empty") || dropData.equals("0")) {
            return;
        }

        RandomSource random = entity.getRandom();

        // Разбиваем на пулы
        String[] pools = dropData.split(java.util.regex.Pattern.quote("|"));

        for (String poolStr : pools) {
            poolStr = poolStr.trim();
            if (!poolStr.startsWith("pool(") || !poolStr.contains(")")) continue;

            int closeParen = poolStr.indexOf(")");
            int openBracket = poolStr.indexOf("[");
            int closeBracket = poolStr.lastIndexOf("]");
            if (openBracket == -1 || closeBracket <= openBracket) continue;

            String header = poolStr.substring(5, closeParen);
            String body = poolStr.substring(openBracket + 1, closeBracket);

            String[] hParts = header.split(",");
            int rolls = 1;
            boolean isPool = true;
            try { rolls = Integer.parseInt(hParts[0].trim()); } catch (Exception ignored) {}
            if (hParts.length > 1) {
                isPool = Boolean.parseBoolean(hParts[1].trim());
            }

            List<DropEntry> entries = new ArrayList<>();
            float totalWeight = 0.0f;

            if (!body.isEmpty()) {
                for (String it : body.split(";")) {
                    it = it.trim();
                    if (it.isEmpty()) continue;

                    int lastColon = it.lastIndexOf(":");
                    if (lastColon == -1) continue;
                    float val = 1.0f;
                    try { val = Float.parseFloat(it.substring(lastColon + 1).trim()); } catch (Exception ignored) {}

                    String rem = it.substring(0, lastColon);
                    int secondLastColon = rem.lastIndexOf(":");
                    if (secondLastColon == -1) continue;

                    String rangeStr = rem.substring(secondLastColon + 1).trim();
                    String id = rem.substring(0, secondLastColon).trim();

                    int min = 1, max = 1;
                    if (rangeStr.contains("-")) {
                        String[] rng = rangeStr.split("-");
                        try { min = Integer.parseInt(rng[0].trim()); } catch (Exception ignored) {}
                        try { max = Integer.parseInt(rng[1].trim()); } catch (Exception ignored) {}
                    } else {
                        try { min = max = Integer.parseInt(rangeStr); } catch (Exception ignored) {}
                    }

                    entries.add(new DropEntry(id, min, max, val));
                    totalWeight += val;
                }
            }

            if (entries.isEmpty()) continue;

            // РЕЖИМ 1: ПУЛ (Рулетка по весу)
            if (isPool) {
                if (totalWeight <= 0) continue;
                for (int r = 0; r < rolls; r++) {
                    float pick = random.nextFloat() * totalWeight;
                    float current = 0.0f;
                    for (DropEntry entry : entries) {
                        current += entry.val();
                        if (pick <= current) {
                            spawnDrop(entity, random, entry);
                            break;
                        }
                    }
                }
            }
            // РЕЖИМ 2: ШАНС % (Каждый предмет чекается отдельно)
            else {
                for (int r = 0; r < rolls; r++) {
                    for (DropEntry entry : entries) {
                        float roll = random.nextFloat() * 100.0f;
                        if (roll <= entry.val()) {
                            spawnDrop(entity, random, entry);
                        }
                    }
                }
            }
        }
    }

    private static void spawnDrop(LivingEntity entity, RandomSource random, DropEntry entry) {
        String id = entry.itemId;
        if (!id.contains(":")) id = "minecraft:" + id;

        ResourceLocation rl = ResourceLocation.tryParse(id.toLowerCase());
        if (rl != null && BuiltInRegistries.ITEM.containsKey(rl)) {
            Item item = BuiltInRegistries.ITEM.get(rl);
            if (item != Items.AIR) {
                int min = Math.min(entry.min, entry.max);
                int max = Math.max(entry.min, entry.max);
                int count = min >= max ? min : min + random.nextInt(max - min + 1);

                while (count > 0) {
                    int stackSize = Math.min(count, item.getDefaultMaxStackSize());
                    count -= stackSize;

                    ItemEntity dropEntity = new ItemEntity(
                            entity.level(),
                            entity.getX(), entity.getY() + 0.5, entity.getZ(),
                            new ItemStack(item, stackSize)
                    );
                    dropEntity.setDefaultPickUpDelay();
                    entity.level().addFreshEntity(dropEntity);
                }
            }
        }
    }
}