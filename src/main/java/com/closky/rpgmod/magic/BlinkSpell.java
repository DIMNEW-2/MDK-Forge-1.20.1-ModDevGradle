package com.closky.rpgmod.magic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Short-range teleport along the view vector (stops at the first solid block). */
public class BlinkSpell extends Spell {
    private final double distance;

    public BlinkSpell(SpellSchool school, Properties properties, double distance) {
        super(school, properties);
        this.distance = distance;
    }

    @Override
    public boolean cast(ServerLevel level, Player caster, ItemStack staff, float power) {
        Vec3 eye = caster.getEyePosition();
        Vec3 look = caster.getViewVector(1.0F);
        Vec3 end = eye.add(look.scale(distance));
        BlockHitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
        Vec3 target = hit.getType() == HitResult.Type.MISS ? end : hit.getLocation().subtract(look.scale(0.5D));
        BlockPos feet = BlockPos.containing(target.x, target.y - caster.getEyeHeight(), target.z);
        // find a free spot: move up to 3 blocks up or down
        BlockPos found = null;
        for (int dy = 0; dy <= 3 && found == null; dy++) {
            for (int sign : new int[]{1, -1}) {
                BlockPos candidate = feet.offset(0, dy * sign, 0);
                if (level.noCollision(caster, caster.getBoundingBox().move(Vec3.atBottomCenterOf(candidate).subtract(caster.position())))) {
                    found = candidate;
                    break;
                }
            }
        }
        if (found == null) {
            return false;
        }
        level.sendParticles(ParticleTypes.PORTAL, caster.getX(), caster.getY() + 1.0D, caster.getZ(), 30, 0.3D, 0.6D, 0.3D, 0.5D);
        caster.teleportTo(found.getX() + 0.5D, found.getY(), found.getZ() + 0.5D);
        caster.fallDistance = 0.0F;
        level.sendParticles(ParticleTypes.PORTAL, caster.getX(), caster.getY() + 1.0D, caster.getZ(), 30, 0.3D, 0.6D, 0.3D, 0.5D);
        return true;
    }
}
