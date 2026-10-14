package com.closky.rpgmod.registry;

import com.closky.rpgmod.RpgMod;
import com.closky.rpgmod.armor.RpgArmorItem;
import com.closky.rpgmod.armor.RpgArmorMaterial;
import com.closky.rpgmod.crafting.ForgingTemplateItem;
import com.closky.rpgmod.item.RelationsMapItem;
import com.closky.rpgmod.item.SkillBookItem;
import com.closky.rpgmod.magic.ManaPotionItem;
import com.closky.rpgmod.magic.SpellSchool;
import com.closky.rpgmod.magic.SpellScrollItem;
import com.closky.rpgmod.magic.StaffItem;
import com.closky.rpgmod.player.PlayerClass;
import com.closky.rpgmod.weapon.BombType;
import com.closky.rpgmod.weapon.ProjectileEffect;
import com.closky.rpgmod.weapon.RpgArrowItem;
import com.closky.rpgmod.weapon.RpgBoltItem;
import com.closky.rpgmod.weapon.RpgBombItem;
import com.closky.rpgmod.weapon.RpgBowItem;
import com.closky.rpgmod.weapon.RpgCrossbowItem;
import com.closky.rpgmod.weapon.RpgMaceItem;
import com.closky.rpgmod.weapon.RpgSpearItem;
import com.closky.rpgmod.weapon.RpgSwordItem;
import com.closky.rpgmod.weapon.WeaponTier;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * All items. Weapons exist in seven tiers each (wood … rune); ids follow the plan:
 * {@code sword_<tier>_rpg}, {@code bow_<tier>_rpg}, {@code bomb_<type>}, {@code arrow_<effect>}, {@code bolt_<effect>},
 * {@code staff_<school>}, {@code scroll_<school>}, {@code <class>_<piece>}.
 */
public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, RpgMod.MODID);

    public static final Map<WeaponTier, RegistryObject<Item>> SWORDS = weaponSet("sword", RpgSwordItem::new);
    public static final Map<WeaponTier, RegistryObject<Item>> MACES = weaponSet("mace", RpgMaceItem::new);
    public static final Map<WeaponTier, RegistryObject<Item>> SPEARS = weaponSet("spear", RpgSpearItem::new);
    public static final Map<WeaponTier, RegistryObject<Item>> BOWS = weaponSet("bow", (tier, props) -> new RpgBowItem(tier, props.durability(tier.getUses())));
    public static final Map<WeaponTier, RegistryObject<Item>> CROSSBOWS = weaponSet("crossbow", (tier, props) -> new RpgCrossbowItem(tier, props.durability(tier.getUses())));

    public static final RegistryObject<Item> SWORD_IRON = SWORDS.get(WeaponTier.IRON);
    public static final RegistryObject<Item> SWORD_DIAMOND = SWORDS.get(WeaponTier.DIAMOND);
    public static final RegistryObject<Item> MACE_IRON = MACES.get(WeaponTier.IRON);
    public static final RegistryObject<Item> MACE_DIAMOND = MACES.get(WeaponTier.DIAMOND);
    public static final RegistryObject<Item> SPEAR_IRON = SPEARS.get(WeaponTier.IRON);
    public static final RegistryObject<Item> BOW_IRON = BOWS.get(WeaponTier.IRON);
    public static final RegistryObject<Item> CROSSBOW_IRON = CROSSBOWS.get(WeaponTier.IRON);

    public static final Map<BombType, RegistryObject<Item>> BOMBS = enumSet(BombType.values(), BombType::getKey, "bomb_",
            type -> new RpgBombItem(type, new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> BOMB_BASIC = BOMBS.get(BombType.BASIC);

    public static final Map<ProjectileEffect, RegistryObject<Item>> ARROWS = new EnumMap<>(ProjectileEffect.class);
    public static final Map<ProjectileEffect, RegistryObject<Item>> BOLTS = new EnumMap<>(ProjectileEffect.class);

    static {
        for (ProjectileEffect effect : ProjectileEffect.values()) {
            if (effect.hasArrow()) {
                ARROWS.put(effect, ITEMS.register("arrow_" + effect.getKey(), () -> new RpgArrowItem(effect, new Item.Properties())));
            }
            if (effect.hasBolt()) {
                BOLTS.put(effect, ITEMS.register("bolt_" + effect.getKey(), () -> new RpgBoltItem(effect, new Item.Properties())));
            }
        }
    }

    public static final Map<SpellSchool, RegistryObject<Item>> STAFFS = enumSet(SpellSchool.values(), SpellSchool::getKey, "staff_",
            school -> new StaffItem(school, WeaponTier.IRON, 0.20F, new Item.Properties().durability(WeaponTier.IRON.getUses() * 2).rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> STAFF_FIRE = STAFFS.get(SpellSchool.FIRE);
    public static final RegistryObject<Item> STAFF_FROST = STAFFS.get(SpellSchool.FROST);
    public static final RegistryObject<Item> STAFF_LIGHTNING = STAFFS.get(SpellSchool.LIGHTNING);
    public static final RegistryObject<Item> STAFF_NATURE = STAFFS.get(SpellSchool.NATURE);

    public static final Map<SpellSchool, RegistryObject<Item>> SCROLLS = enumSet(SpellSchool.values(), SpellSchool::getKey, "scroll_",
            school -> new SpellScrollItem(school, new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));

    public static final Map<PlayerClass, Map<ArmorItem.Type, RegistryObject<Item>>> ARMOR = new EnumMap<>(PlayerClass.class);

    static {
        for (RpgArmorMaterial material : RpgArmorMaterial.values()) {
            Map<ArmorItem.Type, RegistryObject<Item>> pieces = new EnumMap<>(ArmorItem.Type.class);
            for (ArmorItem.Type type : ArmorItem.Type.values()) {
                String name = material.getPlayerClass().getKey() + "_" + type.getName();
                pieces.put(type, ITEMS.register(name, () -> new RpgArmorItem(material, type, new Item.Properties().rarity(Rarity.RARE))));
            }
            ARMOR.put(material.getPlayerClass(), Collections.unmodifiableMap(pieces));
        }
    }

    public static final RegistryObject<Item> FORGING_TEMPLATE = ITEMS.register("forging_template",
            () -> new ForgingTemplateItem(new Item.Properties().rarity(Rarity.RARE)));
    public static final RegistryObject<Item> MANA_POTION = ITEMS.register("mana_potion",
            () -> new ManaPotionItem(25.0F, new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> RELATIONS_MAP = ITEMS.register("relations_map",
            () -> new RelationsMapItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> SKILL_BOOK = ITEMS.register("skill_book",
            () -> new SkillBookItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> LOOT_CHEST = ITEMS.register("loot_chest",
            () -> new BlockItem(ModBlocks.LOOT_CHEST.get(), new Item.Properties()));
    public static final RegistryObject<Item> LOOT_CHEST_RARE = ITEMS.register("loot_chest_rare",
            () -> new BlockItem(ModBlocks.LOOT_CHEST_RARE.get(), new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> LOOT_CHEST_BOSS = ITEMS.register("loot_chest_boss",
            () -> new BlockItem(ModBlocks.LOOT_CHEST_BOSS.get(), new Item.Properties().rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> BOSS_SPAWNER = ITEMS.register("boss_spawner",
            () -> new BlockItem(ModBlocks.BOSS_SPAWNER.get(), new Item.Properties().rarity(Rarity.EPIC)));

    private ModItems() {
    }

    // ------------------------------------------------------------------ helpers

    private static Map<WeaponTier, RegistryObject<Item>> weaponSet(String prefix, BiFunction<WeaponTier, Item.Properties, Item> factory) {
        Map<WeaponTier, RegistryObject<Item>> map = new EnumMap<>(WeaponTier.class);
        for (WeaponTier tier : WeaponTier.values()) {
            map.put(tier, ITEMS.register(prefix + "_" + tier.getKey() + "_rpg", () -> {
                Item.Properties props = new Item.Properties();
                if (tier == WeaponTier.NETHERITE || tier == WeaponTier.RUNE) {
                    props.fireResistant();
                }
                return factory.apply(tier, props);
            }));
        }
        return Collections.unmodifiableMap(map);
    }

    private static <E extends Enum<E>> Map<E, RegistryObject<Item>> enumSet(E[] values, Function<E, String> key, String prefix, Function<E, Item> factory) {
        Map<E, RegistryObject<Item>> map = new EnumMap<>(values[0].getDeclaringClass());
        for (E value : values) {
            map.put(value, ITEMS.register(prefix + key.apply(value), () -> factory.apply(value)));
        }
        return Collections.unmodifiableMap(map);
    }

    public static Item arrowFor(ProjectileEffect effect) {
        RegistryObject<Item> item = ARROWS.get(effect);
        return item != null ? item.get() : ARROWS.get(ProjectileEffect.BASIC).get();
    }

    public static Item boltFor(ProjectileEffect effect) {
        RegistryObject<Item> item = BOLTS.get(effect);
        return item != null ? item.get() : BOLTS.get(ProjectileEffect.BASIC).get();
    }

    public static Item armor(PlayerClass playerClass, ArmorItem.Type type) {
        return ARMOR.get(playerClass).get(type).get();
    }

    /** Every weapon registry object (swords, maces, spears, bows, crossbows), in tier order. */
    public static List<RegistryObject<Item>> allWeapons() {
        List<RegistryObject<Item>> list = new ArrayList<>();
        for (WeaponTier tier : WeaponTier.values()) {
            list.add(SWORDS.get(tier));
            list.add(MACES.get(tier));
            list.add(SPEARS.get(tier));
            list.add(BOWS.get(tier));
            list.add(CROSSBOWS.get(tier));
        }
        return list;
    }
}
