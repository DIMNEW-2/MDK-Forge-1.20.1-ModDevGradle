package com.closky.rpgmod.client;

import com.closky.rpgmod.RpgMod;
import com.closky.rpgmod.capability.PlayerRpgData;
import com.closky.rpgmod.config.RpgConfig;
import com.closky.rpgmod.magic.Spell;
import com.closky.rpgmod.magic.SpellCaster;
import com.closky.rpgmod.magic.StaffItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/** HUD: mana bar, character level / XP, selected spell and its cooldown. */
public class RpgHudOverlay implements IGuiOverlay {
    private static final ResourceLocation TEXTURE = RpgMod.id("textures/gui/hud.png");

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || !RpgConfig.CLIENT.showRpgHud.get()) {
            return;
        }
        PlayerRpgData data = ClientData.playerData();
        if (data == null) {
            return;
        }
        int x = screenWidth / 2 + 10;
        int y = screenHeight - 49;
        if (mc.player.isCreative() || mc.player.isSpectator()) {
            y = screenHeight - 33;
        }
        // mana bar (82x5 background + fill), drawn right of the hunger bar
        float mana = data.getMana();
        float maxMana = Math.max(1.0F, data.getMaxMana(mc.player));
        int width = 81;
        graphics.blit(TEXTURE, x, y - 10, 0, 0, width, 5, 256, 256);
        int fill = (int) (width * Math.min(1.0F, mana / maxMana));
        graphics.blit(TEXTURE, x, y - 10, 0, 5, fill, 5, 256, 256);
        String manaText = (int) mana + "/" + (int) maxMana;
        graphics.drawString(mc.font, manaText, x + width - mc.font.width(manaText), y - 19, 0x55CCFF, true);

        // level and xp progress (small bar on the left side)
        int lx = screenWidth / 2 - 91;
        graphics.blit(TEXTURE, lx, y - 10, 0, 10, width, 5, 256, 256);
        int xpFill = (int) (width * Math.min(1.0F, data.getXpProgress()));
        graphics.blit(TEXTURE, lx, y - 10, 0, 15, xpFill, 5, 256, 256);
        Component levelText = Component.translatable("hud.rpgmod.level", data.getLevel());
        graphics.drawString(mc.font, levelText, lx, y - 19, 0xFFD966, true);

        // selected spell for a held staff
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = mc.player.getItemInHand(hand);
            if (stack.getItem() instanceof StaffItem) {
                Spell spell = StaffItem.getSelectedSpell(stack);
                if (spell == null) {
                    break;
                }
                int cooldown = data.getSpellCooldown(spell.getId());
                Component text = cooldown > 0
                        ? Component.translatable("hud.rpgmod.spell_cooldown", spell.getDisplayName(), String.format("%.1f", cooldown / 20.0F))
                        : Component.translatable("hud.rpgmod.spell_ready", spell.getDisplayName(), (int) SpellCaster.getManaCost(data, spell));
                int tw = mc.font.width(text);
                graphics.drawString(mc.font, text, screenWidth / 2 - tw / 2, y - 31, cooldown > 0 ? 0xAAAAAA : 0xFFFFFF, true);
                break;
            }
        }
    }
}
