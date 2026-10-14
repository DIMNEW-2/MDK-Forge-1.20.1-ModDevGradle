package com.closky.rpgmod.network.packet;

import com.closky.rpgmod.client.ClientPacketHandler;
import com.closky.rpgmod.skill.SkillNode;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

/** Server → client: all skill nodes (sent on login and on data pack reload). */
public class SyncSkillTreePacket {
    private final List<SkillNode> nodes;

    public SyncSkillTreePacket(List<SkillNode> nodes) {
        this.nodes = nodes;
    }

    public List<SkillNode> getNodes() {
        return nodes;
    }

    public static void encode(SyncSkillTreePacket msg, FriendlyByteBuf buf) {
        buf.writeCollection(msg.nodes, (b, node) -> node.write(b));
    }

    public static SyncSkillTreePacket decode(FriendlyByteBuf buf) {
        return new SyncSkillTreePacket(buf.readList(SkillNode::read));
    }

    public static void handle(SyncSkillTreePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleSkillTree(msg)));
        ctx.get().setPacketHandled(true);
    }
}
