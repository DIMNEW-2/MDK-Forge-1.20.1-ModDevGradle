package com.closky.rpgmod.network.packet;

import com.closky.rpgmod.magic.Spell;
import com.closky.rpgmod.magic.SpellCaster;
import com.closky.rpgmod.magic.StaffItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client → server: quick-cast key pressed; casts the spell selected on the held staff. */
public class CastSpellPacket {
    public CastSpellPacket() {
    }

    public static void encode(CastSpellPacket msg, FriendlyByteBuf buf) {
    }

    public static CastSpellPacket decode(FriendlyByteBuf buf) {
        return new CastSpellPacket();
    }

    public static void handle(CastSpellPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }
            for (InteractionHand hand : InteractionHand.values()) {
                ItemStack stack = player.getItemInHand(hand);
                if (stack.getItem() instanceof StaffItem staff) {
                    Spell spell = staff.getOrSelectSpell(stack, player);
                    if (spell != null && SpellCaster.tryCast(player, spell, stack, false)) {
                        stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
                        player.swing(hand, true);
                    }
                    return;
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
