package com.closky.rpgmod;

import com.closky.rpgmod.capability.MobRpgData;
import com.closky.rpgmod.capability.PlayerRpgData;
import com.closky.rpgmod.config.RpgConfig;
import com.closky.rpgmod.network.ModNetwork;
import com.closky.rpgmod.registry.ModAttributes;
import com.closky.rpgmod.registry.ModBlockEntities;
import com.closky.rpgmod.registry.ModBlocks;
import com.closky.rpgmod.registry.ModCreativeTabs;
import com.closky.rpgmod.registry.ModEffects;
import com.closky.rpgmod.registry.ModEntities;
import com.closky.rpgmod.registry.ModItems;
import com.closky.rpgmod.registry.ModLootFunctions;
import com.closky.rpgmod.registry.ModMenus;
import com.closky.rpgmod.registry.ModParticles;
import com.closky.rpgmod.registry.ModRecipes;
import com.closky.rpgmod.registry.ModSounds;
import com.closky.rpgmod.registry.ModSpells;
import com.closky.rpgmod.registry.ModStructures;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.common.crafting.CraftingHelper;
import com.closky.rpgmod.crafting.ConfigCondition;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * RPG Mod entry point.
 *
 * <p>Everything is registered through {@link net.minecraftforge.registries.DeferredRegister}s that live in the
 * {@code registry} package. Gameplay logic is split by domain (weapon, magic, loot, player, enemy, village, ...).
 * The mod has no dependencies besides Minecraft Forge.</p>
 */
@Mod(RpgMod.MODID)
public class RpgMod {
    public static final String MODID = "rpgmod";
    public static final Logger LOGGER = LogUtils.getLogger();

    public RpgMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Registries (order matters only for blocks -> items)
        ModAttributes.ATTRIBUTES.register(modBus);
        ModEffects.EFFECTS.register(modBus);
        ModSounds.SOUNDS.register(modBus);
        ModParticles.PARTICLES.register(modBus);
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModMenus.MENUS.register(modBus);
        ModRecipes.RECIPE_SERIALIZERS.register(modBus);
        ModLootFunctions.LOOT_FUNCTIONS.register(modBus);
        ModSpells.SPELLS.register(modBus);
        ModStructures.STRUCTURE_TYPES.register(modBus);
        ModStructures.STRUCTURE_PIECES.register(modBus);
        ModCreativeTabs.TABS.register(modBus);

        modBus.addListener(this::commonSetup);
        modBus.addListener(this::registerCapabilities);
        modBus.addListener(ModEntities::registerAttributes);
        modBus.addListener(ModAttributes::addPlayerAttributes);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, RpgConfig.COMMON_SPEC, "rpgmod-common.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, RpgConfig.CLIENT_SPEC, "rpgmod-client.toml");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            ModNetwork.register();
            CraftingHelper.register(ConfigCondition.Serializer.INSTANCE);
        });
        LOGGER.info("RPG Mod initialised: {} spells registered", ModSpells.REGISTRY.get().getKeys().size());
    }

    private void registerCapabilities(final RegisterCapabilitiesEvent event) {
        event.register(PlayerRpgData.class);
        event.register(MobRpgData.class);
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MODID, path);
    }
}
