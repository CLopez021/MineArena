package com.clopez021.mine_arena.spell.behavior.onCollision;

import com.clopez021.mine_arena.spell.SpellEntity;

@FunctionalInterface
public interface CollisionBehavior {
    void handle(SpellEntity entity);
}

