package com.closky.rpgmod.registry;

import com.closky.rpgmod.RpgMod;
import com.closky.rpgmod.crafting.ForgingRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, RpgMod.MODID);

    public static final RegistryObject<RecipeSerializer<ForgingRecipe>> FORGING = RECIPE_SERIALIZERS.register("forging", ForgingRecipe.Serializer::new);

    private ModRecipes() {
    }
}
