package com.closky.rpgmod.registry;

import com.closky.rpgmod.RpgMod;
import com.closky.rpgmod.magic.Spell;
import com.closky.rpgmod.magic.SpellScrollItem;
import com.closky.rpgmod.weapon.WeaponTier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import java.util.Map;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, RpgMod.MODID);

    public static final RegistryObject<CreativeModeTab> WEAPONS = TABS.register("weapons", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.rpgmod.weapons"))
            .icon(() -> new ItemStack(ModItems.SWORDS.get(WeaponTier.RUNE).get()))
            .displayItems((params, output) -> {
                ModItems.allWeapons().forEach(ro -> output.accept(ro.get()));
                ModItems.BOMBS.values().forEach(ro -> output.accept(ro.get()));
                ModItems.ARROWS.values().forEach(ro -> output.accept(ro.get()));
                ModItems.BOLTS.values().forEach(ro -> output.accept(ro.get()));
                output.accept(ModItems.FORGING_TEMPLATE.get());
            }).build());

    public static final RegistryObject<CreativeModeTab> MAGIC = TABS.register("magic", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.rpgmod.magic"))
            .icon(() -> new ItemStack(ModItems.STAFF_FIRE.get()))
            .withTabsBefore(WEAPONS.getId())
            .displayItems((params, output) -> {
                ModItems.STAFFS.values().forEach(ro -> output.accept(ro.get()));
                for (Map.Entry<com.closky.rpgmod.magic.SpellSchool, RegistryObject<Item>> entry : ModItems.SCROLLS.entrySet()) {
                    for (Spell spell : ModSpells.bySchool(entry.getKey())) {
                        output.accept(SpellScrollItem.withSpell(new ItemStack(entry.getValue().get()), spell));
                    }
                }
                output.accept(ModItems.MANA_POTION.get());
                output.accept(ModItems.SKILL_BOOK.get());
            }).build());

    public static final RegistryObject<CreativeModeTab> ARMOR = TABS.register("armor", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.rpgmod.armor"))
            .icon(() -> new ItemStack(ModItems.armor(com.closky.rpgmod.player.PlayerClass.WARRIOR, net.minecraft.world.item.ArmorItem.Type.CHESTPLATE)))
            .withTabsBefore(MAGIC.getId())
            .displayItems((params, output) -> ModItems.ARMOR.values().forEach(pieces -> pieces.values().forEach(ro -> output.accept(ro.get())))).build());

    public static final RegistryObject<CreativeModeTab> WORLD = TABS.register("world", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.rpgmod.world"))
            .icon(() -> new ItemStack(ModItems.LOOT_CHEST_BOSS.get()))
            .withTabsBefore(ARMOR.getId())
            .displayItems((params, output) -> {
                output.accept(ModItems.LOOT_CHEST.get());
                output.accept(ModItems.LOOT_CHEST_RARE.get());
                output.accept(ModItems.LOOT_CHEST_BOSS.get());
                output.accept(ModItems.BOSS_SPAWNER.get());
                output.accept(ModItems.RELATIONS_MAP.get());
            }).build());

    private ModCreativeTabs() {
    }
}
