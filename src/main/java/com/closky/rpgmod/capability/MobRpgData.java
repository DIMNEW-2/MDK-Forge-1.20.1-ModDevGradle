package com.closky.rpgmod.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nullable;

/**
 * Per-mob RPG state (section 8): level, whether it has been initialised, boss flag and optional display title.
 * The level is also mirrored into the entity's persistent data so the client nameplate can read it after sync.
 */
public class MobRpgData {
    public static final String NBT_LEVEL = "rpgmod:level";
    public static final String NBT_BOSS = "rpgmod:boss";
    public static final String NBT_TITLE = "rpgmod:title";

    private int level;
    private boolean initialized;
    private boolean boss;
    private String title = "";

    @Nullable
    public static MobRpgData get(LivingEntity entity) {
        return entity.getCapability(MobRpgProvider.CAPABILITY).orElse(null);
    }

    public static LazyOptional<MobRpgData> getLazy(LivingEntity entity) {
        return entity.getCapability(MobRpgProvider.CAPABILITY);
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = Math.max(1, level);
        this.initialized = true;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public boolean isBoss() {
        return boss;
    }

    public void setBoss(boolean boss) {
        this.boss = boss;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title == null ? "" : title;
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Level", level);
        tag.putBoolean("Init", initialized);
        tag.putBoolean("Boss", boss);
        tag.putString("Title", title);
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        level = tag.getInt("Level");
        initialized = tag.getBoolean("Init");
        boss = tag.getBoolean("Boss");
        title = tag.getString("Title");
    }
}
