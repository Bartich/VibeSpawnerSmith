package io.github.bartich.vibespawnersmith;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public class SavePresetScreen extends Screen {
    private final Screen parent;
    private final Consumer<String> onSave;
    private EditBox nameInput;

    public SavePresetScreen(Screen parent, Consumer<String> onSave) {
        super(Component.literal("Сохранить пресет"));
        this.parent = parent;
        this.onSave = onSave;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        nameInput = new EditBox(this.font, centerX - 100, centerY - 20, 200, 20, Component.literal("Имя пресета"));
        nameInput.setValue("Мой кастомный спавнер");
        addRenderableWidget(nameInput);

        addRenderableWidget(Button.builder(Component.literal("§aСохранить"), b -> {
            String name = nameInput.getValue().trim();
            if (!name.isEmpty()) {
                onSave.accept(name);
                this.minecraft.setScreen(parent);
            }
        }).bounds(centerX - 100, centerY + 10, 95, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Отмена"), b -> this.minecraft.setScreen(parent))
                .bounds(centerX + 5, centerY + 10, 95, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 40, 0xFFFFFF);
    }
}