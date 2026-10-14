package com.closky.rpgmod.settlement;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Immutable, network-friendly view of a settlement for the relations map. */
public record SettlementSnapshot(UUID id, String name, VillageClass villageClass, BlockPos center, int population,
                                 int prosperity, int military, int developmentTier, Map<UUID, Integer> relations,
                                 List<UUID> wars, List<UUID> allies, int playerReputation) {

    public RelationState relationState(UUID other) {
        if (wars.contains(other)) {
            return RelationState.WAR;
        }
        if (allies.contains(other)) {
            return RelationState.ALLIED;
        }
        return RelationState.of(relations.getOrDefault(other, 0));
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUUID(id);
        buf.writeUtf(name, 64);
        buf.writeEnum(villageClass);
        buf.writeBlockPos(center);
        buf.writeVarInt(population);
        buf.writeVarInt(prosperity);
        buf.writeVarInt(military);
        buf.writeVarInt(developmentTier);
        buf.writeMap(relations, FriendlyByteBuf::writeUUID, FriendlyByteBuf::writeVarInt);
        buf.writeCollection(wars, FriendlyByteBuf::writeUUID);
        buf.writeCollection(allies, FriendlyByteBuf::writeUUID);
        buf.writeVarInt(playerReputation);
    }

    public static SettlementSnapshot read(FriendlyByteBuf buf) {
        UUID id = buf.readUUID();
        String name = buf.readUtf(64);
        VillageClass villageClass = buf.readEnum(VillageClass.class);
        BlockPos center = buf.readBlockPos();
        int population = buf.readVarInt();
        int prosperity = buf.readVarInt();
        int military = buf.readVarInt();
        int tier = buf.readVarInt();
        Map<UUID, Integer> relations = buf.readMap(FriendlyByteBuf::readUUID, FriendlyByteBuf::readVarInt);
        List<UUID> wars = buf.readList(FriendlyByteBuf::readUUID);
        List<UUID> allies = buf.readList(FriendlyByteBuf::readUUID);
        int reputation = buf.readVarInt();
        return new SettlementSnapshot(id, name, villageClass, center, population, prosperity, military, tier, relations, new ArrayList<>(wars), new ArrayList<>(allies), reputation);
    }
}
