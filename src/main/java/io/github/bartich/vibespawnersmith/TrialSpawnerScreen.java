package io.github.bartich.vibespawnersmith;

import io.github.bartich.vibespawnersmith.network.UpdateSpawnerPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class TrialSpawnerScreen extends Screen {
    private final BlockPos pos;
    private MobListWidget mobList;

    private EditBox totalMobsInput;
    private EditBox simultaneousMobsInput;
    private EditBox ticksBetweenInput;
    private EditBox cooldownInput;
    private EditBox lootTableInput;
    private Checkbox oneShotCheckbox;
    private String currentPresetName = null;

    public TrialSpawnerScreen(BlockPos pos) {
        super(Component.translatable("gui.vibespawnersmith.title"));
        this.pos = pos;
    }

    @Override
    protected void init() {
        int listWidth = this.width - 20;
        int listHeight = 110;
        int listY = 26;

        mobList = new MobListWidget(this.minecraft, listWidth, listHeight, listY, 24);
        mobList.setX(10);
        addRenderableWidget(mobList);

        // Кнопка [+ Добавить моба]
        addRenderableWidget(Button.builder(Component.translatable("gui.vibespawnersmith.add_mob"), b -> {
            mobList.addEntry(new MobEntry("minecraft:zombie", 1));
        }).bounds(10, listY + listHeight + 3, 110, 18).build());

        // Кнопка выбора пресетов
        addRenderableWidget(Button.builder(Component.literal("📁 Пресеты"), b -> {
            saveState(); // Сначала фиксируем всё, что набрано руками!
            if (this.minecraft != null) {
                this.minecraft.setScreen(new PresetSelectionScreen(this, this::loadPreset));
            }
        }).bounds(this.width - 195, 4, 85, 18).build());

        // Кнопка сохранения пресета
        addRenderableWidget(Button.builder(Component.literal("💾 Сохранить"), b -> {
            saveState(); // Фиксируем текущий ввод перед открытием диалога
            if (this.minecraft != null) {
                this.minecraft.setScreen(new SavePresetScreen(this, this::saveCurrentAsPreset));
            }
        }).bounds(this.width - 105, 4, 95, 18).build());

        // Нижняя панель параметров
        int bottomY = listY + listHeight + 24;
        int col1 = this.width / 2 - 150;
        int col2 = this.width / 2 + 10;

        totalMobsInput = new EditBox(this.font, col1 + 50, bottomY, 80, 18, Component.empty());
        totalMobsInput.setValue("6");
        addRenderableWidget(totalMobsInput);

        simultaneousMobsInput = new EditBox(this.font, col2 + 65, bottomY, 75, 18, Component.empty());
        simultaneousMobsInput.setValue("2");
        addRenderableWidget(simultaneousMobsInput);

        ticksBetweenInput = new EditBox(this.font, col1 + 50, bottomY + 22, 80, 18, Component.empty());
        ticksBetweenInput.setValue("40");
        addRenderableWidget(ticksBetweenInput);

        cooldownInput = new EditBox(this.font, col2 + 65, bottomY + 22, 75, 18, Component.empty());
        cooldownInput.setValue("1800");
        addRenderableWidget(cooldownInput);

        lootTableInput = new EditBox(this.font, col1 + 50, bottomY + 44, 240, 18, Component.empty());
        lootTableInput.setMaxLength(256);
        lootTableInput.setValue("minecraft:chests/trial_chambers/reward");
        addRenderableWidget(lootTableInput);

        oneShotCheckbox = Checkbox.builder(Component.translatable("gui.vibespawnersmith.infinite_cd"), this.font)
                .pos(col1, bottomY + 66)
                .selected(false)
                .build();
        addRenderableWidget(oneShotCheckbox);

        addRenderableWidget(Button.builder(Component.translatable("gui.vibespawnersmith.apply"), b -> applySettings())
                .bounds(this.width / 2 - 130, bottomY + 90, 120, 20)
                .build());

        addRenderableWidget(Button.builder(Component.translatable("gui.vibespawnersmith.cancel"), b -> this.onClose())
                .bounds(this.width / 2 + 10, bottomY + 90, 120, 20)
                .build());

        // ВОССТАНАВЛИВАЕМ ПОСЛЕДНИЙ ВВОД (LAST USED)
        if (mobList.children().isEmpty()) {
            PresetManager.SpawnerPreset last = PresetManager.getLastUsed();
            if (last != null) {
                loadPreset(last);
            } else {
                mobList.addEntry(new MobEntry("minecraft:breeze", 1));
            }
        }
    }

    public void loadPreset(PresetManager.SpawnerPreset p) {
        if (p == null) return;
        this.currentPresetName = p.name();

        // Полностью очищаем старый список мобов
        mobList.clearList();

        // Заполняем мобами из пресета
        for (PresetManager.SavedMob m : p.mobs()) {
            MobEntry e = new MobEntry(m.id(), m.weight());
            e.mainhand = m.mainhand() != null ? m.mainhand() : "";
            e.offhand = m.offhand() != null ? m.offhand() : "";
            e.helmet = m.helmet() != null ? m.helmet() : "";
            e.chestplate = m.chestplate() != null ? m.chestplate() : "";
            e.leggings = m.leggings() != null ? m.leggings() : "";
            e.boots = m.boots() != null ? m.boots() : "";
            e.effects = m.effects() != null ? m.effects() : "";
            mobList.addEntry(e);
        }

        // Если в пресете почему-то не было мобов — подстрахуемся
        if (mobList.children().isEmpty()) {
            mobList.addEntry(new MobEntry("minecraft:breeze", 1));
        }

        // Обновляем текстовые поля
        totalMobsInput.setValue(String.valueOf(p.totalMobs()));
        simultaneousMobsInput.setValue(String.valueOf(p.simultaneousMobs()));
        ticksBetweenInput.setValue(String.valueOf(p.ticksBetween()));
        cooldownInput.setValue(String.valueOf(p.cooldownSeconds()));
        lootTableInput.setValue(p.lootTable() != null ? p.lootTable() : "");

        if (oneShotCheckbox.selected() != p.oneShot()) {
            oneShotCheckbox.onPress();
        }
    }

    private PresetManager.SpawnerPreset createCurrentPreset(String name) {
        List<PresetManager.SavedMob> mobs = new ArrayList<>();
        for (MobEntry entry : mobList.children()) {
            int weight = 1;
            try { weight = Integer.parseInt(entry.weightBox.getValue().trim()); } catch (Exception ignored) {}
            mobs.add(new PresetManager.SavedMob(
                    entry.idBox.getValue().trim(),
                    weight,
                    entry.mainhand, entry.offhand,
                    entry.helmet, entry.chestplate,
                    entry.leggings, entry.boots,
                    entry.effects
            ));
        }
        int total = 6, sim = 2, ticks = 40, cd = 1800;
        try { total = Integer.parseInt(totalMobsInput.getValue().trim()); } catch (Exception ignored) {}
        try { sim = Integer.parseInt(simultaneousMobsInput.getValue().trim()); } catch (Exception ignored) {}
        try { ticks = Integer.parseInt(ticksBetweenInput.getValue().trim()); } catch (Exception ignored) {}
        try { cd = Integer.parseInt(cooldownInput.getValue().trim()); } catch (Exception ignored) {}

        return new PresetManager.SpawnerPreset(name, mobs, total, sim, ticks, cd, lootTableInput.getValue().trim(), oneShotCheckbox.selected());
    }

    public void saveCurrentAsPreset(String name) {
        this.currentPresetName = name;
        PresetManager.putPreset(name, createCurrentPreset(name));
    }

    private void saveState() {
        if (mobList == null || totalMobsInput == null) return;

        PresetManager.SpawnerPreset current = createCurrentPreset(
                currentPresetName != null ? currentPresetName : PresetManager.LAST_USED_KEY
        );

        // Всегда обновляем _last_used
        PresetManager.putPreset(PresetManager.LAST_USED_KEY, current);

        // Если правили именованный пресет — обновляем его файл
        if (currentPresetName != null && !currentPresetName.equals(PresetManager.LAST_USED_KEY)) {
            PresetManager.putPreset(currentPresetName, current);
        }
    }

    @Override
    public void onClose() {
        saveState();
        super.onClose();
    }

    private void applySettings() {
        saveState();
        try {
            List<UpdateSpawnerPayload.MobConfig> configs = new ArrayList<>();
            for (MobEntry entry : mobList.children()) {
                String id = entry.idBox.getValue().trim();
                int weight = 1;
                try {
                    weight = Integer.parseInt(entry.weightBox.getValue().trim());
                } catch (NumberFormatException ignored) {}

                if (!id.isEmpty()) {
                    configs.add(new UpdateSpawnerPayload.MobConfig(
                            id, weight,
                            entry.mainhand, entry.offhand,
                            entry.helmet, entry.chestplate,
                            entry.leggings, entry.boots,
                            entry.effects
                    ));
                }
            }

            int total = Integer.parseInt(totalMobsInput.getValue().trim());
            int sim = Integer.parseInt(simultaneousMobsInput.getValue().trim());
            int ticks = Integer.parseInt(ticksBetweenInput.getValue().trim());
            int cd = Integer.parseInt(cooldownInput.getValue().trim());
            String loot = lootTableInput.getValue().trim();
            boolean oneShot = oneShotCheckbox.selected();

            // Сохраняем как последний использованный пресет
            PresetManager.putPreset(PresetManager.LAST_USED_KEY, createCurrentPreset("Last"));

            PacketDistributor.sendToServer(new UpdateSpawnerPayload(pos, configs, total, sim, ticks, cd, loot, oneShot));
            this.onClose();
        } catch (NumberFormatException ignored) {}
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 6, 0xFFFFFF);

        int bottomY = mobList.getY() + mobList.getHeight() + 24;
        int col1 = this.width / 2 - 150;
        int col2 = this.width / 2 + 10;

        graphics.drawString(this.font, "Всего:", col1, bottomY + 5, 0xAAAAAA);
        graphics.drawString(this.font, "На арене:", col2, bottomY + 5, 0xAAAAAA);
        graphics.drawString(this.font, "Пауза (т):", col1, bottomY + 27, 0xAAAAAA);
        graphics.drawString(this.font, "КД (сек):", col2, bottomY + 27, 0xAAAAAA);
        graphics.drawString(this.font, "Лут:", col1, bottomY + 49, 0xAAAAAA);
    }

    public class MobListWidget extends ContainerObjectSelectionList<MobEntry> {
        public MobListWidget(Minecraft mc, int width, int height, int y, int itemHeight) {
            super(mc, width, height, y, itemHeight);
        }

        public void clearList() {
            this.clearEntries();
        }

        @Override
        public int addEntry(MobEntry entry) {
            return super.addEntry(entry);
        }

        @Override
        public boolean removeEntry(MobEntry entry) {
            return super.removeEntry(entry);
        }

        @Override
        public int getRowWidth() {
            return this.width - 20;
        }

        @Override
        protected int getScrollbarPosition() {
            return this.getX() + this.width - 6;
        }
    }

    public class MobEntry extends ContainerObjectSelectionList.Entry<MobEntry> {
        public final AutoCompleteBox idBox;
        public final EditBox weightBox;
        public final Button configBtn;
        public final Button deleteBtn;

        // Данные экипировки и эффектов
        public String mainhand = "";
        public String offhand = "";
        public String helmet = "";
        public String chestplate = "";
        public String leggings = "";
        public String boots = "";
        public String effects = "";

        private final List<GuiEventListener> children = new ArrayList<>();

        public MobEntry(String initialId, int initialWeight) {
            idBox = new AutoCompleteBox(font, 0, 0, 160, 16, AutoCompleteBox.Type.ENTITY);
            idBox.setValue(initialId);

            weightBox = new EditBox(font, 0, 0, 35, 16, Component.empty());
            weightBox.setValue(String.valueOf(initialWeight));

            // Кнопка перехода в отдельный экран тонкой настройки
            configBtn = Button.builder(Component.literal("⚙ Снаряж. / Эффекты"), b -> {
                if (minecraft != null) {
                    minecraft.setScreen(new MobConfigScreen(TrialSpawnerScreen.this, this));
                }
            }).bounds(0, 0, 130, 16).build();

            deleteBtn = Button.builder(Component.literal("§c✕"), b -> mobList.removeEntry(this))
                    .bounds(0, 0, 18, 16).build();

            children.add(idBox);
            children.add(weightBox);
            children.add(configBtn);
            children.add(deleteBtn);
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isHovered, float partialTick) {
            graphics.fill(left, top, left + width, top + height - 2, 0x33FFFFFF);

            graphics.drawString(font, "ID:", left + 4, top + 5, 0xCCCCCC);
            idBox.setX(left + 22);
            idBox.setY(top + 1);
            idBox.render(graphics, mouseX, mouseY, partialTick);

            graphics.drawString(font, "W:", left + 188, top + 5, 0xCCCCCC);
            weightBox.setX(left + 202);
            weightBox.setY(top + 1);
            weightBox.render(graphics, mouseX, mouseY, partialTick);

            configBtn.setX(left + 245);
            configBtn.setY(top + 1);
            configBtn.render(graphics, mouseX, mouseY, partialTick);

            deleteBtn.setX(left + width - 22);
            deleteBtn.setY(top + 1);
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

    public static class AutoCompleteBox extends EditBox {
        public enum Type { ENTITY, ITEM, EFFECT }

        private final Type type;
        private String suggestion = "";

        public AutoCompleteBox(net.minecraft.client.gui.Font font, int x, int y, int width, int height, Type type) {
            super(font, x, y, width, height, Component.empty());
            this.type = type;
            this.setMaxLength(512);
            this.setResponder(this::updateSuggestion);
        }

        private void updateSuggestion(String text) {
            this.suggestion = "";
            String token = text.trim();
            if (type == Type.EFFECT && token.contains(",")) {
                token = token.substring(token.lastIndexOf(",") + 1).trim();
            }

            if (token.isEmpty()) return;

            String query = token.toLowerCase();
            boolean hasNamespace = query.contains(":");
            String queryPath = hasNamespace ? query.substring(query.indexOf(":") + 1) : query;

            List<ResourceLocation> pool = switch (type) {
                case ENTITY -> BuiltInRegistries.ENTITY_TYPE.keySet().stream().toList();
                case ITEM -> BuiltInRegistries.ITEM.keySet().stream().toList();
                case EFFECT -> BuiltInRegistries.MOB_EFFECT.keySet().stream().toList();
            };

            for (ResourceLocation rl : pool) {
                String full = rl.toString();      // например "minecraft:zombie"
                String path = rl.getPath();        // например "zombie"

                // Если игрок вводит без префикса (например "zom")
                if (!hasNamespace && path.startsWith(queryPath) && !path.equals(queryPath)) {
                    this.suggestion = path.substring(queryPath.length());
                    break;
                }
                // Если игрок вводит полный id (например "minecraft:zom")
                else if (hasNamespace && full.startsWith(query) && !full.equals(query)) {
                    this.suggestion = full.substring(query.length());
                    break;
                }
            }
        }



        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (keyCode == GLFW.GLFW_KEY_TAB && !suggestion.isEmpty()) {
                if (type == Type.EFFECT && this.getValue().contains(",")) {
                    int lastComma = this.getValue().lastIndexOf(",");
                    String prefix = this.getValue().substring(0, lastComma + 1) + " ";
                    String currentToken = this.getValue().substring(lastComma + 1).trim();
                    this.setValue(prefix + currentToken + suggestion);
                } else {
                    this.setValue(this.getValue() + suggestion);
                }
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            super.renderWidget(graphics, mouseX, mouseY, partialTick);

            if (this.isFocused() && !suggestion.isEmpty()) {
                int textWidth = Minecraft.getInstance().font.width(this.getValue());
                int renderX = this.getX() + 4 + textWidth;
                int renderY = this.getY() + (this.height - 8) / 2;
                if (renderX < this.getX() + this.width - 6) {
                    graphics.drawString(Minecraft.getInstance().font, suggestion, renderX, renderY, 0x666666, false);
                }
            }
        }
    }
}