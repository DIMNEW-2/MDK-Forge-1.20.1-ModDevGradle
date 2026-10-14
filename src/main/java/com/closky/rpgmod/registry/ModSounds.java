package com.closky.rpgmod.registry;

import com.closky.rpgmod.RpgMod;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Sound events. The bundled {@code sounds.json} maps them onto vanilla sounds (type "event"), so no external audio
 * assets are required; replacing an entry with a custom .ogg later is a resource-pack-only change.
 */
public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, RpgMod.MODID);

    public static final RegistryObject<SoundEvent> CASINO_SPIN = register("casino_spin");
    public static final RegistryObject<SoundEvent> CASINO_STOP = register("casino_stop");
    public static final RegistryObject<SoundEvent> CASINO_WIN = register("casino_win");
    public static final RegistryObject<SoundEvent> CASINO_JACKPOT = register("casino_jackpot");
    public static final RegistryObject<SoundEvent> SPELL_CAST = register("spell_cast");
    public static final RegistryObject<SoundEvent> SPELL_FAIL = register("spell_fail");
    public static final RegistryObject<SoundEvent> SPELL_LEARN = register("spell_learn");
    public static final RegistryObject<SoundEvent> LEVEL_UP = register("level_up");
    public static final RegistryObject<SoundEvent> SKILL_UNLOCK = register("skill_unlock");
    public static final RegistryObject<SoundEvent> FORGE = register("forge");
    public static final RegistryObject<SoundEvent> SPEAR_THROW = register("spear_throw");
    public static final RegistryObject<SoundEvent> SPEAR_HIT = register("spear_hit");
    public static final RegistryObject<SoundEvent> BOMB_FUSE = register("bomb_fuse");
    public static final RegistryObject<SoundEvent> BOMB_EXPLODE = register("bomb_explode");
    public static final RegistryObject<SoundEvent> MACE_SMASH = register("mace_smash");
    public static final RegistryObject<SoundEvent> SWORD_DASH = register("sword_dash");
    public static final RegistryObject<SoundEvent> BOW_DRAW = register("bow_draw");
    public static final RegistryObject<SoundEvent> CROSSBOW_RELOAD = register("crossbow_reload");
    public static final RegistryObject<SoundEvent> BOSS_SPAWN = register("boss_spawn");
    public static final RegistryObject<SoundEvent> SPELL_IMPACT = register("spell_impact");
    public static final RegistryObject<SoundEvent> HEAL = register("heal");
    public static final RegistryObject<SoundEvent> CHEST_OPEN = register("chest_open");

    private ModSounds() {
    }

    private static RegistryObject<SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(RpgMod.id(name)));
    }
}
