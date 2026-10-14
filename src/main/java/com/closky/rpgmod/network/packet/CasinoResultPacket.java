package com.closky.rpgmod.network.packet;

import com.closky.rpgmod.client.ClientPacketHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

/** Server → client: the rolled rewards (and decoy items for the reels) of a loot chest. */
public class CasinoResultPacket {
    private final BlockPos pos;
    private final List<ItemStack> rewards;
    private final List<ItemStack> decoys;
    private final boolean jackpot;
    private final int zoneLevel;

    public CasinoResultPacket(BlockPos pos, List<ItemStack> rewards, List<ItemStack> decoys, boolean jackpot, int zoneLevel) {
        this.pos = pos;
        this.rewards = rewards;
        this.decoys = decoys;
        this.jackpot = jackpot;
        this.zoneLevel = zoneLevel;
    }

    public BlockPos getPos() {
        return pos;
    }

    public List<ItemStack> getRewards() {
        return rewards;
    }

    public List<ItemStack> getDecoys() {
        return decoys;
    }

    public boolean isJackpot() {
        return jackpot;
    }

    public int getZoneLevel() {
        return zoneLevel;
    }

    public static void encode(CasinoResultPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeCollection(msg.rewards, FriendlyByteBuf::writeItem);
        buf.writeCollection(msg.decoys, FriendlyByteBuf::writeItem);
        buf.writeBoolean(msg.jackpot);
        buf.writeVarInt(msg.zoneLevel);
    }

    public static CasinoResultPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        List<ItemStack> rewards = buf.readList(FriendlyByteBuf::readItem);
        List<ItemStack> decoys = buf.readList(FriendlyByteBuf::readItem);
        return new CasinoResultPacket(pos, rewards, decoys, buf.readBoolean(), buf.readVarInt());
    }

    public static void handle(CasinoResultPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleCasinoResult(msg)));
        ctx.get().setPacketHandled(true);
    }
}
