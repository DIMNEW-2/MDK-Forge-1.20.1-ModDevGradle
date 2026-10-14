package com.closky.rpgmod.item;

import com.closky.rpgmod.network.ModNetwork;
import com.closky.rpgmod.network.packet.SyncSettlementsPacket;
import com.closky.rpgmod.settlement.SettlementManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * "Map of relations" (section 17.6): right click asks the server for a settlement snapshot and opens the
 * relations map screen on the client once it arrives.
 */
public class RelationsMapItem extends Item {
    public RelationsMapItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            SettlementManager manager = SettlementManager.get((ServerLevel) level);
            ModNetwork.sendToPlayer(serverPlayer, new SyncSettlementsPacket(manager.snapshot(serverPlayer), true));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.rpgmod.relations_map").withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
