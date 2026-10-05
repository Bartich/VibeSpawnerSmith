package io.github.bartich.vibespawnersmith;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class MobConfigScreen extends Screen {
    private final Screen parent;
    private final TrialSpawnerScreen.MobEntry entry;

    private TrialSpawnerScreen.AutoCompleteBox mainhandInput;
    private EditBox mainDropInput;

    private TrialSpawnerScreen.AutoCompleteBox offhandInput;
    private EditBox offDropInput;

    private TrialSpawnerScreen.AutoCompleteBox helmetInput;
    private EditBox helmDropInput;

    private TrialSpawnerScreen.AutoCompleteBox chestplateInput;
    private EditBox chestDropInput;

    private TrialSpawnerScreen.AutoCompleteBox leggingsInput;
    private EditBox legsDropInput;

    private TrialSpawnerScreen.AutoCompleteBox bootsInput;
    private EditBox bootsDropInput;

    private TrialSpawnerScreen.AutoCompleteBox effectsInput;

    public MobConfigScreen(Screen parent, TrialSpawnerScreen.MobEntry entry) {
        super(Component.literal("Настройка экипировки: " + entry.idBox.getValue()));
        this.parent = parent;
        this.entry = entry;
    }

    @Override
    protected void init() {
        int itemWidth = 170;
        int chanceWidth = 35;
        int totalRowWidth = itemWidth + chanceWidth + 6;
        int startX = this.width / 2 - totalRowWidth / 2 + 15;
        int startY = 32;
        int stepY = 22;

        Tooltip dropChanceTip = Tooltip.create(Component.literal("Шанс выпадения надетого предмета (0 - 100%)\nПо умолчанию: 8.5%"));

        // Оружие
        mainhandInput = new TrialSpawnerScreen.AutoCompleteBox(this.font, startX, startY, itemWidth, 18, TrialSpawnerScreen.AutoCompleteBox.Type.ITEM);
        mainhandInput.setValue(entry.mainhand);
        addRenderableWidget(mainhandInput);

        mainDropInput = new EditBox(this.font, startX + itemWidth + 4, startY, chanceWidth, 18, Component.empty());
        mainDropInput.setValue(formatChance(entry.mainDropChance));
        mainDropInput.setTooltip(dropChanceTip);
        addRenderableWidget(mainDropInput);

        // 2-я рука
        offhandInput = new TrialSpawnerScreen.AutoCompleteBox(this.font, startX, startY + stepY, itemWidth, 18, TrialSpawnerScreen.AutoCompleteBox.Type.ITEM);
        offhandInput.setValue(entry.offhand);
        addRenderableWidget(offhandInput);

        offDropInput = new EditBox(this.font, startX + itemWidth + 4, startY + stepY, chanceWidth, 18, Component.empty());
        offDropInput.setValue(formatChance(entry.offDropChance));
        offDropInput.setTooltip(dropChanceTip);
        addRenderableWidget(offDropInput);

        // Шлем
        helmetInput = new TrialSpawnerScreen.AutoCompleteBox(this.font, startX, startY + stepY * 2, itemWidth, 18, TrialSpawnerScreen.AutoCompleteBox.Type.ITEM);
        helmetInput.setValue(entry.helmet);
        addRenderableWidget(helmetInput);

        helmDropInput = new EditBox(this.font, startX + itemWidth + 4, startY + stepY * 2, chanceWidth, 18, Component.empty());
        helmDropInput.setValue(formatChance(entry.helmDropChance));
        helmDropInput.setTooltip(dropChanceTip);
        addRenderableWidget(helmDropInput);

        // Нагрудник
        chestplateInput = new TrialSpawnerScreen.AutoCompleteBox(this.font, startX, startY + stepY * 3, itemWidth, 18, TrialSpawnerScreen.AutoCompleteBox.Type.ITEM);
        chestplateInput.setValue(entry.chestplate);
        addRenderableWidget(chestplateInput);

        chestDropInput = new EditBox(this.font, startX + itemWidth + 4, startY + stepY * 3, chanceWidth, 18, Component.empty());
        chestDropInput.setValue(formatChance(entry.chestDropChance));
        chestDropInput.setTooltip(dropChanceTip);
        addRenderableWidget(chestDropInput);

        // Поножи
        leggingsInput = new TrialSpawnerScreen.AutoCompleteBox(this.font, startX, startY + stepY * 4, itemWidth, 18, TrialSpawnerScreen.AutoCompleteBox.Type.ITEM);
        leggingsInput.setValue(entry.leggings);
        addRenderableWidget(leggingsInput);

        legsDropInput = new EditBox(this.font, startX + itemWidth + 4, startY + stepY * 4, chanceWidth, 18, Component.empty());
        legsDropInput.setValue(formatChance(entry.legsDropChance));
        legsDropInput.setTooltip(dropChanceTip);
        addRenderableWidget(legsDropInput);

        // Ботинки
        bootsInput = new TrialSpawnerScreen.AutoCompleteBox(this.font, startX, startY + stepY * 5, itemWidth, 18, TrialSpawnerScreen.AutoCompleteBox.Type.ITEM);
        bootsInput.setValue(entry.boots);
        addRenderableWidget(bootsInput);

        bootsDropInput = new EditBox(this.font, startX + itemWidth + 4, startY + stepY * 5, chanceWidth, 18, Component.empty());
        bootsDropInput.setValue(formatChance(entry.bootsDropChance));
        bootsDropInput.setTooltip(dropChanceTip);
        addRenderableWidget(bootsDropInput);

        // Эффекты
        effectsInput = new TrialSpawnerScreen.AutoCompleteBox(this.font, startX, startY + stepY * 6, totalRowWidth, 18, TrialSpawnerScreen.AutoCompleteBox.Type.EFFECT);
        effectsInput.setMaxLength(512);
        effectsInput.setValue(entry.effects);
        effectsInput.setTooltip(Tooltip.create(Component.literal("§eЭффекты моба:\n§fФормат: §aэффект:уровень, эффект:уровень\n§7Пример: §bspeed:2, strength:1")));
        addRenderableWidget(effectsInput);

        // Кнопка Готово
        addRenderableWidget(Button.builder(Component.literal("Готово"), b -> saveAndClose())
                .bounds(this.width / 2 - 50, startY + stepY * 7 + 10, 100, 20).build());
    }

    private String formatChance(float chance) {
        if (chance <= 0.0f) return "0";
        if (chance >= 100.0f) return "100";
        return String.valueOf(chance);
    }

    private float parseChance(String text) {
        try {
            return Math.max(0.0f, Math.min(100.0f, Float.parseFloat(text.trim())));
        } catch (Exception ignored) {
            return 0.0f;
        }
    }

    private void saveAndClose() {
        entry.mainhand = mainhandInput.getValue().trim();
        entry.mainDropChance = parseChance(mainDropInput.getValue());

        entry.offhand = offhandInput.getValue().trim();
        entry.offDropChance = parseChance(offDropInput.getValue());

        entry.helmet = helmetInput.getValue().trim();
        entry.helmDropChance = parseChance(helmDropInput.getValue());

        entry.chestplate = chestplateInput.getValue().trim();
        entry.chestDropChance = parseChance(chestDropInput.getValue());

        entry.leggings = leggingsInput.getValue().trim();
        entry.legsDropChance = parseChance(legsDropInput.getValue());

        entry.boots = bootsInput.getValue().trim();
        entry.bootsDropChance = parseChance(bootsDropInput.getValue());

        entry.effects = effectsInput.getValue().trim();

        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
    }

    @Override
    public void onClose() {
        saveAndClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFF);

        int itemWidth = 170;
        int chanceWidth = 35;
        int totalRowWidth = itemWidth + chanceWidth + 6;
        int startX = this.width / 2 - totalRowWidth / 2 + 15;
        int startY = 32;
        int stepY = 22;

        graphics.drawString(this.font, "§7Предмет", startX, startY - 10, 0xAAAAAA, false);
        graphics.drawString(this.font, "§7% Дроп", startX + itemWidth + 4, startY - 10, 0xAAAAAA, false);

        graphics.drawString(this.font, "Оружие:", startX - 58, startY + 5, 0xCCCCCC, false);
        graphics.drawString(this.font, "2-я рука:", startX - 58, startY + stepY + 5, 0xCCCCCC, false);
        graphics.drawString(this.font, "Шлем:", startX - 58, startY + stepY * 2 + 5, 0xCCCCCC, false);
        graphics.drawString(this.font, "Нагрудник:", startX - 70, startY + stepY * 3 + 5, 0xCCCCCC, false);
        graphics.drawString(this.font, "Поножи:", startX - 58, startY + stepY * 4 + 5, 0xCCCCCC, false);
        graphics.drawString(this.font, "Ботинки:", startX - 58, startY + stepY * 5 + 5, 0xCCCCCC, false);
        graphics.drawString(this.font, "Эффекты:", startX - 60, startY + stepY * 6 + 5, 0xCCCCCC, false);
    }
}