package com.closky.rpgmod.network.packet;

import com.closky.rpgmod.client.ClientPacketHandler;
import com.closky.rpgmod.settlement.SettlementSnapshot;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

/** Server → client: snapshot of all known settlements and their relations (for the relations map). */
public class SyncSettlementsPacket {
    private final List<SettlementSnapshot> settlements;
    private final boolean openScreen;

    public SyncSettlementsPacket(List<SettlementSnapshot> settlements, boolean openScreen) {
        this.settlements = settlements;
        this.openScreen = openScreen;
    }

    public List<SettlementSnapshot> getSettlements() {
        return settlements;
    }

    public boolean shouldOpenScreen() {
        return openScreen;
    }

    public static void encode(SyncSettlementsPacket msg, FriendlyByteBuf buf) {
        buf.writeCollection(msg.settlements, (b, s) -> s.write(b));
        buf.writeBoolean(msg.openScreen);
    }

    public static SyncSettlementsPacket decode(FriendlyByteBuf buf) {
        return new SyncSettlementsPacket(buf.readList(SettlementSnapshot::read), buf.readBoolean());
    }

    public static void handle(SyncSettlementsPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleSettlements(msg)));
        ctx.get().setPacketHandled(true);
    }
}
