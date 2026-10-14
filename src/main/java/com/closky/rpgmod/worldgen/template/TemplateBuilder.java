package com.closky.rpgmod.worldgen.template;

import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds vanilla structure-template NBT ({@code size / palette / blocks / entities}) from a simple 3D block map.
 * Block states are given as strings ({@code minecraft:stone_bricks} or {@code minecraft:jigsaw[orientation=north_up]}).
 * Unset positions are left out of the template (treated as structure void → terrain stays).
 */
public class TemplateBuilder {
    public static final String AIR = "minecraft:air";

    private final int sizeX;
    private final int sizeY;
    private final int sizeZ;
    private final Map<Long, Integer> blocks = new LinkedHashMap<>();
    private final Map<Long, CompoundTag> blockNbt = new HashMap<>();
    private final List<String> palette = new ArrayList<>();
    private final Map<String, Integer> paletteIndex = new HashMap<>();

    public TemplateBuilder(int sizeX, int sizeY, int sizeZ) {
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
    }

    public int sizeX() {
        return sizeX;
    }

    public int sizeY() {
        return sizeY;
    }

    public int sizeZ() {
        return sizeZ;
    }

    public boolean inside(int x, int y, int z) {
        return x >= 0 && y >= 0 && z >= 0 && x < sizeX && y < sizeY && z < sizeZ;
    }

    private static long key(int x, int y, int z) {
        return ((long) x & 0xFFFFF) << 40 | ((long) y & 0xFFFFF) << 20 | ((long) z & 0xFFFFF);
    }

    public void set(int x, int y, int z, String state) {
        setNbt(x, y, z, state, null);
    }

    public void setNbt(int x, int y, int z, String state, @Nullable CompoundTag nbt) {
        if (!inside(x, y, z)) {
            return;
        }
        Integer index = paletteIndex.get(state);
        if (index == null) {
            index = palette.size();
            palette.add(state);
            paletteIndex.put(state, index);
        }
        long k = key(x, y, z);
        blocks.put(k, index);
        if (nbt != null) {
            blockNbt.put(k, nbt);
        } else {
            blockNbt.remove(k);
        }
    }

    @Nullable
    public String get(int x, int y, int z) {
        Integer index = blocks.get(key(x, y, z));
        return index == null ? null : palette.get(index);
    }

    public boolean isAir(int x, int y, int z) {
        return AIR.equals(get(x, y, z));
    }

    public void fill(int x1, int y1, int z1, int x2, int y2, int z2, String state) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++) {
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
                    set(x, y, z, state);
                }
            }
        }
    }

    public CompoundTag build() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("DataVersion", SharedConstants.getCurrentVersion().getDataVersion().getVersion());
        tag.put("size", intList(sizeX, sizeY, sizeZ));
        ListTag paletteTag = new ListTag();
        for (String state : palette) {
            paletteTag.add(parseState(state));
        }
        tag.put("palette", paletteTag);
        ListTag blocksTag = new ListTag();
        for (Map.Entry<Long, Integer> entry : blocks.entrySet()) {
            long k = entry.getKey();
            int x = (int) ((k >> 40) & 0xFFFFF);
            int y = (int) ((k >> 20) & 0xFFFFF);
            int z = (int) (k & 0xFFFFF);
            CompoundTag block = new CompoundTag();
            block.put("pos", intList(x, y, z));
            block.putInt("state", entry.getValue());
            CompoundTag nbt = blockNbt.get(k);
            if (nbt != null) {
                block.put("nbt", nbt);
            }
            blocksTag.add(block);
        }
        tag.put("blocks", blocksTag);
        tag.put("entities", new ListTag());
        return tag;
    }

    private static ListTag intList(int... values) {
        ListTag list = new ListTag();
        for (int v : values) {
            list.add(IntTag.valueOf(v));
        }
        return list;
    }

    /** {@code ns:name[a=b,c=d]} → {Name, Properties}. */
    public static CompoundTag parseState(String state) {
        CompoundTag tag = new CompoundTag();
        int bracket = state.indexOf('[');
        if (bracket < 0) {
            tag.putString("Name", state);
            return tag;
        }
        tag.putString("Name", state.substring(0, bracket));
        CompoundTag props = new CompoundTag();
        String inner = state.substring(bracket + 1, state.length() - 1);
        for (String pair : inner.split(",")) {
            int eq = pair.indexOf('=');
            if (eq > 0) {
                props.putString(pair.substring(0, eq).trim(), pair.substring(eq + 1).trim());
            }
        }
        tag.put("Properties", props);
        return tag;
    }

    // ------------------------------------------------------------------ block entity helpers

    public static CompoundTag jigsawNbt(String name, String target, String pool, String finalState, String joint) {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", "minecraft:jigsaw");
        tag.putString("name", name);
        tag.putString("target", target);
        tag.putString("pool", pool);
        tag.putString("final_state", finalState);
        tag.putString("joint", joint);
        return tag;
    }

    public static CompoundTag lootChestNbt(String lootTable) {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", "rpgmod:loot_chest");
        tag.putString("LootTable", lootTable);
        tag.putLong("LootTableSeed", 0L);
        return tag;
    }

    public static CompoundTag bossSpawnerNbt(String bossId) {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", "rpgmod:boss_spawner");
        tag.putString("Boss", bossId);
        return tag;
    }

    public static CompoundTag mobSpawnerNbt(String entityId) {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", "minecraft:mob_spawner");
        CompoundTag entity = new CompoundTag();
        entity.putString("id", entityId);
        CompoundTag spawnData = new CompoundTag();
        spawnData.put("entity", entity);
        tag.put("SpawnData", spawnData);
        ListTag potentials = new ListTag();
        CompoundTag potential = new CompoundTag();
        potential.putInt("weight", 1);
        potential.put("data", spawnData.copy());
        potentials.add(potential);
        tag.put("SpawnPotentials", potentials);
        tag.putShort("Delay", (short) 20);
        tag.putShort("MinSpawnDelay", (short) 200);
        tag.putShort("MaxSpawnDelay", (short) 800);
        tag.putShort("SpawnCount", (short) 3);
        tag.putShort("MaxNearbyEntities", (short) 6);
        tag.putShort("RequiredPlayerRange", (short) 16);
        tag.putShort("SpawnRange", (short) 4);
        return tag;
    }
}
