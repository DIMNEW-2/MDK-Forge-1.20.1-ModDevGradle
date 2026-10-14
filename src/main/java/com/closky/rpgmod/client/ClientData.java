package com.closky.rpgmod.client;

import com.closky.rpgmod.capability.PlayerRpgData;
import com.closky.rpgmod.settlement.SettlementSnapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Client-side cache of synced server data (player RPG state, mob levels, settlements). */
public final class ClientData {
    public record MobInfo(int level, boolean boss, String title) {
    }

    private static final Map<Integer, MobInfo> MOB_LEVELS = new HashMap<>();
    private static List<SettlementSnapshot> settlements = new ArrayList<>();
    private static long settlementsTimestamp;

    private ClientData() {
    }

    /** The local player's capability instance (kept in sync by {@code SyncPlayerRpgPacket}). */
    @Nullable
    public static PlayerRpgData playerData() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player == null ? null : PlayerRpgData.get(mc.player);
    }

    public static void applyPlayerSync(CompoundTag tag) {
        PlayerRpgData data = playerData();
        if (data != null) {
            data.deserializeNBT(tag);
            data.clearDirty();
            if (Minecraft.getInstance().player != null) {
                data.applyAttributeModifiers(Minecraft.getInstance().player);
            }
        }
    }

    public static void setMobInfo(int entityId, MobInfo info) {
        MOB_LEVELS.put(entityId, info);
    }

    @Nullable
    public static MobInfo getMobInfo(int entityId) {
        return MOB_LEVELS.get(entityId);
    }

    public static void clearMobs() {
        MOB_LEVELS.clear();
    }

    public static List<SettlementSnapshot> getSettlements() {
        return settlements;
    }

    public static void setSettlements(List<SettlementSnapshot> list) {
        settlements = list;
        settlementsTimestamp = System.currentTimeMillis();
    }

    public static long getSettlementsTimestamp() {
        return settlementsTimestamp;
    }
}
