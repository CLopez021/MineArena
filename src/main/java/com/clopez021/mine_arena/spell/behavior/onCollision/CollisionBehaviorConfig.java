package com.clopez021.mine_arena.spell.behavior.onCollision;

import com.clopez021.mine_arena.spell.BaseConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/**
 * Config for selecting and parameterizing a collision behavior.
 * Stores name (registry key), radius, and damage.
 */
public class CollisionBehaviorConfig extends BaseConfig {
    private String name = OnCollisionBehaviors.DEFAULT_KEY;
    private float radius = 2.0f;
    private float damage = 0.0f;

    public CollisionBehaviorConfig() {}

    public CollisionBehaviorConfig(String name, float radius, float damage) {
        setName(name);
        this.radius = radius;
        this.damage = damage;
    }

    public String getName() { return name; }
    public float getRadius() { return radius; }
    public float getDamage() { return damage; }
    public void setName(String name) { this.name = (name == null || name.isEmpty()) ? OnCollisionBehaviors.DEFAULT_KEY : name; }
    public void setRadius(float radius) { this.radius = radius; }
    public void setDamage(float damage) { this.damage = damage; }

    @Override
    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("name", name);
        tag.putFloat("radius", radius);
        tag.putFloat("damage", damage);
        return tag;
    }

    public static CollisionBehaviorConfig fromNBT(CompoundTag tag) {
        CollisionBehaviorConfig c = new CollisionBehaviorConfig();
        if (tag == null) return c;
        if (tag.contains("name", Tag.TAG_STRING)) c.setName(tag.getString("name"));
        if (tag.contains("radius", Tag.TAG_FLOAT)) c.setRadius(tag.getFloat("radius"));
        if (tag.contains("damage", Tag.TAG_FLOAT)) c.setDamage(tag.getFloat("damage"));
        return c;
    }
}
