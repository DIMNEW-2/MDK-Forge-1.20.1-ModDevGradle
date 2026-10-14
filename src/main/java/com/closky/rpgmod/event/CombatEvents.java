package com.closky.rpgmod.event;

import com.closky.rpgmod.RpgMod;
import com.closky.rpgmod.boss.BossManager;
import com.closky.rpgmod.capability.PlayerRpgData;
import com.closky.rpgmod.enemy.MobLevel;
import com.closky.rpgmod.loot.RpgItemData;
import com.closky.rpgmod.loot.WeaponMaterial;
import com.closky.rpgmod.player.CharacterLevel;
import com.closky.rpgmod.player.PlayerClass;
import com.closky.rpgmod.registry.ModAttributes;
import com.closky.rpgmod.registry.ModEffects;
import com.closky.rpgmod.settlement.SettlementManager;
import com.closky.rpgmod.weapon.RpgMaceItem;
import com.closky.rpgmod.weapon.RpgSpearItem;
import com.closky.rpgmod.weapon.RpgSwordItem;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.CriticalHitEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Combat rules that cannot live inside the items themselves (sections 2, 7.3, 8.3):
 * stun cancels attacks, empowered sword hits, class set bonuses, material on-hit effects, level difference scaling,
 * crit chance, character XP for kills and boss drops.
 */
@Mod.EventBusSubscriber(modid = RpgMod.MODID)
public final class CombatEvents {
    private CombatEvents() {
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (player.hasEffect(ModEffects.STUN.get())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        Entity attacker = event.getSource().getEntity();
        if (attacker instanceof LivingEntity living && !(attacker instanceof Player) && living.hasEffect(ModEffects.STUN.get())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onCriticalHit(CriticalHitEvent event) {
        Player player = event.getEntity();
        if (event.isVanillaCritical() || player.level().isClientSide) {
            return;
        }
        AttributeInstance crit = player.getAttribute(ModAttributes.CRIT_CHANCE.get());
        if (crit == null) {
            return;
        }
        double chance = crit.getValue();
        if (chance > 0 && player.getRandom().nextDouble() < chance) {
            event.setResult(Event.Result.ALLOW);
            event.setDamageModifier(Math.max(event.getDamageModifier(), 1.5F));
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide) {
            return;
        }
        Entity source = event.getSource().getEntity();
        float amount = event.getAmount();

        // Player -> mob
        if (source instanceof ServerPlayer player) {
            amount = applyPlayerOffense(player, target, event, amount);
        }
        // Mob/anything -> player
        if (target instanceof ServerPlayer player) {
            amount = applyPlayerDefense(player, event, amount);
        }
        event.setAmount(amount);
    }

    private static float applyPlayerOffense(ServerPlayer player, LivingEntity target, LivingHurtEvent event, float amount) {
        PlayerRpgData data = PlayerRpgData.get(player);
        ItemStack weapon = player.getMainHandItem();
        boolean melee = event.getSource().getDirectEntity() == player;

        if (data != null && melee) {
            // Sword dash empowerment
            if (weapon.getItem() instanceof RpgSwordItem && data.getEmpoweredUntil() > player.level().getGameTime()) {
                amount *= RpgSwordItem.EMPOWERED_MULTIPLIER;
                data.setEmpoweredUntil(0L);
                ((ServerLevel) player.level()).sendParticles(ParticleTypes.CRIT, target.getX(), target.getY(0.5D), target.getZ(), 12, 0.3D, 0.3D, 0.3D, 0.2D);
                player.level().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0F, 1.2F);
            }
            // Class set bonuses (weapon specific part)
            PlayerClass playerClass = data.getActiveClass();
            if (playerClass == PlayerClass.WARRIOR) {
                if (weapon.getItem() instanceof RpgSwordItem) {
                    amount *= 1.10F;
                } else if (weapon.getItem() instanceof RpgMaceItem) {
                    amount *= 1.15F;
                }
            } else if (playerClass == PlayerClass.SPEARMAN && weapon.getItem() instanceof RpgSpearItem) {
                amount *= 1.15F;
            } else if (playerClass == PlayerClass.ROGUE && isBehind(player, target)) {
                amount *= 1.30F;
            }
            // Skill flags
            if (data.hasSkillFlag("executioner") && target.getHealth() / target.getMaxHealth() < 0.25F) {
                amount *= 1.25F;
            }
        }

        // Material on-hit effects (melee)
        if (melee && RpgItemData.isRpgItem(weapon)) {
            WeaponMaterial material = RpgItemData.getMaterial(weapon);
            if (material == WeaponMaterial.COPPER) {
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0));
            } else if (material == WeaponMaterial.AMETHYST) {
                float magic = material.getMagicDamage();
                AttributeInstance power = player.getAttribute(ModAttributes.MAGIC_POWER.get());
                if (power != null) {
                    magic *= (float) (1.0D + power.getValue());
                }
                target.invulnerableTime = 0;
                target.hurt(player.damageSources().indirectMagic(player, player), magic);
            }
        }

        // Armor penetration: convert a share of the damage to bypass armor by lowering the target's armor temporarily
        AttributeInstance pen = player.getAttribute(ModAttributes.ARMOR_PENETRATION.get());
        if (pen != null && pen.getValue() > 0 && target.getArmorValue() > 0) {
            float armor = target.getArmorValue();
            float penetrated = (float) Math.min(1.0D, pen.getValue());
            // vanilla reduction approximation: damage * (1 - armor/25); refund the penetrated part
            float refund = amount * (armor / 25.0F) * penetrated * 0.5F;
            amount += refund;
        }

        // Level difference (section 8.3)
        if (target instanceof Mob) {
            int mobLevel = MobLevel.getLevel(target);
            int playerLevel = data == null ? 1 : data.getLevel();
            amount *= MobLevel.playerDamageMultiplier(playerLevel, mobLevel);
        }
        return amount;
    }

    private static float applyPlayerDefense(ServerPlayer player, LivingHurtEvent event, float amount) {
        PlayerRpgData data = PlayerRpgData.get(player);
        if (data == null) {
            return amount;
        }
        PlayerClass playerClass = data.getActiveClass();
        boolean explosion = event.getSource().is(DamageTypes.EXPLOSION) || event.getSource().is(DamageTypes.PLAYER_EXPLOSION);
        if (playerClass == PlayerClass.BOMBER && explosion) {
            amount *= 0.5F;
        }
        if (playerClass == PlayerClass.MAGE && (event.getSource().is(DamageTypes.MAGIC) || event.getSource().is(DamageTypes.INDIRECT_MAGIC))) {
            amount *= 0.8F;
        }
        if (playerClass == PlayerClass.DRUID && (event.getSource().is(DamageTypes.CACTUS) || event.getSource().is(DamageTypes.SWEET_BERRY_BUSH) || event.getSource().is(DamageTypes.FALL))) {
            amount *= 0.5F;
        }
        if (data.hasSkillFlag("iron_will") && amount > player.getMaxHealth() * 0.5F) {
            amount = player.getMaxHealth() * 0.5F;
        }
        return amount;
    }

    /** True when the attacker stands behind the target (the target looks away from the attacker). */
    private static boolean isBehind(Player player, LivingEntity target) {
        double dx = target.getX() - player.getX();
        double dz = target.getZ() - player.getZ();
        double lookX = -Math.sin(Math.toRadians(target.getYRot()));
        double lookZ = Math.cos(Math.toRadians(target.getYRot()));
        double dot = dx * lookX + dz * lookZ;
        return dot > 0;
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (dead.level().isClientSide) {
            return;
        }
        Entity killer = event.getSource().getEntity();
        ServerLevel level = (ServerLevel) dead.level();
        if (dead instanceof Villager villager) {
            SettlementManager.get(level).onVillagerKilled(killer instanceof Player p ? p : null, villager);
        } else if (dead instanceof AbstractIllager illager && killer instanceof Player p) {
            SettlementManager.get(level).onRaiderKilled(p, illager);
        }
        if (killer instanceof ServerPlayer player && dead instanceof Mob) {
            PlayerRpgData data = PlayerRpgData.get(player);
            if (data != null) {
                int mobLevel = MobLevel.getLevel(dead);
                int xp = CharacterLevel.xpForKill(mobLevel, data.getLevel());
                if (BossManager.isBoss(dead)) {
                    xp = 0; // boss XP is handled in BossManager for every nearby player
                }
                if (xp > 0) {
                    data.addXp(player, xp);
                    data.sync(player);
                }
            }
        }
        if (BossManager.isBoss(dead)) {
            BossManager.removeBossBar(dead);
        }
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        LivingEntity dead = event.getEntity();
        if (dead.level().isClientSide || !BossManager.isBoss(dead)) {
            return;
        }
        Entity killer = event.getSource().getEntity();
        BossManager.onBossDeath(dead, event.getDrops(), killer instanceof Player p ? p : null);
    }
}
