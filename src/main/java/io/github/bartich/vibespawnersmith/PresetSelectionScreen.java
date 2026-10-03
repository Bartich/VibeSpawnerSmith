package io.github.bartich.vibespawnersmith;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class PresetSelectionScreen extends Screen {
    private final Screen parent;
    private final Consumer<PresetManager.SpawnerPreset> onSelect;
    private PresetList list;

    public PresetSelectionScreen(Screen parent, Consumer<PresetManager.SpawnerPreset> onSelect) {
        super(Component.literal("Выбор и управление пресетами"));
        this.parent = parent;
        this.onSelect = onSelect;
    }

    @Override
    protected void init() {
        list = new PresetList(this.minecraft, this.width, this.height - 64, 32, 24);
        addRenderableWidget(list);

        reloadPresets();

        addRenderableWidget(Button.builder(Component.literal("Назад"), b -> this.minecraft.setScreen(parent))
                .bounds(this.width / 2 - 50, this.height - 26, 100, 20).build());
    }

    public void reloadPresets() {
        list.clearList();
        for (String name : PresetManager.getPresetNames()) {
            list.addPreset(name);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFF);
    }

    public class PresetList extends ContainerObjectSelectionList<PresetList.PresetEntry> {
        public PresetList(Minecraft mc, int width, int height, int y, int itemHeight) {
            super(mc, width, height, y, itemHeight);
        }

        public void clearList() {
            this.clearEntries();
        }

        public void addPreset(String name) {
            addEntry(new PresetEntry(name));
        }

        // ... остальной код PresetList

        @Override
        public int getRowWidth() {
            return this.width - 40;
        }

        @Override
        protected int getScrollbarPosition() {
            return this.getX() + this.width - 10;
        }

        public class PresetEntry extends ContainerObjectSelectionList.Entry<PresetEntry> {
            private final String presetName;
            private final Button selectBtn;
            private final Button deleteBtn;
            private final List<GuiEventListener> children = new ArrayList<>();

            public PresetEntry(String name) {
                this.presetName = name;

                this.selectBtn = Button.builder(Component.literal("✦ " + presetName), b -> {
                    PresetManager.SpawnerPreset preset = PresetManager.getPreset(presetName);
                    if (preset != null) {
                        minecraft.setScreen(parent);
                        onSelect.accept(preset);
                    }
                }).bounds(0, 0, 200, 20).build();

                this.deleteBtn = Button.builder(Component.literal("§c✕"), b -> {
                    PresetManager.removePreset(presetName);
                    reloadPresets();
                }).bounds(0, 0, 20, 20).build();

                children.add(selectBtn);
                children.add(deleteBtn);
            }

            @Override
            public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isHovered, float partialTick) {
                selectBtn.setX(left);
                selectBtn.setY(top);
                selectBtn.setWidth(width - 26);
                selectBtn.render(graphics, mouseX, mouseY, partialTick);

                deleteBtn.setX(left + width - 22);
                deleteBtn.setY(top);
                deleteBtn.render(graphics, mouseX, mouseY, partialTick);
            }

            @Override
            public List<? extends GuiEventListener> children() {
                return children;
            }

            @Override
            public List<? extends NarratableEntry> narratables() {
                return children.stream().filter(c -> c instanceof NarratableEntry).map(c -> (NarratableEntry) c).toList();
            }
        }
    }
}