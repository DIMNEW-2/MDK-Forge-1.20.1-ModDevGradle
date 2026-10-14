package com.closky.rpgmod.client.screen;

import com.closky.rpgmod.capability.PlayerRpgData;
import com.closky.rpgmod.client.ClientData;
import com.closky.rpgmod.magic.Spell;
import com.closky.rpgmod.magic.SpellCaster;
import com.closky.rpgmod.magic.StaffItem;
import com.closky.rpgmod.network.ModNetwork;
import com.closky.rpgmod.network.packet.SelectSpellPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Quick spell picker for the staff in hand (opened with the "select spell" key). */
public class SpellSelectScreen extends Screen {
    private final InteractionHand hand;
    private final StaffItem staff;
    private final ItemStack stack;

    public SpellSelectScreen(InteractionHand hand, StaffItem staff, ItemStack stack) {
        super(Component.translatable("screen.rpgmod.spell_select"));
        this.hand = hand;
        this.staff = staff;
        this.stack = stack;
    }

    @Override
    protected void init() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        List<Spell> spells = staff.getAvailableSpells(mc.player);
        PlayerRpgData data = ClientData.playerData();
        Spell selected = StaffItem.getSelectedSpell(stack);
        int y = height / 2 - spells.size() * 11;
        for (Spell spell : spells) {
            Component label = spell.getDisplayName().copy().withStyle(spell.getSchool().getColor());
            if (data != null) {
                label = label.copy().append(Component.literal("  ").append(Component.translatable("screen.rpgmod.spell_select.cost",
                        (int) SpellCaster.getManaCost(data, spell), SpellCaster.getCooldown(data, spell) / 20).withStyle(ChatFormatting.GRAY)));
            }
            if (spell == selected) {
                label = Component.literal("> ").append(label).append(" <");
            }
            Spell s = spell;
            addRenderableWidget(Button.builder(label, b -> select(s)).bounds(width / 2 - 100, y, 200, 20).build());
            y += 22;
        }
        if (spells.isEmpty()) {
            addRenderableWidget(Button.builder(Component.translatable("message.rpgmod.no_spells"), b -> onClose())
                    .bounds(width / 2 - 100, height / 2 - 10, 200, 20).build());
        }
    }

    private void select(Spell spell) {
        ModNetwork.sendToServer(new SelectSpellPacket(hand, spell.getId()));
        StaffItem.setSelectedSpell(stack, spell); // optimistic client update
        onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        Component header = Component.translatable("screen.rpgmod.spell_select.header", staff.getSchool().getDisplayName());
        graphics.drawString(font, header, width / 2 - font.width(header) / 2, 20, 0xFFFFFF, true);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
