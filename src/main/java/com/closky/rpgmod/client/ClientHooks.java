package com.closky.rpgmod.client;

import com.closky.rpgmod.client.screen.CharacterScreen;
import com.closky.rpgmod.client.screen.RelationsMapScreen;
import com.closky.rpgmod.client.screen.SkillTreeScreen;
import com.closky.rpgmod.client.screen.SpellSelectScreen;
import com.closky.rpgmod.magic.StaffItem;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/** Entry points into client-only code, called through {@code DistExecutor} from common code. */
public final class ClientHooks {
    private ClientHooks() {
    }

    public static void openSkillTree() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.setScreen(new SkillTreeScreen());
        }
    }

    public static void openCharacter() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.setScreen(new CharacterScreen());
        }
    }

    public static void openRelations() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.setScreen(new RelationsMapScreen());
        }
    }

    /** Opens the spell selection wheel for the staff in the player's hand, if any. */
    public static boolean openSpellSelect() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return false;
        }
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = mc.player.getItemInHand(hand);
            if (stack.getItem() instanceof StaffItem staff) {
                mc.setScreen(new SpellSelectScreen(hand, staff, stack));
                return true;
            }
        }
        return false;
    }
}
