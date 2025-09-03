package com.clopez021.mine_arena.spell;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Spell entity data backed by a Map of positions to block states, plus microScale.
 * Provides a CODEC for future serialization, no extra helpers.
 */
public record SpellEntityData(Map<BlockPos, BlockState> blocks, float microScale) {

    // Simplified blockstate codec: only serializes the Block; properties are not encoded.
    public static final Codec<BlockState> SIMPLE_BLOCKSTATE_CODEC =
        BuiltInRegistries.BLOCK.byNameCodec().xmap(Block::defaultBlockState, BlockState::getBlock);

    private static final Codec<Map.Entry<BlockPos, BlockState>> ENTRY_CODEC = RecordCodecBuilder.create(i ->
        i.group(
            BlockPos.CODEC.fieldOf("pos").forGetter(Map.Entry::getKey),
            SIMPLE_BLOCKSTATE_CODEC.fieldOf("state").forGetter(Map.Entry::getValue)
        ).apply(i, Map::entry)
    );

    private static final Codec<Map<BlockPos, BlockState>> BLOCKS_CODEC = Codec.list(ENTRY_CODEC)
        .xmap(list -> {
            Map<BlockPos, BlockState> m = new HashMap<>();
            for (var e : list) m.put(e.getKey(), e.getValue());
            return m;
        }, m -> List.copyOf(m.entrySet()));

    public static final Codec<SpellEntityData> CODEC = RecordCodecBuilder.create(i ->
        i.group(
            BLOCKS_CODEC.fieldOf("blocks").forGetter(SpellEntityData::blocks),
            Codec.FLOAT.fieldOf("microScale").forGetter(SpellEntityData::microScale)
        ).apply(i, SpellEntityData::new)
    );

    public SpellEntityData {
        if (blocks == null) blocks = Map.of();
    }

    public static SpellEntityData empty() { return new SpellEntityData(Map.of(), 1.0f); }
}
