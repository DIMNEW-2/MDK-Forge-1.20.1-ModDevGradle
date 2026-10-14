package com.closky.rpgmod.settlement;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/** Persistent data of one settlement (section 17.2). */
public class Settlement {
    private static final String[] SYLLABLES = {"ka", "ri", "to", "ven", "mar", "el", "dor", "an", "is", "ul", "bre", "nor", "fal", "mi", "sa", "ter", "ox", "lun", "gar", "wyn"};

    private final UUID id;
    private String name;
    private VillageClass villageClass;
    private BlockPos center;
    private int population;
    private int prosperity;
    private int military;
    private int developmentTier;
    private final Map<UUID, Integer> relations = new HashMap<>();
    private final Set<UUID> atWarWith = new HashSet<>();
    private final Set<UUID> allies = new HashSet<>();
    private long lastSeenTick;

    public Settlement(UUID id, String name, VillageClass villageClass, BlockPos center) {
        this.id = id;
        this.name = name;
        this.villageClass = villageClass;
        this.center = center;
        this.population = 8;
        this.prosperity = 40;
        this.military = 20;
        this.developmentTier = 1;
    }

    public static Settlement create(BlockPos center, long seed) {
        Random random = new Random(seed ^ center.asLong());
        UUID id = new UUID(random.nextLong(), random.nextLong());
        return new Settlement(id, generateName(random), VillageClass.byOrdinal(random.nextInt(VillageClass.values().length)), center);
    }

    public static String generateName(Random random) {
        int parts = 2 + random.nextInt(2);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts; i++) {
            sb.append(SYLLABLES[random.nextInt(SYLLABLES.length)]);
        }
        String name = sb.toString();
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    // ------------------------------------------------------------------ accessors

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public VillageClass getVillageClass() {
        return villageClass;
    }

    public void setVillageClass(VillageClass villageClass) {
        this.villageClass = villageClass;
    }

    public BlockPos getCenter() {
        return center;
    }

    public void setCenter(BlockPos center) {
        this.center = center;
    }

    public int getPopulation() {
        return population;
    }

    public void setPopulation(int population) {
        this.population = Math.max(0, population);
    }

    public int getProsperity() {
        return prosperity;
    }

    public void setProsperity(int prosperity) {
        this.prosperity = Math.max(0, Math.min(100, prosperity));
    }

    public int getMilitary() {
        return military;
    }

    public void setMilitary(int military) {
        this.military = Math.max(0, Math.min(100, military));
    }

    public int getDevelopmentTier() {
        return developmentTier;
    }

    public void setDevelopmentTier(int developmentTier) {
        this.developmentTier = Math.max(1, Math.min(5, developmentTier));
    }

    public Map<UUID, Integer> getRelations() {
        return relations;
    }

    public int getRelation(UUID other) {
        return relations.getOrDefault(other, 0);
    }

    public void setRelation(UUID other, int value) {
        relations.put(other, RelationMath.clamp(value));
    }

    public void changeRelation(UUID other, int delta) {
        setRelation(other, getRelation(other) + delta);
    }

    public RelationState getRelationState(UUID other) {
        if (atWarWith.contains(other)) {
            return RelationState.WAR;
        }
        if (allies.contains(other)) {
            return RelationState.ALLIED;
        }
        return RelationState.of(getRelation(other));
    }

    public Set<UUID> getAtWarWith() {
        return atWarWith;
    }

    public Set<UUID> getAllies() {
        return allies;
    }

    public long getLastSeenTick() {
        return lastSeenTick;
    }

    public void setLastSeenTick(long lastSeenTick) {
        this.lastSeenTick = lastSeenTick;
    }

    // ------------------------------------------------------------------ nbt

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Id", id);
        tag.putString("Name", name);
        tag.putString("Class", villageClass.getKey());
        tag.put("Center", NbtUtils.writeBlockPos(center));
        tag.putInt("Population", population);
        tag.putInt("Prosperity", prosperity);
        tag.putInt("Military", military);
        tag.putInt("Tier", developmentTier);
        tag.putLong("LastSeen", lastSeenTick);
        ListTag rel = new ListTag();
        relations.forEach((other, value) -> {
            CompoundTag e = new CompoundTag();
            e.putUUID("Id", other);
            e.putInt("Value", value);
            rel.add(e);
        });
        tag.put("Relations", rel);
        tag.put("Wars", uuidList(atWarWith));
        tag.put("Allies", uuidList(allies));
        return tag;
    }

    public static Settlement load(CompoundTag tag) {
        Settlement s = new Settlement(tag.getUUID("Id"), tag.getString("Name"), VillageClass.byKey(tag.getString("Class")), NbtUtils.readBlockPos(tag.getCompound("Center")));
        s.population = tag.getInt("Population");
        s.prosperity = tag.getInt("Prosperity");
        s.military = tag.getInt("Military");
        s.developmentTier = Math.max(1, tag.getInt("Tier"));
        s.lastSeenTick = tag.getLong("LastSeen");
        ListTag rel = tag.getList("Relations", Tag.TAG_COMPOUND);
        for (int i = 0; i < rel.size(); i++) {
            CompoundTag e = rel.getCompound(i);
            if (e.hasUUID("Id")) {
                s.relations.put(e.getUUID("Id"), e.getInt("Value"));
            }
        }
        s.atWarWith.addAll(readUuids(tag.getList("Wars", Tag.TAG_INT_ARRAY)));
        s.allies.addAll(readUuids(tag.getList("Allies", Tag.TAG_INT_ARRAY)));
        return s;
    }

    private static ListTag uuidList(Set<UUID> ids) {
        ListTag list = new ListTag();
        for (UUID id : ids) {
            list.add(NbtUtils.createUUID(id));
        }
        return list;
    }

    private static Set<UUID> readUuids(ListTag list) {
        Set<UUID> set = new HashSet<>();
        for (Tag t : list) {
            set.add(NbtUtils.loadUUID(t));
        }
        return set;
    }
}
