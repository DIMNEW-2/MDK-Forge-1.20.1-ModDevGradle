package com.closky.rpgmod.settlement;

import com.closky.rpgmod.RpgMod;
import com.closky.rpgmod.capability.PlayerRpgData;
import com.closky.rpgmod.config.RpgConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * World-level registry and simulation of settlements (sections 17.4–17.6). Villages are discovered when players
 * walk into them; relations, prosperity and wars are simulated abstractly every {@code settlementTickInterval}
 * ticks. Wars become physical (raiding parties) only near players.
 */
public class SettlementManager extends SavedData {
    private static final String DATA_NAME = RpgMod.MODID + "_settlements";
    private static final int DISCOVERY_MERGE_DISTANCE = 96;

    private final Map<UUID, Settlement> settlements = new LinkedHashMap<>();
    private final List<WarState> wars = new ArrayList<>();
    private final Random random = new Random();
    private int tickCounter;

    public static SettlementManager get(ServerLevel level) {
        ServerLevel overworld = level.getServer().overworld();
        return overworld.getDataStorage().computeIfAbsent(SettlementManager::load, SettlementManager::new, DATA_NAME);
    }

    // ------------------------------------------------------------------ discovery

    /** Registers the village at the player's position (if any). Returns the settlement or null. */
    @Nullable
    public Settlement discover(ServerLevel level, ServerPlayer player) {
        StructureStart start = level.structureManager().getStructureWithPieceAt(player.blockPosition(), StructureTags.VILLAGE);
        if (!start.isValid()) {
            return null;
        }
        BoundingBox box = start.getBoundingBox();
        BlockPos center = new BlockPos((box.minX() + box.maxX()) / 2, (box.minY() + box.maxY()) / 2, (box.minZ() + box.maxZ()) / 2);
        Settlement existing = findNearest(center, DISCOVERY_MERGE_DISTANCE);
        if (existing != null) {
            existing.setLastSeenTick(level.getGameTime());
            return existing;
        }
        Settlement settlement = Settlement.create(center, level.getSeed());
        settlement.setLastSeenTick(level.getGameTime());
        // Initial relations with known neighbours: closer villages like each other a bit more
        for (Settlement other : settlements.values()) {
            double dist = Math.sqrt(other.getCenter().distSqr(center));
            int base = dist < 400 ? 20 : dist < 1000 ? 5 : -5;
            if (other.getVillageClass() == settlement.getVillageClass()) {
                base += 10;
            } else if (other.getVillageClass() == VillageClass.MILITARY || settlement.getVillageClass() == VillageClass.MILITARY) {
                base -= 10;
            }
            base += random.nextInt(21) - 10;
            settlement.setRelation(other.getId(), base);
            other.setRelation(settlement.getId(), base);
        }
        settlements.put(settlement.getId(), settlement);
        setDirty();
        player.displayClientMessage(Component.translatable("message.rpgmod.settlement_discovered", settlement.getName(), settlement.getVillageClass().getDisplayName()).withStyle(ChatFormatting.AQUA), false);
        PlayerRpgData data = PlayerRpgData.get(player);
        if (data != null) {
            data.addXp(player, 4);
            data.sync(player);
        }
        return settlement;
    }

    @Nullable
    public Settlement findNearest(BlockPos pos, double maxDistance) {
        Settlement best = null;
        double bestDist = maxDistance * maxDistance;
        for (Settlement s : settlements.values()) {
            double d = s.getCenter().distSqr(pos);
            if (d <= bestDist) {
                bestDist = d;
                best = s;
            }
        }
        return best;
    }

    @Nullable
    public Settlement get(UUID id) {
        return settlements.get(id);
    }

    public Map<UUID, Settlement> getSettlements() {
        return settlements;
    }

    public List<WarState> getWars() {
        return wars;
    }

    @Nullable
    public WarState getWar(UUID a, UUID b) {
        for (WarState war : wars) {
            if (war.involves(a) && war.involves(b)) {
                return war;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ events

    public void onPlayerTrade(ServerPlayer player, Villager villager) {
        Settlement settlement = findNearest(villager.blockPosition(), 128);
        if (settlement == null) {
            return;
        }
        changeReputation(player, settlement, 1);
        settlement.setProsperity(settlement.getProsperity() + 1);
        setDirty();
    }

    public void onVillagerKilled(@Nullable Player killer, Villager villager) {
        Settlement settlement = findNearest(villager.blockPosition(), 128);
        if (settlement == null) {
            return;
        }
        settlement.setPopulation(settlement.getPopulation() - 1);
        if (killer instanceof ServerPlayer player) {
            changeReputation(player, settlement, -10);
        }
        setDirty();
    }

    public void onRaiderKilled(@Nullable Player killer, Mob raider) {
        Settlement settlement = findNearest(raider.blockPosition(), 96);
        if (settlement == null || !(killer instanceof ServerPlayer player)) {
            return;
        }
        changeReputation(player, settlement, 2);
        setDirty();
    }

    public void changeReputation(ServerPlayer player, Settlement settlement, int delta) {
        PlayerRpgData data = PlayerRpgData.get(player);
        if (data == null) {
            return;
        }
        int before = data.getReputation(settlement.getId());
        if (delta > 0 && data.hasSkillFlag("diplomat")) {
            delta *= 2; // social skill: reputation gains doubled
        } else if (delta < 0 && data.hasSkillFlag("diplomat")) {
            delta = Math.min(-1, delta / 2);
        }
        data.changeReputation(settlement.getId(), delta);
        int after = data.getReputation(settlement.getId());
        if (before / 10 != after / 10) {
            player.displayClientMessage(Component.translatable("message.rpgmod.reputation_changed", settlement.getName(), after)
                    .withStyle(delta > 0 ? ChatFormatting.GREEN : ChatFormatting.RED), true);
        }
        data.markDirty();
    }

    /** Admin / debug: forces a relation value and updates war state accordingly. */
    public void setRelation(UUID a, UUID b, int value) {
        Settlement sa = settlements.get(a);
        Settlement sb = settlements.get(b);
        if (sa == null || sb == null) {
            return;
        }
        sa.setRelation(b, value);
        sb.setRelation(a, value);
        setDirty();
    }

    // ------------------------------------------------------------------ simulation

    public void tick(ServerLevel level) {
        if (!RpgConfig.COMMON.enableSettlementSimulation.get()) {
            return;
        }
        int interval = Math.max(20, RpgConfig.COMMON.settlementTickInterval.get());
        if (++tickCounter < interval) {
            return;
        }
        tickCounter = 0;
        if (settlements.isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        int decay = RpgConfig.COMMON.relationDecayRate.get();
        List<Settlement> list = new ArrayList<>(settlements.values());
        for (Settlement s : list) {
            simulateEconomy(s);
            for (Map.Entry<UUID, Integer> e : s.getRelations().entrySet()) {
                e.setValue(RelationMath.decay(e.getValue(), decay));
            }
        }
        // random diplomatic events
        if (list.size() >= 2 && random.nextFloat() < 0.5F) {
            Settlement a = list.get(random.nextInt(list.size()));
            Settlement b = list.get(random.nextInt(list.size()));
            if (a != b) {
                diplomaticEvent(level, a, b);
            }
        }
        // alliances
        for (Settlement a : list) {
            for (Settlement b : list) {
                if (a == b) {
                    continue;
                }
                int rel = a.getRelation(b.getId());
                if (rel >= RelationState.ALLIED.getMin() && !a.getAllies().contains(b.getId())) {
                    a.getAllies().add(b.getId());
                    b.getAllies().add(a.getId());
                    broadcast(level, Component.translatable("message.rpgmod.alliance_formed", a.getName(), b.getName()).withStyle(ChatFormatting.GREEN));
                } else if (rel < RelationState.FRIENDLY.getMin() && a.getAllies().contains(b.getId())) {
                    a.getAllies().remove(b.getId());
                    b.getAllies().remove(a.getId());
                }
            }
        }
        // wars
        if (RpgConfig.COMMON.enableVillageWars.get()) {
            simulateWars(level, now);
            for (Settlement a : list) {
                for (Settlement b : list) {
                    if (a == b || getWar(a.getId(), b.getId()) != null) {
                        continue;
                    }
                    int rel = a.getRelation(b.getId());
                    if (RelationMath.shouldDeclareWar(rel, a.getMilitary(), RpgConfig.COMMON.villageWarFrequency.get(), random.nextDouble())) {
                        declareWar(level, a, b, now);
                    }
                }
            }
        }
        setDirty();
    }

    private void simulateEconomy(Settlement s) {
        VillageClass cls = s.getVillageClass();
        int prosperityDelta = random.nextInt(3) - 1 + (random.nextFloat() < cls.getProsperityRate() * 0.5F ? 1 : 0);
        if (!s.getAtWarWith().isEmpty()) {
            prosperityDelta -= 2;
        }
        s.setProsperity(s.getProsperity() + prosperityDelta);
        int militaryDelta = (random.nextFloat() < cls.getMilitaryRate() * 0.4F ? 1 : 0) + (s.getProsperity() > 60 ? 1 : 0) - (s.getProsperity() < 20 ? 1 : 0);
        s.setMilitary(s.getMilitary() + militaryDelta);
        if (s.getProsperity() > 70 && random.nextFloat() < 0.2F) {
            s.setPopulation(s.getPopulation() + 1);
        } else if (s.getProsperity() < 15 && random.nextFloat() < 0.2F) {
            s.setPopulation(s.getPopulation() - 1);
        }
        int tier = 1 + s.getProsperity() / 25 + (s.getPopulation() > 20 ? 1 : 0);
        s.setDevelopmentTier(tier);
    }

    private void diplomaticEvent(ServerLevel level, Settlement a, Settlement b) {
        float roll = random.nextFloat();
        int delta;
        String key;
        if (roll < 0.35F) {
            delta = 3 + random.nextInt(6);
            key = "trade_agreement";
            a.setProsperity(a.getProsperity() + 2);
            b.setProsperity(b.getProsperity() + 2);
        } else if (roll < 0.6F) {
            delta = -(3 + random.nextInt(8));
            key = "border_dispute";
        } else if (roll < 0.75F) {
            delta = 5 + random.nextInt(5);
            key = "festival";
        } else if (roll < 0.9F) {
            delta = -(5 + random.nextInt(10));
            key = "raid_blamed";
        } else {
            delta = 0;
            key = null;
        }
        if (key == null) {
            return;
        }
        a.changeRelation(b.getId(), delta);
        b.changeRelation(a.getId(), delta);
        for (UUID allyId : a.getAllies()) {
            Settlement ally = settlements.get(allyId);
            if (ally != null && ally != b) {
                ally.changeRelation(b.getId(), delta / 2);
                b.changeRelation(allyId, delta / 2);
            }
        }
        broadcastNear(level, a.getCenter(), Component.translatable("message.rpgmod.diplomacy." + key, a.getName(), b.getName())
                .withStyle(delta >= 0 ? ChatFormatting.AQUA : ChatFormatting.YELLOW));
    }

    private void declareWar(ServerLevel level, Settlement attacker, Settlement defender, long now) {
        wars.add(new WarState(attacker.getId(), defender.getId(), now));
        attacker.getAtWarWith().add(defender.getId());
        defender.getAtWarWith().add(attacker.getId());
        for (UUID allyId : defender.getAllies()) {
            Settlement ally = settlements.get(allyId);
            if (ally != null) {
                ally.changeRelation(attacker.getId(), RelationMath.allyOutrage());
                attacker.changeRelation(allyId, RelationMath.allyOutrage());
            }
        }
        broadcast(level, Component.translatable("message.rpgmod.war_declared", attacker.getName(), defender.getName()).withStyle(ChatFormatting.DARK_RED));
    }

    private void simulateWars(ServerLevel level, long now) {
        Iterator<WarState> it = wars.iterator();
        while (it.hasNext()) {
            WarState war = it.next();
            Settlement attacker = settlements.get(war.getAttacker());
            Settlement defender = settlements.get(war.getDefender());
            if (attacker == null || defender == null) {
                it.remove();
                continue;
            }
            int[] losses = RelationMath.resolveBattle(attacker.getMilitary(), defender.getMilitary(), random);
            attacker.setMilitary(attacker.getMilitary() - losses[0]);
            defender.setMilitary(defender.getMilitary() - losses[1]);
            String entry = (now / 24000L) + ":" + losses[0] + ":" + losses[1];
            war.recordRound(losses[0], losses[1], entry);
            // physical raid near players
            spawnRaidIfWatched(level, attacker, defender);
            if (RelationMath.shouldMakePeace(attacker.getRelation(defender.getId()), attacker.getMilitary(), defender.getMilitary(), war.getRounds())) {
                boolean attackerWon = defender.getMilitary() < attacker.getMilitary();
                Settlement winner = attackerWon ? attacker : defender;
                Settlement loser = attackerWon ? defender : attacker;
                loser.setProsperity(loser.getProsperity() - 15);
                winner.setProsperity(winner.getProsperity() + 5);
                attacker.getAtWarWith().remove(defender.getId());
                defender.getAtWarWith().remove(attacker.getId());
                attacker.setRelation(defender.getId(), -30);
                defender.setRelation(attacker.getId(), -30);
                it.remove();
                broadcast(level, Component.translatable("message.rpgmod.war_ended", winner.getName(), loser.getName()).withStyle(ChatFormatting.GOLD));
            }
        }
    }

    /** If a player is near the defender, a small raiding party appears so the war is visible in the world. */
    private void spawnRaidIfWatched(ServerLevel level, Settlement attacker, Settlement defender) {
        BlockPos center = defender.getCenter();
        ServerPlayer watcher = level.getNearestPlayer(center.getX(), center.getY(), center.getZ(), 96.0D, false) instanceof ServerPlayer sp ? sp : null;
        if (watcher == null || !level.isLoaded(center)) {
            return;
        }
        int count = 2 + random.nextInt(3);
        double angle = random.nextDouble() * Math.PI * 2;
        for (int i = 0; i < count; i++) {
            double distance = 20 + random.nextInt(12);
            int x = center.getX() + (int) (Math.cos(angle) * distance) + random.nextInt(5) - 2;
            int z = center.getZ() + (int) (Math.sin(angle) * distance) + random.nextInt(5) - 2;
            BlockPos pos = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
            Mob raider = attacker.getVillageClass() == VillageClass.MILITARY || random.nextBoolean()
                    ? EntityType.VINDICATOR.create(level) : EntityType.PILLAGER.create(level);
            if (raider == null) {
                continue;
            }
            raider.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, random.nextFloat() * 360.0F, 0.0F);
            raider.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null, null);
            raider.setCustomName(Component.translatable("entity.rpgmod.raider", attacker.getName()));
            raider.getPersistentData().putUUID("rpgmod:raid_from", attacker.getId());
            raider.getPersistentData().putUUID("rpgmod:raid_target", defender.getId());
            if (raider instanceof Pillager || raider instanceof Vindicator) {
                raider.setPersistenceRequired();
            }
            level.addFreshEntity(raider);
        }
        watcher.displayClientMessage(Component.translatable("message.rpgmod.raid_incoming", attacker.getName(), defender.getName()).withStyle(ChatFormatting.RED), false);
    }

    private void broadcast(ServerLevel level, Component message) {
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            player.displayClientMessage(message, false);
        }
    }

    private void broadcastNear(ServerLevel level, BlockPos pos, Component message) {
        for (ServerPlayer player : level.getPlayers(p -> p.distanceToSqr(Vec3.atCenterOf(pos)) < 256 * 256)) {
            player.displayClientMessage(message, false);
        }
    }

    // ------------------------------------------------------------------ snapshot

    public List<SettlementSnapshot> snapshot(@Nullable ServerPlayer player) {
        PlayerRpgData data = player == null ? null : PlayerRpgData.get(player);
        List<SettlementSnapshot> list = new ArrayList<>();
        for (Settlement s : settlements.values()) {
            int reputation = data == null ? 0 : data.getReputation(s.getId());
            list.add(new SettlementSnapshot(s.getId(), s.getName(), s.getVillageClass(), s.getCenter(), s.getPopulation(),
                    s.getProsperity(), s.getMilitary(), s.getDevelopmentTier(), new HashMap<>(s.getRelations()),
                    new ArrayList<>(s.getAtWarWith()), new ArrayList<>(s.getAllies()), reputation));
        }
        return list;
    }

    /** Whether villagers / golems of the settlement near {@code pos} should be hostile to the player. */
    public boolean isHostileTo(ServerPlayer player, BlockPos pos) {
        Settlement settlement = findNearest(pos, 128);
        if (settlement == null) {
            return false;
        }
        PlayerRpgData data = PlayerRpgData.get(player);
        return data != null && data.getReputation(settlement.getId()) <= -50;
    }

    public AABB areaOf(Settlement settlement) {
        return new AABB(settlement.getCenter()).inflate(64.0D, 32.0D, 64.0D);
    }

    // ------------------------------------------------------------------ nbt

    public static SettlementManager load(CompoundTag tag) {
        SettlementManager manager = new SettlementManager();
        ListTag list = tag.getList("Settlements", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            Settlement s = Settlement.load(list.getCompound(i));
            manager.settlements.put(s.getId(), s);
        }
        ListTag warList = tag.getList("Wars", Tag.TAG_COMPOUND);
        for (int i = 0; i < warList.size(); i++) {
            manager.wars.add(WarState.load(warList.getCompound(i)));
        }
        return manager;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        settlements.values().forEach(s -> list.add(s.save()));
        tag.put("Settlements", list);
        ListTag warList = new ListTag();
        wars.forEach(w -> warList.add(w.save()));
        tag.put("Wars", warList);
        return tag;
    }
}
