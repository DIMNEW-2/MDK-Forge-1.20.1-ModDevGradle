package com.closky.rpgmod.network.packet;

import com.closky.rpgmod.block.LootChestBlockEntity;
import com.closky.rpgmod.menu.LootChestMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client → server: "claim" button pressed in the casino screen. */
public class CasinoClaimPacket {
    private final BlockPos pos;

    public CasinoClaimPacket(BlockPos pos) {
        this.pos = pos;
    }

    public static void encode(CasinoClaimPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
    }

    public static CasinoClaimPacket decode(FriendlyByteBuf buf) {
        return new CasinoClaimPacket(buf.readBlockPos());
    }

    public static void handle(CasinoClaimPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null || !(player.containerMenu instanceof LootChestMenu menu) || !menu.getPos().equals(msg.pos)) {
                return;
            }
            if (player.level().getBlockEntity(msg.pos) instanceof LootChestBlockEntity chest) {
                chest.claim(player);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
