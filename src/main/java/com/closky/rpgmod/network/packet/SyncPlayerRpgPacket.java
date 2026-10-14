package com.closky.rpgmod.network.packet;

import com.closky.rpgmod.client.ClientPacketHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server → client: full copy of the player's RPG capability. */
public class SyncPlayerRpgPacket {
    private final CompoundTag data;

    public SyncPlayerRpgPacket(CompoundTag data) {
        this.data = data;
    }

    public CompoundTag getData() {
        return data;
    }

    public static void encode(SyncPlayerRpgPacket msg, FriendlyByteBuf buf) {
        buf.writeNbt(msg.data);
    }

    public static SyncPlayerRpgPacket decode(FriendlyByteBuf buf) {
        CompoundTag tag = buf.readNbt();
        return new SyncPlayerRpgPacket(tag == null ? new CompoundTag() : tag);
    }

    public static void handle(SyncPlayerRpgPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handlePlayerSync(msg)));
        ctx.get().setPacketHandled(true);
    }
}
