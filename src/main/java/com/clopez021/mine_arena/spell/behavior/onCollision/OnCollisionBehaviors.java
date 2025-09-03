package com.clopez021.mine_arena.spell.behavior.onCollision;

import com.clopez021.mine_arena.spell.SpellEntity;
import net.minecraft.world.level.Level;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class OnCollisionBehaviors {
    public static final String DEFAULT_KEY = "explode";

    private static final Map<String, CollisionBehavior> REGISTRY;

    static {
        Map<String, CollisionBehavior> map = new HashMap<>();

        // Default: explode at the entity's position, then discard the entity
        map.put("explode", (SpellEntity e) -> {
            if (e == null || e.level() == null || e.level().isClientSide) return;
            Level level = e.level();
            // Basic explosion; adjust power or interaction type as desired
            level.explode(e, e.getX(), e.getY(), e.getZ(), 2.0f, Level.ExplosionInteraction.BLOCK);
            e.discard();
        });

        REGISTRY = Collections.unmodifiableMap(map);
    }

    private OnCollisionBehaviors() {}

    public static Map<String, CollisionBehavior> registry() {
        return REGISTRY;
    }

    public static CollisionBehavior byKey(String key) {
        if (key == null) key = DEFAULT_KEY;
        return REGISTRY.getOrDefault(key, REGISTRY.get(DEFAULT_KEY));
    }
}

