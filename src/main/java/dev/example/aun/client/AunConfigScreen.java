package dev.example.aun.client;

import dev.example.aun.config.AunConfig;
import dev.example.aun.config.AunConfigManager;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The A-U-N settings screen.
 *
 *   top      : one tab per category (QOL, Vanilla+, Performance, Fixes)
 *   left     : one vertical tab per sub-category (Clear Vision, HUD, ...)
 *   right    : the options of the chosen sub-category (long lists scroll with the mouse wheel)
 *   bottom   : Cancel / Save & Quit
 *
 * The whole menu is built from the fields of {@link AunConfig}, so a new option, group or
 * category added to that class shows up here automatically. Labels come from the language
 * file (text.autoconfig.aun.*), exactly like before.
 */
public class AunConfigScreen extends Screen {

    // ---- layout ----
    private static final int TAB_H = 24;
    private static final int SIDE_W = 140;
    private static final int SIDE_TAB_H = 22;
    private static final int MARGIN = 12;
    private static final int GAP = 10;
    private static final int ROW_H = 24;
    private static final int CONTROL_W = 96;

    // ---- colours (ARGB) ----
    private static final int WHITE = 0xFFFFFFFF;
    private static final int GREY = 0xFFB4B4B4;
    private static final int GREEN = 0xFF55FF55;
    private static final int RED = 0xFFFF5555;
    private static final int TAB_BAR = 0xCC1E2228;
    private static final int TAB_SELECTED = 0xFF2C3139;
    private static final int TAB_NORMAL = 0xFF000000;
    private static final int TAB_HOVER = 0xFF1C1C1C;
    private static final int BORDER = 0xFF3C3C3C;
    private static final int PANEL = 0x66000000;

    // ---- model ----
    private enum Kind { BOOL, TEXT }

    private static final class Option {
        final Field groupField; // null for options that sit directly in the config class
        final Field field;
        final String labelKey;
        final Kind kind;

        Option(Field groupField, Field field, String labelKey) {
            this.groupField = groupField;
            this.field = field;
            this.labelKey = labelKey;
            this.kind = field.getType() == boolean.class ? Kind.BOOL : Kind.TEXT;
        }

        private Object holder() throws IllegalAccessException {
            AunConfig config = AunConfigManager.get();
            return groupField == null ? config : groupField.get(config);
        }

        boolean getBool() {
            try {
                return field.getBoolean(holder());
            } catch (IllegalAccessException | RuntimeException e) {
                return false;
            }
        }

        void setBool(boolean value) {
            try {
                field.setBoolean(holder(), value);
            } catch (IllegalAccessException | RuntimeException ignored) {
                // ignore
            }
        }

        String getText() {
            try {
                Object value = field.get(holder());
                return value == null ? "" : value.toString();
            } catch (IllegalAccessException | RuntimeException e) {
                return "";
            }
        }

        void setText(String text) {
            try {
                Class<?> type = field.getType();
                if (type == String.class) {
                    field.set(holder(), text);
                } else if (type == int.class) {
                    field.setInt(holder(), Integer.parseInt(text.trim()));
                } else if (type == double.class) {
                    field.setDouble(holder(), Double.parseDouble(text.trim()));
                }
            } catch (IllegalAccessException | RuntimeException ignored) {
                // half-typed numbers are simply ignored until they are valid
            }
        }
    }

    private record Group(String id, Component title, List<Option> options) {}

    private record Category(String id, Component title, List<Group> groups) {}

    // ---- state ----
    private final Screen parent;
    private final String snapshot;
    private final List<Category> categories;
    private int categoryIndex;
    private int groupIndex;
    private int scroll;
    private int mouseX;
    private int mouseY;
    private Option hoveredOption;

    public AunConfigScreen(Screen parent) {
        super(Component.translatable("text.autoconfig.aun.title"));
        this.parent = parent;
        this.snapshot = AunConfigManager.snapshot();
        this.categories = buildModel();
    }

    // =====================================================================================
    // building the model from the config class
    // =====================================================================================
    private static List<Category> buildModel() {
        Map<String, Map<String, Group>> byCategory = new LinkedHashMap<>();
        for (Field field : AunConfig.class.getDeclaredFields()) {
            if (!isEditableField(field) && !field.isAnnotationPresent(ConfigEntry.Gui.CollapsibleObject.class)) {
                continue;
            }
            int modifiers = field.getModifiers();
            if (Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers) || field.isSynthetic()
                || field.isAnnotationPresent(ConfigEntry.Gui.Excluded.class)) {
                continue;
            }
            ConfigEntry.Category category = field.getAnnotation(ConfigEntry.Category.class);
            String categoryId = category != null ? category.value() : "qol";
            Map<String, Group> groups = byCategory.computeIfAbsent(categoryId, k -> new LinkedHashMap<>());

            if (field.isAnnotationPresent(ConfigEntry.Gui.CollapsibleObject.class)) {
                Group group = groups.computeIfAbsent(field.getName(), id ->
                    new Group(id, translatedOrName("text.autoconfig.aun.option." + id, id), new ArrayList<>()));
                for (Field inner : field.getType().getDeclaredFields()) {
                    if (isEditableField(inner) && !Modifier.isTransient(inner.getModifiers())) {
                        group.options().add(new Option(field, inner,
                            "text.autoconfig.aun.option." + field.getName() + "." + inner.getName()));
                    }
                }
            } else {
                Group group = groups.computeIfAbsent("general", id ->
                    new Group(id, Component.translatable("aun.gui.general"), new ArrayList<>()));
                group.options().add(new Option(null, field, "text.autoconfig.aun.option." + field.getName()));
            }
        }

        List<Category> result = new ArrayList<>();
        for (Map.Entry<String, Map<String, Group>> entry : byCategory.entrySet()) {
            List<Group> groups = new ArrayList<>();
            for (Group group : entry.getValue().values()) {
                if (!group.options().isEmpty()) {
                    groups.add(group);
                }
            }
            if (!groups.isEmpty()) {
                result.add(new Category(entry.getKey(),
                    translatedOrName("text.autoconfig.aun.category." + entry.getKey(), entry.getKey()), groups));
            }
        }
        return result;
    }

    private static boolean isEditableField(Field field) {
        Class<?> type = field.getType();
        int modifiers = field.getModifiers();
        return !Modifier.isStatic(modifiers) && !Modifier.isFinal(modifiers)
            && (type == boolean.class || type == String.class || type == int.class || type == double.class);
    }

    private static Component translatedOrName(String key, String fallback) {
        return Language.getInstance().has(key) ? Component.translatable(key) : Component.literal(fallback);
    }

    // =====================================================================================
    // small helpers
    // =====================================================================================
    private Category category() {
        return categories.get(categoryIndex);
    }

    private Group group() {
        return category().groups().get(groupIndex);
    }

    private int contentLeft() {
        return MARGIN + SIDE_W + GAP;
    }

    private int contentRight() {
        return this.width - MARGIN;
    }

    private int areaTop() {
        return TAB_H + 10;
    }

    private int areaBottom() {
        return this.height - 38;
    }

    private int visibleRows() {
        return Math.max(1, (areaBottom() - areaTop()) / ROW_H);
    }

    private int maxScroll() {
        return Math.max(0, group().options().size() - visibleRows());
    }

    private static boolean inside(int px, int py, int x, int y, int w, int h) {
        return px >= x && px < x + w && py >= y && py < y + h;
    }

    private int tabWidth(Component title) {
        return Math.max(80, this.font.width(title) + 28);
    }

    private int firstTabX() {
        int total = 0;
        for (Category category : categories) {
            total += tabWidth(category.title()) + 2;
        }
        return (this.width - (total - 2)) / 2;
    }

    // =====================================================================================
    // widgets (only the text boxes are real widgets; everything else is drawn by hand)
    // =====================================================================================
    @Override
    protected void init() {
        refreshControls();
    }

    private void refreshControls() {
        this.clearWidgets();
        if (categories.isEmpty()) {
            return;
        }
        List<Option> options = group().options();
        int first = scroll;
        int last = Math.min(options.size(), first + visibleRows());
        for (int i = first; i < last; i++) {
            Option option = options.get(i);
            if (option.kind != Kind.TEXT) {
                continue;
            }
            int y = areaTop() + (i - first) * ROW_H;
            EditBox box = new EditBox(this.font, contentRight() - 12 - CONTROL_W, y + 2, CONTROL_W, 18, Component.empty());
            box.setMaxLength(512);
            box.setValue(option.getText());
            box.setResponder(text -> {
                option.setText(text);
                AunConfigManager.get().syncMirrors();
            });
            this.addRenderableWidget(box);
        }
    }

    // =====================================================================================
    // drawing
    // =====================================================================================
    /** Everything except the text boxes and the tooltip is drawn here, so the boxes end up on top. */
    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.hoveredOption = null;
        if (categories.isEmpty()) {
            graphics.centeredText(this.font, Component.literal("No options found"), this.width / 2, this.height / 2, WHITE);
            return;
        }

        // top bar with the category tabs
        graphics.fill(0, 0, this.width, TAB_H, TAB_BAR);
        graphics.fill(0, TAB_H, this.width, TAB_H + 1, 0xFF000000);
        int x = firstTabX();
        for (int i = 0; i < categories.size(); i++) {
            Component title = categories.get(i).title();
            int w = tabWidth(title);
            drawTab(graphics, x, 0, w, TAB_H, title, i == categoryIndex, inside(mouseX, mouseY, x, 0, w, TAB_H), false);
            x += w + 2;
        }

        // left column with the sub-category tabs
        int sideTop = areaTop();
        int sideHeight = areaBottom() - sideTop;
        graphics.fill(MARGIN, sideTop, MARGIN + SIDE_W, sideTop + sideHeight, PANEL);
        graphics.outline(MARGIN, sideTop, SIDE_W, sideHeight, BORDER);
        List<Group> groups = category().groups();
        for (int i = 0; i < groups.size(); i++) {
            int ty = sideTop + 4 + i * (SIDE_TAB_H + 2);
            if (ty + SIDE_TAB_H > sideTop + sideHeight) {
                break;
            }
            drawTab(graphics, MARGIN + 4, ty, SIDE_W - 8, SIDE_TAB_H, groups.get(i).title(), i == groupIndex,
                inside(mouseX, mouseY, MARGIN + 4, ty, SIDE_W - 8, SIDE_TAB_H), true);
        }

        // right side with the options
        int left = contentLeft();
        int right = contentRight();
        int top = areaTop();
        int bottom = areaBottom();
        graphics.fill(left, top, right, bottom, PANEL);
        graphics.outline(left, top, right - left, bottom - top, BORDER);

        List<Option> options = group().options();
        int first = scroll;
        int last = Math.min(options.size(), first + visibleRows());
        for (int i = first; i < last; i++) {
            Option option = options.get(i);
            int y = top + (i - first) * ROW_H;
            boolean rowHovered = inside(mouseX, mouseY, left, y, right - left, ROW_H);
            if (rowHovered) {
                this.hoveredOption = option;
                graphics.fill(left + 1, y, right - 1, y + ROW_H, 0x22FFFFFF);
            }
            int labelRoom = right - left - CONTROL_W - 40;
            graphics.text(this.font, fit(Component.translatable(option.labelKey).getString(), labelRoom),
                left + 8, y + (ROW_H - 8) / 2, WHITE);

            if (option.kind == Kind.BOOL) {
                int bx = right - 12 - CONTROL_W;
                boolean on = option.getBool();
                boolean buttonHovered = inside(mouseX, mouseY, bx, y + 2, CONTROL_W, 18);
                graphics.fill(bx, y + 2, bx + CONTROL_W, y + 20, buttonHovered ? 0xFF2A2A2A : 0xFF121212);
                graphics.outline(bx, y + 2, CONTROL_W, 18, buttonHovered ? 0xFF8A8A8A : 0xFF4A4A4A);
                graphics.centeredText(this.font, Component.translatable(on ? "aun.gui.on" : "aun.gui.off"),
                    bx + CONTROL_W / 2, y + 2 + 5, on ? GREEN : RED);
            }
        }

        // scrollbar for long lists
        if (maxScroll() > 0) {
            int trackTop = top + 2;
            int trackHeight = bottom - top - 4;
            int thumbHeight = Math.max(14, trackHeight * visibleRows() / options.size());
            int thumbTop = trackTop + (trackHeight - thumbHeight) * scroll / maxScroll();
            graphics.fill(right - 7, trackTop, right - 3, trackTop + trackHeight, 0x55000000);
            graphics.fill(right - 7, thumbTop, right - 3, thumbTop + thumbHeight, 0xFFA0A0A0);
        }

        // bottom buttons
        int by = this.height - 28;
        drawButton(graphics, this.width / 2 - 155, by, 150, 20, Component.translatable("aun.gui.cancel"));
        drawButton(graphics, this.width / 2 + 5, by, 150, 20, Component.translatable("aun.gui.save"));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        if (hoveredOption == null) {
            return;
        }
        String key = hoveredOption.labelKey + ".@Tooltip";
        if (!Language.getInstance().has(key)) {
            return;
        }
        List<FormattedCharSequence> lines = this.font.split(Component.translatable(key), 220);
        int width = 0;
        for (FormattedCharSequence line : lines) {
            width = Math.max(width, this.font.width(line));
        }
        int height = lines.size() * 10;
        int tx = Math.min(mouseX + 12, this.width - width - 10);
        int ty = Math.max(4, Math.min(mouseY - 4, this.height - height - 10));
        graphics.fill(tx - 4, ty - 4, tx + width + 4, ty + height + 2, 0xF0100010);
        graphics.outline(tx - 4, ty - 4, width + 8, height + 6, 0xFF5A5A5A);
        for (int i = 0; i < lines.size(); i++) {
            graphics.text(this.font, lines.get(i), tx, ty + i * 10, WHITE);
        }
    }

    /** A tab in the style of the world-creation screen: dark box, bright underline when selected. */
    private void drawTab(GuiGraphicsExtractor graphics, int x, int y, int w, int h, Component title,
                         boolean selected, boolean hovered, boolean vertical) {
        graphics.fill(x, y, x + w, y + h, selected ? TAB_SELECTED : hovered ? TAB_HOVER : TAB_NORMAL);
        graphics.outline(x, y, w, h, BORDER);
        if (selected) {
            if (vertical) {
                graphics.fill(x, y, x + 3, y + h, WHITE);
            } else {
                graphics.fill(x + 2, y + h - 2, x + w - 2, y + h, WHITE);
            }
        }
        int color = selected || hovered ? WHITE : GREY;
        if (vertical) {
            graphics.text(this.font, fit(title.getString(), w - 16), x + 10, y + (h - 8) / 2, color);
        } else {
            graphics.centeredText(this.font, title, x + w / 2, y + (h - 8) / 2 - 1, color);
        }
    }

    private void drawButton(GuiGraphicsExtractor graphics, int x, int y, int w, int h, Component title) {
        boolean hovered = inside(mouseX, mouseY, x, y, w, h);
        graphics.fill(x, y, x + w, y + h, hovered ? 0xFF2A2A2A : 0xFF121212);
        graphics.outline(x, y, w, h, hovered ? 0xFF9A9A9A : 0xFF4A4A4A);
        graphics.centeredText(this.font, title, x + w / 2, y + (h - 8) / 2, WHITE);
    }

    /** Shortens a text with "..." until it fits into the given width. */
    private String fit(String text, int maxWidth) {
        if (this.font.width(text) <= maxWidth) {
            return text;
        }
        String cut = text;
        while (cut.length() > 1 && this.font.width(cut + "...") > maxWidth) {
            cut = cut.substring(0, cut.length() - 1);
        }
        return cut + "...";
    }

    // =====================================================================================
    // input
    // =====================================================================================
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (categories.isEmpty()) {
            return super.mouseClicked(event, doubleClick);
        }
        int mx = this.mouseX;
        int my = this.mouseY;

        // category tabs
        int x = firstTabX();
        for (int i = 0; i < categories.size(); i++) {
            int w = tabWidth(categories.get(i).title());
            if (inside(mx, my, x, 0, w, TAB_H)) {
                selectCategory(i);
                return true;
            }
            x += w + 2;
        }

        // sub-category tabs
        List<Group> groups = category().groups();
        for (int i = 0; i < groups.size(); i++) {
            int ty = areaTop() + 4 + i * (SIDE_TAB_H + 2);
            if (inside(mx, my, MARGIN + 4, ty, SIDE_W - 8, SIDE_TAB_H)) {
                selectGroup(i);
                return true;
            }
        }

        // ON/OFF buttons
        List<Option> options = group().options();
        int first = scroll;
        int last = Math.min(options.size(), first + visibleRows());
        for (int i = first; i < last; i++) {
            Option option = options.get(i);
            int y = areaTop() + (i - first) * ROW_H;
            if (option.kind == Kind.BOOL
                && inside(mx, my, contentRight() - 12 - CONTROL_W, y + 2, CONTROL_W, 18)) {
                option.setBool(!option.getBool());
                AunConfigManager.get().syncMirrors();
                this.setFocused(null);
                return true;
            }
        }

        // bottom buttons
        int by = this.height - 28;
        if (inside(mx, my, this.width / 2 - 155, by, 150, 20)) {
            onClose();
            return true;
        }
        if (inside(mx, my, this.width / 2 + 5, by, 150, 20)) {
            AunConfigManager.save();
            Minecraft.getInstance().setScreen(parent);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!categories.isEmpty() && maxScroll() > 0
            && inside((int) mouseX, (int) mouseY, contentLeft(), areaTop(), contentRight() - contentLeft(),
            areaBottom() - areaTop())) {
            scroll = Math.max(0, Math.min(maxScroll(), scroll + (scrollY < 0 ? 1 : -1)));
            refreshControls();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void selectCategory(int index) {
        categoryIndex = index;
        groupIndex = 0;
        scroll = 0;
        this.setFocused(null);
        refreshControls();
    }

    private void selectGroup(int index) {
        groupIndex = index;
        scroll = 0;
        this.setFocused(null);
        refreshControls();
    }

    /** Escape and the Cancel button: throw away every change made in this screen. */
    @Override
    public void onClose() {
        AunConfigManager.restore(snapshot);
        Minecraft.getInstance().setScreen(parent);
    }
}
