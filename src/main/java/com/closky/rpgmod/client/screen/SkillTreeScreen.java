package com.closky.rpgmod.client.screen;

import com.closky.rpgmod.RpgMod;
import com.closky.rpgmod.capability.PlayerRpgData;
import com.closky.rpgmod.client.ClientData;
import com.closky.rpgmod.config.RpgConfig;
import com.closky.rpgmod.network.ModNetwork;
import com.closky.rpgmod.network.packet.RespecPacket;
import com.closky.rpgmod.network.packet.UnlockSkillPacket;
import com.closky.rpgmod.player.PlayerClass;
import com.closky.rpgmod.skill.SkillBranch;
import com.closky.rpgmod.skill.SkillEffect;
import com.closky.rpgmod.skill.SkillNode;
import com.closky.rpgmod.skill.SkillTree;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Skill tree (section 19): a pannable canvas with branch-coloured nodes. Left click unlocks a node (server
 * validates); the "Respec" button refunds all points for the configured price.
 */
public class SkillTreeScreen extends Screen {
    private static final ResourceLocation TEXTURE = RpgMod.id("textures/gui/skill_tree.png");
    private static final int NODE_SIZE = 22;
    private static final int GRID = 40;

    private double panX;
    private double panY;
    private boolean dragging;
    private SkillBranch filter;
    private Button respecButton;

    public SkillTreeScreen() {
        super(Component.translatable("screen.rpgmod.skill_tree"));
    }

    @Override
    protected void init() {
        panX = width / 2.0 - 2 * GRID;
        panY = 50;
        int bx = 6;
        for (SkillBranch branch : SkillBranch.values()) {
            SkillBranch b = branch;
            Button button = Button.builder(branch.getDisplayName(), btn -> filter = filter == b ? null : b)
                    .bounds(bx, height - 24, 60, 18).build();
            addRenderableWidget(button);
            bx += 62;
            if (bx + 60 > width) {
                break;
            }
        }
        respecButton = addRenderableWidget(Button.builder(Component.translatable("screen.rpgmod.skill_tree.respec"), b -> ModNetwork.sendToServer(new RespecPacket()))
                .bounds(width - 86, 6, 80, 18).build());
        respecButton.active = RpgConfig.COMMON.allowRespec.get();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        PlayerRpgData data = ClientData.playerData();
        SkillTree tree = SkillTree.get();
        List<SkillNode> nodes = new ArrayList<>(tree.getNodes());
        // connections first
        for (SkillNode node : nodes) {
            if (filter != null && node.branch() != filter) {
                continue;
            }
            for (ResourceLocation parentId : node.parents()) {
                SkillNode parent = tree.getNode(parentId);
                if (parent == null) {
                    continue;
                }
                boolean lit = data != null && data.hasNode(node.id()) && data.hasNode(parentId);
                drawLine(graphics, nodeX(parent) + NODE_SIZE / 2, nodeY(parent) + NODE_SIZE / 2,
                        nodeX(node) + NODE_SIZE / 2, nodeY(node) + NODE_SIZE / 2, lit ? 0xFFFFFFFF : 0xFF555555);
            }
        }
        SkillNode hovered = null;
        for (SkillNode node : nodes) {
            if (filter != null && node.branch() != filter) {
                continue;
            }
            int x = nodeX(node);
            int y = nodeY(node);
            boolean unlocked = data != null && data.hasNode(node.id());
            boolean available = data != null && !unlocked && data.canUnlock(node);
            int border = unlocked ? 0xFFFFD700 : available ? 0xFF55FF55 : 0xFF333333;
            int fill = (unlocked ? 0xFF000000 : 0xC0000000) | (node.branch().getColor() & 0xFFFFFF);
            if (!unlocked && !available) {
                fill = 0xC0202020;
            }
            graphics.fill(x - 1, y - 1, x + NODE_SIZE + 1, y + NODE_SIZE + 1, border);
            graphics.fill(x, y, x + NODE_SIZE, y + NODE_SIZE, fill);
            graphics.renderItem(node.getIconStack(), x + 3, y + 3);
            if (mouseX >= x && mouseX < x + NODE_SIZE && mouseY >= y && mouseY < y + NODE_SIZE) {
                hovered = node;
            }
        }
        // header
        graphics.fill(0, 0, width, 30, 0xA0000000);
        graphics.drawString(font, title, 8, 6, 0xFFFFFF, true);
        if (data != null) {
            Component points = Component.translatable("screen.rpgmod.skill_tree.points", data.getSkillPoints(), data.getLevel());
            graphics.drawString(font, points, 8, 17, 0xAAFFAA, true);
            Component cls = Component.translatable("screen.rpgmod.skill_tree.class", data.getActiveClass().getDisplayName());
            graphics.drawString(font, cls, width / 2 - font.width(cls) / 2, 17, 0xDDDDDD, true);
        }
        if (filter != null) {
            Component f = Component.translatable("screen.rpgmod.skill_tree.filter", filter.getDisplayName());
            graphics.drawString(font, f, width / 2 - font.width(f) / 2, 6, filter.getColor(), true);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
        if (hovered != null) {
            graphics.renderComponentTooltip(font, tooltip(hovered, data), mouseX, mouseY);
        }
    }

    private List<Component> tooltip(SkillNode node, @Nullable PlayerRpgData data) {
        List<Component> lines = new ArrayList<>();
        lines.add(node.getTitle().copy().withStyle(ChatFormatting.BOLD));
        lines.add(node.getDescription().copy().withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("screen.rpgmod.skill_tree.branch", node.branch().getDisplayName()));
        for (SkillEffect effect : node.effects()) {
            lines.add(effect.describe().copy().withStyle(ChatFormatting.AQUA));
        }
        lines.add(Component.translatable("screen.rpgmod.skill_tree.cost", node.cost(), node.requiredLevel()).withStyle(ChatFormatting.YELLOW));
        if (node.requiredClass() != null && node.requiredClass() != PlayerClass.NONE) {
            lines.add(Component.translatable("screen.rpgmod.skill_tree.requires_class", node.requiredClass().getDisplayName()).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        if (data != null) {
            if (data.hasNode(node.id())) {
                lines.add(Component.translatable("screen.rpgmod.skill_tree.unlocked").withStyle(ChatFormatting.GOLD));
            } else if (data.canUnlock(node)) {
                lines.add(Component.translatable("screen.rpgmod.skill_tree.click_to_unlock").withStyle(ChatFormatting.GREEN));
            } else {
                lines.add(Component.translatable("screen.rpgmod.skill_tree.locked").withStyle(ChatFormatting.RED));
            }
        }
        return lines;
    }

    private int nodeX(SkillNode node) {
        return (int) (panX + node.x() * GRID);
    }

    private int nodeY(SkillNode node) {
        return (int) (panY + node.y() * GRID);
    }

    private static void drawLine(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));
        if (steps == 0) {
            return;
        }
        for (int i = 0; i <= steps; i++) {
            int x = x1 + (x2 - x1) * i / steps;
            int y = y1 + (y2 - y1) * i / steps;
            graphics.fill(x, y, x + 2, y + 2, color);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (button == 0) {
            for (SkillNode node : SkillTree.get().getNodes()) {
                if (filter != null && node.branch() != filter) {
                    continue;
                }
                int x = nodeX(node);
                int y = nodeY(node);
                if (mouseX >= x && mouseX < x + NODE_SIZE && mouseY >= y && mouseY < y + NODE_SIZE) {
                    PlayerRpgData data = ClientData.playerData();
                    if (data != null && !data.hasNode(node.id()) && data.canUnlock(node)) {
                        ModNetwork.sendToServer(new UnlockSkillPacket(node.id()));
                    }
                    return true;
                }
            }
            dragging = true;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragging) {
            panX += dragX;
            panY += dragY;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        panY += delta * 20;
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
