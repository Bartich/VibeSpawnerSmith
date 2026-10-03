package io.github.bartich.vibespawnersmith;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.TrialSpawnerBlockEntity;

public class DeveloperCrapItem extends Item {
    public DeveloperCrapItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();

        if (player == null) return InteractionResult.PASS;

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof TrialSpawnerBlockEntity) {
            if (level.isClientSide) {
                ClientHooks.openConfigScreen(pos);
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }
}