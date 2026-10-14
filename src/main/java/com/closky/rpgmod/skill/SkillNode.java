package com.closky.rpgmod.skill;

import com.closky.rpgmod.player.PlayerClass;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * A node of the skill tree (section 19). Loaded from {@code data/rpgmod/skill_tree/<path>.json}; the file path
 * (without extension) becomes the node id.
 */
public record SkillNode(ResourceLocation id, SkillBranch branch, int x, int y, int cost, int requiredLevel,
                        List<ResourceLocation> parents, ResourceLocation icon, List<SkillEffect> effects,
                        PlayerClass requiredClass) {

    public String translationKey() {
        return "skill." + id.getNamespace() + "." + id.getPath().replace('/', '.');
    }

    public Component getTitle() {
        return Component.translatable(translationKey());
    }

    public Component getDescription() {
        return Component.translatable(translationKey() + ".desc");
    }

    public ItemStack getIconStack() {
        Item item = ForgeRegistries.ITEMS.getValue(icon);
        return new ItemStack(item == null || item == Items.AIR ? Items.BOOK : item);
    }

    public static SkillNode fromJson(ResourceLocation id, JsonObject json) {
        SkillBranch branch = SkillBranch.byKey(GsonHelper.getAsString(json, "branch"));
        int x = GsonHelper.getAsInt(json, "x");
        int y = GsonHelper.getAsInt(json, "y");
        int cost = GsonHelper.getAsInt(json, "cost", 1);
        int level = GsonHelper.getAsInt(json, "required_level", 1);
        List<ResourceLocation> parents = new ArrayList<>();
        if (json.has("parents")) {
            for (JsonElement e : GsonHelper.getAsJsonArray(json, "parents")) {
                parents.add(new ResourceLocation(e.getAsString()));
            }
        }
        ResourceLocation icon = new ResourceLocation(GsonHelper.getAsString(json, "icon", "minecraft:book"));
        List<SkillEffect> effects = new ArrayList<>();
        if (json.has("effects")) {
            for (JsonElement e : GsonHelper.getAsJsonArray(json, "effects")) {
                effects.add(SkillEffect.fromJson(e.getAsJsonObject()));
            }
        }
        PlayerClass requiredClass = PlayerClass.byKey(GsonHelper.getAsString(json, "required_class", "none"));
        return new SkillNode(id, branch, x, y, cost, level, List.copyOf(parents), icon, List.copyOf(effects), requiredClass);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("branch", branch.getKey());
        json.addProperty("x", x);
        json.addProperty("y", y);
        json.addProperty("cost", cost);
        json.addProperty("required_level", requiredLevel);
        JsonArray parentsJson = new JsonArray();
        parents.forEach(p -> parentsJson.add(p.toString()));
        json.add("parents", parentsJson);
        json.addProperty("icon", icon.toString());
        JsonArray effectsJson = new JsonArray();
        effects.forEach(e -> effectsJson.add(e.toJson()));
        json.add("effects", effectsJson);
        if (requiredClass != PlayerClass.NONE) {
            json.addProperty("required_class", requiredClass.getKey());
        }
        return json;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeResourceLocation(id);
        buf.writeEnum(branch);
        buf.writeVarInt(x);
        buf.writeVarInt(y);
        buf.writeVarInt(cost);
        buf.writeVarInt(requiredLevel);
        buf.writeCollection(parents, FriendlyByteBuf::writeResourceLocation);
        buf.writeResourceLocation(icon);
        buf.writeCollection(effects, (b, e) -> e.write(b));
        buf.writeEnum(requiredClass);
    }

    public static SkillNode read(FriendlyByteBuf buf) {
        ResourceLocation id = buf.readResourceLocation();
        SkillBranch branch = buf.readEnum(SkillBranch.class);
        int x = buf.readVarInt();
        int y = buf.readVarInt();
        int cost = buf.readVarInt();
        int level = buf.readVarInt();
        List<ResourceLocation> parents = buf.readList(FriendlyByteBuf::readResourceLocation);
        ResourceLocation icon = buf.readResourceLocation();
        List<SkillEffect> effects = buf.readList(SkillEffect::read);
        PlayerClass requiredClass = buf.readEnum(PlayerClass.class);
        return new SkillNode(id, branch, x, y, cost, level, parents, icon, effects, requiredClass);
    }
}
