package io.github.bartich.vibespawnersmith;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Map;
import java.util.function.Consumer;

public class PresetSelectionScreen extends Screen {
    private final Screen parent;
    private final Consumer<PresetManager.SpawnerPreset> onSelect;
    private PresetListWidget list;

    public PresetSelectionScreen(Screen parent, Consumer<PresetManager.SpawnerPreset> onSelect) {
        super(Component.literal("Выбор пресета"));
        this.parent = parent;
        this.onSelect = onSelect;
    }

    @Override
    protected void init() {
        list = new PresetListWidget(this.minecraft, this.width, this.height - 64, 32, 24);
        addRenderableWidget(list);
        reloadPresets();

        addRenderableWidget(Button.builder(Component.literal("Назад"), b -> {
            if (this.minecraft != null) {
                this.minecraft.setScreen(parent);
            }
        }).bounds(this.width / 2 - 50, this.height - 26, 100, 20).build());
    }

    public void reloadPresets() {
        if (list == null) return;
        list.clearAll();

        Map<String, PresetManager.SpawnerPreset> presets = PresetManager.getAllPresets();
        for (Map.Entry<String, PresetManager.SpawnerPreset> entry : presets.entrySet()) {
            if (!entry.getKey().equals(PresetManager.LAST_USED_KEY)) {
                list.addPreset(entry.getValue());
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFF);
    }

    public class PresetListWidget extends ObjectSelectionList<PresetEntry> {
        public PresetListWidget(Minecraft mc, int width, int height, int y, int itemHeight) {
            super(mc, width, height, y, itemHeight);
        }

        public void addPreset(PresetManager.SpawnerPreset preset) {
            this.addEntry(new PresetEntry(preset));
        }

        // Открываем публичный доступ к очистке списка
        public void clearAll() {
            this.clearEntries();
        }

        @Override
        public int getRowWidth() {
            return 260;
        }

        @Override
        protected int getScrollbarPosition() {
            return this.getX() + this.width / 2 + 135;
        }
    }

    public class PresetEntry extends ObjectSelectionList.Entry<PresetEntry> {
        private final PresetManager.SpawnerPreset preset;

        public PresetEntry(PresetManager.SpawnerPreset preset) {
            this.preset = preset;
        }

        @Override
        public Component getNarration() {
            return Component.literal(preset.name());
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isHovered, float partialTick) {
            if (isHovered) {
                graphics.fill(left, top, left + width, top + height, 0x33FFFFFF);
            }

            // Название пресета
            graphics.drawString(font, "📄 " + preset.name(), left + 5, top + 6, 0xFFFFFF);

            // Количество мобов
            graphics.drawString(font, "Мобы: " + preset.mobs().size() + " шт.", left + width - 105, top + 6, 0x888888);

            // Кнопка удаления (крестик) справа
            boolean deleteHovered = mouseX >= left + width - 20 && mouseX <= left + width - 4
                    && mouseY >= top + 2 && mouseY <= top + height - 2;

            if (deleteHovered) {
                graphics.fill(left + width - 20, top + 2, left + width - 4, top + height - 2, 0x55FF0000);
            }
            graphics.drawString(font, "§c✕", left + width - 16, top + 6, 0xFF5555);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 0) {
                int left = list.getRowLeft();
                int width = list.getRowWidth();

                // Проверяем клик по крестику справа
                if (mouseX >= left + width - 20 && mouseX <= left + width - 4) {
                    PresetManager.removePreset(preset.name());
                    reloadPresets();
                    return true;
                }

                // Клик по всей остальной строке — выбор пресета
                onSelect.accept(preset);
                if (minecraft != null) {
                    minecraft.setScreen(parent);
                }
                return true;
            }
            return false;
        }
    }
}