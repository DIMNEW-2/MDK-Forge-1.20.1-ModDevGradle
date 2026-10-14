package com.closky.rpgmod.network.packet;

import com.closky.rpgmod.capability.PlayerRpgData;
import com.closky.rpgmod.magic.Spell;
import com.closky.rpgmod.magic.SpellCaster;
import com.closky.rpgmod.magic.StaffItem;
import com.closky.rpgmod.registry.ModSpells;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client → server: select a spell on the staff held in the given hand (from the spell selection screen). */
public class SelectSpellPacket {
    private final InteractionHand hand;
    private final ResourceLocation spellId;

    public SelectSpellPacket(InteractionHand hand, ResourceLocation spellId) {
        this.hand = hand;
        this.spellId = spellId;
    }

    public static void encode(SelectSpellPacket msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.hand);
        buf.writeResourceLocation(msg.spellId);
    }

    public static SelectSpellPacket decode(FriendlyByteBuf buf) {
        return new SelectSpellPacket(buf.readEnum(InteractionHand.class), buf.readResourceLocation());
    }

    public static void handle(SelectSpellPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }
            ItemStack stack = player.getItemInHand(msg.hand);
            Spell spell = ModSpells.REGISTRY.get().getValue(msg.spellId);
            PlayerRpgData data = PlayerRpgData.get(player);
            if (!(stack.getItem() instanceof StaffItem staff) || spell == null || data == null) {
                return;
            }
            if (spell.getSchool() == staff.getSchool() && SpellCaster.knows(data, spell)) {
                StaffItem.setSelectedSpell(stack, spell);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
