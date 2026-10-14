package com.closky.rpgmod.client.screen;

import com.closky.rpgmod.RpgMod;
import com.closky.rpgmod.capability.PlayerRpgData;
import com.closky.rpgmod.client.ClientData;
import com.closky.rpgmod.client.ClientHooks;
import com.closky.rpgmod.network.ModNetwork;
import com.closky.rpgmod.network.packet.AllocateStatPacket;
import com.closky.rpgmod.player.StatType;
import com.closky.rpgmod.registry.ModSpells;
import com.closky.rpgmod.magic.Spell;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Character sheet: level, XP, class, mana, stat allocation and known spells (section 7). */
public class CharacterScreen extends Screen {
    private static final ResourceLocation TEXTURE = RpgMod.id("textures/gui/character.png");
    private static final int PANEL_W = 230;
    private static final int PANEL_H = 200;

    private final Map<StatType, Button> statButtons = new EnumMap<>(StatType.class);
    private int left;
    private int top;

    public CharacterScreen() {
        super(Component.translatable("screen.rpgmod.character"));
    }

    @Override
    protected void init() {
        left = (width - PANEL_W) / 2;
        top = (height - PANEL_H) / 2;
        int y = top + 58;
        for (StatType type : StatType.values()) {
            StatType t = type;
            Button button = Button.builder(Component.literal("+"), b -> ModNetwork.sendToServer(new AllocateStatPacket(t)))
                    .bounds(left + 118, y - 1, 14, 12).build();
            statButtons.put(type, button);
            addRenderableWidget(button);
            y += 14;
        }
        addRenderableWidget(Button.builder(Component.translatable("screen.rpgmod.character.skills"), b -> ClientHooks.openSkillTree())
                .bounds(left + PANEL_W - 76, top + PANEL_H - 24, 70, 18).build());
    }

    @Override
    public void tick() {
        PlayerRpgData data = ClientData.playerData();
        boolean canAllocate = data != null && data.getStatPoints() > 0;
        for (Button button : statButtons.values()) {
            button.active = canAllocate;
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.blit(TEXTURE, left, top, 0, 0, PANEL_W, PANEL_H, 256, 256);
        PlayerRpgData data = ClientData.playerData();
        Minecraft mc = Minecraft.getInstance();
        graphics.drawString(font, title, left + 8, top + 6, 0x404040, false);
        if (data != null && mc.player != null) {
            graphics.drawString(font, Component.translatable("screen.rpgmod.character.level", data.getLevel()), left + 8, top + 20, 0xFFD966, true);
            graphics.drawString(font, Component.translatable("screen.rpgmod.character.xp", data.getXp(), data.getXpForNextLevel()), left + 8, top + 31, 0xDDDDDD, true);
            Component cls = Component.translatable("screen.rpgmod.character.class", data.getActiveClass().getDisplayName().copy().withStyle(data.getActiveClass().getColor()));
            graphics.drawString(font, cls, left + 120, top + 20, 0xDDDDDD, true);
            graphics.drawString(font, Component.translatable("screen.rpgmod.character.mana", (int) data.getMana(), (int) data.getMaxMana(mc.player)), left + 120, top + 31, 0x55CCFF, true);
            // stats
            graphics.drawString(font, Component.translatable("screen.rpgmod.character.stat_points", data.getStatPoints()), left + 8, top + 46, 0xAAFFAA, true);
            int y = top + 58;
            StatType hovered = null;
            for (StatType type : StatType.values()) {
                Component line = Component.translatable("screen.rpgmod.character.stat_line", type.getDisplayName(), data.getStat(type));
                graphics.drawString(font, line, left + 10, y, 0xFFFFFF, true);
                if (mouseX >= left + 10 && mouseX < left + 116 && mouseY >= y - 2 && mouseY < y + 10) {
                    hovered = type;
                }
                y += 14;
            }
            // known spells
            graphics.drawString(font, Component.translatable("screen.rpgmod.character.spells"), left + 140, top + 46, 0xCC99FF, true);
            int sy = top + 58;
            List<Spell> known = new ArrayList<>();
            for (Spell spell : ModSpells.REGISTRY.get().getValues()) {
                if (spell.isBasic() || data.knowsSpell(spell.getId())) {
                    known.add(spell);
                }
            }
            known.sort((a, b) -> a.getSchool().ordinal() != b.getSchool().ordinal() ? Integer.compare(a.getSchool().ordinal(), b.getSchool().ordinal()) : Integer.compare(a.getRequiredLevel(), b.getRequiredLevel()));
            for (int i = 0; i < known.size() && i < 9; i++) {
                Spell spell = known.get(i);
                graphics.drawString(font, spell.getDisplayName().copy().withStyle(spell.getSchool().getColor()), left + 140, sy, 0xFFFFFF, true);
                sy += 11;
            }
            if (known.size() > 9) {
                graphics.drawString(font, Component.literal("+" + (known.size() - 9)), left + 140, sy, 0x888888, true);
            }
            // class bonus description
            graphics.drawWordWrap(font, data.getActiveClass().getBonusDescription().copy().withStyle(ChatFormatting.GRAY), left + 8, top + 150, PANEL_W - 90, 0xAAAAAA);
            super.render(graphics, mouseX, mouseY, partialTick);
            if (hovered != null) {
                graphics.renderTooltip(font, hovered.getDescription(), mouseX, mouseY);
            }
            return;
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
