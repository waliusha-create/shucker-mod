package com.shulkerfinder;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.ShulkerEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ShulkerFinderMod implements ModInitializer {

    public static final String MOD_ID = "shulkerfinder";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("ShulkerFinder mod loaded!");

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            // /findshulker - finds nearest shulker to the command sender
            dispatcher.register(
                CommandManager.literal("findshulker")
                    .requires(source -> source.hasPermissionLevel(2))
                    .executes(ctx -> findShulker(ctx, null))
                    .then(CommandManager.argument("player", EntityArgumentType.player())
                        .executes(ctx -> {
                            try {
                                ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");
                                return findShulker(ctx, target);
                            } catch (CommandSyntaxException e) {
                                ctx.getSource().sendError(Text.literal("Player not found."));
                                return 0;
                            }
                        })
                    )
            );

            // /tpshulker - teleports the sender (or specified player) to nearest shulker
            dispatcher.register(
                CommandManager.literal("tpshulker")
                    .requires(source -> source.hasPermissionLevel(2))
                    .executes(ctx -> tpToShulker(ctx, null))
                    .then(CommandManager.argument("player", EntityArgumentType.player())
                        .executes(ctx -> {
                            try {
                                ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");
                                return tpToShulker(ctx, target);
                            } catch (CommandSyntaxException e) {
                                ctx.getSource().sendError(Text.literal("Player not found."));
                                return 0;
                            }
                        })
                    )
            );

            // /listshulkers - lists all shulkers in current dimension with coords
            dispatcher.register(
                CommandManager.literal("listshulkers")
                    .requires(source -> source.hasPermissionLevel(2))
                    .executes(ShulkerFinderMod::listShulkers)
            );
        });
    }

    private static List<ShulkerEntity> getShulkersInWorld(ServerWorld world) {
        // Search in a very large box covering the whole world height and a wide range
        Box searchBox = new Box(-30000000, world.getBottomY(), -30000000,
                                 30000000, world.getTopY(), 30000000);
        return world.getEntitiesByClass(ShulkerEntity.class, searchBox, e -> true);
    }

    private static int findShulker(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity targetPlayer) {
        ServerCommandSource source = ctx.getSource();

        ServerPlayerEntity player;
        try {
            player = targetPlayer != null ? targetPlayer : source.getPlayerOrThrow();
        } catch (CommandSyntaxException e) {
            source.sendError(Text.literal("Must be a player or specify one with /findshulker <player>"));
            return 0;
        }

        ServerWorld world = player.getServerWorld();
        List<ShulkerEntity> shulkers = getShulkersInWorld(world);

        if (shulkers.isEmpty()) {
            source.sendFeedback(() -> Text.literal("§eNo shulkers found in " + getDimName(world) + "."), false);
            return 0;
        }

        BlockPos playerPos = player.getBlockPos();
        ShulkerEntity nearest = shulkers.stream()
            .min(Comparator.comparingDouble(s -> s.getBlockPos().getSquaredDistance(playerPos)))
            .orElse(null);

        if (nearest == null) return 0;

        BlockPos pos = nearest.getBlockPos();
        double dist = Math.sqrt(nearest.getBlockPos().getSquaredDistance(playerPos));

        source.sendFeedback(() -> Text.literal(
            String.format("§aNearest shulker to §e%s§a: §bX:%d Y:%d Z:%d §7(%.1f blocks away) §6[%s]",
                player.getName().getString(), pos.getX(), pos.getY(), pos.getZ(),
                dist, getDimName(world))
        ), false);

        if (shulkers.size() > 1) {
            source.sendFeedback(() -> Text.literal(
                String.format("§7(Found %d shulker(s) total in this dimension — use §f/listshulkers§7 to see all)", shulkers.size())
            ), false);
        }

        return 1;
    }

    private static int tpToShulker(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity targetPlayer) {
        ServerCommandSource source = ctx.getSource();

        ServerPlayerEntity player;
        try {
            player = targetPlayer != null ? targetPlayer : source.getPlayerOrThrow();
        } catch (CommandSyntaxException e) {
            source.sendError(Text.literal("Must be a player or specify one with /tpshulker <player>"));
            return 0;
        }

        ServerWorld world = player.getServerWorld();
        List<ShulkerEntity> shulkers = getShulkersInWorld(world);

        if (shulkers.isEmpty()) {
            source.sendFeedback(() -> Text.literal("§eNo shulkers found in " + getDimName(world) + "."), false);
            return 0;
        }

        BlockPos playerPos = player.getBlockPos();
        ShulkerEntity nearest = shulkers.stream()
            .min(Comparator.comparingDouble(s -> s.getBlockPos().getSquaredDistance(playerPos)))
            .orElse(null);

        if (nearest == null) return 0;

        BlockPos pos = nearest.getBlockPos();

        // Teleport player 1 block above the shulker so they don't land inside it
        player.teleport(world, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                        player.getYaw(), player.getPitch());

        source.sendFeedback(() -> Text.literal(
            String.format("§aTeleported §e%s §ato shulker at §bX:%d Y:%d Z:%d §6[%s]",
                player.getName().getString(), pos.getX(), pos.getY(), pos.getZ(), getDimName(world))
        ), false);

        return 1;
    }

    private static int listShulkers(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource source = ctx.getSource();
        ServerWorld world;

        try {
            world = source.getPlayerOrThrow().getServerWorld();
        } catch (CommandSyntaxException e) {
            world = source.getWorld() instanceof ServerWorld sw ? sw : null;
        }

        if (world == null) {
            source.sendError(Text.literal("Could not determine world."));
            return 0;
        }

        List<ShulkerEntity> shulkers = getShulkersInWorld(world);

        if (shulkers.isEmpty()) {
            source.sendFeedback(() -> Text.literal("§eNo shulkers found in " + getDimName(world) + "."), false);
            return 0;
        }

        source.sendFeedback(() -> Text.literal(
            String.format("§a--- Shulkers in §6%s §a(%d found) ---", getDimName(world), shulkers.size())
        ), false);

        for (int i = 0; i < shulkers.size(); i++) {
            ShulkerEntity s = shulkers.get(i);
            BlockPos pos = s.getBlockPos();
            int idx = i + 1;
            source.sendFeedback(() -> Text.literal(
                String.format("§7#%d §bX:%d Y:%d Z:%d", idx, pos.getX(), pos.getY(), pos.getZ())
            ), false);
        }

        return 1;
    }

    private static String getDimName(ServerWorld world) {
        String key = world.getRegistryKey().getValue().getPath();
        return switch (key) {
            case "the_end"    -> "The End";
            case "the_nether" -> "The Nether";
            default           -> "Overworld";
        };
    }
}
