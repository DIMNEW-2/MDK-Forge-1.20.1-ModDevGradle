package com.closky.rpgmod.client;

import com.closky.rpgmod.RpgMod;
import com.closky.rpgmod.client.render.RpgArrowRenderer;
import com.closky.rpgmod.client.render.SpellProjectileRenderer;
import com.closky.rpgmod.client.render.ThrownSpearRenderer;
import com.closky.rpgmod.client.screen.CasinoScreen;
import com.closky.rpgmod.registry.ModEntities;
import com.closky.rpgmod.registry.ModItems;
import com.closky.rpgmod.registry.ModMenus;
import com.closky.rpgmod.registry.ModParticles;
import com.closky.rpgmod.weapon.RpgBowItem;
import com.closky.rpgmod.weapon.RpgCrossbowItem;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.particle.EndRodParticle;
import net.minecraft.client.particle.FlameParticle;
import net.minecraft.client.particle.SuspendedTownParticle;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.registries.RegistryObject;

/** Mod-bus client registration: item model predicates, screens, renderers, particles, keys and overlays. */
@Mod.EventBusSubscriber(modid = RpgMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenus.LOOT_CHEST.get(), CasinoScreen::new);
            registerItemProperties();
        });
    }

    private static void registerItemProperties() {
        ResourceLocation pull = new ResourceLocation("pull");
        ResourceLocation pulling = new ResourceLocation("pulling");
        ResourceLocation charged = new ResourceLocation("charged");
        ResourceLocation firework = new ResourceLocation("firework");
        for (RegistryObject<Item> bow : ModItems.BOWS.values()) {
            ItemProperties.register(bow.get(), pull, (stack, level, entity, seed) -> {
                if (entity == null || entity.getUseItem() != stack) {
                    return 0.0F;
                }
                return ((RpgBowItem) stack.getItem()).getPower(stack, stack.getUseDuration() - entity.getUseItemRemainingTicks());
            });
            ItemProperties.register(bow.get(), pulling, (stack, level, entity, seed) ->
                    entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F);
        }
        for (RegistryObject<Item> crossbow : ModItems.CROSSBOWS.values()) {
            ItemProperties.register(crossbow.get(), pull, (stack, level, entity, seed) -> {
                if (entity == null || CrossbowItem.isCharged(stack)) {
                    return 0.0F;
                }
                return ((RpgCrossbowItem) stack.getItem()).getChargeProgress(stack, stack.getUseDuration() - entity.getUseItemRemainingTicks());
            });
            ItemProperties.register(crossbow.get(), pulling, (stack, level, entity, seed) ->
                    entity != null && entity.isUsingItem() && entity.getUseItem() == stack && !CrossbowItem.isCharged(stack) ? 1.0F : 0.0F);
            ItemProperties.register(crossbow.get(), charged, (stack, level, entity, seed) -> CrossbowItem.isCharged(stack) ? 1.0F : 0.0F);
            ItemProperties.register(crossbow.get(), firework, (stack, level, entity, seed) ->
                    CrossbowItem.isCharged(stack) && RpgCrossbowItem.isLoadedWith(stack, Items.FIREWORK_ROCKET) ? 1.0F : 0.0F);
        }
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.THROWN_SPEAR.get(), ThrownSpearRenderer::new);
        event.registerEntityRenderer(ModEntities.THROWN_BOMB.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.RPG_ARROW.get(), RpgArrowRenderer::new);
        event.registerEntityRenderer(ModEntities.RPG_BOLT.get(), RpgArrowRenderer::new);
        event.registerEntityRenderer(ModEntities.SPELL_PROJECTILE.get(), SpellProjectileRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.MANA_SPARK.get(), EndRodParticle.Provider::new);
        event.registerSpriteSet(ModParticles.CASINO_SPARKLE.get(), FlameParticle.Provider::new);
        event.registerSpriteSet(ModParticles.NATURE_LEAF.get(), SuspendedTownParticle.Provider::new);
    }

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(KeyBindings.OPEN_SKILLS);
        event.register(KeyBindings.OPEN_CHARACTER);
        event.register(KeyBindings.OPEN_RELATIONS);
        event.register(KeyBindings.CAST_SPELL);
        event.register(KeyBindings.SELECT_SPELL);
    }

    @SubscribeEvent
    public static void onRegisterOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAbove(VanillaGuiOverlay.EXPERIENCE_BAR.id(), "rpg_hud", new RpgHudOverlay());
    }
}
