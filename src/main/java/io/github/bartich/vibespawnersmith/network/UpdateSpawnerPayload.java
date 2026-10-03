package io.github.bartich.vibespawnersmith.network;

import io.github.bartich.vibespawnersmith.VibeSpawnerSmith;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public record UpdateSpawnerPayload(
        BlockPos pos,
        List<MobConfig> mobs,
        int totalMobs,
        int simultaneousMobs,
        int ticksBetween,
        int cooldownSeconds,
        String lootTable,
        boolean oneShot
) implements CustomPacketPayload {

    public record MobConfig(
            String entityId,
            int weight,
            String mainhand,
            String offhand,
            String helmet,
            String chestplate,
            String leggings,
            String boots,
            String effects // Формат: "speed:1, strength:2"
    ) {
        public static void encode(ByteBuf buf, MobConfig mob) {
            ByteBufCodecs.STRING_UTF8.encode(buf, mob.entityId());
            ByteBufCodecs.VAR_INT.encode(buf, mob.weight());
            ByteBufCodecs.STRING_UTF8.encode(buf, mob.mainhand());
            ByteBufCodecs.STRING_UTF8.encode(buf, mob.offhand());
            ByteBufCodecs.STRING_UTF8.encode(buf, mob.helmet());
            ByteBufCodecs.STRING_UTF8.encode(buf, mob.chestplate());
            ByteBufCodecs.STRING_UTF8.encode(buf, mob.leggings());
            ByteBufCodecs.STRING_UTF8.encode(buf, mob.boots());
            ByteBufCodecs.STRING_UTF8.encode(buf, mob.effects());
        }

        public static MobConfig decode(ByteBuf buf) {
            return new MobConfig(
                    ByteBufCodecs.STRING_UTF8.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.STRING_UTF8.decode(buf),
                    ByteBufCodecs.STRING_UTF8.decode(buf),
                    ByteBufCodecs.STRING_UTF8.decode(buf),
                    ByteBufCodecs.STRING_UTF8.decode(buf),
                    ByteBufCodecs.STRING_UTF8.decode(buf),
                    ByteBufCodecs.STRING_UTF8.decode(buf),
                    ByteBufCodecs.STRING_UTF8.decode(buf)
            );
        }
    }

    public static final Type<UpdateSpawnerPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(VibeSpawnerSmith.MODID, "update_spawner")
    );

    public static final StreamCodec<ByteBuf, UpdateSpawnerPayload> STREAM_CODEC = StreamCodec.of(
            UpdateSpawnerPayload::encode,
            UpdateSpawnerPayload::decode
    );

    private static void encode(ByteBuf buffer, UpdateSpawnerPayload payload) {
        BlockPos.STREAM_CODEC.encode(buffer, payload.pos());
        ByteBufCodecs.VAR_INT.encode(buffer, payload.mobs().size());
        for (MobConfig m : payload.mobs()) {
            MobConfig.encode(buffer, m);
        }
        ByteBufCodecs.VAR_INT.encode(buffer, payload.totalMobs());
        ByteBufCodecs.VAR_INT.encode(buffer, payload.simultaneousMobs());
        ByteBufCodecs.VAR_INT.encode(buffer, payload.ticksBetween());
        ByteBufCodecs.VAR_INT.encode(buffer, payload.cooldownSeconds());
        ByteBufCodecs.STRING_UTF8.encode(buffer, payload.lootTable());
        ByteBufCodecs.BOOL.encode(buffer, payload.oneShot());
    }

    private static UpdateSpawnerPayload decode(ByteBuf buffer) {
        BlockPos pos = BlockPos.STREAM_CODEC.decode(buffer);
        int size = ByteBufCodecs.VAR_INT.decode(buffer);
        List<MobConfig> mobs = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            mobs.add(MobConfig.decode(buffer));
        }
        int totalMobs = ByteBufCodecs.VAR_INT.decode(buffer);
        int simultaneousMobs = ByteBufCodecs.VAR_INT.decode(buffer);
        int ticksBetween = ByteBufCodecs.VAR_INT.decode(buffer);
        int cooldownSeconds = ByteBufCodecs.VAR_INT.decode(buffer);
        String lootTable = ByteBufCodecs.STRING_UTF8.decode(buffer);
        boolean oneShot = ByteBufCodecs.BOOL.decode(buffer);
        return new UpdateSpawnerPayload(pos, mobs, totalMobs, simultaneousMobs, ticksBetween, cooldownSeconds, lootTable, oneShot);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}