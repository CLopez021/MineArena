package com.clopez021.mine_arena.spell.behavior.onCollision;

import com.clopez021.mine_arena.spell.SpellEntity;
import java.util.function.Consumer;

public final class CollisionBehavior {
    private final String name;
    private final String description;
    private final float radius;
    private final float damage;
    private final Consumer<SpellEntity> handler;

    // Preferred: provide name + tuned radius/damage; description/handler pulled from registry
    public CollisionBehavior(String name, float radius, float damage) {
        if (name == null || name.isEmpty()) name = OnCollisionBehaviors.DEFAULT_KEY;
        this.name = name;
        OnCollisionBehaviors.BehaviorDef def = OnCollisionBehaviors.definitionFor(name);
        if (def == null) def = OnCollisionBehaviors.definitionFor(OnCollisionBehaviors.DEFAULT_KEY);
        this.description = def.description();
        this.handler = def.handler();
        this.radius = radius;
        this.damage = damage;
    }

    // Convenience: use registry defaults for radius/damage
    public CollisionBehavior(String name) {
        if (name == null || name.isEmpty()) name = OnCollisionBehaviors.DEFAULT_KEY;
        OnCollisionBehaviors.BehaviorDef def = OnCollisionBehaviors.definitionFor(name);
        if (def == null) def = OnCollisionBehaviors.definitionFor(OnCollisionBehaviors.DEFAULT_KEY);
        this.name = name;
        this.description = def.description();
        this.radius = def.radius();
        this.damage = def.damage();
        this.handler = def.handler();
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public float getRadius() { return radius; }
    public float getDamage() { return damage; }

    public void handle(SpellEntity entity) {
        if (handler != null) handler.accept(entity);
    }
}
