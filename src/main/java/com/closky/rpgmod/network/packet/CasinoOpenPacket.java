package com.closky.rpgmod.network.packet;

import com.closky.rpgmod.block.LootChestBlockEntity;
import com.closky.rpgmod.menu.LootChestMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client → server: "spin" button pressed in the casino screen. */
public class CasinoOpenPacket {
    private final BlockPos pos;

    public CasinoOpenPacket(BlockPos pos) {
        this.pos = pos;
    }

    public static void encode(CasinoOpenPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
    }

    public static CasinoOpenPacket decode(FriendlyByteBuf buf) {
        return new CasinoOpenPacket(buf.readBlockPos());
    }

    public static void handle(CasinoOpenPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null || !(player.containerMenu instanceof LootChestMenu menu) || !menu.getPos().equals(msg.pos)) {
                return;
            }
            if (!player.level().isLoaded(msg.pos) || player.distanceToSqr(msg.pos.getX() + 0.5D, msg.pos.getY() + 0.5D, msg.pos.getZ() + 0.5D) > 64.0D) {
                return;
            }
            if (player.level().getBlockEntity(msg.pos) instanceof LootChestBlockEntity chest) {
                chest.roll(player);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
