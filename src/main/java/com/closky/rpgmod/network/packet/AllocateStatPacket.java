package com.closky.rpgmod.network.packet;

import com.closky.rpgmod.capability.PlayerRpgData;
import com.closky.rpgmod.player.StatType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client → server: spend one stat point. */
public class AllocateStatPacket {
    private final StatType stat;

    public AllocateStatPacket(StatType stat) {
        this.stat = stat;
    }

    public static void encode(AllocateStatPacket msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.stat);
    }

    public static AllocateStatPacket decode(FriendlyByteBuf buf) {
        return new AllocateStatPacket(buf.readEnum(StatType.class));
    }

    public static void handle(AllocateStatPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }
            PlayerRpgData data = PlayerRpgData.get(player);
            if (data != null && data.allocateStat(player, msg.stat)) {
                data.sync(player);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
