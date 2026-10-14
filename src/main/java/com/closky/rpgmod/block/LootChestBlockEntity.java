package com.closky.rpgmod.block;

import com.closky.rpgmod.RpgMod;
import com.closky.rpgmod.capability.PlayerRpgData;
import com.closky.rpgmod.config.RpgConfig;
import com.closky.rpgmod.enemy.MobLevel;
import com.closky.rpgmod.loot.LootItemHelper;
import com.closky.rpgmod.menu.LootChestMenu;
import com.closky.rpgmod.network.ModNetwork;
import com.closky.rpgmod.network.packet.CasinoResultPacket;
import com.closky.rpgmod.player.CharacterLevel;
import com.closky.rpgmod.registry.ModBlockEntities;
import com.closky.rpgmod.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Block entity of the loot chest. Holds the loot table reference, the rolled (but not yet claimed) rewards and the
 * player entitled to them. All randomness happens on the server; the client only animates.
 */
public class LootChestBlockEntity extends BlockEntity implements MenuProvider {
    public static final int MAX_REWARDS = 9;
    public static final int DECOY_COUNT = 16;

    @Nullable
    private ResourceLocation lootTable;
    private long lootSeed;
    private int zoneLevel;
    private boolean rolled;
    private boolean claimed;
    private boolean jackpot;
    @Nullable
    private UUID pendingPlayer;
    private long resultTick;
    private final NonNullList<ItemStack> rewards = NonNullList.withSize(MAX_REWARDS, ItemStack.EMPTY);
    private final List<ItemStack> decoys = new ArrayList<>();

    public LootChestBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LOOT_CHEST.get(), pos, state);
    }

    public ChestTier getTier() {
        return LootChestBlock.tierOf(getBlockState());
    }

    public void setLootTable(ResourceLocation lootTable, long seed) {
        this.lootTable = lootTable;
        this.lootSeed = seed;
        setChanged();
    }

    public ResourceLocation getLootTableId() {
        return lootTable != null ? lootTable : getTier().getDefaultLootTable();
    }

    public int getZoneLevel() {
        return zoneLevel;
    }

    public void setZoneLevel(int zoneLevel) {
        this.zoneLevel = zoneLevel;
        setChanged();
    }

    public void setZoneLevelIfUnset(Level level, BlockPos pos) {
        if (zoneLevel <= 0) {
            setZoneLevel(Math.max(1, MobLevel.zoneLevel(level, pos) + getTier().getLevel()));
        }
    }

    public boolean isRolled() {
        return rolled;
    }

    public boolean isClaimed() {
        return claimed;
    }

    @Nullable
    public UUID getPendingPlayer() {
        return pendingPlayer;
    }

    public List<ItemStack> getRewards() {
        List<ItemStack> list = new ArrayList<>();
        for (ItemStack stack : rewards) {
            if (!stack.isEmpty()) {
                list.add(stack);
            }
        }
        return list;
    }

    // ------------------------------------------------------------------ casino

    /** Handles the "spin" request: rolls the loot (once) and sends the result to the player. */
    public void roll(ServerPlayer player) {
        if (!(this.level instanceof ServerLevel server) || claimed) {
            return;
        }
        if (rolled) {
            if (player.getUUID().equals(pendingPlayer)) {
                sendResult(player);
            } else {
                player.displayClientMessage(Component.translatable("message.rpgmod.chest_taken").withStyle(ChatFormatting.RED), true);
            }
            return;
        }
        setZoneLevelIfUnset(server, worldPosition);
        RandomSource random = lootSeed == 0L ? server.getRandom() : RandomSource.create(lootSeed ^ worldPosition.asLong());
        LootTable table = server.getServer().getLootData().getLootTable(getLootTableId());
        LootParams params = new LootParams.Builder(server)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(worldPosition))
                .withOptionalParameter(LootContextParams.THIS_ENTITY, player)
                .withLuck(player.getLuck())
                .create(LootContextParamSets.CHEST);
        List<ItemStack> generated = new ArrayList<>(table.getRandomItems(params));
        jackpot = random.nextFloat() < getTier().getJackpotChance() + player.getLuck() * 0.01F;
        if (jackpot) {
            LootTable jackpotTable = server.getServer().getLootData().getLootTable(RpgMod.id("chests/jackpot"));
            generated.addAll(jackpotTable.getRandomItems(params));
        }
        if (generated.isEmpty()) {
            generated.add(new ItemStack(Items.EMERALD, 1 + random.nextInt(3)));
        }
        for (int i = 0; i < rewards.size(); i++) {
            rewards.set(i, ItemStack.EMPTY);
        }
        int count = 0;
        for (ItemStack stack : generated) {
            if (count >= MAX_REWARDS) {
                break;
            }
            LootItemHelper.finalizeLoot(stack, zoneLevel, random);
            rewards.set(count++, stack);
        }
        // decoys: extra rolls shown on the reels but never awarded
        decoys.clear();
        for (int i = 0; i < 3 && decoys.size() < DECOY_COUNT; i++) {
            for (ItemStack stack : table.getRandomItems(params)) {
                LootItemHelper.finalizeLoot(stack, zoneLevel, random);
                decoys.add(stack);
                if (decoys.size() >= DECOY_COUNT) {
                    break;
                }
            }
        }
        while (decoys.size() < DECOY_COUNT) {
            decoys.add(new ItemStack(LootItemHelper.randomFiller(random)));
        }
        rolled = true;
        pendingPlayer = player.getUUID();
        resultTick = server.getGameTime();
        setChanged();
        sendResult(player);
        server.playSound(null, worldPosition, ModSounds.CASINO_SPIN.get(), SoundSource.BLOCKS, 0.8F, 1.0F);
    }

    private void sendResult(ServerPlayer player) {
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new CasinoResultPacket(worldPosition, getRewards(), new ArrayList<>(decoys), jackpot, zoneLevel));
    }

    /** Handles the "claim" request: gives the rewards and removes the chest. */
    public void claim(ServerPlayer player) {
        if (!(this.level instanceof ServerLevel server) || !rolled || claimed || !player.getUUID().equals(pendingPlayer)) {
            return;
        }
        long minDelay = RpgConfig.COMMON.casinoMinClaimDelayTicks.get();
        if (server.getGameTime() - resultTick < minDelay) {
            return;
        }
        for (ItemStack stack : getRewards()) {
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
        }
        PlayerRpgData data = PlayerRpgData.get(player);
        if (data != null) {
            data.addXp(player, CharacterLevel.xpForChest(getTier().getLevel()));
            data.sync(player);
        }
        claimed = true;
        setChanged();
        player.closeContainer();
        server.playSound(null, worldPosition, jackpot ? ModSounds.CASINO_JACKPOT.get() : ModSounds.CASINO_WIN.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        server.sendParticles(ParticleTypes.POOF, worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D, 20, 0.3D, 0.3D, 0.3D, 0.02D);
        server.removeBlock(worldPosition, false);
    }

    // ------------------------------------------------------------------ menu

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.rpgmod.loot_chest." + getTier().getSerializedName());
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new LootChestMenu(id, inventory, worldPosition, getTier(), zoneLevel);
    }

    // ------------------------------------------------------------------ nbt

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (lootTable != null) {
            tag.putString("LootTable", lootTable.toString());
            tag.putLong("LootTableSeed", lootSeed);
        }
        tag.putInt("ZoneLevel", zoneLevel);
        tag.putBoolean("Rolled", rolled);
        tag.putBoolean("Claimed", claimed);
        tag.putBoolean("Jackpot", jackpot);
        if (pendingPlayer != null) {
            tag.putUUID("PendingPlayer", pendingPlayer);
        }
        tag.putLong("ResultTick", resultTick);
        ContainerHelper.saveAllItems(tag, rewards);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        lootTable = tag.contains("LootTable") ? ResourceLocation.tryParse(tag.getString("LootTable")) : null;
        lootSeed = tag.getLong("LootTableSeed");
        zoneLevel = tag.getInt("ZoneLevel");
        rolled = tag.getBoolean("Rolled");
        claimed = tag.getBoolean("Claimed");
        jackpot = tag.getBoolean("Jackpot");
        pendingPlayer = tag.hasUUID("PendingPlayer") ? tag.getUUID("PendingPlayer") : null;
        resultTick = tag.getLong("ResultTick");
        rewards.clear();
        ContainerHelper.loadAllItems(tag, rewards);
    }
}
