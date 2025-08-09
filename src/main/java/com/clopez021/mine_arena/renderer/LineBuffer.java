package com.clopez021.mine_arena.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * A utility class used for rendering lines.
 */
public class LineBuffer {
    /**
     * Buffer used for adding vertices.
     */
    private final BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);

    /**
     * Transformation matrix.
     */
    private final Matrix4f matrix4f;

    /**
     * Rotation quaternion
     */
    private final Quaternionf rotation;

    /**
     * Coordinates for the start point of a line.
     */
    private float x1, y1, z1;

    /**
     * Default color values.
     */
    private int red = 255, green = 255, blue = 255, alpha = 255;

    public LineBuffer(Matrix4f m4, Quaternionf r) {
        //RenderSystem Settings
        RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();

        matrix4f = m4;
        rotation = r;
    }

    /**
     * End the buffer and render.
     */
    public void draw() {
        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    /**
     * Sets the color of this LineBuffer.
     * @param r Red.
     * @param g Green.
     * @param b Blue.
     * @param a Alpha.
     * @return This LineBuffer.
     */
    public LineBuffer setColor(int r, int g, int b, int a) {
        red = r;
        green = g;
        blue = b;
        alpha = a;
        return this;
    }

    /**
     * Sets the start point of the line to draw.
     * @param x X-coordinate.
     * @param y Y-coordinate.
     * @param z Z-coordinate.
     * @return This LineBuffer.
     */
    public LineBuffer beginLine(float x, float y, float z) {
        x1 = x;
        y1 = y;
        z1 = z;
        return this;
    }

    /**
     * Creates a line from the start point to this end point and writes it to the buffer.
     * @param x X-coordinate.
     * @param y Y-coordinate.
     * @param z Z-coordinate.
     * @return This LineBuffer.
     */
    public LineBuffer endLine(float x, float y, float z) {
        float dx = x-x1, dy = y-y1, dz = z-z1;
        float distance = (float) Math.sqrt(dx*dx + dy*dy + dz*dz);
        Vector3f normal = new Vector3f(dx/distance, dy/distance, dz/distance);
        normal = normal.rotate(rotation);
        buffer.addVertex(matrix4f, x1, y1, z1).setColor(red, green, blue, alpha).setNormal(normal.x, normal.y, normal.z);
        buffer.addVertex(matrix4f, x, y, z).setColor(red, green, blue, alpha).setNormal(normal.x, normal.y, normal.z);
        return this;
    }
}