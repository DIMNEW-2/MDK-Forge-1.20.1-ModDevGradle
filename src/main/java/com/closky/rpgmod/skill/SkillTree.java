package com.closky.rpgmod.skill;

import com.closky.rpgmod.RpgMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Holder of all skill nodes. The server loads them from data packs ({@link Loader}); clients receive a copy via
 * {@code SyncSkillTreePacket}. {@link #get()} returns whichever copy is relevant for the current side.
 */
public final class SkillTree {
    private static final Gson GSON = new GsonBuilder().create();
    private static SkillTree server = new SkillTree(Map.of());
    private static SkillTree client = new SkillTree(Map.of());

    private final Map<ResourceLocation, SkillNode> nodes;

    public SkillTree(Map<ResourceLocation, SkillNode> nodes) {
        this.nodes = Collections.unmodifiableMap(new LinkedHashMap<>(nodes));
    }

    public static SkillTree get() {
        return server.nodes.isEmpty() ? client : server;
    }

    public static SkillTree server() {
        return server;
    }

    public static void setClient(SkillTree tree) {
        client = tree;
    }

    public static void setServer(SkillTree tree) {
        server = tree;
    }

    @Nullable
    public SkillNode getNode(ResourceLocation id) {
        return nodes.get(id);
    }

    public Collection<SkillNode> getNodes() {
        return nodes.values();
    }

    public List<SkillNode> getNodes(SkillBranch branch) {
        List<SkillNode> list = new ArrayList<>();
        for (SkillNode node : nodes.values()) {
            if (node.branch() == branch) {
                list.add(node);
            }
        }
        return list;
    }

    public List<SkillNode> getMagicNodes() {
        List<SkillNode> list = new ArrayList<>();
        for (SkillNode node : nodes.values()) {
            if (node.branch().isMagic()) {
                list.add(node);
            }
        }
        return list;
    }

    public List<SkillNode> getSkillNodes() {
        List<SkillNode> list = new ArrayList<>();
        for (SkillNode node : nodes.values()) {
            if (!node.branch().isMagic()) {
                list.add(node);
            }
        }
        return list;
    }

    public int size() {
        return nodes.size();
    }

    /** Data pack loader for {@code data/<ns>/skill_tree/**.json}. */
    public static class Loader extends SimpleJsonResourceReloadListener {
        public Loader() {
            super(GSON, "skill_tree");
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> jsons, ResourceManager manager, ProfilerFiller profiler) {
            Map<ResourceLocation, SkillNode> parsed = new LinkedHashMap<>();
            for (Map.Entry<ResourceLocation, JsonElement> entry : jsons.entrySet()) {
                try {
                    parsed.put(entry.getKey(), SkillNode.fromJson(entry.getKey(), entry.getValue().getAsJsonObject()));
                } catch (Exception e) {
                    RpgMod.LOGGER.error("Invalid skill node {}: {}", entry.getKey(), e.getMessage());
                }
            }
            Map<String, List<String>> graph = new HashMap<>();
            for (SkillNode node : parsed.values()) {
                List<String> parents = new ArrayList<>();
                node.parents().forEach(p -> parents.add(p.toString()));
                graph.put(node.id().toString(), parents);
            }
            for (String problem : SkillGraphValidator.validate(graph)) {
                RpgMod.LOGGER.warn("Skill tree: {}", problem);
            }
            setServer(new SkillTree(parsed));
            RpgMod.LOGGER.info("Loaded {} skill nodes", parsed.size());
        }
    }
}
