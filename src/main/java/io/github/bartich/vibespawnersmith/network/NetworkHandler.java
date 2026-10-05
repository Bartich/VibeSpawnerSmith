package io.github.bartich.vibespawnersmith.network;

import io.github.bartich.vibespawnersmith.VibeSpawnerSmith;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.Locale;

public class NetworkHandler {
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1.0.0");

        registrar.playToServer(
                UpdateSpawnerPayload.TYPE,
                UpdateSpawnerPayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        ServerPlayer player = (ServerPlayer) context.player();
                        if (!player.hasPermissions(2)) return;

                        BlockPos p = payload.pos();
                        int cooldownTicks = payload.oneShot() ? 2147483647 : Math.max(0, payload.cooldownSeconds() * 20);

                        StringBuilder potentialsBuilder = new StringBuilder();
                        String firstMobTag = "{id:\"minecraft:breeze\"}";
                        boolean first = true;

                        for (UpdateSpawnerPayload.MobConfig mob : payload.mobs()) {
                            String entityId = ensureNamespace(mob.entityId().trim());
                            if (entityId.isEmpty() || !BuiltInRegistries.ENTITY_TYPE.containsKey(ResourceLocation.parse(entityId))) {
                                entityId = "minecraft:breeze";
                            }

                            StringBuilder nbt = new StringBuilder();
                            nbt.append(String.format("id:\"%s\"", entityId));

                            // Сборка экипировки
                            assembleGear(nbt, mob);

                            // Сборка Эффектов
                            assembleEffects(nbt, mob);

                            // Сборка Кастомного Дропа
                            if (mob.customDrops() != null && !mob.customDrops().trim().isEmpty()) {
                                String cleanDrop = mob.customDrops().trim().replace("\"", "");
                                // Отключаем ванильные кости/стрелы на уровне самой сущности
                                nbt.append(",DeathLootTable:\"minecraft:empty\"");
                                // Записываем кастомный лут во все совместимые теги
                                nbt.append(String.format(",vibe_drops:\"%s\",NeoForgeData:{vibe_drops:\"%s\"}", cleanDrop, cleanDrop));
                            }

                            String fullEntityTag = "{" + nbt + "}";

                            if (first) {
                                firstMobTag = fullEntityTag;
                                first = false;
                            } else {
                                potentialsBuilder.append(",");
                            }

                            potentialsBuilder.append(String.format("{weight:%d,data:{entity:%s}}", Math.max(1, mob.weight()), fullEntityTag));
                        }

                        if (potentialsBuilder.isEmpty()) {
                            potentialsBuilder.append("{weight:1,data:{entity:{id:\"minecraft:breeze\"}}}");
                        }

                        String cmd = String.format(
                                "data merge block %d %d %d {trial_spawner_state:\"waiting_for_players\",target_cooldown_length:%d,spawn_data:{entity:%s},normal_config:{loot_tables_to_eject:[{data:\"%s\",weight:1}],simultaneous_mobs:%d.0f,spawn_potentials:[%s],ticks_between_spawn:%d,total_mobs:%d.0f}}",
                                p.getX(), p.getY(), p.getZ(),
                                cooldownTicks,
                                firstMobTag,
                                payload.lootTable(),
                                payload.simultaneousMobs(),
                                potentialsBuilder.toString(),
                                payload.ticksBetween(),
                                payload.totalMobs()
                        );

                        CommandSourceStack source = player.server.createCommandSourceStack().withPermission(4).withSuppressedOutput();
                        player.server.getCommands().performPrefixedCommand(source, cmd);
                    });
                }
        );
    }

    private static void assembleGear(StringBuilder nbt, UpdateSpawnerPayload.MobConfig mob) {
        String mh = fixItem(mob.mainhand());
        String oh = fixItem(mob.offhand());
        if (!mh.isEmpty() || !oh.isEmpty()) {
            nbt.append(String.format(",HandItems:[%s,%s]",
                    mh.isEmpty() ? "{}" : String.format("{id:\"%s\",count:1}", mh),
                    oh.isEmpty() ? "{}" : String.format("{id:\"%s\",count:1}", oh)
            ));
            float mhC = Math.max(0f, Math.min(1f, mob.mainDropChance() / 100f));
            float ohC = Math.max(0f, Math.min(1f, mob.offDropChance() / 100f));
            nbt.append(String.format(Locale.ROOT, ",HandDropChances:[%.2ff,%.2ff]", mhC, ohC));
        }
        String boots = fixItem(mob.boots());
        String legs = fixItem(mob.leggings());
        String chest = fixItem(mob.chestplate());
        String helm = fixItem(mob.helmet());
        if (!boots.isEmpty() || !legs.isEmpty() || !chest.isEmpty() || !helm.isEmpty()) {
            nbt.append(String.format(",ArmorItems:[%s,%s,%s,%s]",
                    boots.isEmpty() ? "{}" : String.format("{id:\"%s\",count:1}", boots),
                    legs.isEmpty() ? "{}" : String.format("{id:\"%s\",count:1}", legs),
                    chest.isEmpty() ? "{}" : String.format("{id:\"%s\",count:1}", chest),
                    helm.isEmpty() ? "{}" : String.format("{id:\"%s\",count:1}", helm)
            ));
            float bC = Math.max(0f, Math.min(1f, mob.bootsDropChance() / 100f));
            float lC = Math.max(0f, Math.min(1f, mob.legsDropChance() / 100f));
            float cC = Math.max(0f, Math.min(1f, mob.chestDropChance() / 100f));
            float hC = Math.max(0f, Math.min(1f, mob.helmDropChance() / 100f));
            nbt.append(String.format(Locale.ROOT, ",ArmorDropChances:[%.2ff,%.2ff,%.2ff,%.2ff]", bC, lC, cC, hC));
        }
    }

    private static void assembleEffects(StringBuilder nbt, UpdateSpawnerPayload.MobConfig mob) {
        if (!mob.effects().trim().isEmpty()) {
            StringBuilder effNbt = new StringBuilder();
            boolean firstEff = true;
            for (String effRaw : mob.effects().split(",")) {
                String eff = effRaw.trim();
                if (eff.isEmpty()) continue;
                String id = eff;
                int amp = 0;
                if (eff.contains(":")) {
                    String[] parts = eff.split(":");
                    id = parts[0].trim();
                    try { amp = Math.max(0, Integer.parseInt(parts[1].trim()) - 1); } catch (Exception ignored) {}
                }
                if (!firstEff) effNbt.append(",");
                effNbt.append(String.format("{id:\"%s\",amplifier:%db,duration:99999,show_particles:0b}", ensureNamespace(id), amp));
                firstEff = false;
            }
            if (!effNbt.isEmpty()) nbt.append(String.format(",active_effects:[%s]", effNbt));
        }
    }

    private static String ensureNamespace(String id) {
        if (id.isEmpty()) return "";
        return id.contains(":") ? id : "minecraft:" + id;
    }

    private static String fixItem(String id) {
        return ensureNamespace(id.trim());
    }
}