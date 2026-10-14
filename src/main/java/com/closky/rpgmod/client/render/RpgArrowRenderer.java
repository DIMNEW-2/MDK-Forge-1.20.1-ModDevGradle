package com.closky.rpgmod.client.render;

import com.closky.rpgmod.RpgMod;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.projectile.AbstractArrow;

/** Arrow / bolt renderer with the mod's own projectile texture. */
public class RpgArrowRenderer<T extends AbstractArrow> extends ArrowRenderer<T> {
    private static final ResourceLocation TEXTURE = RpgMod.id("textures/entity/projectiles/rpg_arrow.png");

    public RpgArrowRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return TEXTURE;
    }
}
