package com.closky.rpgmod.client.screen;

import com.closky.rpgmod.RpgMod;
import com.closky.rpgmod.config.RpgConfig;
import com.closky.rpgmod.menu.LootChestMenu;
import com.closky.rpgmod.network.ModNetwork;
import com.closky.rpgmod.network.packet.CasinoClaimPacket;
import com.closky.rpgmod.network.packet.CasinoOpenPacket;
import com.closky.rpgmod.network.packet.CasinoResultPacket;
import com.closky.rpgmod.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Slot-machine loot chest screen (section 9). The server rolls the rewards once; the client only animates
 * three reels with decoy items and finally shows the real rewards. "Claim" asks the server to hand them out.
 */
public class CasinoScreen extends AbstractContainerScreen<LootChestMenu> {
    private static final ResourceLocation TEXTURE = RpgMod.id("textures/gui/casino.png");
    private static final int REELS = 3;
    private static final int REEL_X = 38;
    private static final int REEL_Y = 30;
    private static final int REEL_W = 32;
    private static final int REEL_H = 54;
    private static final int REEL_GAP = 36;
    private static final int STOP_BASE = 45;
    private static final int STOP_STEP = 25;

    private final Random random = new Random();
    private CasinoResultPacket result;
    private int ticksOpen;
    private int spinStartTick = -1;
    private boolean finished;
    private boolean claimed;
    private final List<List<ItemStack>> reelStrips = new ArrayList<>();
    private final float[] reelOffset = new float[REELS];
    private final boolean[] reelStopped = new boolean[REELS];
    private Button claimButton;
    private Button skipButton;

    public CasinoScreen(LootChestMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 190;
    }

    @Override
    protected void init() {
        super.init();
        claimButton = addRenderableWidget(Button.builder(Component.translatable("container.rpgmod.loot_chest.claim"), b -> claim())
                .bounds(leftPos + 18, topPos + 160, 68, 20).build());
        claimButton.active = false;
        skipButton = addRenderableWidget(Button.builder(Component.translatable("container.rpgmod.loot_chest.skip"), b -> skipAnimation())
                .bounds(leftPos + 90, topPos + 160, 68, 20).build());
        skipButton.active = false;
        ModNetwork.sendToServer(new CasinoOpenPacket(menu.getPos()));
    }

    public void onResult(CasinoResultPacket packet) {
        this.result = packet;
        reelStrips.clear();
        List<ItemStack> pool = new ArrayList<>(packet.getDecoys());
        if (pool.isEmpty()) {
            pool.add(new ItemStack(Items.EMERALD));
        }
        for (int reel = 0; reel < REELS; reel++) {
            List<ItemStack> strip = new ArrayList<>();
            int length = 12 + reel * 4;
            for (int i = 0; i < length - 1; i++) {
                strip.add(pool.get(random.nextInt(pool.size())));
            }
            strip.add(finalItem(reel));
            reelStrips.add(strip);
            reelOffset[reel] = 0.0F;
            reelStopped[reel] = false;
        }
        spinStartTick = ticksOpen;
        skipButton.active = true;
        if (!RpgConfig.CLIENT.enableCasinoAnimation.get()) {
            skipAnimation();
        } else {
            playSound(ModSounds.CASINO_SPIN.get(), 1.0F);
        }
    }

    private ItemStack finalItem(int reel) {
        List<ItemStack> rewards = result.getRewards();
        if (result.isJackpot() && !rewards.isEmpty()) {
            return rewards.get(0);
        }
        if (rewards.isEmpty()) {
            return new ItemStack(Items.COBWEB);
        }
        return rewards.get(reel % rewards.size());
    }

    @Override
    protected void containerTick() {
        ticksOpen++;
        if (result == null || finished) {
            updateClaimButton();
            return;
        }
        int elapsed = ticksOpen - spinStartTick;
        boolean allStopped = true;
        for (int reel = 0; reel < REELS; reel++) {
            int stopAt = STOP_BASE + reel * STOP_STEP;
            if (elapsed >= stopAt) {
                if (!reelStopped[reel]) {
                    reelStopped[reel] = true;
                    reelOffset[reel] = reelStrips.get(reel).size() - 1;
                    playSound(ModSounds.CASINO_STOP.get(), 1.0F);
                }
            } else {
                allStopped = false;
                // ease out towards the final item
                float progress = elapsed / (float) stopAt;
                float eased = 1.0F - (1.0F - progress) * (1.0F - progress);
                reelOffset[reel] = eased * (reelStrips.get(reel).size() - 1);
            }
        }
        if (allStopped) {
            finish();
        }
        updateClaimButton();
    }

    private void finish() {
        if (finished) {
            return;
        }
        finished = true;
        for (int reel = 0; reel < REELS; reel++) {
            reelStopped[reel] = true;
            reelOffset[reel] = reelStrips.get(reel).size() - 1;
        }
        skipButton.active = false;
        if (result != null) {
            playSound(result.isJackpot() ? ModSounds.CASINO_JACKPOT.get() : ModSounds.CASINO_WIN.get(), 1.0F);
        }
    }

    private void skipAnimation() {
        if (result != null) {
            finish();
        }
    }

    private void updateClaimButton() {
        int minDelay = RpgConfig.COMMON.casinoMinClaimDelayTicks.get();
        claimButton.active = result != null && finished && !claimed && ticksOpen >= minDelay;
    }

    private void claim() {
        if (claimed || result == null) {
            return;
        }
        claimed = true;
        claimButton.active = false;
        ModNetwork.sendToServer(new CasinoClaimPacket(menu.getPos()));
        onClose();
    }

    private void playSound(net.minecraft.sounds.SoundEvent sound, float pitch) {
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch));
        }
    }

    // ------------------------------------------------------------------ rendering

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (result != null && finished) {
            // tooltips for the reward row
            int rx = leftPos + 16;
            int ry = topPos + 118;
            for (int i = 0; i < result.getRewards().size() && i < 8; i++) {
                int x = rx + i * 18;
                if (mouseX >= x && mouseX < x + 16 && mouseY >= ry && mouseY < ry + 16) {
                    graphics.renderTooltip(font, result.getRewards().get(i), mouseX, mouseY);
                }
            }
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        // reels
        for (int reel = 0; reel < REELS; reel++) {
            int x = leftPos + REEL_X + reel * REEL_GAP;
            int y = topPos + REEL_Y;
            graphics.fill(x, y, x + REEL_W, y + REEL_H, 0xFF1B1B1B);
            if (result == null || reelStrips.size() <= reel) {
                continue;
            }
            List<ItemStack> strip = reelStrips.get(reel);
            float offset = reelOffset[reel];
            graphics.enableScissor(x, y, x + REEL_W, y + REEL_H);
            // draw the item at the offset and its neighbours above / below
            for (int k = -1; k <= 1; k++) {
                int index = Mth.floor(offset) + k;
                if (index < 0 || index >= strip.size()) {
                    continue;
                }
                float frac = offset - Mth.floor(offset);
                int iy = y + REEL_H / 2 - 8 + Math.round((k - frac) * 20.0F);
                graphics.renderItem(strip.get(index), x + REEL_W / 2 - 8, iy);
            }
            graphics.disableScissor();
            if (reelStopped[reel]) {
                int color = result.isJackpot() ? 0x80FFD700 : 0x4000FF00;
                graphics.fill(x, y, x + REEL_W, y + 2, color);
                graphics.fill(x, y + REEL_H - 2, x + REEL_W, y + REEL_H, color);
            }
        }
        // reward row
        if (result != null && finished) {
            int rx = leftPos + 16;
            int ry = topPos + 118;
            for (int i = 0; i < result.getRewards().size() && i < 8; i++) {
                ItemStack stack = result.getRewards().get(i);
                graphics.renderItem(stack, rx + i * 18, ry);
                graphics.renderItemDecorations(font, stack, rx + i * 18, ry);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        Component title = Component.translatable("container.rpgmod.loot_chest." + menu.getTier().getSerializedName())
                .withStyle(menu.getTier().getColor());
        graphics.drawString(font, title, imageWidth / 2 - font.width(title) / 2, 8, 0x404040, false);
        Component zone = Component.translatable("container.rpgmod.loot_chest.zone_level", menu.getZoneLevel());
        graphics.drawString(font, zone, imageWidth / 2 - font.width(zone) / 2, 18, 0x606060, false);
        Component status;
        if (result == null) {
            status = Component.translatable("container.rpgmod.loot_chest.rolling").withStyle(ChatFormatting.GRAY);
        } else if (!finished) {
            status = Component.translatable("container.rpgmod.loot_chest.spinning").withStyle(ChatFormatting.YELLOW);
        } else if (result.isJackpot()) {
            status = Component.translatable("container.rpgmod.loot_chest.jackpot").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
        } else if (result.getRewards().isEmpty()) {
            status = Component.translatable("container.rpgmod.loot_chest.nothing").withStyle(ChatFormatting.DARK_GRAY);
        } else {
            status = Component.translatable("container.rpgmod.loot_chest.rewards", result.getRewards().size()).withStyle(ChatFormatting.GREEN);
        }
        graphics.drawString(font, status, imageWidth / 2 - font.width(status) / 2, 100, 0xFFFFFF, true);
        if (result != null && finished && !claimButton.active) {
            int remaining = RpgConfig.COMMON.casinoMinClaimDelayTicks.get() - ticksOpen;
            if (remaining > 0) {
                Component wait = Component.translatable("container.rpgmod.loot_chest.wait", String.format("%.1f", remaining / 20.0F));
                graphics.drawString(font, wait, imageWidth / 2 - font.width(wait) / 2, 142, 0x808080, false);
            }
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 32 && finished && claimButton.active) { // space = claim
            claim();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
