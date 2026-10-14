package com.closky.rpgmod.crafting;

import com.closky.rpgmod.loot.RpgItemData;
import com.closky.rpgmod.loot.WeaponMaterial;
import com.closky.rpgmod.registry.ModItems;
import com.closky.rpgmod.registry.ModRecipes;
import com.closky.rpgmod.weapon.IRpgWeapon;
import com.google.gson.JsonObject;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.level.Level;

/**
 * Smithing-table recipe "forging" (section 2.5): template + RPG weapon + material ingredient → the same weapon
 * with the material applied (NBT). Weapons themselves are never craftable; forging only modifies loot.
 */
public class ForgingRecipe implements SmithingRecipe {
    private final ResourceLocation id;

    public ForgingRecipe(ResourceLocation id) {
        this.id = id;
    }

    @Override
    public boolean isTemplateIngredient(ItemStack stack) {
        return stack.is(ModItems.FORGING_TEMPLATE.get());
    }

    @Override
    public boolean isBaseIngredient(ItemStack stack) {
        return stack.getItem() instanceof IRpgWeapon weapon && weapon.isForgeable();
    }

    @Override
    public boolean isAdditionIngredient(ItemStack stack) {
        return WeaponMaterial.byIngredient(stack.getItem()) != null;
    }

    @Override
    public boolean matches(Container container, Level level) {
        ItemStack base = container.getItem(1);
        ItemStack addition = container.getItem(2);
        if (!isTemplateIngredient(container.getItem(0)) || !isBaseIngredient(base) || !isAdditionIngredient(addition)) {
            return false;
        }
        WeaponMaterial material = WeaponMaterial.byIngredient(addition.getItem());
        return material != null && RpgItemData.getMaterial(base) != material;
    }

    @Override
    public ItemStack assemble(Container container, RegistryAccess access) {
        ItemStack base = container.getItem(1);
        ItemStack result = base.copy();
        result.setCount(1);
        WeaponMaterial material = WeaponMaterial.byIngredient(container.getItem(2).getItem());
        if (material == null) {
            return ItemStack.EMPTY;
        }
        RpgItemData.setMaterial(result, material);
        // Forging improves the item level slightly, mirroring the material's power.
        int level = RpgItemData.getItemLevel(base);
        RpgItemData.setItemLevel(result, level + material.getItemLevelBonus());
        return result;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess access) {
        return ItemStack.EMPTY;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.FORGING.get();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    public static class Serializer implements RecipeSerializer<ForgingRecipe> {
        @Override
        public ForgingRecipe fromJson(ResourceLocation id, JsonObject json) {
            return new ForgingRecipe(id);
        }

        @Override
        public ForgingRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            return new ForgingRecipe(id);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, ForgingRecipe recipe) {
        }
    }
}
