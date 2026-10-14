package com.closky.rpgmod.network.packet;

import com.closky.rpgmod.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server → client: level / boss data of a tracked mob, used by nameplates. */
public class SyncMobLevelPacket {
    private final int entityId;
    private final int level;
    private final boolean boss;
    private final String title;

    public SyncMobLevelPacket(int entityId, int level, boolean boss, String title) {
        this.entityId = entityId;
        this.level = level;
        this.boss = boss;
        this.title = title;
    }

    public int getEntityId() {
        return entityId;
    }

    public int getLevel() {
        return level;
    }

    public boolean isBoss() {
        return boss;
    }

    public String getTitle() {
        return title;
    }

    public static void encode(SyncMobLevelPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entityId);
        buf.writeVarInt(msg.level);
        buf.writeBoolean(msg.boss);
        buf.writeUtf(msg.title, 64);
    }

    public static SyncMobLevelPacket decode(FriendlyByteBuf buf) {
        return new SyncMobLevelPacket(buf.readVarInt(), buf.readVarInt(), buf.readBoolean(), buf.readUtf(64));
    }

    public static void handle(SyncMobLevelPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleMobLevel(msg)));
        ctx.get().setPacketHandled(true);
    }
}
