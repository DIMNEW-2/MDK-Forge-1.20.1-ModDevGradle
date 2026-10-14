package com.closky.rpgmod.client.render;

import com.closky.rpgmod.RpgMod;
import com.closky.rpgmod.magic.SpellProjectileEntity;
import com.closky.rpgmod.magic.SpellSchool;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.EnumMap;
import java.util.Map;

/** Camera-facing glowing quad coloured by spell school. */
public class SpellProjectileRenderer extends EntityRenderer<SpellProjectileEntity> {
    private static final Map<SpellSchool, ResourceLocation> TEXTURES = new EnumMap<>(SpellSchool.class);

    static {
        for (SpellSchool school : SpellSchool.values()) {
            TEXTURES.put(school, RpgMod.id("textures/entity/spell/" + school.getKey() + ".png"));
        }
    }

    public SpellProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(SpellProjectileEntity entity, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        float spin = (entity.tickCount + partialTick) * 12.0F;
        poseStack.mulPose(Axis.ZP.rotationDegrees(spin));
        float size = 0.35F;
        poseStack.scale(size, size, size);
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucentEmissive(getTextureLocation(entity)));
        vertex(consumer, matrix, normal, -1.0F, -1.0F, 0.0F, 1.0F, packedLight);
        vertex(consumer, matrix, normal, 1.0F, -1.0F, 1.0F, 1.0F, packedLight);
        vertex(consumer, matrix, normal, 1.0F, 1.0F, 1.0F, 0.0F, packedLight);
        vertex(consumer, matrix, normal, -1.0F, 1.0F, 0.0F, 0.0F, packedLight);
        poseStack.popPose();
        super.render(entity, yaw, partialTick, poseStack, buffer, packedLight);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal, float x, float y, float u, float v, int light) {
        consumer.vertex(matrix, x, y, 0.0F).color(255, 255, 255, 255).uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
    }

    @Override
    protected int getBlockLightLevel(SpellProjectileEntity entity, net.minecraft.core.BlockPos pos) {
        return 15;
    }

    @Override
    public ResourceLocation getTextureLocation(SpellProjectileEntity entity) {
        return TEXTURES.get(entity.getSchool());
    }
}
