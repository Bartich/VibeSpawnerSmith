package io.github.bartich.vibespawnersmith;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class MobLootScreen extends Screen {
    private final Screen parent;
    private final TrialSpawnerScreen.MobEntry entry;
    private PoolListWidget poolList;

    public MobLootScreen(Screen parent, TrialSpawnerScreen.MobEntry entry) {
        super(Component.literal("Редактор лута: " + entry.idBox.getValue()));
        this.parent = parent;
        this.entry = entry;
    }

    @Override
    protected void init() {
        int listWidth = this.width - 20;
        int listHeight = this.height - 70;
        int listY = 30;

        poolList = new PoolListWidget(this.minecraft, listWidth, listHeight, listY, 86);
        poolList.setX(10);
        addRenderableWidget(poolList);

        // Парсим текущую строку кастомного дропа
        parseExistingDrops();

        // Кнопка [+ Добавить группу]
        addRenderableWidget(Button.builder(Component.literal("+ Добавить группу"), b -> {
            char nextChar = (char) ('A' + poolList.children().size());
            poolList.addEntry(new PoolGroupEntry(String.valueOf(nextChar), true, 1));
        }).bounds(10, 6, 130, 20).build());

        // Кнопка [Без лута (none)]
        addRenderableWidget(Button.builder(Component.literal("Без лута (none)"), b -> {
            poolList.clearAll();
            saveAndClose("none");
        }).bounds(145, 6, 120, 20).build());

        // Кнопка Готово
        addRenderableWidget(Button.builder(Component.literal("Готово"), b -> saveAndClose(serializeDrops()))
                .bounds(this.width - 90, 6, 80, 20).build());
    }

    private void parseExistingDrops() {
        String data = entry.customDrops != null ? entry.customDrops.trim() : "";
        if (data.isEmpty() || data.equalsIgnoreCase("none") || data.equalsIgnoreCase("empty")) return;

        try {
            String[] pools = data.split(java.util.regex.Pattern.quote("|"));
            char groupChar = 'A';

            for (String pStr : pools) {
                pStr = pStr.trim();
                if (!pStr.startsWith("pool(") || !pStr.contains(")")) continue;

                int closeParen = pStr.indexOf(")");
                int openBracket = pStr.indexOf("[");
                int closeBracket = pStr.lastIndexOf("]");

                if (openBracket == -1 || closeBracket <= openBracket) continue;

                String header = pStr.substring(5, closeParen);
                String body = pStr.substring(openBracket + 1, closeBracket);

                String[] hParts = header.split(",");
                int rolls = 1;
                boolean isPool = true;
                try { rolls = Integer.parseInt(hParts[0].trim()); } catch (Exception ignored) {}
                if (hParts.length > 1) {
                    isPool = Boolean.parseBoolean(hParts[1].trim());
                }

                PoolGroupEntry pEntry = new PoolGroupEntry(String.valueOf(groupChar++), isPool, rolls);

                if (!body.isEmpty()) {
                    String[] items = body.split(";");
                    for (String it : items) {
                        it = it.trim();
                        if (it.isEmpty()) continue;

                        int lastColon = it.lastIndexOf(":");
                        if (lastColon == -1) continue;
                        float val = 1.0f;
                        try { val = Float.parseFloat(it.substring(lastColon + 1).trim()); } catch (Exception ignored) {}

                        String rem = it.substring(0, lastColon);
                        int secondLastColon = rem.lastIndexOf(":");
                        if (secondLastColon == -1) continue;

                        String rangeStr = rem.substring(secondLastColon + 1).trim();
                        String id = rem.substring(0, secondLastColon).trim();

                        int min = 1, max = 1;
                        if (rangeStr.contains("-")) {
                            String[] rng = rangeStr.split("-");
                            try { min = Integer.parseInt(rng[0].trim()); } catch (Exception ignored) {}
                            try { max = Integer.parseInt(rng[1].trim()); } catch (Exception ignored) {}
                        } else {
                            try { min = max = Integer.parseInt(rangeStr); } catch (Exception ignored) {}
                        }

                        pEntry.addItem(id, min, max, val);
                    }
                }
                poolList.addEntry(pEntry);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (poolList.children().isEmpty()) {
            PoolGroupEntry defaultGroup = new PoolGroupEntry("A", true, 1);
            defaultGroup.addItem("minecraft:diamond", 1, 1, 1);
            poolList.addEntry(defaultGroup);
        }
    }

    private String serializeDrops() {
        if (poolList.children().isEmpty()) return "none";

        StringBuilder sb = new StringBuilder();
        boolean firstPool = true;

        for (PoolGroupEntry group : poolList.children()) {
            if (group.items.isEmpty()) continue;
            if (!firstPool) sb.append("|");

            int rolls = 1;
            try { rolls = Math.max(1, Integer.parseInt(group.rollsBox.getValue().trim())); } catch (Exception ignored) {}

            sb.append(String.format(java.util.Locale.ROOT, "pool(%d,%b)[", rolls, group.isPoolMode));

            boolean firstItem = true;
            for (DropItem item : group.items) {
                String id = item.idBox.getValue().trim();
                if (id.isEmpty()) continue;

                int min = 1, max = 1;
                float val = 1.0f;
                try { min = Integer.parseInt(item.minBox.getValue().trim()); } catch (Exception ignored) {}
                try { max = Integer.parseInt(item.maxBox.getValue().trim()); } catch (Exception ignored) {}
                try { val = Float.parseFloat(item.valBox.getValue().trim()); } catch (Exception ignored) {}

                if (!firstItem) sb.append(";");
                sb.append(String.format(java.util.Locale.ROOT, "%s:%d-%d:%.1f", id, min, max, val));
                firstItem = false;
            }
            sb.append("]");
            firstPool = false;
        }

        return sb.toString();
    }

    private void saveAndClose(String result) {
        entry.customDrops = result;
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
    }

    @Override
    public void onClose() {
        saveAndClose(serializeDrops());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    // --- Виджет списка пулов ---
    public class PoolListWidget extends ContainerObjectSelectionList<PoolGroupEntry> {
        public PoolListWidget(Minecraft mc, int width, int height, int y, int itemHeight) {
            super(mc, width, height, y, itemHeight);
        }

        @Override
        public int addEntry(PoolGroupEntry entry) {
            return super.addEntry(entry);
        }

        public void clearAll() {
            super.clearEntries();
        }

        public boolean removePool(PoolGroupEntry entry) {
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

    // --- Элемент выпадающего предмета ---
    public class DropItem {
        public final TrialSpawnerScreen.AutoCompleteBox idBox;
        public final EditBox minBox;
        public final EditBox maxBox;
        public final EditBox valBox;
        public final Button removeBtn;

        public DropItem(String id, int min, int max, float val, Runnable onRemove) {
            idBox = new TrialSpawnerScreen.AutoCompleteBox(font, 0, 0, 160, 16, TrialSpawnerScreen.AutoCompleteBox.Type.ITEM);
            idBox.setValue(id);

            minBox = new EditBox(font, 0, 0, 24, 16, Component.empty());
            minBox.setValue(String.valueOf(min));

            maxBox = new EditBox(font, 0, 0, 24, 16, Component.empty());
            maxBox.setValue(String.valueOf(max));

            valBox = new EditBox(font, 0, 0, 32, 16, Component.empty());
            valBox.setValue(val == (int) val ? String.valueOf((int) val) : String.valueOf(val));

            removeBtn = Button.builder(Component.literal("§7-"), b -> onRemove.run())
                    .bounds(0, 0, 16, 16)
                    .build();
        }
    }

    // --- Карточка пула ---
    public class PoolGroupEntry extends ContainerObjectSelectionList.Entry<PoolGroupEntry> {
        public final String label;
        public boolean isPoolMode;
        public final EditBox rollsBox;
        public final Button modeBtn;
        public final Button addItemBtn;
        public final Button removeGroupBtn;
        public final List<DropItem> items = new ArrayList<>();
        private final List<GuiEventListener> children = new ArrayList<>();

        public PoolGroupEntry(String label, boolean isPoolMode, int rolls) {
            this.label = label;
            this.isPoolMode = isPoolMode;

            rollsBox = new EditBox(font, 0, 0, 30, 16, Component.empty());
            rollsBox.setValue(String.valueOf(rolls));

            modeBtn = Button.builder(Component.literal(isPoolMode ? "Режим: ПУЛ (Вес)" : "Режим: ШАНС (%)"), b -> {
                this.isPoolMode = !this.isPoolMode;
                b.setMessage(Component.literal(this.isPoolMode ? "Режим: ПУЛ (Вес)" : "Режим: ШАНС (%)"));
            }).bounds(0, 0, 125, 16).build();

            addItemBtn = Button.builder(Component.literal("+ Предмет"), b -> {
                if (items.size() < 3) {
                    addItem("minecraft:diamond", 1, 1, this.isPoolMode ? 1 : 100);
                }
            }).bounds(0, 0, 70, 16).build();

            removeGroupBtn = Button.builder(Component.literal("§c✕"), b -> poolList.removePool(this))
                    .bounds(0, 0, 18, 16).build();

            rebuildChildren();
        }

        public void addItem(String id, int min, int max, float val) {
            DropItem[] itemRef = new DropItem[1];
            DropItem item = new DropItem(id, min, max, val, () -> {
                items.remove(itemRef[0]);
                rebuildChildren();
            });
            itemRef[0] = item;
            items.add(item);
            rebuildChildren();
        }

        private void rebuildChildren() {
            children.clear();
            children.add(modeBtn);
            children.add(rollsBox);
            children.add(addItemBtn);
            children.add(removeGroupBtn);
            for (DropItem it : items) {
                children.add(it.idBox);
                children.add(it.minBox);
                children.add(it.maxBox);
                children.add(it.valBox);
                children.add(it.removeBtn);
            }
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isHovered, float partialTick) {
            graphics.fill(left, top, left + width, top + height - 4, 0x44222222);

            // Шапка карточки
            graphics.drawString(font, "§6[" + label + "]", left + 5, top + 4, 0xFFFFFF);

            modeBtn.setX(left + 28);
            modeBtn.setY(top + 1);
            modeBtn.render(graphics, mouseX, mouseY, partialTick);

            graphics.drawString(font, "Бросков:", left + 158, top + 5, 0xAAAAAA);
            rollsBox.setX(left + 208);
            rollsBox.setY(top + 1);
            rollsBox.render(graphics, mouseX, mouseY, partialTick);

            addItemBtn.setX(left + 245);
            addItemBtn.setY(top + 1);
            addItemBtn.render(graphics, mouseX, mouseY, partialTick);

            removeGroupBtn.setX(left + width - 22);
            removeGroupBtn.setY(top + 1);
            removeGroupBtn.render(graphics, mouseX, mouseY, partialTick);

            // Отрисовка предметов группы
            int itemY = top + 22;
            for (int i = 0; i < items.size(); i++) {
                DropItem it = items.get(i);
                int rowY = itemY + (i * 20);

                it.idBox.setX(left + 15);
                it.idBox.setY(rowY);
                it.idBox.render(graphics, mouseX, mouseY, partialTick);

                graphics.drawString(font, "x", left + 180, rowY + 4, 0x888888);
                it.minBox.setX(left + 188);
                it.minBox.setY(rowY);
                it.minBox.render(graphics, mouseX, mouseY, partialTick);

                graphics.drawString(font, "-", left + 215, rowY + 4, 0x888888);
                it.maxBox.setX(left + 222);
                it.maxBox.setY(rowY);
                it.maxBox.render(graphics, mouseX, mouseY, partialTick);

                graphics.drawString(font, isPoolMode ? "W:" : "%:", left + 252, rowY + 4, 0xAAAAAA);
                it.valBox.setX(left + 268);
                it.valBox.setY(rowY);
                it.valBox.render(graphics, mouseX, mouseY, partialTick);

                it.removeBtn.setX(left + 305);
                it.removeBtn.setY(rowY);
                it.removeBtn.render(graphics, mouseX, mouseY, partialTick);
            }
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