package com.closky.rpgmod.client.screen;

import com.closky.rpgmod.client.ClientData;
import com.closky.rpgmod.settlement.RelationState;
import com.closky.rpgmod.settlement.SettlementSnapshot;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Relations map (section 18): known settlements drawn on a schematic top-down map, connected by lines
 * coloured by relation state; clicking a settlement shows its details and the player's reputation.
 */
public class RelationsMapScreen extends Screen {
    private static final int MAP_MARGIN = 20;
    private static final int PANEL_W = 150;

    private SettlementSnapshot selected;
    private double scale = 1.0;
    private double originX;
    private double originZ;

    public RelationsMapScreen() {
        super(Component.translatable("screen.rpgmod.relations"));
    }

    @Override
    protected void init() {
        fitMap();
    }

    private void fitMap() {
        List<SettlementSnapshot> list = ClientData.getSettlements();
        Minecraft mc = Minecraft.getInstance();
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;
        List<BlockPos> points = new ArrayList<>();
        for (SettlementSnapshot s : list) {
            points.add(s.center());
        }
        if (mc.player != null) {
            points.add(mc.player.blockPosition());
        }
        for (BlockPos p : points) {
            minX = Math.min(minX, p.getX());
            maxX = Math.max(maxX, p.getX());
            minZ = Math.min(minZ, p.getZ());
            maxZ = Math.max(maxZ, p.getZ());
        }
        if (points.isEmpty()) {
            minX = minZ = -500;
            maxX = maxZ = 500;
        }
        int spanX = Math.max(200, maxX - minX);
        int spanZ = Math.max(200, maxZ - minZ);
        int mapW = width - PANEL_W - MAP_MARGIN * 2;
        int mapH = height - MAP_MARGIN * 2 - 20;
        scale = Math.min(mapW / (double) (spanX * 1.2), mapH / (double) (spanZ * 1.2));
        originX = (minX + maxX) / 2.0;
        originZ = (minZ + maxZ) / 2.0;
    }

    private int screenX(int worldX) {
        int mapW = width - PANEL_W - MAP_MARGIN * 2;
        return (int) (MAP_MARGIN + mapW / 2.0 + (worldX - originX) * scale);
    }

    private int screenY(int worldZ) {
        int mapH = height - MAP_MARGIN * 2 - 20;
        return (int) (MAP_MARGIN + 20 + mapH / 2.0 + (worldZ - originZ) * scale);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int mapRight = width - PANEL_W - MAP_MARGIN;
        graphics.fill(MAP_MARGIN, MAP_MARGIN + 20, mapRight, height - MAP_MARGIN, 0xC0101820);
        graphics.drawString(font, title, MAP_MARGIN, MAP_MARGIN + 6, 0xFFFFFF, true);
        List<SettlementSnapshot> list = ClientData.getSettlements();
        if (list.isEmpty()) {
            Component empty = Component.translatable("screen.rpgmod.relations.empty");
            graphics.drawString(font, empty, width / 2 - font.width(empty) / 2, height / 2, 0xAAAAAA, true);
        }
        // relation lines
        for (int i = 0; i < list.size(); i++) {
            for (int j = i + 1; j < list.size(); j++) {
                SettlementSnapshot a = list.get(i);
                SettlementSnapshot b = list.get(j);
                RelationState state = a.relationState(b.id());
                if (state == RelationState.NEUTRAL && selected != a && selected != b) {
                    continue; // keep the map readable
                }
                int color = 0xFF000000 | state.getColor();
                drawLine(graphics, screenX(a.center().getX()), screenY(a.center().getZ()), screenX(b.center().getX()), screenY(b.center().getZ()), color, state == RelationState.WAR ? 2 : 1);
            }
        }
        // settlements
        SettlementSnapshot hovered = null;
        for (SettlementSnapshot s : list) {
            int x = screenX(s.center().getX());
            int y = screenY(s.center().getZ());
            int r = 3 + s.developmentTier();
            int color = 0xFF000000 | s.villageClass().getColor();
            graphics.fill(x - r, y - r, x + r, y + r, color);
            if (s == selected) {
                graphics.fill(x - r - 1, y - r - 1, x + r + 1, y - r, 0xFFFFFFFF);
                graphics.fill(x - r - 1, y + r, x + r + 1, y + r + 1, 0xFFFFFFFF);
                graphics.fill(x - r - 1, y - r, x - r, y + r, 0xFFFFFFFF);
                graphics.fill(x + r, y - r, x + r + 1, y + r, 0xFFFFFFFF);
            }
            if (!s.wars().isEmpty()) {
                graphics.drawString(font, "⚔", x + r + 1, y - r - 2, 0xFFFF5555, true);
            }
            graphics.drawString(font, s.name(), x - font.width(s.name()) / 2, y + r + 2, 0xFFFFFF, true);
            if (Math.abs(mouseX - x) <= r + 2 && Math.abs(mouseY - y) <= r + 2) {
                hovered = s;
            }
        }
        // player marker
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            int px = screenX(mc.player.getBlockX());
            int py = screenY(mc.player.getBlockZ());
            graphics.fill(px - 2, py - 2, px + 2, py + 2, 0xFFFFFFFF);
            graphics.drawString(font, Component.translatable("screen.rpgmod.relations.you"), px + 4, py - 4, 0xFFFFFF, true);
        }
        // legend
        int ly = height - MAP_MARGIN - 10 * RelationState.values().length - 4;
        for (RelationState state : RelationState.values()) {
            graphics.fill(MAP_MARGIN + 4, ly + 1, MAP_MARGIN + 12, ly + 8, 0xFF000000 | state.getColor());
            graphics.drawString(font, Component.translatable(state.getTranslationKey()), MAP_MARGIN + 16, ly, 0xDDDDDD, true);
            ly += 10;
        }
        renderPanel(graphics, list);
        super.render(graphics, mouseX, mouseY, partialTick);
        if (hovered != null && hovered != selected) {
            graphics.renderTooltip(font, Component.literal(hovered.name()).append(" - ").append(hovered.villageClass().getDisplayName()), mouseX, mouseY);
        }
    }

    private void renderPanel(GuiGraphics graphics, List<SettlementSnapshot> list) {
        int px = width - PANEL_W - MAP_MARGIN + 6;
        int py = MAP_MARGIN;
        graphics.fill(px - 2, py, width - MAP_MARGIN + 6, height - MAP_MARGIN, 0xC0202020);
        if (selected == null) {
            graphics.drawWordWrap(font, Component.translatable("screen.rpgmod.relations.hint"), px + 2, py + 6, PANEL_W - 12, 0xAAAAAA);
            return;
        }
        SettlementSnapshot s = selected;
        int y = py + 6;
        graphics.drawString(font, Component.literal(s.name()).withStyle(ChatFormatting.BOLD), px + 2, y, 0xFFFFFF, true);
        y += 12;
        graphics.drawString(font, s.villageClass().getDisplayName().copy().withStyle(ChatFormatting.AQUA), px + 2, y, 0xFFFFFF, true);
        y += 12;
        graphics.drawString(font, Component.translatable("screen.rpgmod.relations.tier", s.developmentTier()), px + 2, y, 0xDDDDDD, true);
        y += 10;
        graphics.drawString(font, Component.translatable("screen.rpgmod.relations.population", s.population()), px + 2, y, 0xDDDDDD, true);
        y += 10;
        graphics.drawString(font, Component.translatable("screen.rpgmod.relations.prosperity", s.prosperity()), px + 2, y, 0xDDDDDD, true);
        y += 10;
        graphics.drawString(font, Component.translatable("screen.rpgmod.relations.military", s.military()), px + 2, y, 0xDDDDDD, true);
        y += 10;
        graphics.drawString(font, Component.translatable("screen.rpgmod.relations.position", s.center().getX(), s.center().getZ()), px + 2, y, 0xDDDDDD, true);
        y += 12;
        int rep = s.playerReputation();
        ChatFormatting repColor = rep <= -50 ? ChatFormatting.DARK_RED : rep < 0 ? ChatFormatting.RED : rep >= 50 ? ChatFormatting.GREEN : ChatFormatting.YELLOW;
        graphics.drawString(font, Component.translatable("screen.rpgmod.relations.reputation", rep).withStyle(repColor), px + 2, y, 0xFFFFFF, true);
        y += 14;
        graphics.drawString(font, Component.translatable("screen.rpgmod.relations.relations").withStyle(ChatFormatting.UNDERLINE), px + 2, y, 0xFFFFFF, true);
        y += 11;
        for (Map.Entry<UUID, Integer> entry : s.relations().entrySet()) {
            SettlementSnapshot other = find(list, entry.getKey());
            if (other == null) {
                continue;
            }
            RelationState state = s.relationState(other.id());
            Component line = Component.literal(other.name() + ": ").append(Component.translatable(state.getTranslationKey()).withStyle(style -> style.withColor(state.getColor())))
                    .append(Component.literal(" (" + entry.getValue() + ")").withStyle(ChatFormatting.GRAY));
            graphics.drawString(font, line, px + 2, y, 0xFFFFFF, true);
            y += 10;
            if (y > height - MAP_MARGIN - 10) {
                break;
            }
        }
    }

    @Nullable
    private static SettlementSnapshot find(List<SettlementSnapshot> list, UUID id) {
        for (SettlementSnapshot s : list) {
            if (s.id().equals(id)) {
                return s;
            }
        }
        return null;
    }

    private static void drawLine(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color, int thickness) {
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));
        if (steps == 0) {
            return;
        }
        for (int i = 0; i <= steps; i++) {
            int x = x1 + (x2 - x1) * i / steps;
            int y = y1 + (y2 - y1) * i / steps;
            graphics.fill(x, y, x + thickness, y + thickness, color);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (SettlementSnapshot s : ClientData.getSettlements()) {
                int x = screenX(s.center().getX());
                int y = screenY(s.center().getZ());
                int r = 3 + s.developmentTier();
                if (Math.abs(mouseX - x) <= r + 2 && Math.abs(mouseY - y) <= r + 2) {
                    selected = s;
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        scale = Math.max(0.01, Math.min(4.0, scale * (delta > 0 ? 1.15 : 0.87)));
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        originX -= dragX / scale;
        originZ -= dragY / scale;
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
