package com.clopez021.mine_arena.command;

import com.clopez021.mine_arena.MineArena;
import com.clopez021.mine_arena.entity.SpellEntity;
import com.clopez021.mine_arena.entity.ModEntities;
import com.clopez021.mine_arena.models.util.Point;

import com.clopez021.mine_arena.command.arguments.ModelFileArgument;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Map;

import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import com.clopez021.mine_arena.models.Model;
import java.util.HashMap;
/**
 * The main class for the "model" command.
 */
public class ModelCommand {
	/**
	 * Registers the "model" command and all of its subcommands.
	 * @param dispatcher CommandDispatcher to register commands.
	 */
	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(literal("model")
			.then(literal("load").then(argument("filename", ModelFileArgument.modelFileArgument()).executes(LoadCommand::load)))
			.then(literal("place").executes(PlaceCommand::place))
			.then(literal("generate")
				.then(literal("cancel").executes(GenerateCommand::cancelGeneration))
				.then(argument("prompt", greedyString()).executes(GenerateCommand::generateModel)
					.then(argument("model_name", string()).executes(GenerateCommand::generateModel))
				)
			)
			.then(literal("entity")
				.executes(ctx -> spawnEntityWithScale(ctx, 1/16f))
				.then(argument("microScale", FloatArgumentType.floatArg(0.001f, 1.0f)).executes(ModelCommand::spawnEntity))
			)
		);
	}

	public static Map<BlockPos, BlockState> buildVoxels(Model model) {
		if (model == null) return Map.of();
		Map<BlockPos, BlockState> blocks = model.getTextureToBlocks();
		return new HashMap<>(blocks);
	}

	/**
	 * Runs when a command tried to execute without a loaded Model.
	 * @param command The executed command.
	 * @return 0
	 */
	protected static int noModelLoaded(CommandContext<CommandSourceStack> command) {
		command.getSource().sendFailure(Component.literal("Error: Load a Model first."));
		return 0;
	}

	private static int spawnEntityWithScale(CommandContext<CommandSourceStack> command, float micro) {
		if (MineArena.model == null) return noModelLoaded(command);

		var source = command.getSource();
		var level = source.getLevel();
		Vec3 pos = source.getPosition();

		var entityType = ModEntities.SPELL_ENTITY.get();
		SpellEntity e = entityType.create(level);
		if (e == null) return 0;
		e.setPos(pos.x, pos.y, pos.z);
		e.setMicroScaleServer(micro);
		e.setBlocksServer(buildVoxels(MineArena.model));
		// set render/collision bounds based on current model
		e.setMinCornerServer(MineArena.model.minCorner);
		e.setFromModelBounds(MineArena.model.minCorner, MineArena.model.maxCorner);
		level.addFreshEntity(e);
		
		source.sendSystemMessage(Component.literal("Spawned spell entity with microScale=" + micro + ", blocks=" + e.blocks.size()));
		return 1;
	}

	private static int spawnEntity(CommandContext<CommandSourceStack> command) {
		float micro = FloatArgumentType.getFloat(command, "microScale");
		return spawnEntityWithScale(command, micro);
	}
}
