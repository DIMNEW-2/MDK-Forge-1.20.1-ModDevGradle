package com.closky.rpgmod.settlement;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** An ongoing war between two settlements (section 17.5). */
public class WarState {
    private final UUID attacker;
    private final UUID defender;
    private final long startTick;
    private int rounds;
    private int attackerLosses;
    private int defenderLosses;
    private final List<String> log = new ArrayList<>();

    public WarState(UUID attacker, UUID defender, long startTick) {
        this.attacker = attacker;
        this.defender = defender;
        this.startTick = startTick;
    }

    public UUID getAttacker() {
        return attacker;
    }

    public UUID getDefender() {
        return defender;
    }

    public long getStartTick() {
        return startTick;
    }

    public int getRounds() {
        return rounds;
    }

    public int getAttackerLosses() {
        return attackerLosses;
    }

    public int getDefenderLosses() {
        return defenderLosses;
    }

    public List<String> getLog() {
        return log;
    }

    public boolean involves(UUID id) {
        return attacker.equals(id) || defender.equals(id);
    }

    public void recordRound(int attackerLoss, int defenderLoss, String entry) {
        rounds++;
        attackerLosses += attackerLoss;
        defenderLosses += defenderLoss;
        log.add(entry);
        while (log.size() > 10) {
            log.remove(0);
        }
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Attacker", attacker);
        tag.putUUID("Defender", defender);
        tag.putLong("Start", startTick);
        tag.putInt("Rounds", rounds);
        tag.putInt("AttackerLosses", attackerLosses);
        tag.putInt("DefenderLosses", defenderLosses);
        ListTag list = new ListTag();
        log.forEach(s -> list.add(StringTag.valueOf(s)));
        tag.put("Log", list);
        return tag;
    }

    public static WarState load(CompoundTag tag) {
        WarState war = new WarState(tag.getUUID("Attacker"), tag.getUUID("Defender"), tag.getLong("Start"));
        war.rounds = tag.getInt("Rounds");
        war.attackerLosses = tag.getInt("AttackerLosses");
        war.defenderLosses = tag.getInt("DefenderLosses");
        ListTag list = tag.getList("Log", Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            war.log.add(list.getString(i));
        }
        return war;
    }
}
