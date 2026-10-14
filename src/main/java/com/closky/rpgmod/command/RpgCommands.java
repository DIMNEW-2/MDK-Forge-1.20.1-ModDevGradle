package com.closky.rpgmod.command;

import com.closky.rpgmod.RpgMod;
import com.closky.rpgmod.boss.BossDefinition;
import com.closky.rpgmod.boss.BossManager;
import com.closky.rpgmod.capability.PlayerRpgData;
import com.closky.rpgmod.loot.LootItemHelper;
import com.closky.rpgmod.loot.RpgItemData;
import com.closky.rpgmod.loot.WeaponMaterial;
import com.closky.rpgmod.network.ModNetwork;
import com.closky.rpgmod.network.packet.OpenClientScreenPacket;
import com.closky.rpgmod.network.packet.SyncSettlementsPacket;
import com.closky.rpgmod.player.StatType;
import com.closky.rpgmod.settlement.Settlement;
import com.closky.rpgmod.settlement.SettlementManager;
import com.closky.rpgmod.worldgen.DungeonRegistry;
import com.closky.rpgmod.worldgen.template.DungeonTemplateGenerator;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

/**
 * {@code /rpgmod ...} commands (sections 5.6, 18.1, 19.1, debugging):
 * <pre>
 * /rpgmod skills | relations | character
 * /rpgmod level set &lt;player&gt; &lt;level&gt;   /rpgmod xp add &lt;player&gt; &lt;amount&gt;
 * /rpgmod points &lt;stat|skill&gt; &lt;player&gt; &lt;amount&gt;
 * /rpgmod mana set &lt;player&gt; &lt;amount&gt;
 * /rpgmod item level &lt;level&gt; | material &lt;material&gt; | reroll
 * /rpgmod boss spawn &lt;boss&gt;
 * /rpgmod settlement list | relation &lt;a&gt; &lt;b&gt; &lt;value&gt;
 * /rpgmod dungeon list | export &lt;name&gt; &lt;from&gt; &lt;to&gt; | import &lt;name&gt; [pos] [rotation] | generate &lt;id&gt; | generate_templates
 * </pre>
 */
public final class RpgCommands {
    private RpgCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
        dispatcher.register(Commands.literal("rpgmod")
                .then(Commands.literal("skills").executes(ctx -> openScreen(ctx, OpenClientScreenPacket.SKILL_TREE)))
                .then(Commands.literal("character").executes(ctx -> openScreen(ctx, OpenClientScreenPacket.CHARACTER)))
                .then(Commands.literal("relations").executes(RpgCommands::openRelations))
                .then(Commands.literal("level").requires(s -> s.hasPermission(2))
                        .then(Commands.literal("set").then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("level", IntegerArgumentType.integer(1, 100)).executes(RpgCommands::setLevel))))
                        .then(Commands.literal("get").then(Commands.argument("player", EntityArgument.player()).executes(RpgCommands::getLevel))))
                .then(Commands.literal("xp").requires(s -> s.hasPermission(2))
                        .then(Commands.literal("add").then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1)).executes(RpgCommands::addXp)))))
                .then(Commands.literal("points").requires(s -> s.hasPermission(2))
                        .then(Commands.literal("stat").then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1)).executes(ctx -> addPoints(ctx, true)))))
                        .then(Commands.literal("skill").then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1)).executes(ctx -> addPoints(ctx, false))))))
                .then(Commands.literal("mana").requires(s -> s.hasPermission(2))
                        .then(Commands.literal("set").then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("amount", IntegerArgumentType.integer(0)).executes(RpgCommands::setMana)))))
                .then(Commands.literal("item").requires(s -> s.hasPermission(2))
                        .then(Commands.literal("level").then(Commands.argument("level", IntegerArgumentType.integer(1, 100)).executes(RpgCommands::setItemLevel)))
                        .then(Commands.literal("material").then(Commands.argument("material", StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(materialNames(), builder)).executes(RpgCommands::setItemMaterial)))
                        .then(Commands.literal("reroll").executes(RpgCommands::rerollItem)))
                .then(Commands.literal("boss").requires(s -> s.hasPermission(2))
                        .then(Commands.literal("spawn").then(Commands.argument("boss", StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(bossNames(), builder)).executes(RpgCommands::spawnBoss))))
                .then(Commands.literal("settlement").requires(s -> s.hasPermission(2))
                        .then(Commands.literal("list").executes(RpgCommands::listSettlements))
                        .then(Commands.literal("relation").then(Commands.argument("a", StringArgumentType.word()).then(Commands.argument("b", StringArgumentType.word())
                                .then(Commands.argument("value", IntegerArgumentType.integer(-100, 100)).executes(RpgCommands::setRelation))))))
                .then(Commands.literal("dungeon").requires(s -> s.hasPermission(2))
                        .then(Commands.literal("list").executes(RpgCommands::listDungeons))
                        .then(Commands.literal("export").then(Commands.argument("name", ResourceLocationArgument.id())
                                .then(Commands.argument("from", BlockPosArgument.blockPos()).then(Commands.argument("to", BlockPosArgument.blockPos()).executes(RpgCommands::exportTemplate)))))
                        .then(Commands.literal("import").then(Commands.argument("name", ResourceLocationArgument.id())
                                .executes(ctx -> importTemplate(ctx, null, Rotation.NONE))
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .executes(ctx -> importTemplate(ctx, BlockPosArgument.getLoadedBlockPos(ctx, "pos"), Rotation.NONE))
                                        .then(Commands.argument("rotation", IntegerArgumentType.integer(0, 3))
                                                .executes(ctx -> importTemplate(ctx, BlockPosArgument.getLoadedBlockPos(ctx, "pos"), Rotation.values()[IntegerArgumentType.getInteger(ctx, "rotation")]))))))
                        .then(Commands.literal("generate").then(Commands.argument("id", StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(DungeonRegistry.ids(), builder)).executes(RpgCommands::generateDungeon)))
                        .then(Commands.literal("generate_templates").executes(RpgCommands::generateTemplates))));
    }

    // ------------------------------------------------------------------ screens

    private static int openScreen(CommandContext<CommandSourceStack> ctx, int screen) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ModNetwork.sendToPlayer(player, new OpenClientScreenPacket(screen));
        return 1;
    }

    private static int openRelations(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        SettlementManager manager = SettlementManager.get(player.serverLevel());
        ModNetwork.sendToPlayer(player, new SyncSettlementsPacket(manager.snapshot(player), true));
        return 1;
    }

    // ------------------------------------------------------------------ player data

    private static int setLevel(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        int level = IntegerArgumentType.getInteger(ctx, "level");
        PlayerRpgData data = PlayerRpgData.get(player);
        if (data == null) {
            return 0;
        }
        data.setLevel(player, level);
        data.sync(player);
        ctx.getSource().sendSuccess(() -> Component.translatable("command.rpgmod.level_set", player.getDisplayName(), level), true);
        return 1;
    }

    private static int getLevel(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        PlayerRpgData data = PlayerRpgData.get(player);
        if (data == null) {
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("command.rpgmod.level_get", player.getDisplayName(), data.getLevel(), data.getXp(), data.getXpForNextLevel(), data.getActiveClass().getDisplayName()), false);
        return data.getLevel();
    }

    private static int addXp(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        int amount = IntegerArgumentType.getInteger(ctx, "amount");
        PlayerRpgData data = PlayerRpgData.get(player);
        if (data == null) {
            return 0;
        }
        data.addXp(player, amount);
        data.sync(player);
        ctx.getSource().sendSuccess(() -> Component.translatable("command.rpgmod.xp_added", amount, player.getDisplayName()), true);
        return 1;
    }

    private static int addPoints(CommandContext<CommandSourceStack> ctx, boolean stat) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        int amount = IntegerArgumentType.getInteger(ctx, "amount");
        PlayerRpgData data = PlayerRpgData.get(player);
        if (data == null) {
            return 0;
        }
        if (stat) {
            data.grantStatPoints(amount);
        } else {
            data.addSkillPoints(amount);
        }
        data.sync(player);
        ctx.getSource().sendSuccess(() -> Component.translatable("command.rpgmod.points_added", amount, player.getDisplayName()), true);
        return 1;
    }

    private static int setMana(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        int amount = IntegerArgumentType.getInteger(ctx, "amount");
        PlayerRpgData data = PlayerRpgData.get(player);
        if (data == null) {
            return 0;
        }
        data.setMana(Math.min(amount, data.getMaxMana(player)));
        data.sync(player);
        return 1;
    }

    // ------------------------------------------------------------------ items

    private static int setItemLevel(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ItemStack stack = player.getMainHandItem();
        if (!RpgItemData.isRpgItem(stack)) {
            ctx.getSource().sendFailure(Component.translatable("command.rpgmod.not_rpg_item"));
            return 0;
        }
        RpgItemData.setItemLevel(stack, IntegerArgumentType.getInteger(ctx, "level"));
        return 1;
    }

    private static int setItemMaterial(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ItemStack stack = player.getMainHandItem();
        WeaponMaterial material = WeaponMaterial.byName(StringArgumentType.getString(ctx, "material"));
        if (!RpgItemData.isRpgItem(stack) || material == null) {
            ctx.getSource().sendFailure(Component.translatable("command.rpgmod.not_rpg_item"));
            return 0;
        }
        RpgItemData.setMaterial(stack, material);
        return 1;
    }

    private static int rerollItem(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ItemStack stack = player.getMainHandItem();
        if (!RpgItemData.isRpgItem(stack)) {
            ctx.getSource().sendFailure(Component.translatable("command.rpgmod.not_rpg_item"));
            return 0;
        }
        int count = Math.max(1, LootItemHelper.bonusCount(RpgItemData.getItemLevel(stack), player.getRandom()));
        RpgItemData.rollBonuses(stack, new Random(), count);
        return 1;
    }

    private static List<String> materialNames() {
        List<String> names = new ArrayList<>();
        for (WeaponMaterial m : WeaponMaterial.values()) {
            names.add(m.getSerializedName());
        }
        return names;
    }

    // ------------------------------------------------------------------ bosses

    private static int spawnBoss(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        BossDefinition definition = BossDefinition.byKey(StringArgumentType.getString(ctx, "boss"));
        if (definition == null) {
            ctx.getSource().sendFailure(Component.translatable("command.rpgmod.unknown_boss"));
            return 0;
        }
        BlockPos pos = player.blockPosition().relative(player.getDirection(), 4);
        BossManager.spawn(player.serverLevel(), pos, definition);
        return 1;
    }

    private static List<String> bossNames() {
        List<String> names = new ArrayList<>();
        for (BossDefinition b : BossDefinition.values()) {
            names.add(b.getKey());
        }
        return names;
    }

    // ------------------------------------------------------------------ settlements

    private static int listSettlements(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        SettlementManager manager = SettlementManager.get(level);
        if (manager.getSettlements().isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.translatable("command.rpgmod.no_settlements"), false);
            return 0;
        }
        for (Settlement s : manager.getSettlements().values()) {
            ctx.getSource().sendSuccess(() -> Component.literal(String.format("%s [%s] @ %d,%d,%d  pop=%d prosperity=%d military=%d wars=%d allies=%d",
                    s.getName(), s.getVillageClass().getKey(), s.getCenter().getX(), s.getCenter().getY(), s.getCenter().getZ(),
                    s.getPopulation(), s.getProsperity(), s.getMilitary(), s.getAtWarWith().size(), s.getAllies().size())), false);
        }
        return manager.getSettlements().size();
    }

    private static int setRelation(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        SettlementManager manager = SettlementManager.get(level);
        Settlement a = byName(manager, StringArgumentType.getString(ctx, "a"));
        Settlement b = byName(manager, StringArgumentType.getString(ctx, "b"));
        if (a == null || b == null || a == b) {
            ctx.getSource().sendFailure(Component.translatable("command.rpgmod.unknown_settlement"));
            return 0;
        }
        int value = IntegerArgumentType.getInteger(ctx, "value");
        manager.setRelation(a.getId(), b.getId(), value);
        ctx.getSource().sendSuccess(() -> Component.literal(a.getName() + " <-> " + b.getName() + " = " + value), true);
        return 1;
    }

    private static Settlement byName(SettlementManager manager, String name) {
        for (Settlement s : manager.getSettlements().values()) {
            if (s.getName().equalsIgnoreCase(name)) {
                return s;
            }
        }
        try {
            return manager.get(UUID.fromString(name));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // ------------------------------------------------------------------ dungeons

    private static int listDungeons(CommandContext<CommandSourceStack> ctx) {
        for (DungeonRegistry.DungeonInfo info : DungeonRegistry.all()) {
            ctx.getSource().sendSuccess(() -> Component.literal(String.format("%s  level=%d  boss=%s  %s", info.id(), info.level(), info.boss().getKey(), info.underground() ? "underground" : "surface")), false);
        }
        return DungeonRegistry.all().size();
    }

    private static int exportTemplate(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerLevel level = ctx.getSource().getLevel();
        ResourceLocation name = ResourceLocationArgument.getId(ctx, "name");
        BlockPos from = BlockPosArgument.getLoadedBlockPos(ctx, "from");
        BlockPos to = BlockPosArgument.getLoadedBlockPos(ctx, "to");
        BoundingBox box = BoundingBox.fromCorners(from, to);
        Vec3i size = new Vec3i(box.getXSpan(), box.getYSpan(), box.getZSpan());
        if (size.getX() > 48 || size.getY() > 48 || size.getZ() > 48) {
            ctx.getSource().sendFailure(Component.translatable("command.rpgmod.template_too_big"));
            return 0;
        }
        StructureTemplateManager manager = level.getStructureManager();
        StructureTemplate template = manager.getOrCreate(name);
        template.fillFromWorld(level, new BlockPos(box.minX(), box.minY(), box.minZ()), size, true, Blocks.STRUCTURE_VOID);
        template.setAuthor(ctx.getSource().getTextName());
        boolean saved = manager.save(name);
        if (saved) {
            ctx.getSource().sendSuccess(() -> Component.translatable("command.rpgmod.template_saved", name.toString()), true);
        } else {
            ctx.getSource().sendFailure(Component.translatable("command.rpgmod.template_save_failed", name.toString()));
        }
        return saved ? 1 : 0;
    }

    private static int importTemplate(CommandContext<CommandSourceStack> ctx, BlockPos pos, Rotation rotation) throws CommandSyntaxException {
        ServerLevel level = ctx.getSource().getLevel();
        ResourceLocation name = ResourceLocationArgument.getId(ctx, "name");
        BlockPos at = pos != null ? pos : BlockPos.containing(ctx.getSource().getPosition());
        Optional<StructureTemplate> template = level.getStructureManager().get(name);
        if (template.isEmpty()) {
            ctx.getSource().sendFailure(Component.translatable("command.rpgmod.template_not_found", name.toString()));
            return 0;
        }
        StructurePlaceSettings settings = new StructurePlaceSettings().setRotation(rotation).setIgnoreEntities(false);
        boolean placed = template.get().placeInWorld(level, at, at, settings, level.getRandom(), 2);
        if (placed) {
            ctx.getSource().sendSuccess(() -> Component.translatable("command.rpgmod.template_placed", name.toString()), true);
        }
        return placed ? 1 : 0;
    }

    private static int generateDungeon(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String id = StringArgumentType.getString(ctx, "id");
        if (DungeonRegistry.get(id) == null) {
            ctx.getSource().sendFailure(Component.translatable("command.rpgmod.unknown_dungeon", id));
            return 0;
        }
        // Delegates to vanilla structure placement, which honours the jigsaw definition exactly like world generation.
        CommandSourceStack source = ctx.getSource();
        source.getServer().getCommands().performPrefixedCommand(source, "place structure " + RpgMod.MODID + ":" + id);
        return 1;
    }

    private static int generateTemplates(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        int count = DungeonTemplateGenerator.generateAll(level);
        ctx.getSource().sendSuccess(() -> Component.translatable("command.rpgmod.templates_generated", count).withStyle(ChatFormatting.GREEN), true);
        return count;
    }
}
