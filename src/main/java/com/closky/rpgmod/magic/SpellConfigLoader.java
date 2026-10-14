package com.closky.rpgmod.magic;

import com.closky.rpgmod.RpgMod;
import com.closky.rpgmod.registry.ModSpells;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

/**
 * Data-driven spell tuning: {@code data/<namespace>/spells/<spell>.json} overrides the numbers of the registered
 * spell with the same id (see {@link Spell#applyOverrides}). Shipped defaults live in {@code data/rpgmod/spells}.
 */
public class SpellConfigLoader extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().create();

    public SpellConfigLoader() {
        super(GSON, "spells");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsons, ResourceManager manager, ProfilerFiller profiler) {
        int applied = 0;
        for (Map.Entry<ResourceLocation, JsonElement> entry : jsons.entrySet()) {
            Spell spell = ModSpells.REGISTRY.get().getValue(entry.getKey());
            if (spell == null) {
                RpgMod.LOGGER.warn("Spell override for unknown spell {}", entry.getKey());
                continue;
            }
            if (entry.getValue().isJsonObject()) {
                spell.applyOverrides(entry.getValue().getAsJsonObject());
                applied++;
            }
        }
        RpgMod.LOGGER.info("Applied {} spell overrides", applied);
    }
}
