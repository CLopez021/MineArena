package com.knkevin.ai_builder.models;

import com.knkevin.ai_builder.models.util.Point;
import com.knkevin.ai_builder.models.util.Triangle;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

import java.util.*;

/**
 * Represents a 3D model created from an stl file.
 */
public class StlModel extends Model {
    /**
     * A list of triangles that make up the model.
     */
    protected List<Triangle> triangles = new ArrayList<>();

    /**
     * The default block that makes up this model.
     */
    private final BlockState block = Blocks.STONE.defaultBlockState();

    /**
     * @param triangle Adds the triangle to the list of triangles, updating the size of the model.
     */
    protected void addTriangle(Triangle triangle) {
        for (Point p: triangle.getVertices()) {
            Vector3f temp = new Vector3f(p.x, p.y, p.z);
            minCorner.min(temp);
            maxCorner.max(temp);
        }
        this.triangles.add(triangle);
    }

    /**
     * @see Model#getTriangles()
     */
    public List<Triangle> getTriangles() {
        List<Triangle> triangles = new ArrayList<>();
        for (Triangle triangle: this.triangles) {
            triangles.add(triangle.transformed(getTransformationMatrix()));
        }
        return triangles;
    }

    /**
     * @see Model#centerModel()
     */
    protected void centerModel() {
        Vector3f center = maxCorner.sub(minCorner, new Vector3f()).div(2);
        for (Triangle triangle: this.triangles)
            for (Point p: triangle.getVertices()) {
                p.x -= minCorner.x + center.x;
                p.y -= minCorner.y + center.y;
                p.z -= minCorner.z + center.z;
            }
        center.mul(-1, minCorner);
        center.mul(1, maxCorner);
    }

    /**
     * @see Model#getTextureToBlocks()
     */
    public Map<BlockPos, BlockState> getTextureToBlocks() {
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        Set<Point> points = new HashSet<>();
        for (Triangle triangle: this.triangles)
            for (Point p: triangle.transformed(this.getTransformationMatrix()).getBlockPoints(points))
                blocks.put(p.blockPos(), this.block);
        return blocks;
    }

    /**
     * @see Model#updateBlockFaces()
     */
    public void updateBlockFaces() {
        textureToBlocks.clear();
        renderFaces.clear();
        Set<Point> defaultBlock  = new HashSet<>();
        for (Triangle triangle: this.triangles) {
            for (Point p: triangle.transformed(this.getTransformationMatrix()).getBlockPoints(new HashSet<>())) {
                renderFaces.putIfAbsent(p, 0);
                defaultBlock.add(p);
            }
        }
        textureToBlocks.put("iron_block", defaultBlock);
    }
}