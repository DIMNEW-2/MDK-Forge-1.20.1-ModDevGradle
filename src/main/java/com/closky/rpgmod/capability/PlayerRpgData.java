package com.closky.rpgmod.capability;

import com.closky.rpgmod.config.RpgConfig;
import com.closky.rpgmod.network.ModNetwork;
import com.closky.rpgmod.network.packet.SyncPlayerRpgPacket;
import com.closky.rpgmod.player.CharacterLevel;
import com.closky.rpgmod.player.PlayerClass;
import com.closky.rpgmod.player.StatType;
import com.closky.rpgmod.registry.ModAttributes;
import com.closky.rpgmod.registry.ModEffects;
import com.closky.rpgmod.registry.ModSounds;
import com.closky.rpgmod.skill.SkillEffect;
import com.closky.rpgmod.skill.SkillNode;
import com.closky.rpgmod.skill.SkillTree;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Per-player RPG state (sections 7, 12, 19.5): character level & XP, mana, stat / skill points, unlocked skill
 * nodes, active class, known spells, spell cooldowns and village reputation.
 *
 * <p>All attribute bonuses derived from this data (stats, skill nodes, class set) are recomputed by
 * {@link #applyAttributeModifiers(Player)} from a single place so they never drift.</p>
 */
public class PlayerRpgData {
    private static final UUID SKILL_MODIFIER_BASE = UUID.fromString("9e2c4b10-7a3d-4f2b-8c1e-5a6b7c8d0000");

    private int characterLevel = 1;
    private int xp;
    private int statPoints;
    private int skillPoints;
    private float mana = 20.0F;
    private final EnumMap<StatType, Integer> stats = new EnumMap<>(StatType.class);
    private final Set<ResourceLocation> unlockedNodes = new LinkedHashSet<>();
    private PlayerClass activeClass = PlayerClass.NONE;
    private final Map<UUID, Integer> villageReputation = new HashMap<>();
    private final Set<ResourceLocation> knownSpells = new LinkedHashSet<>();
    private final Map<ResourceLocation, Integer> spellCooldowns = new HashMap<>();
    private final Set<ResourceLocation> visitedBiomes = new HashSet<>();
    private long empoweredUntil;
    private boolean starterKitGiven;
    private boolean dirty = true;

    // ------------------------------------------------------------------ access

    @Nullable
    public static PlayerRpgData get(Player player) {
        return player.getCapability(PlayerRpgProvider.CAPABILITY).orElse(null);
    }

    public static LazyOptional<PlayerRpgData> getLazy(Player player) {
        return player.getCapability(PlayerRpgProvider.CAPABILITY);
    }

    // ------------------------------------------------------------------ level & xp

    public int getLevel() {
        return characterLevel;
    }

    public int getXp() {
        return xp;
    }

    public int getXpForNextLevel() {
        return CharacterLevel.xpToNext(characterLevel);
    }

    public float getXpProgress() {
        return Math.min(1.0F, xp / (float) getXpForNextLevel());
    }

    /** Adds character XP and handles level-ups (points, sound, chat message). */
    public void addXp(Player player, int amount) {
        if (amount <= 0 || characterLevel >= CharacterLevel.MAX_LEVEL) {
            return;
        }
        xp += amount;
        boolean levelled = false;
        while (xp >= getXpForNextLevel() && characterLevel < CharacterLevel.MAX_LEVEL) {
            xp -= getXpForNextLevel();
            characterLevel++;
            statPoints += CharacterLevel.statPointsForLevel(characterLevel);
            skillPoints += CharacterLevel.skillPointsForLevel(characterLevel);
            levelled = true;
        }
        if (levelled && !player.level().isClientSide) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.LEVEL_UP.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            player.displayClientMessage(Component.translatable("message.rpgmod.level_up", characterLevel).withStyle(ChatFormatting.GOLD), false);
            applyAttributeModifiers(player);
        }
        markDirty();
    }

    public void setLevel(Player player, int level) {
        this.characterLevel = Math.max(1, Math.min(CharacterLevel.MAX_LEVEL, level));
        this.xp = 0;
        applyAttributeModifiers(player);
        markDirty();
    }

    // ------------------------------------------------------------------ mana

    public float getMana() {
        return mana;
    }

    public float getMaxMana(Player player) {
        AttributeInstance instance = player.getAttribute(ModAttributes.MAX_MANA.get());
        return instance == null ? 20.0F : (float) instance.getValue();
    }

    public void setMana(float value) {
        this.mana = Math.max(0.0F, value);
        markDirty();
    }

    public boolean consumeMana(float amount) {
        if (mana < amount) {
            return false;
        }
        mana -= amount;
        markDirty();
        return true;
    }

    public void addMana(Player player, float amount) {
        float max = getMaxMana(player);
        float before = mana;
        mana = Math.min(max, mana + amount);
        if (mana != before) {
            markDirty();
        }
    }

    /** Called once per second on the server. */
    public void regenerateMana(Player player) {
        float max = getMaxMana(player);
        if (mana >= max) {
            if (mana > max) {
                mana = max;
                markDirty();
            }
            return;
        }
        AttributeInstance regen = player.getAttribute(ModAttributes.MANA_REGEN.get());
        float perSecond = regen == null ? 1.0F : (float) regen.getValue();
        perSecond += (float) (RpgConfig.COMMON.baseManaRegenPerSecond.get() - 1.0D);
        if (player.hasEffect(ModEffects.MANA_SURGE.get())) {
            perSecond += 2.0F * (player.getEffect(ModEffects.MANA_SURGE.get()).getAmplifier() + 1);
        }
        mana = Math.min(max, mana + Math.max(0.0F, perSecond));
        markDirty();
    }

    // ------------------------------------------------------------------ stats

    public int getStatPoints() {
        return statPoints;
    }

    public void grantStatPoints(int amount) {
        statPoints = Math.max(0, statPoints + amount);
        markDirty();
    }

    public int getStat(StatType type) {
        return stats.getOrDefault(type, 0);
    }

    public boolean allocateStat(Player player, StatType type) {
        if (statPoints <= 0) {
            return false;
        }
        statPoints--;
        stats.merge(type, 1, Integer::sum);
        applyAttributeModifiers(player);
        markDirty();
        return true;
    }

    // ------------------------------------------------------------------ skills

    public int getSkillPoints() {
        return skillPoints;
    }

    public void addSkillPoints(int amount) {
        skillPoints = Math.max(0, skillPoints + amount);
        markDirty();
    }

    public Set<ResourceLocation> getUnlockedNodes() {
        return unlockedNodes;
    }

    public boolean hasNode(ResourceLocation id) {
        return unlockedNodes.contains(id);
    }

    /** Whether the node can be unlocked right now (points, level, parents, class). */
    public boolean canUnlock(SkillNode node) {
        if (unlockedNodes.contains(node.id()) || skillPoints < node.cost() || characterLevel < node.requiredLevel()) {
            return false;
        }
        if (node.requiredClass() != PlayerClass.NONE && node.requiredClass() != activeClass) {
            return false;
        }
        if (node.parents().isEmpty()) {
            return true;
        }
        for (ResourceLocation parent : node.parents()) {
            if (unlockedNodes.contains(parent)) {
                return true;
            }
        }
        return false;
    }

    public boolean unlockNode(Player player, SkillNode node) {
        if (!canUnlock(node)) {
            return false;
        }
        skillPoints -= node.cost();
        unlockedNodes.add(node.id());
        for (SkillEffect effect : node.effects()) {
            effect.onUnlock(player, this);
        }
        applyAttributeModifiers(player);
        markDirty();
        return true;
    }

    /** Refunds all skill points; nothing is lost (section 7.7). */
    public int respec(Player player) {
        int refunded = 0;
        for (ResourceLocation id : unlockedNodes) {
            SkillNode node = SkillTree.get().getNode(id);
            refunded += node == null ? 1 : node.cost();
        }
        unlockedNodes.clear();
        skillPoints += refunded;
        applyAttributeModifiers(player);
        markDirty();
        return refunded;
    }

    /** Sum of a numeric skill effect across unlocked nodes (e.g. mana cost reduction). */
    public float sumSkillValue(SkillEffect.Type type) {
        float total = 0.0F;
        for (ResourceLocation id : unlockedNodes) {
            SkillNode node = SkillTree.get().getNode(id);
            if (node == null) {
                continue;
            }
            for (SkillEffect effect : node.effects()) {
                if (effect.type() == type) {
                    total += (float) effect.amount();
                }
            }
        }
        return total;
    }

    public boolean hasSkillFlag(String flag) {
        for (ResourceLocation id : unlockedNodes) {
            SkillNode node = SkillTree.get().getNode(id);
            if (node == null) {
                continue;
            }
            for (SkillEffect effect : node.effects()) {
                if (effect.type() == SkillEffect.Type.FLAG && flag.equals(effect.target())) {
                    return true;
                }
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ class

    public PlayerClass getActiveClass() {
        return activeClass;
    }

    /** Re-evaluates the class from the worn armor; returns true if it changed. */
    public boolean updateClass(Player player) {
        PlayerClass detected = PlayerClass.of(player);
        if (detected != activeClass) {
            activeClass = detected;
            applyAttributeModifiers(player);
            markDirty();
            return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ reputation

    public Map<UUID, Integer> getReputation() {
        return villageReputation;
    }

    public int getReputation(UUID settlementId) {
        return villageReputation.getOrDefault(settlementId, 0);
    }

    public void changeReputation(UUID settlementId, int delta) {
        int value = Math.max(-100, Math.min(100, getReputation(settlementId) + delta));
        villageReputation.put(settlementId, value);
        markDirty();
    }

    // ------------------------------------------------------------------ spells

    public Set<ResourceLocation> getKnownSpells() {
        return knownSpells;
    }

    public boolean knowsSpell(ResourceLocation id) {
        return knownSpells.contains(id);
    }

    public boolean learnSpell(ResourceLocation id) {
        boolean added = knownSpells.add(id);
        if (added) {
            markDirty();
        }
        return added;
    }

    public int getSpellCooldown(ResourceLocation id) {
        return spellCooldowns.getOrDefault(id, 0);
    }

    public void setSpellCooldown(ResourceLocation id, int ticks) {
        if (ticks <= 0) {
            spellCooldowns.remove(id);
        } else {
            spellCooldowns.put(id, ticks);
        }
        markDirty();
    }

    /** Ticks down every cooldown; called each tick on both sides (client predicts, server is authoritative). */
    public void tickCooldowns() {
        if (spellCooldowns.isEmpty()) {
            return;
        }
        spellCooldowns.replaceAll((id, ticks) -> ticks - 1);
        spellCooldowns.values().removeIf(ticks -> ticks <= 0);
    }

    public Map<ResourceLocation, Integer> getSpellCooldowns() {
        return spellCooldowns;
    }

    // ------------------------------------------------------------------ misc

    public long getEmpoweredUntil() {
        return empoweredUntil;
    }

    public void setEmpoweredUntil(long tick) {
        this.empoweredUntil = tick;
    }

    public boolean markBiomeVisited(ResourceLocation biome) {
        boolean added = visitedBiomes.add(biome);
        if (added) {
            markDirty();
        }
        return added;
    }

    public boolean isStarterKitGiven() {
        return starterKitGiven;
    }

    public void setStarterKitGiven(boolean given) {
        this.starterKitGiven = given;
        markDirty();
    }

    public boolean isDirty() {
        return dirty;
    }

    public void markDirty() {
        this.dirty = true;
    }

    public void clearDirty() {
        this.dirty = false;
    }

    // ------------------------------------------------------------------ attribute modifiers

    /**
     * Recomputes every attribute modifier owned by this capability: stat points, unlocked skill nodes and the active
     * class set bonus. Safe to call often; modifiers are keyed by fixed UUIDs.
     */
    public void applyAttributeModifiers(Player player) {
        // Stats
        for (StatType type : StatType.values()) {
            AttributeInstance instance = player.getAttribute(type.getAttribute());
            if (instance == null) {
                continue;
            }
            instance.removeModifier(type.getUuid());
            int points = getStat(type);
            if (points > 0) {
                instance.addPermanentModifier(new AttributeModifier(type.getUuid(), "rpgmod stat " + type.getKey(), points * type.getPerPoint(), type.getOperation()));
            }
        }
        // Skill nodes: aggregate by attribute + operation
        Map<String, double[]> skillTotals = new HashMap<>();
        Map<String, Attribute> attributes = new HashMap<>();
        Map<String, AttributeModifier.Operation> operations = new HashMap<>();
        for (ResourceLocation id : unlockedNodes) {
            SkillNode node = SkillTree.get().getNode(id);
            if (node == null) {
                continue;
            }
            for (SkillEffect effect : node.effects()) {
                if (effect.type() != SkillEffect.Type.ATTRIBUTE || effect.attribute() == null) {
                    continue;
                }
                String key = effect.target() + "|" + effect.operation().name();
                skillTotals.computeIfAbsent(key, k -> new double[1])[0] += effect.amount();
                attributes.put(key, effect.attribute());
                operations.put(key, effect.operation());
            }
        }
        // Remove previously applied skill modifiers (all slots), then add current ones.
        for (int i = 0; i < SkillEffect.MAX_SKILL_MODIFIERS; i++) {
            UUID uuid = skillUuid(i);
            for (AttributeInstance instance : player.getAttributes().getSyncableAttributes()) {
                instance.removeModifier(uuid);
            }
            for (Attribute attribute : SkillEffect.knownAttributes()) {
                AttributeInstance instance = player.getAttribute(attribute);
                if (instance != null) {
                    instance.removeModifier(uuid);
                }
            }
        }
        int slot = 0;
        List<String> keys = new ArrayList<>(skillTotals.keySet());
        keys.sort(String::compareTo);
        for (String key : keys) {
            if (slot >= SkillEffect.MAX_SKILL_MODIFIERS) {
                break;
            }
            AttributeInstance instance = player.getAttribute(attributes.get(key));
            if (instance != null) {
                instance.addPermanentModifier(new AttributeModifier(skillUuid(slot), "rpgmod skill " + key, skillTotals.get(key)[0], operations.get(key)));
            }
            slot++;
        }
        // Class set bonus
        for (PlayerClass playerClass : PlayerClass.values()) {
            for (int i = 0; i < playerClass.getModifiers().size(); i++) {
                AttributeInstance instance = player.getAttribute(playerClass.getModifiers().get(i).attribute().get());
                if (instance != null) {
                    instance.removeModifier(PlayerClass.modifierUuid(i));
                }
            }
        }
        for (int i = 0; i < activeClass.getModifiers().size(); i++) {
            PlayerClass.ClassModifier mod = activeClass.getModifiers().get(i);
            AttributeInstance instance = player.getAttribute(mod.attribute().get());
            if (instance != null) {
                instance.addPermanentModifier(new AttributeModifier(PlayerClass.modifierUuid(i), "rpgmod class " + activeClass.getKey(), mod.amount(), mod.operation()));
            }
        }
        // Keep health within the new maximum
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    private static UUID skillUuid(int index) {
        return new UUID(SKILL_MODIFIER_BASE.getMostSignificantBits(), SKILL_MODIFIER_BASE.getLeastSignificantBits() + index);
    }

    // ------------------------------------------------------------------ sync

    public void sync(ServerPlayer player) {
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncPlayerRpgPacket(serializeNBT()));
        clearDirty();
    }

    // ------------------------------------------------------------------ nbt

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Level", characterLevel);
        tag.putInt("Xp", xp);
        tag.putInt("StatPoints", statPoints);
        tag.putInt("SkillPoints", skillPoints);
        tag.putFloat("Mana", mana);
        CompoundTag statsTag = new CompoundTag();
        for (Map.Entry<StatType, Integer> e : stats.entrySet()) {
            statsTag.putInt(e.getKey().getKey(), e.getValue());
        }
        tag.put("Stats", statsTag);
        tag.put("Nodes", stringList(unlockedNodes));
        tag.putString("Class", activeClass.getKey());
        ListTag rep = new ListTag();
        for (Map.Entry<UUID, Integer> e : villageReputation.entrySet()) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("Id", e.getKey());
            entry.putInt("Value", e.getValue());
            rep.add(entry);
        }
        tag.put("Reputation", rep);
        tag.put("Spells", stringList(knownSpells));
        CompoundTag cds = new CompoundTag();
        for (Map.Entry<ResourceLocation, Integer> e : spellCooldowns.entrySet()) {
            cds.putInt(e.getKey().toString(), e.getValue());
        }
        tag.put("Cooldowns", cds);
        tag.put("Biomes", stringList(visitedBiomes));
        tag.putLong("Empowered", empoweredUntil);
        tag.putBoolean("StarterKit", starterKitGiven);
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        characterLevel = Math.max(1, tag.getInt("Level"));
        xp = tag.getInt("Xp");
        statPoints = tag.getInt("StatPoints");
        skillPoints = tag.getInt("SkillPoints");
        mana = tag.getFloat("Mana");
        stats.clear();
        CompoundTag statsTag = tag.getCompound("Stats");
        for (StatType type : StatType.values()) {
            if (statsTag.contains(type.getKey())) {
                stats.put(type, statsTag.getInt(type.getKey()));
            }
        }
        unlockedNodes.clear();
        unlockedNodes.addAll(readIds(tag.getList("Nodes", Tag.TAG_STRING)));
        activeClass = PlayerClass.byKey(tag.getString("Class"));
        villageReputation.clear();
        ListTag rep = tag.getList("Reputation", Tag.TAG_COMPOUND);
        for (int i = 0; i < rep.size(); i++) {
            CompoundTag entry = rep.getCompound(i);
            if (entry.hasUUID("Id")) {
                villageReputation.put(entry.getUUID("Id"), entry.getInt("Value"));
            }
        }
        knownSpells.clear();
        knownSpells.addAll(readIds(tag.getList("Spells", Tag.TAG_STRING)));
        spellCooldowns.clear();
        CompoundTag cds = tag.getCompound("Cooldowns");
        for (String key : cds.getAllKeys()) {
            ResourceLocation id = ResourceLocation.tryParse(key);
            if (id != null) {
                spellCooldowns.put(id, cds.getInt(key));
            }
        }
        visitedBiomes.clear();
        visitedBiomes.addAll(readIds(tag.getList("Biomes", Tag.TAG_STRING)));
        empoweredUntil = tag.getLong("Empowered");
        starterKitGiven = tag.getBoolean("StarterKit");
        markDirty();
    }

    /** Copies persistent progress from another instance (death / dimension change). */
    public void copyFrom(PlayerRpgData other) {
        deserializeNBT(other.serializeNBT());
    }

    private static ListTag stringList(Set<ResourceLocation> ids) {
        ListTag list = new ListTag();
        for (ResourceLocation id : ids) {
            list.add(StringTag.valueOf(id.toString()));
        }
        return list;
    }

    private static List<ResourceLocation> readIds(ListTag list) {
        List<ResourceLocation> ids = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            ResourceLocation id = ResourceLocation.tryParse(list.getString(i));
            if (id != null) {
                ids.add(id);
            }
        }
        return ids;
    }
}
