package com.shulkerfinder;
import net.fabricmc.api.ModInitializer;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.BlockPos;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Comparator;
import java.util.List;
public class ShulkerFinderMod implements ModInitializer {
    public static final String MOD_ID = "shulkerfinder";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    @Override
    public void onInitialize() {
        LOGGER.info("ShulkerFinder loaded!");
        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(Commands.literal("findshulker").requires(s -> s.hasPermissionLevel(2)).executes(ctx -> findShulker(ctx, null)).then(Commands.argument("player", EntityArgument.player()).executes(ctx -> { try { return findShulker(ctx, EntityArgument.getPlayer(ctx, "player")); } catch (CommandSyntaxException e) { ctx.getSource().sendFailure(Component.literal("Player not found.")); return 0; } })));
            dispatcher.register(Commands.literal("tpshulker").requires(s -> s.hasPermissionLevel(2)).executes(ctx -> tpToShulker(ctx, null)).then(Commands.argument("player", EntityArgument.player()).executes(ctx -> { try { return tpToShulker(ctx, EntityArgument.getPlayer(ctx, "player")); } catch (CommandSyntaxException e) { ctx.getSource().sendFailure(Component.literal("Player not found.")); return 0; } })));
            dispatcher.register(Commands.literal("listshulkers").requires(s -> s.hasPermissionLevel(2)).executes(ShulkerFinderMod::listShulkers));
        });
    }
    private static List<Shulker> getShulkers(ServerLevel level) { return level.getEntitiesOfClass(Shulker.class, new AABB(-30000000, -64, -30000000, 30000000, 320, 30000000)); }
    private static int findShulker(CommandContext<CommandSourceStack> ctx, ServerPlayer target) {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player;
        try { player = target != null ? target : source.getPlayerOrException(); } catch (CommandSyntaxException e) { source.sendFailure(Component.literal("Must be a player.")); return 0; }
        List<Shulker> shulkers = getShulkers((ServerLevel)player.level());
        if (shulkers.isEmpty()) { source.sendSuccess(() -> Component.literal("No shulkers found."), false); return 0; }
        BlockPos pp = player.blockPosition();
        Shulker nearest = shulkers.stream().min(Comparator.comparingDouble(s -> s.blockPosition().distSqr(pp))).orElse(null);
        if (nearest == null) return 0;
        BlockPos pos = nearest.blockPosition();
        double dist = Math.sqrt(pos.distSqr(pp));
        source.sendSuccess(() -> Component.literal(String.format("Nearest shulker: X:0 Y:0 Z:0 (0.0 blocks away)", pos.getX(), pos.getY(), pos.getZ(), dist)), false);
        return 1;
    }
    private static int tpToShulker(CommandContext<CommandSourceStack> ctx, ServerPlayer target) {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player;
        try { player = target != null ? target : source.getPlayerOrException(); } catch (CommandSyntaxException e) { source.sendFailure(Component.literal("Must be a player.")); return 0; }
        List<Shulker> shulkers = getShulkers((ServerLevel)player.level());
        if (shulkers.isEmpty()) { source.sendSuccess(() -> Component.literal("No shulkers found."), false); return 0; }
        BlockPos pp = player.blockPosition();
        Shulker nearest = shulkers.stream().min(Comparator.comparingDouble(s -> s.blockPosition().distSqr(pp))).orElse(null);
        if (nearest == null) return 0;
        BlockPos pos = nearest.blockPosition();
        player.teleportTo(pos.getX()+0.5, pos.getY()+1.0, pos.getZ()+0.5);
        source.sendSuccess(() -> Component.literal(String.format("Teleported to X:0 Y:0 Z:0", pos.getX(), pos.getY(), pos.getZ())), false);
        return 1;
    }
    private static int listShulkers(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        List<Shulker> shulkers = getShulkers(source.getLevel());
        if (shulkers.isEmpty()) { source.sendSuccess(() -> Component.literal("No shulkers found."), false); return 0; }
        source.sendSuccess(() -> Component.literal("Shulkers: " + shulkers.size()), false);
        for (int i = 0; i < shulkers.size(); i++) { BlockPos pos = shulkers.get(i).blockPosition(); int idx = i+1; source.sendSuccess(() -> Component.literal(String.format("#0 X:0 Y:0 Z:0", idx, pos.getX(), pos.getY(), pos.getZ())), false); }
        return 1;
    }
}
