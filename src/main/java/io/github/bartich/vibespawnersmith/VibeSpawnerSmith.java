package io.github.bartich.vibespawnersmith;

import io.github.bartich.vibespawnersmith.network.NetworkHandler;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(VibeSpawnerSmith.MODID)
public class VibeSpawnerSmith {
    public static final String MODID = "vibespawnersmith";

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);

    public static final DeferredItem<Item> CRAP_TOOL = ITEMS.register("crap_imposed_by_developers",
            () -> new DeveloperCrapItem(new Item.Properties().stacksTo(1)));

    public VibeSpawnerSmith(IEventBus modEventBus) {
        // Убедись, что этот вызов ровно ОДИН:
        ITEMS.register(modEventBus);

        modEventBus.addListener(this::addCreative);
        modEventBus.addListener(NetworkHandler::register);

        PresetManager.load();
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES || event.getTabKey() == CreativeModeTabs.OP_BLOCKS) {
            event.accept(CRAP_TOOL);
        }
    }
}