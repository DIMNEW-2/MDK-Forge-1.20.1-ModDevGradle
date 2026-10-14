package com.closky.rpgmod.registry;

import com.closky.rpgmod.RpgMod;
import com.closky.rpgmod.menu.LootChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, RpgMod.MODID);

    public static final RegistryObject<MenuType<LootChestMenu>> LOOT_CHEST = MENUS.register("loot_chest",
            () -> IForgeMenuType.create(LootChestMenu::new));

    private ModMenus() {
    }
}
