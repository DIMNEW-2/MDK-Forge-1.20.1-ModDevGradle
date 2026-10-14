package com.closky.rpgmod.client;

import com.closky.rpgmod.RpgMod;
import com.closky.rpgmod.config.RpgConfig;
import com.closky.rpgmod.network.ModNetwork;
import com.closky.rpgmod.network.packet.CastSpellPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Forge-bus client events: key handling and mob level nameplates. */
@Mod.EventBusSubscriber(modid = RpgMod.MODID, value = Dist.CLIENT)
public final class ClientEvents {
    private ClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) {
            return;
        }
        while (KeyBindings.OPEN_SKILLS.consumeClick()) {
            ClientHooks.openSkillTree();
        }
        while (KeyBindings.OPEN_CHARACTER.consumeClick()) {
            ClientHooks.openCharacter();
        }
        while (KeyBindings.OPEN_RELATIONS.consumeClick()) {
            ClientHooks.openRelations();
        }
        while (KeyBindings.CAST_SPELL.consumeClick()) {
            ModNetwork.sendToServer(new CastSpellPacket());
        }
        while (KeyBindings.SELECT_SPELL.consumeClick()) {
            ClientHooks.openSpellSelect();
        }
    }

    @SubscribeEvent
    public static void onKey(InputEvent.Key event) {
        // key mappings are polled in the tick handler; this hook only exists to keep the event wiring explicit
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientData.clearMobs();
    }

    /** Appends "[Lv. N]" (and a boss title) to the nameplate of levelled mobs. */
    @SubscribeEvent
    public static void onRenderNameTag(RenderNameTagEvent event) {
        if (!RpgConfig.CLIENT.showMobNameplates.get()) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity living) || living instanceof Player) {
            return;
        }
        ClientData.MobInfo info = ClientData.getMobInfo(living.getId());
        if (info == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        double distSq = mc.player.distanceToSqr(living);
        double range = RpgConfig.CLIENT.nameplateRange.get();
        if (distSq > range * range) {
            return;
        }
        MutableComponent name = Component.empty();
        if (info.boss()) {
            name.append(Component.translatable("boss.rpgmod." + info.title()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        } else {
            name.append(living.getDisplayName().copy());
        }
        name.append(Component.literal(" "));
        name.append(Component.translatable("tooltip.rpgmod.mob_level", info.level()).withStyle(levelColor(info.level())));
        event.setContent(name);
        event.setResult(Event.Result.ALLOW);
    }

    private static ChatFormatting levelColor(int mobLevel) {
        Minecraft mc = Minecraft.getInstance();
        int playerLevel = 1;
        if (mc.player != null && ClientData.playerData() != null) {
            playerLevel = ClientData.playerData().getLevel();
        }
        int diff = mobLevel - playerLevel;
        if (diff >= 10) {
            return ChatFormatting.DARK_RED;
        }
        if (diff >= 5) {
            return ChatFormatting.RED;
        }
        if (diff >= 0) {
            return ChatFormatting.YELLOW;
        }
        if (diff >= -5) {
            return ChatFormatting.GREEN;
        }
        return ChatFormatting.GRAY;
    }
}
