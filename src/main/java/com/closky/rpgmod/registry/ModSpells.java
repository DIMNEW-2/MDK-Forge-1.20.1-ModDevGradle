package com.closky.rpgmod.registry;

import com.closky.rpgmod.RpgMod;
import com.closky.rpgmod.magic.AoeSpell;
import com.closky.rpgmod.magic.BlinkSpell;
import com.closky.rpgmod.magic.HealSpell;
import com.closky.rpgmod.magic.LightningSpell;
import com.closky.rpgmod.magic.ProjectileSpell;
import com.closky.rpgmod.magic.SelfSpell;
import com.closky.rpgmod.magic.Spell;
import com.closky.rpgmod.magic.SpellSchool;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryBuilder;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Spell registry ({@code rpgmod:spells}). Spells are registry entries so that data packs, scrolls and skill nodes
 * can reference them by id.
 */
public final class ModSpells {
    public static final ResourceKey<Registry<Spell>> KEY = ResourceKey.createRegistryKey(RpgMod.id("spells"));
    public static final DeferredRegister<Spell> SPELLS = DeferredRegister.create(KEY, RpgMod.MODID);
    public static final Supplier<IForgeRegistry<Spell>> REGISTRY = SPELLS.makeRegistry(() -> new RegistryBuilder<Spell>().disableSaving());

    // ---------------------------------------------------------------- fire
    public static final RegistryObject<Spell> FIREBALL = SPELLS.register("fireball",
            () -> new ProjectileSpell(SpellSchool.FIRE, new Spell.Properties().manaCost(8).cooldown(30).damage(6).basic(), 1.6F, 1, 0));
    public static final RegistryObject<Spell> FIRE_NOVA = SPELLS.register("fire_nova",
            () -> new AoeSpell(SpellSchool.FIRE, new Spell.Properties().manaCost(20).cooldown(200).damage(5).radius(4).requiredLevel(5), true));
    public static final RegistryObject<Spell> FLAME_BURST = SPELLS.register("flame_burst",
            () -> new ProjectileSpell(SpellSchool.FIRE, new Spell.Properties().manaCost(18).cooldown(120).damage(4).requiredLevel(8), 1.5F, 3, 12));
    public static final RegistryObject<Spell> INFERNO = SPELLS.register("inferno",
            () -> new ProjectileSpell(SpellSchool.FIRE, new Spell.Properties().manaCost(30).cooldown(300).damage(9).radius(2.5F).requiredLevel(12), 1.3F, 1, 0));

    // ---------------------------------------------------------------- frost
    public static final RegistryObject<Spell> ICE_SHARD = SPELLS.register("ice_shard",
            () -> new ProjectileSpell(SpellSchool.FROST, new Spell.Properties().manaCost(7).cooldown(25).damage(5).basic(), 1.8F, 1, 0));
    public static final RegistryObject<Spell> FROST_NOVA = SPELLS.register("frost_nova",
            () -> new AoeSpell(SpellSchool.FROST, new Spell.Properties().manaCost(18).cooldown(200).damage(3).radius(4).requiredLevel(5), false));
    public static final RegistryObject<Spell> ICE_ARMOR = SPELLS.register("ice_armor",
            () -> new SelfSpell(SpellSchool.FROST, new Spell.Properties().manaCost(15).cooldown(400).requiredLevel(8),
                    List.of(new SelfSpell.EffectEntry(() -> MobEffects.DAMAGE_RESISTANCE, 300, 0),
                            new SelfSpell.EffectEntry(() -> MobEffects.ABSORPTION, 300, 1))));
    public static final RegistryObject<Spell> BLIZZARD = SPELLS.register("blizzard",
            () -> new ProjectileSpell(SpellSchool.FROST, new Spell.Properties().manaCost(25).cooldown(240).damage(4).radius(1.5F).requiredLevel(12), 1.4F, 3, 10));

    // ---------------------------------------------------------------- lightning
    public static final RegistryObject<Spell> SPARK = SPELLS.register("spark",
            () -> new ProjectileSpell(SpellSchool.LIGHTNING, new Spell.Properties().manaCost(6).cooldown(20).damage(4).basic(), 2.5F, 1, 0));
    public static final RegistryObject<Spell> LIGHTNING_BOLT = SPELLS.register("lightning_bolt",
            () -> new LightningSpell(SpellSchool.LIGHTNING, new Spell.Properties().manaCost(15).cooldown(100).damage(8).requiredLevel(5), 24.0D, 0));
    public static final RegistryObject<Spell> STATIC_CHARGE = SPELLS.register("static_charge",
            () -> new SelfSpell(SpellSchool.LIGHTNING, new Spell.Properties().manaCost(12).cooldown(400).requiredLevel(8),
                    List.of(new SelfSpell.EffectEntry(() -> MobEffects.MOVEMENT_SPEED, 300, 1),
                            new SelfSpell.EffectEntry(ModEffects.MANA_SURGE::get, 200, 0))));
    public static final RegistryObject<Spell> BLINK = SPELLS.register("blink",
            () -> new BlinkSpell(SpellSchool.LIGHTNING, new Spell.Properties().manaCost(14).cooldown(160).requiredLevel(6), 10.0D));
    public static final RegistryObject<Spell> CHAIN_LIGHTNING = SPELLS.register("chain_lightning",
            () -> new LightningSpell(SpellSchool.LIGHTNING, new Spell.Properties().manaCost(25).cooldown(240).damage(7).requiredLevel(10), 20.0D, 3));

    // ---------------------------------------------------------------- nature
    public static final RegistryObject<Spell> HEAL = SPELLS.register("heal",
            () -> new HealSpell(SpellSchool.NATURE, new Spell.Properties().manaCost(10).cooldown(60).basic().sound(ModSounds.HEAL.get()), 6.0F, 0));
    public static final RegistryObject<Spell> THORN_BOLT = SPELLS.register("thorn_bolt",
            () -> new ProjectileSpell(SpellSchool.NATURE, new Spell.Properties().manaCost(8).cooldown(30).damage(5).requiredLevel(3), 1.6F, 1, 0));
    public static final RegistryObject<Spell> ENTANGLE = SPELLS.register("entangle",
            () -> new AoeSpell(SpellSchool.NATURE, new Spell.Properties().manaCost(16).cooldown(200).damage(2).radius(4).requiredLevel(4), false));
    public static final RegistryObject<Spell> REGROWTH = SPELLS.register("regrowth",
            () -> new HealSpell(SpellSchool.NATURE, new Spell.Properties().manaCost(20).cooldown(300).radius(5).requiredLevel(6).sound(ModSounds.HEAL.get()), 4.0F, 100));
    public static final RegistryObject<Spell> BARK_SKIN = SPELLS.register("bark_skin",
            () -> new SelfSpell(SpellSchool.NATURE, new Spell.Properties().manaCost(15).cooldown(500).requiredLevel(8),
                    List.of(new SelfSpell.EffectEntry(ModEffects.BARK_SKIN::get, 400, 0),
                            new SelfSpell.EffectEntry(() -> MobEffects.DAMAGE_RESISTANCE, 400, 0))));

    private ModSpells() {
    }

    @Nullable
    public static Spell basicSpell(SpellSchool school) {
        for (Spell spell : REGISTRY.get().getValues()) {
            if (spell.getSchool() == school && spell.isBasic()) {
                return spell;
            }
        }
        return null;
    }

    public static List<Spell> bySchool(SpellSchool school) {
        List<Spell> list = new ArrayList<>();
        for (Spell spell : REGISTRY.get().getValues()) {
            if (spell.getSchool() == school) {
                list.add(spell);
            }
        }
        list.sort((a, b) -> Integer.compare(a.getRequiredLevel(), b.getRequiredLevel()));
        return list;
    }
}
