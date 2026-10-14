package com.closky.rpgmod.client;

import com.closky.rpgmod.client.screen.CasinoScreen;
import com.closky.rpgmod.network.packet.CasinoResultPacket;
import com.closky.rpgmod.network.packet.OpenClientScreenPacket;
import com.closky.rpgmod.network.packet.SyncMobLevelPacket;
import com.closky.rpgmod.network.packet.SyncPlayerRpgPacket;
import com.closky.rpgmod.network.packet.SyncSettlementsPacket;
import com.closky.rpgmod.network.packet.SyncSkillTreePacket;
import com.closky.rpgmod.skill.SkillNode;
import com.closky.rpgmod.skill.SkillTree;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;

/** Client-side packet handling (runs on the render thread). */
public final class ClientPacketHandler {
    private ClientPacketHandler() {
    }

    public static void handlePlayerSync(SyncPlayerRpgPacket packet) {
        ClientData.applyPlayerSync(packet.getData());
    }

    public static void handleMobLevel(SyncMobLevelPacket packet) {
        ClientData.setMobInfo(packet.getEntityId(), new ClientData.MobInfo(packet.getLevel(), packet.isBoss(), packet.getTitle()));
    }

    public static void handleCasinoResult(CasinoResultPacket packet) {
        if (Minecraft.getInstance().screen instanceof CasinoScreen screen) {
            screen.onResult(packet);
        }
    }

    public static void handleSkillTree(SyncSkillTreePacket packet) {
        Map<ResourceLocation, SkillNode> nodes = new LinkedHashMap<>();
        for (SkillNode node : packet.getNodes()) {
            nodes.put(node.id(), node);
        }
        SkillTree.setClient(new SkillTree(nodes));
    }

    public static void handleSettlements(SyncSettlementsPacket packet) {
        ClientData.setSettlements(packet.getSettlements());
        if (packet.shouldOpenScreen()) {
            ClientHooks.openRelations();
        }
    }

    public static void handleOpenScreen(OpenClientScreenPacket packet) {
        switch (packet.getScreen()) {
            case OpenClientScreenPacket.SKILL_TREE -> ClientHooks.openSkillTree();
            case OpenClientScreenPacket.RELATIONS -> ClientHooks.openRelations();
            case OpenClientScreenPacket.CHARACTER -> ClientHooks.openCharacter();
            default -> {
            }
        }
    }
}
