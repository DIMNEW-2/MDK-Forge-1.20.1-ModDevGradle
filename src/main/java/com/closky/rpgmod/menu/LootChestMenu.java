package com.closky.rpgmod.menu;

import com.closky.rpgmod.block.ChestTier;
import com.closky.rpgmod.block.LootChestBlock;
import com.closky.rpgmod.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/**
 * Menu backing the casino screen. It has no slots: items are only shown client-side from packets and granted by the
 * server on claim. The menu exists so the screen is tied to a block, closes properly and works in multiplayer.
 */
public class LootChestMenu extends AbstractContainerMenu {
    private final BlockPos pos;
    private final ChestTier tier;
    private final int zoneLevel;

    public LootChestMenu(int id, Inventory inventory, FriendlyByteBuf buf) {
        this(id, inventory, buf.readBlockPos(), buf.readEnum(ChestTier.class), buf.readVarInt());
    }

    public LootChestMenu(int id, Inventory inventory, BlockPos pos, ChestTier tier, int zoneLevel) {
        super(ModMenus.LOOT_CHEST.get(), id);
        this.pos = pos;
        this.tier = tier;
        this.zoneLevel = zoneLevel;
    }

    public BlockPos getPos() {
        return pos;
    }

    public ChestTier getTier() {
        return tier;
    }

    public int getZoneLevel() {
        return zoneLevel;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        if (!(player.level().getBlockState(pos).getBlock() instanceof LootChestBlock)) {
            return false;
        }
        return player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D;
    }
}
