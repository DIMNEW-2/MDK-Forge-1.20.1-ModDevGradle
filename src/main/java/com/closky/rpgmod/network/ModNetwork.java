package com.closky.rpgmod.network;

import com.closky.rpgmod.RpgMod;
import com.closky.rpgmod.network.packet.AllocateStatPacket;
import com.closky.rpgmod.network.packet.CasinoClaimPacket;
import com.closky.rpgmod.network.packet.CasinoOpenPacket;
import com.closky.rpgmod.network.packet.CasinoResultPacket;
import com.closky.rpgmod.network.packet.CastSpellPacket;
import com.closky.rpgmod.network.packet.OpenClientScreenPacket;
import com.closky.rpgmod.network.packet.RespecPacket;
import com.closky.rpgmod.network.packet.SelectSpellPacket;
import com.closky.rpgmod.network.packet.SyncMobLevelPacket;
import com.closky.rpgmod.network.packet.SyncPlayerRpgPacket;
import com.closky.rpgmod.network.packet.SyncSettlementsPacket;
import com.closky.rpgmod.network.packet.SyncSkillTreePacket;
import com.closky.rpgmod.network.packet.UnlockSkillPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

/** SimpleChannel setup (section 15). */
public final class ModNetwork {
    private static final String PROTOCOL_VERSION = "1";
    public static final ResourceLocation CHANNEL_ID = RpgMod.id("main");
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(CHANNEL_ID,
            () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);

    private static int nextId;

    private ModNetwork() {
    }

    public static void register() {
        // server -> client
        CHANNEL.registerMessage(nextId++, SyncPlayerRpgPacket.class, SyncPlayerRpgPacket::encode, SyncPlayerRpgPacket::decode,
                SyncPlayerRpgPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(nextId++, SyncMobLevelPacket.class, SyncMobLevelPacket::encode, SyncMobLevelPacket::decode,
                SyncMobLevelPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(nextId++, CasinoResultPacket.class, CasinoResultPacket::encode, CasinoResultPacket::decode,
                CasinoResultPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(nextId++, SyncSkillTreePacket.class, SyncSkillTreePacket::encode, SyncSkillTreePacket::decode,
                SyncSkillTreePacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(nextId++, SyncSettlementsPacket.class, SyncSettlementsPacket::encode, SyncSettlementsPacket::decode,
                SyncSettlementsPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(nextId++, OpenClientScreenPacket.class, OpenClientScreenPacket::encode, OpenClientScreenPacket::decode,
                OpenClientScreenPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        // client -> server
        CHANNEL.registerMessage(nextId++, CasinoOpenPacket.class, CasinoOpenPacket::encode, CasinoOpenPacket::decode,
                CasinoOpenPacket::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(nextId++, CasinoClaimPacket.class, CasinoClaimPacket::encode, CasinoClaimPacket::decode,
                CasinoClaimPacket::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(nextId++, AllocateStatPacket.class, AllocateStatPacket::encode, AllocateStatPacket::decode,
                AllocateStatPacket::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(nextId++, UnlockSkillPacket.class, UnlockSkillPacket::encode, UnlockSkillPacket::decode,
                UnlockSkillPacket::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(nextId++, RespecPacket.class, RespecPacket::encode, RespecPacket::decode,
                RespecPacket::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(nextId++, CastSpellPacket.class, CastSpellPacket::encode, CastSpellPacket::decode,
                CastSpellPacket::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(nextId++, SelectSpellPacket.class, SelectSpellPacket::encode, SelectSpellPacket::decode,
                SelectSpellPacket::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }

    public static void sendToPlayer(ServerPlayer player, Object packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToServer(Object packet) {
        CHANNEL.sendToServer(packet);
    }

    public static void sendToAll(Object packet) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), packet);
    }

    public static void sendToTracking(net.minecraft.world.entity.Entity entity, Object packet) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), packet);
    }
}
