package com.closky.rpgmod.registry;

import com.closky.rpgmod.RpgMod;
import com.closky.rpgmod.magic.SpellProjectileEntity;
import com.closky.rpgmod.weapon.RpgArrowEntity;
import com.closky.rpgmod.weapon.RpgBoltEntity;
import com.closky.rpgmod.weapon.ThrownBombEntity;
import com.closky.rpgmod.weapon.ThrownSpearEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, RpgMod.MODID);

    public static final RegistryObject<EntityType<ThrownSpearEntity>> THROWN_SPEAR = ENTITIES.register("thrown_spear",
            () -> EntityType.Builder.<ThrownSpearEntity>of(ThrownSpearEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).clientTrackingRange(4).updateInterval(20).build(RpgMod.id("thrown_spear").toString()));
    public static final RegistryObject<EntityType<ThrownBombEntity>> THROWN_BOMB = ENTITIES.register("thrown_bomb",
            () -> EntityType.Builder.<ThrownBombEntity>of(ThrownBombEntity::new, MobCategory.MISC)
                    .sized(0.4F, 0.4F).clientTrackingRange(4).updateInterval(10).build(RpgMod.id("thrown_bomb").toString()));
    public static final RegistryObject<EntityType<RpgArrowEntity>> RPG_ARROW = ENTITIES.register("rpg_arrow",
            () -> EntityType.Builder.<RpgArrowEntity>of(RpgArrowEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).clientTrackingRange(4).updateInterval(20).build(RpgMod.id("rpg_arrow").toString()));
    public static final RegistryObject<EntityType<RpgBoltEntity>> RPG_BOLT = ENTITIES.register("rpg_bolt",
            () -> EntityType.Builder.<RpgBoltEntity>of(RpgBoltEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).clientTrackingRange(4).updateInterval(20).build(RpgMod.id("rpg_bolt").toString()));
    public static final RegistryObject<EntityType<SpellProjectileEntity>> SPELL_PROJECTILE = ENTITIES.register("spell_projectile",
            () -> EntityType.Builder.<SpellProjectileEntity>of(SpellProjectileEntity::new, MobCategory.MISC)
                    .sized(0.35F, 0.35F).clientTrackingRange(4).updateInterval(10).build(RpgMod.id("spell_projectile").toString()));

    private ModEntities() {
    }

    /** No living entities are added by the mod; kept for completeness and future bosses. */
    public static void registerAttributes(EntityAttributeCreationEvent event) {
    }
}
