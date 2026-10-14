package com.closky.rpgmod.event;

import com.closky.rpgmod.RpgMod;
import com.closky.rpgmod.boss.BossManager;
import com.closky.rpgmod.capability.MobRpgData;
import com.closky.rpgmod.capability.MobRpgProvider;
import com.closky.rpgmod.capability.PlayerRpgData;
import com.closky.rpgmod.capability.PlayerRpgProvider;
import com.closky.rpgmod.command.RpgCommands;
import com.closky.rpgmod.config.RpgConfig;
import com.closky.rpgmod.enemy.MobLevel;
import com.closky.rpgmod.magic.SpellConfigLoader;
import com.closky.rpgmod.network.ModNetwork;
import com.closky.rpgmod.network.packet.SyncSkillTreePacket;
import com.closky.rpgmod.player.CharacterLevel;
import com.closky.rpgmod.registry.ModEffects;
import com.closky.rpgmod.registry.ModItems;
import com.closky.rpgmod.settlement.Settlement;
import com.closky.rpgmod.settlement.SettlementManager;
import com.closky.rpgmod.skill.SkillTree;
import com.closky.rpgmod.weapon.WeaponTier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.TradeWithVillagerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/** Lifecycle / capability / tick glue (sections 7, 8, 12, 17). */
@Mod.EventBusSubscriber(modid = RpgMod.MODID)
public final class CommonEvents {
    private CommonEvents() {
    }

    // ------------------------------------------------------------------ capabilities

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        Entity entity = event.getObject();
        if (entity instanceof Player) {
            PlayerRpgProvider provider = new PlayerRpgProvider();
            event.addCapability(PlayerRpgProvider.ID, provider);
            event.addListener(provider::invalidate);
        } else if (entity instanceof LivingEntity) {
            MobRpgProvider provider = new MobRpgProvider();
            event.addCapability(MobRpgProvider.ID, provider);
            event.addListener(provider::invalidate);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        Player original = event.getOriginal();
        original.reviveCaps();
        PlayerRpgData oldData = PlayerRpgData.get(original);
        PlayerRpgData newData = PlayerRpgData.get(event.getEntity());
        if (oldData != null && newData != null) {
            newData.copyFrom(oldData);
            if (event.isWasDeath()) {
                newData.setMana(newData.getMaxMana(event.getEntity()) * 0.5F);
            }
        }
        original.invalidateCaps();
    }

    // ------------------------------------------------------------------ login / respawn

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        PlayerRpgData data = PlayerRpgData.get(player);
        if (data == null) {
            return;
        }
        ModNetwork.sendToPlayer(player, new SyncSkillTreePacket(new ArrayList<>(SkillTree.server().getNodes())));
        data.updateClass(player);
        data.applyAttributeModifiers(player);
        if (RpgConfig.COMMON.giveStarterKit.get() && !data.isStarterKitGiven()) {
            giveStarterKit(player);
            data.setStarterKitGiven(true);
            player.displayClientMessage(Component.translatable("message.rpgmod.welcome").withStyle(ChatFormatting.GOLD), false);
        }
        data.sync(player);
    }

    private static void giveStarterKit(ServerPlayer player) {
        List<ItemStack> kit = List.of(
                new ItemStack(ModItems.SWORDS.get(WeaponTier.WOOD).get()),
                new ItemStack(ModItems.BOWS.get(WeaponTier.WOOD).get()),
                new ItemStack(ModItems.arrowFor(com.closky.rpgmod.weapon.ProjectileEffect.BASIC), 16),
                new ItemStack(ModItems.SKILL_BOOK.get()),
                new ItemStack(ModItems.RELATIONS_MAP.get()),
                new ItemStack(ModItems.MANA_POTION.get(), 2));
        for (ItemStack stack : kit) {
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerRpgData data = PlayerRpgData.get(player);
            if (data != null) {
                data.updateClass(player);
                data.applyAttributeModifiers(player);
                data.sync(player);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerRpgData data = PlayerRpgData.get(player);
            if (data != null) {
                data.applyAttributeModifiers(player);
                data.sync(player);
            }
        }
    }

    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getSlot().getType() == net.minecraft.world.entity.EquipmentSlot.Type.ARMOR) {
            PlayerRpgData data = PlayerRpgData.get(player);
            if (data != null && data.updateClass(player)) {
                if (data.getActiveClass() != com.closky.rpgmod.player.PlayerClass.NONE) {
                    player.displayClientMessage(Component.translatable("message.rpgmod.class_activated", data.getActiveClass().getDisplayName()).withStyle(ChatFormatting.GREEN), true);
                }
                data.sync(player);
            }
        }
    }

    // ------------------------------------------------------------------ ticks

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Player player = event.player;
        PlayerRpgData data = PlayerRpgData.get(player);
        if (data == null) {
            return;
        }
        data.tickCooldowns();
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        int tick = serverPlayer.tickCount;
        if (tick % 20 == 0) {
            data.regenerateMana(serverPlayer);
        }
        if (tick % 100 == 0) {
            discoverBiome(serverPlayer, data);
            ServerLevel level = serverPlayer.serverLevel();
            if (level.dimension() == Level.OVERWORLD) {
                SettlementManager manager = SettlementManager.get(level);
                manager.discover(level, serverPlayer);
                handleHostileVillage(level, manager, serverPlayer);
            }
        }
        if (data.isDirty() && tick % 10 == 0) {
            data.sync(serverPlayer);
        }
    }

    private static void discoverBiome(ServerPlayer player, PlayerRpgData data) {
        Holder<Biome> biome = player.level().getBiome(player.blockPosition());
        ResourceLocation id = biome.unwrapKey().map(k -> k.location()).orElse(null);
        if (id != null && data.markBiomeVisited(id)) {
            data.addXp(player, CharacterLevel.xpForNewBiome());
            player.displayClientMessage(Component.translatable("message.rpgmod.biome_discovered", Component.translatable("biome." + id.getNamespace() + "." + id.getPath())).withStyle(ChatFormatting.DARK_AQUA), true);
        }
    }

    private static void handleHostileVillage(ServerLevel level, SettlementManager manager, ServerPlayer player) {
        if (!manager.isHostileTo(player, player.blockPosition())) {
            return;
        }
        Settlement settlement = manager.findNearest(player.blockPosition(), 128);
        if (settlement == null) {
            return;
        }
        for (IronGolem golem : level.getEntitiesOfClass(IronGolem.class, player.getBoundingBox().inflate(24.0D))) {
            if (golem.getTarget() == null) {
                golem.setTarget(player);
            }
        }
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) {
            return;
        }
        SettlementManager.get(level).tick(level);
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.hasEffect(ModEffects.STUN.get()) || entity.hasEffect(ModEffects.ROOT.get())) {
            Vec3 motion = entity.getDeltaMovement();
            if (motion.y > 0.0D || Math.abs(motion.x) > 0.01D || Math.abs(motion.z) > 0.01D) {
                entity.setDeltaMovement(0.0D, Math.min(motion.y, 0.0D), 0.0D);
            }
            if (entity instanceof Mob mob) {
                mob.getNavigation().stop();
            }
        }
        if (!entity.level().isClientSide && entity.tickCount % 10 == 0 && BossManager.isBoss(entity)) {
            BossManager.tickBossBar(entity);
        }
    }

    // ------------------------------------------------------------------ mobs

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide || !(event.getEntity() instanceof Mob mob)) {
            return;
        }
        MobRpgData data = MobRpgData.get(mob);
        if (data == null) {
            return;
        }
        if (!data.isInitialized()) {
            MobLevel.initialize(mob);
        } else {
            MobLevel.sync(mob);
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getTarget() instanceof LivingEntity living && !(living instanceof Player)) {
            MobLevel.syncTo(living, player);
        }
    }

    @SubscribeEvent
    public static void onTrade(TradeWithVillagerEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getAbstractVillager() instanceof Villager villager) {
            SettlementManager.get(player.serverLevel()).onPlayerTrade(player, villager);
        }
    }

    // ------------------------------------------------------------------ data / server

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new SkillTree.Loader());
        event.addListener(new SpellConfigLoader());
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        SyncSkillTreePacket packet = new SyncSkillTreePacket(new ArrayList<>(SkillTree.server().getNodes()));
        if (event.getPlayer() != null) {
            ModNetwork.sendToPlayer(event.getPlayer(), packet);
        } else {
            for (ServerPlayer player : event.getPlayerList().getPlayers()) {
                ModNetwork.sendToPlayer(player, packet);
            }
        }
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        RpgCommands.register(event.getDispatcher(), event.getBuildContext());
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        BossManager.clearAll();
    }
}
