package com.closky.rpgmod.network.packet;

import com.closky.rpgmod.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server → client: asks the client to open one of the client-only screens (used by commands). */
public class OpenClientScreenPacket {
    public static final int SKILL_TREE = 0;
    public static final int RELATIONS = 1;
    public static final int CHARACTER = 2;

    private final int screen;

    public OpenClientScreenPacket(int screen) {
        this.screen = screen;
    }

    public int getScreen() {
        return screen;
    }

    public static void encode(OpenClientScreenPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.screen);
    }

    public static OpenClientScreenPacket decode(FriendlyByteBuf buf) {
        return new OpenClientScreenPacket(buf.readVarInt());
    }

    public static void handle(OpenClientScreenPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleOpenScreen(msg)));
        ctx.get().setPacketHandled(true);
    }
}
