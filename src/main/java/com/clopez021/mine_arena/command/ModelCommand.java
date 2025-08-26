package com.clopez021.mine_arena.command;

import com.clopez021.mine_arena.MineArena;
import com.clopez021.mine_arena.entity.ModelEntity;
import com.clopez021.mine_arena.entity.ModEntities;
import com.clopez021.mine_arena.packets.ModelEntitySyncPacket;
import com.clopez021.mine_arena.packets.PacketHandler;
import com.clopez021.mine_arena.command.arguments.ApplySetArgument;
import com.clopez021.mine_arena.command.arguments.AxisArgument;
import com.clopez021.mine_arena.command.arguments.DirectionArgument;
import com.clopez021.mine_arena.command.arguments.ModelFileArgument;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

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
				.executes(ctx -> spawnEntityWithScale(ctx, 16f/16f))
				.then(argument("microScale", FloatArgumentType.floatArg(0.001f, 1.0f)).executes(ModelCommand::spawnEntity))
			)
			.then(literal("entity_one").executes(ModelCommand::spawnSingleBlockEntity))
		);
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

		var entityType = ModEntities.MODEL_ENTITY.get();
		ModelEntity e = entityType.create(level);
		if (e == null) return 0;
		e.setPos(pos.x, pos.y, pos.z);
		e.microScale = micro;
		e.setVoxels(com.clopez021.mine_arena.models.util.VoxelHelper.buildVoxels(MineArena.model));
		// set render/collision bounds based on current model
		e.setFromModelBounds(MineArena.model.minCorner, MineArena.model.maxCorner);
		level.addFreshEntity(e);
		
		// sync voxel data to clients
		PacketHandler.INSTANCE.send(new ModelEntitySyncPacket(e.getId(), e.microScale, e.minCorner.x, e.minCorner.y, e.minCorner.z, e.voxels), PacketDistributor.TRACKING_ENTITY.with(e));
		
		source.sendSystemMessage(Component.literal("Spawned model entity with microScale=" + micro + ", voxels=" + e.voxels.size()));
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

		var entityType = ModEntities.MODEL_ENTITY.get();
		System.out.println("entityType: " + entityType);
		ModelEntity e = entityType.create(level);
		if (e == null) return 0;
		// place entity at player pos
		e.setPos(pos.x, pos.y, pos.z);
		// full-size single block
		e.microScale = 1.0f;
		// one voxel at (0, 2, 0) so it appears above the player
		java.util.HashMap<net.minecraft.core.BlockPos, BlockState> one = new java.util.HashMap<>();
		one.put(new net.minecraft.core.BlockPos(0, 2, 0), state);
		e.setVoxels(one);
		System.out.println("e.voxels: " + e.voxels.entrySet());
		// bounds that encompass the single block
		e.minCorner.set(0, 0, 0);
		e.setFromModelBounds(new org.joml.Vector3f(0, 0, 0), new org.joml.Vector3f(1, 3, 1));
		level.addFreshEntity(e);
		
		// sync voxel data to clients
		PacketHandler.INSTANCE.send(new ModelEntitySyncPacket(e.getId(), e.microScale, e.minCorner.x, e.minCorner.y, e.minCorner.z, e.voxels), PacketDistributor.TRACKING_ENTITY.with(e));
		
		source.sendSystemMessage(Component.literal("Spawned one-block model entity."));
		return 1;
	}
}
