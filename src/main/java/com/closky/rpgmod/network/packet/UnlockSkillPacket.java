package com.closky.rpgmod.network.packet;

import com.closky.rpgmod.capability.PlayerRpgData;
import com.closky.rpgmod.config.RpgConfig;
import com.closky.rpgmod.registry.ModSounds;
import com.closky.rpgmod.skill.SkillNode;
import com.closky.rpgmod.skill.SkillTree;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client → server: unlock a skill node. */
public class UnlockSkillPacket {
    private final ResourceLocation nodeId;

    public UnlockSkillPacket(ResourceLocation nodeId) {
        this.nodeId = nodeId;
    }

    public static void encode(UnlockSkillPacket msg, FriendlyByteBuf buf) {
        buf.writeResourceLocation(msg.nodeId);
    }

    public static UnlockSkillPacket decode(FriendlyByteBuf buf) {
        return new UnlockSkillPacket(buf.readResourceLocation());
    }

    public static void handle(UnlockSkillPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null || !RpgConfig.COMMON.enableSkillTree.get()) {
                return;
            }
            SkillNode node = SkillTree.server().getNode(msg.nodeId);
            PlayerRpgData data = PlayerRpgData.get(player);
            if (node == null || data == null) {
                return;
            }
            if (data.unlockNode(player, node)) {
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.SKILL_UNLOCK.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
                data.sync(player);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
