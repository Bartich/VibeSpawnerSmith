package io.github.bartich.vibespawnersmith;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

public class ClientHooks {
    public static void openConfigScreen(BlockPos pos) {
        Minecraft.getInstance().setScreen(new TrialSpawnerScreen(pos));
    }
}