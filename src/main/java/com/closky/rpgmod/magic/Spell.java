package com.closky.rpgmod.magic;

import com.closky.rpgmod.registry.ModSpells;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Base class of all spells (section 6). Spells live in the custom registry {@code rpgmod:spells}; the concrete
 * behaviour is implemented by subclasses, the numbers come from {@link Properties}.
 */
public abstract class Spell {
    public static class Properties {
        float manaCost = 10.0F;
        int cooldown = 40;
        int requiredLevel = 1;
        float baseDamage = 0.0F;
        float radius = 0.0F;
        boolean basic;
        @Nullable
        SoundEvent sound;

        public Properties manaCost(float manaCost) {
            this.manaCost = manaCost;
            return this;
        }

        public Properties cooldown(int ticks) {
            this.cooldown = ticks;
            return this;
        }

        public Properties requiredLevel(int level) {
            this.requiredLevel = level;
            return this;
        }

        public Properties damage(float damage) {
            this.baseDamage = damage;
            return this;
        }

        public Properties radius(float radius) {
            this.radius = radius;
            return this;
        }

        /** Basic spells are usable with any staff of the school without learning them. */
        public Properties basic() {
            this.basic = true;
            return this;
        }

        public Properties sound(SoundEvent sound) {
            this.sound = sound;
            return this;
        }
    }

    private final SpellSchool school;
    private float manaCost;
    private int cooldown;
    private int requiredLevel;
    private float baseDamage;
    private float radius;
    private boolean basic;
    @Nullable
    private final SoundEvent sound;
    @Nullable
    private String descriptionId;

    protected Spell(SpellSchool school, Properties properties) {
        this.school = school;
        this.manaCost = properties.manaCost;
        this.cooldown = properties.cooldown;
        this.requiredLevel = properties.requiredLevel;
        this.baseDamage = properties.baseDamage;
        this.radius = properties.radius;
        this.basic = properties.basic;
        this.sound = properties.sound;
    }

    /**
     * Performs the spell on the server.
     *
     * @param power damage / healing multiplier (1.0 = no bonuses)
     * @return true if the spell was actually cast (mana and cooldown are then applied)
     */
    public abstract boolean cast(ServerLevel level, Player caster, ItemStack staff, float power);

    public SpellSchool getSchool() {
        return school;
    }

    public float getManaCost() {
        return manaCost;
    }

    public int getCooldown() {
        return cooldown;
    }

    public int getRequiredLevel() {
        return requiredLevel;
    }

    public float getBaseDamage() {
        return baseDamage;
    }

    public float getRadius() {
        return radius;
    }

    public boolean isBasic() {
        return basic;
    }

    @Nullable
    public SoundEvent getSound() {
        return sound;
    }

    public float getDamage(float power) {
        return baseDamage * power;
    }

    /**
     * Applies data-driven overrides from {@code data/<ns>/spells/<path>.json}. Only the present keys are changed:
     * {@code mana_cost}, {@code cooldown}, {@code required_level}, {@code damage}, {@code radius}, {@code basic}.
     */
    public void applyOverrides(com.google.gson.JsonObject json) {
        if (json.has("mana_cost")) {
            manaCost = json.get("mana_cost").getAsFloat();
        }
        if (json.has("cooldown")) {
            cooldown = json.get("cooldown").getAsInt();
        }
        if (json.has("required_level")) {
            requiredLevel = json.get("required_level").getAsInt();
        }
        if (json.has("damage")) {
            baseDamage = json.get("damage").getAsFloat();
        }
        if (json.has("radius")) {
            radius = json.get("radius").getAsFloat();
        }
        if (json.has("basic")) {
            basic = json.get("basic").getAsBoolean();
        }
    }

    @Nullable
    public ResourceLocation getId() {
        return ModSpells.REGISTRY.get().getKey(this);
    }

    public String getDescriptionId() {
        if (descriptionId == null) {
            ResourceLocation id = getId();
            descriptionId = id == null ? "spell.rpgmod.unknown" : "spell." + id.getNamespace() + "." + id.getPath();
        }
        return descriptionId;
    }

    public Component getDisplayName() {
        return Component.translatable(getDescriptionId()).withStyle(school.getColor());
    }

    public Component getDescription() {
        return Component.translatable(getDescriptionId() + ".desc");
    }

    /** Tooltip lines shared by scrolls, staffs and the spell screen. */
    public void appendTooltip(List<Component> tooltip) {
        tooltip.add(getDescription().copy().withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.rpgmod.spell.cost", (int) manaCost, cooldown / 20.0F).withStyle(ChatFormatting.DARK_AQUA));
        if (baseDamage > 0) {
            tooltip.add(Component.translatable("tooltip.rpgmod.spell.damage", baseDamage).withStyle(ChatFormatting.DARK_AQUA));
        }
        if (requiredLevel > 1) {
            tooltip.add(Component.translatable("tooltip.rpgmod.spell.level", requiredLevel).withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    @Override
    public String toString() {
        return String.valueOf(getId());
    }
}
