package com.clopez021.mine_arena.command;

import com.clopez021.mine_arena.MineArena;
import com.clopez021.mine_arena.entity.SpellEntity;
import com.clopez021.mine_arena.entity.ModEntities;
import com.clopez021.mine_arena.models.util.Point;
import com.clopez021.mine_arena.command.arguments.ApplySetArgument;
import com.clopez021.mine_arena.command.arguments.AxisArgument;
import com.clopez021.mine_arena.command.arguments.DirectionArgument;
import com.clopez021.mine_arena.command.arguments.ModelFileArgument;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.Map;

import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static com.mojang.brigadier.arguments.FloatArgumentType.floatArg;

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
			.then(literal("undo").executes(UndoCommand::undo))
			// .then(literal("scale").then(argument("applySet", ApplySetArgument.applySetArg())
			//     .then(argument("scale", floatArg()).executes(ScaleCommand::scaleAll))
			//     .then(argument("x-scale", floatArg()).then(argument("y-scale", floatArg()).then(argument("z-scale", floatArg()).executes(ScaleCommand::scale))))
			//     .then(argument("axis", axisArg()).then(argument("scale", floatArg()).executes(ScaleCommand::scaleAxis)))
			// ))
			// .then(literal("rotate")
			//     .then(argument("x-angle", floatArg()).then(argument("y-angle", floatArg()).then(argument("z-angle", floatArg()).executes(RotateCommand::rotate))))
			//     .then(argument("axis", axisArg()).then(argument("angle", floatArg()).executes(RotateCommand::rotateAxis))
			// ))
			// .then(literal("move")
			//     .then(argument("distance", floatArg()).executes(MoveCommand::move)
			//         .then(argument("direction", directionArg()).executes(MoveCommand::moveDirection))
			//     )
			// )
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
			.then(literal("entity_one").executes(ModelCommand::spawnSingleBlockEntity))
		);
	}

	/**
	 * Gets high-density voxels from the model by temporarily increasing precision and scale.
	 * @return A map of BlockPos to BlockState with higher block density than the default.
	 */
	private static Map<BlockPos, BlockState> getHighDensityVoxels() {
		if (MineArena.model == null) return Map.of();
		
		// Save current values
		float originalPrecision = Point.precision;
		Vector3f originalScale = new Vector3f(MineArena.model.scale);
		
		try {
			// Increase precision and scale for higher block density
			Point.precision = 8f;  // 4x higher than default 2f
			MineArena.model.setScale(originalScale.x * 4f, originalScale.y * 4f, originalScale.z * 4f);
			
			// Get the high-density blocks
			return MineArena.model.getTextureToBlocks();
		} finally {
			// Always restore original values
			Point.precision = originalPrecision;
			MineArena.model.setScale(originalScale.x, originalScale.y, originalScale.z);
		}
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
		e.setBlocksServer(getHighDensityVoxels());
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

	private static int spawnSingleBlockEntity(CommandContext<CommandSourceStack> command) {
		if (MineArena.model == null) return noModelLoaded(command);
		var source = command.getSource();
		var level = source.getLevel();
		Vec3 pos = source.getPosition();

		// Pick a single blockstate from the model, fallback to iron block
		BlockState state = Blocks.IRON_BLOCK.defaultBlockState();

		var entityType = ModEntities.SPELL_ENTITY.get();
		System.out.println("entityType: " + entityType);
		SpellEntity e = entityType.create(level);
		if (e == null) return 0;
		// place entity at player pos
		e.setPos(pos.x, pos.y, pos.z);
		// full-size single block
		e.setMicroScaleServer(.2f);
		// one block at (0, 2, 0) so it appears above the player
		java.util.HashMap<net.minecraft.core.BlockPos, BlockState> one = new java.util.HashMap<>();
		one.put(new net.minecraft.core.BlockPos(0, 2, 0), state);
		e.setBlocksServer(one);
		System.out.println("e.blocks: " + e.blocks.entrySet());
		// bounds that encompass the single block
		e.setMinCornerServer(new org.joml.Vector3f(0, 0, 0));
		e.setFromModelBounds(new org.joml.Vector3f(0, 0, 0), new org.joml.Vector3f(1, 3, 1));
		level.addFreshEntity(e);
		
		source.sendSystemMessage(Component.literal("Spawned one-block spell entity."));
		return 1;
	}
}
