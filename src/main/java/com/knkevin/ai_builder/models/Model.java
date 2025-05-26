package com.knkevin.ai_builder.models;

import com.knkevin.ai_builder.items.custom.HammerModes;
import com.knkevin.ai_builder.models.util.Point;
import com.knkevin.ai_builder.models.util.Triangle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Math;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Represents a 3D model.
 */
public abstract class Model {
    /**
     * This Model's rotation represented as a quaternion.
     */
    public final Quaternionf rotation = new Quaternionf();

    /**
     * This Model's scale represented as a vector.
     */
    public final Vector3f scale = new Vector3f(1,1,1);

    /**
     * This Model's position in the world represented as a vector.
     */
    public final Vector3f position = new Vector3f();

    /**
     * The minimum and maximum corners of this Model's bounding box.
     */
    public final Vector3f minCorner = new Vector3f(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE), maxCorner = new Vector3f(Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE);

    /**
     * Textures mapped to collection of points representing blocks.
     */
    public final ConcurrentMap<String, Set<Point>> textureToBlocks = new ConcurrentHashMap<>();

    /**
     * Points representing blocks mapped to integers representing which faces to render.
     */
    public final ConcurrentMap<Point, Integer> renderFaces = new ConcurrentHashMap<>();

    /**
     * Stores block coordinates mapped to BlockStates, representing the blocks before the model was placed.
     */
    private final Map<BlockPos, BlockState> undo = new HashMap<>();

    /**
     * @return A 4x4 transformation from this Model's rotation and scale.
     */
    public Matrix4f getTransformationMatrix() {
        return new Matrix4f().rotate(rotation).scale(scale);
    }

    /**
     * @return A map of block positions mapped to block states representing this 3d model as blocks.
     */
    public abstract Map<BlockPos, BlockState> getTextureToBlocks();

    /**
     * Computes whether to render a particular face.
     * @param p A point representing a block.
     * @param faceNum The index of the face to compute.
     * @return Whether to render.
     */
    public boolean shouldRenderFace(Point p, int faceNum) {
        Point adj  = new Point(p.x, p.y, p.z);
        switch (faceNum) {
            case 0 -> --adj.x;
            case 1 -> ++adj.x;
            case 2 -> --adj.y;
            case 3 -> ++adj.y;
            case 4 -> --adj.z;
            case 5 -> ++adj.z;
        }
        return !renderFaces.containsKey(adj);
    }

    /**
     * Centers this Model.
     */
    protected abstract void centerModel();

    /**
     * @return A list of triangles that make up this model.
     */
    public abstract List<Triangle> getTriangles();

    /**
     * Recalculates the blocks and faces to be rendered by this Model's preview.
     */
    protected abstract void updateBlockFaces();

    /**
     * Converts the model into Minecraft by representing it as blocks.
     * @param level The world to place the model in.
     * @return The number of blocks placed.
     */
    public int placeBlocks(Level level) {
        this.undo.clear();
        int count = 0;
        float prevPrecision = Point.precision;
        Point.precision = 2f;
        for (Map.Entry<BlockPos, BlockState> entry: this.getTextureToBlocks().entrySet()) {
            BlockPos blockPos = entry.getKey().offset((int)Math.floor(position.x), (int)Math.floor(position.y), (int)Math.floor(position.z));
            BlockState blockState = entry.getValue();
            this.undo.put(blockPos, level.getBlockState(blockPos));
            level.setBlockAndUpdate(blockPos, blockState);
            ++count;
        }
        Point.precision = prevPrecision;
        return count;
    }

    /**
     * Undoes the previous placement of blocks.
     * @param level The world to undo the placement.
     */
    public void undo(Level level) {
        for (Map.Entry<BlockPos, BlockState> entry: this.undo.entrySet()) {
            BlockPos blockPos = entry.getKey();
            BlockState blockState = entry.getValue();
            level.setBlockAndUpdate(blockPos, blockState);
        }
    }

    /**
     * @param x_axis The angle in degrees to rotate around the x-axis.
     * @param y_axis The angle in degrees to rotate around the y-axis.
     * @param z_axis The angle in degrees to rotate around the z-axis.
     */
    public void applyRotation(float x_axis, float y_axis, float z_axis) {
        float x_angle = Math.toRadians(x_axis), y_angle = Math.toRadians(y_axis), z_angle = Math.toRadians(z_axis);
        rotation.rotateXYZ(-x_angle, -y_angle, -z_angle);
        if (HammerModes.viewMode == HammerModes.ViewMode.BLOCKS) this.updateBlockFaces();
    }

    /**
     * @param axis The axis to rotate around.
     * @param angle The angle in degrees to rotate.
     */
    public void applyAxisRotation(String axis, float angle) {
        switch (axis) {
            case "x" -> applyRotation(angle, 0, 0);
            case "y" -> applyRotation(0, angle, 0);
            case "z" -> applyRotation(0, 0, angle);
        }
    }

    /**
     * @param xScale The scale to set the x-component to.
     * @param yScale The scale to set the y-component to.
     * @param zScale The scale to set the z-component to.
     */
    public void setScale(float xScale, float yScale, float zScale) {
        Vector3f rotatedMin = new Vector3f(minCorner).rotate(rotation);
        Vector3f rotatedMax = new Vector3f(maxCorner).rotate(rotation);
        float offset = Math.min(rotatedMin.y, rotatedMax.y);

        // Offset y position so that the model scales from the bottom
        this.position.set(this.position.x, this.position.y + Math.ceil(offset * this.scale.y), this.position.z);
        this.scale.set(xScale, yScale, zScale);
        this.scale.max(new Vector3f(0,0,0));
        this.position.set(this.position.x, this.position.y - Math.ceil(offset * this.scale.y), this.position.z);
        if (HammerModes.viewMode == HammerModes.ViewMode.BLOCKS) this.updateBlockFaces();
    }

    /**
     * @param scale The scale to set the Model to.
     */
    public void setScale(float scale) {
        this.setScale(scale, scale, scale);
    }

    /**
     * @param axis The axis to set the scale of.
     * @param scale The scalar.
     */
    public void setAxisScale(String axis, float scale) {
        switch (axis) {
            case "x" -> this.setScale(scale, this.scale.y, this.scale.z);
            case "y" -> this.setScale(this.scale.x, scale, this.scale.z);
            case "z" -> this.setScale(this.scale.x, this.scale.y, scale);
        }
    }

    /**
     * @param xScale The amount to scale the x-component.
     * @param yScale The amount to scale the x-component.
     * @param zScale The amount to scale the x-component.
     */
    public void applyScale(float xScale, float yScale, float zScale) {
        this.setScale(this.scale.x * xScale, this.scale.y * yScale, this.scale.z * zScale);
    }

    /**
     * @param scale The scalar.
     */
    public void applyScale(float scale) {
        this.setScale(this.scale.x * scale, this.scale.y * scale, this.scale.z * scale);
    }

    /**
     * @param axis The axis to scale.
     * @param scale The scalar.
     */
    public void applyAxisScale(String axis, float scale) {
        switch (axis) {
            case "x" -> this.applyScale(scale,1,1);
            case "y" -> this.applyScale(1, scale,1);
            case "z" -> this.applyScale(1,1, scale);
        }
    }

    /**
     * @param direction The direction to move the model in.
     * @param distance The distance to move the model.
     */
    public void move(Direction direction, float distance) {
        position.add(direction.step().mul(distance));
    }

    /**
     * @param axis The axis to move the model.
     * @param distance The distance to move the model.
     */
    public void move(String axis, float distance) {
        switch (axis) {
            case "x" -> position.add(distance,0,0);
            case "y" -> position.add(0,distance,0);
            case "z" -> position.add(0,0,distance);
        }
    }
}
