package com.closky.rpgmod.magic;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Fires one or more {@link SpellProjectileEntity}. */
public class ProjectileSpell extends Spell {
    private final float speed;
    private final int count;
    private final float spreadDegrees;

    public ProjectileSpell(SpellSchool school, Properties properties, float speed, int count, float spreadDegrees) {
        super(school, properties);
        this.speed = speed;
        this.count = count;
        this.spreadDegrees = spreadDegrees;
    }

    public ProjectileSpell(SpellSchool school, Properties properties) {
        this(school, properties, 1.5F, 1, 0.0F);
    }

    @Override
    public boolean cast(ServerLevel level, Player caster, ItemStack staff, float power) {
        for (int i = 0; i < count; i++) {
            float yawOffset = count == 1 ? 0.0F : spreadDegrees * (i - (count - 1) / 2.0F);
            SpellProjectileEntity projectile = new SpellProjectileEntity(level, caster, getSchool(), getDamage(power), getRadius());
            projectile.setPos(caster.getX(), caster.getEyeY() - 0.2D, caster.getZ());
            projectile.shootFromRotation(caster, caster.getXRot(), caster.getYRot() + yawOffset, 0.0F, speed, 0.5F);
            level.addFreshEntity(projectile);
        }
        return true;
    }
}
