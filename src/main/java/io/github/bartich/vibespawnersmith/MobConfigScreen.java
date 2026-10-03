package io.github.bartich.vibespawnersmith;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class MobConfigScreen extends Screen {
    private final Screen parentScreen;
    private final TrialSpawnerScreen.MobEntry entry;

    private TrialSpawnerScreen.AutoCompleteBox mainhandBox;
    private TrialSpawnerScreen.AutoCompleteBox offhandBox;
    private TrialSpawnerScreen.AutoCompleteBox helmetBox;
    private TrialSpawnerScreen.AutoCompleteBox chestplateBox;
    private TrialSpawnerScreen.AutoCompleteBox leggingsBox;
    private TrialSpawnerScreen.AutoCompleteBox bootsBox;
    private TrialSpawnerScreen.AutoCompleteBox effectsBox;

    public MobConfigScreen(Screen parentScreen, TrialSpawnerScreen.MobEntry entry) {
        super(Component.literal("Настройка экипировки и эффектов: " + entry.idBox.getValue()));
        this.parentScreen = parentScreen;
        this.entry = entry;
    }

    @Override
    protected void init() {
        int leftCol = this.width / 2 - 120;
        int startY = 35;
        int gap = 24;

        // Поля снаряжения — теперь просторные, по 240px в ширину
        mainhandBox = new TrialSpawnerScreen.AutoCompleteBox(this.font, leftCol, startY, 240, 18, TrialSpawnerScreen.AutoCompleteBox.Type.ITEM);
        mainhandBox.setValue(entry.mainhand);
        mainhandBox.setHint(Component.literal("minecraft:diamond_sword"));
        addRenderableWidget(mainhandBox);

        offhandBox = new TrialSpawnerScreen.AutoCompleteBox(this.font, leftCol, startY + gap, 240, 18, TrialSpawnerScreen.AutoCompleteBox.Type.ITEM);
        offhandBox.setValue(entry.offhand);
        offhandBox.setHint(Component.literal("minecraft:shield"));
        addRenderableWidget(offhandBox);

        helmetBox = new TrialSpawnerScreen.AutoCompleteBox(this.font, leftCol, startY + gap * 2, 240, 18, TrialSpawnerScreen.AutoCompleteBox.Type.ITEM);
        helmetBox.setValue(entry.helmet);
        helmetBox.setHint(Component.literal("minecraft:iron_helmet"));
        addRenderableWidget(helmetBox);

        chestplateBox = new TrialSpawnerScreen.AutoCompleteBox(this.font, leftCol, startY + gap * 3, 240, 18, TrialSpawnerScreen.AutoCompleteBox.Type.ITEM);
        chestplateBox.setValue(entry.chestplate);
        chestplateBox.setHint(Component.literal("minecraft:iron_chestplate"));
        addRenderableWidget(chestplateBox);

        leggingsBox = new TrialSpawnerScreen.AutoCompleteBox(this.font, leftCol, startY + gap * 4, 240, 18, TrialSpawnerScreen.AutoCompleteBox.Type.ITEM);
        leggingsBox.setValue(entry.leggings);
        leggingsBox.setHint(Component.literal("minecraft:iron_leggings"));
        addRenderableWidget(leggingsBox);

        bootsBox = new TrialSpawnerScreen.AutoCompleteBox(this.font, leftCol, startY + gap * 5, 240, 18, TrialSpawnerScreen.AutoCompleteBox.Type.ITEM);
        bootsBox.setValue(entry.boots);
        bootsBox.setHint(Component.literal("minecraft:iron_boots"));
        addRenderableWidget(bootsBox);

        effectsBox = new TrialSpawnerScreen.AutoCompleteBox(this.font, leftCol, startY + gap * 6, 240, 18, TrialSpawnerScreen.AutoCompleteBox.Type.EFFECT);
        effectsBox.setValue(entry.effects);
        effectsBox.setHint(Component.literal("speed:2, strength:1, resistance:1"));
        addRenderableWidget(effectsBox);

        // Кнопка [Готово]
        addRenderableWidget(Button.builder(Component.literal("§aГотово"), b -> saveAndBack())
                .bounds(this.width / 2 - 60, startY + gap * 7 + 8, 120, 20)
                .build());
    }

    private void saveAndBack() {
        entry.mainhand = mainhandBox.getValue().trim();
        entry.offhand = offhandBox.getValue().trim();
        entry.helmet = helmetBox.getValue().trim();
        entry.chestplate = chestplateBox.getValue().trim();
        entry.leggings = leggingsBox.getValue().trim();
        entry.boots = bootsBox.getValue().trim();
        entry.effects = effectsBox.getValue().trim();

        if (this.minecraft != null) {
            this.minecraft.setScreen(parentScreen);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFF);

        int labelX = this.width / 2 - 185;
        int startY = 40;
        int gap = 24;

        graphics.drawString(this.font, "Оружие:", labelX, startY, 0xAAAAAA);
        graphics.drawString(this.font, "2-я рука:", labelX, startY + gap, 0xAAAAAA);
        graphics.drawString(this.font, "Шлем:", labelX, startY + gap * 2, 0xAAAAAA);
        graphics.drawString(this.font, "Нагрудник:", labelX, startY + gap * 3, 0xAAAAAA);
        graphics.drawString(this.font, "Поножи:", labelX, startY + gap * 4, 0xAAAAAA);
        graphics.drawString(this.font, "Ботинки:", labelX, startY + gap * 5, 0xAAAAAA);
        graphics.drawString(this.font, "Эффекты:", labelX, startY + gap * 6, 0xAAAAAA);
    }
}