package com.closky.rpgmod.network.packet;

import com.closky.rpgmod.capability.PlayerRpgData;
import com.closky.rpgmod.config.RpgConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client → server: reset all skill nodes. Costs one emerald block unless in creative (section 7.7). */
public class RespecPacket {
    public RespecPacket() {
    }

    public static void encode(RespecPacket msg, FriendlyByteBuf buf) {
    }

    public static RespecPacket decode(FriendlyByteBuf buf) {
        return new RespecPacket();
    }

    public static void handle(RespecPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null || !RpgConfig.COMMON.allowRespec.get()) {
                return;
            }
            PlayerRpgData data = PlayerRpgData.get(player);
            if (data == null || data.getUnlockedNodes().isEmpty()) {
                return;
            }
            if (!player.getAbilities().instabuild) {
                int slot = player.getInventory().findSlotMatchingItem(new ItemStack(Items.EMERALD_BLOCK));
                if (slot < 0) {
                    player.displayClientMessage(Component.translatable("message.rpgmod.respec_cost").withStyle(ChatFormatting.RED), false);
                    return;
                }
                player.getInventory().removeItem(slot, 1);
            }
            int refunded = data.respec(player);
            data.sync(player);
            player.displayClientMessage(Component.translatable("message.rpgmod.respec_done", refunded).withStyle(ChatFormatting.GREEN), false);
        });
        ctx.get().setPacketHandled(true);
    }
}
