package com.knkevin.ai_builder.renderer;

import com.knkevin.ai_builder.AIBuilder;
import com.knkevin.ai_builder.items.ModItems;
import com.knkevin.ai_builder.items.custom.HammerModes;
import com.knkevin.ai_builder.models.Model;
import com.knkevin.ai_builder.models.util.Point;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.joml.*;

import java.lang.Math;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.knkevin.ai_builder.items.custom.HammerModes.selectedAxis;
import static com.knkevin.ai_builder.items.custom.HammerModes.viewMode;

/**
 * Handles rendering transformation guides and rendering either a blocks preview or bounding box of the currently loaded Model.
 */
public class BoxRenderer {
    /**
     * The center of the Model.
     */
    private static final Vector3f center = new Vector3f();

    /**
     * The size of the Model.
     */
    private static final Vector3f size = new Vector3f();

    /**
     * The alphas for the x, y, and z axes.
     */
    private static final Vector3i alpha = new Vector3i();

    /**
     * The default texture to be used when rendering the blocks preview of the model.
     */
    private static final ResourceLocation defaultTexture = ResourceLocation.withDefaultNamespace("textures/block/iron_block.png");

    /**
     * A list of points around a circle
     */
    private static final List<Vector2f> circlePoints = new ArrayList<>();

    /**
     * The number of points to be generated around in a circle.
     */
    private static final int numCirclePoints = 32;

    //Initialize points around the circle
    static {
        float radius = size.get(size.maxComponent()) + 1;
        for (int i = 0; i < numCirclePoints; ++i) {
            double angle = 2 * Math.PI * i / numCirclePoints;
            float x = radius * (float) Math.cos(angle);
            float y = radius * (float) Math.sin(angle);
            circlePoints.add(new Vector2f(x, y));
        }
    }

    /**
     * Calls the appropriate rendering functions.
     */
    public static void renderEvent(RenderLevelStageEvent event) {
        Player player = Minecraft.getInstance().player;
        Model model = AIBuilder.model;
        //Only render if player is holding a ModelHammer and if a Model is loaded.
        if (player == null || !player.getMainHandItem().getItem().equals(ModItems.MODEL_HAMMER.get()) || model == null) return;;

        //Size of bounding box.
        size.set(model.maxCorner).mul(model.scale);

        //Center of model.
        center.set(model.position);

        //Set appropriate axis alpha to 255 and unselected axes to 64.
        if (selectedAxis == HammerModes.Axis.ALL) alpha.set(255);
        else alpha.set(64).setComponent(selectedAxis.component, 255);

        Camera camera = event.getCamera();
        Vector3f camPos = camera.getPosition().toVector3f();
        Matrix4f matrix4f = new Matrix4f().scale(1.001f);
        matrix4f.translate(camPos.negate(new Vector3f()));
        Matrix4f unrotatedMatrix4 = new Matrix4f(matrix4f);
        matrix4f.translate(center).rotate(model.rotation).translate(center.negate(new Vector3f()));

        //Render blocks preview.
        if (viewMode == HammerModes.ViewMode.BLOCKS) renderBlocksPreview(unrotatedMatrix4, camPos, model.blockFaces);

        //Render appropriate visual guides for the transform mode.
        RenderSystem.lineWidth(3);
        switch (HammerModes.transformMode) {
            case ROTATE -> renderRotateGuides(matrix4f, model.rotation);
            case SCALE -> renderScaleGuides(matrix4f, model.rotation);
            case TRANSLATE -> renderTranslationGuides(unrotatedMatrix4, model.rotation);
        }

        //Render bounding box.
        if (viewMode == HammerModes.ViewMode.BOX) {
            //Corners of bounding box.
            Vector3f cornerOne = new Vector3f(center).sub((float) Math.floor(size.x) + .5f, (float) Math.floor(size.y) + .5f, (float) Math.floor(size.z) + .5f);
            Vector3f cornerTwo = new Vector3f(center).add((float) Math.floor(size.x) + .5f, (float) Math.floor(size.y) + .5f, (float) Math.floor(size.z) + .5f);

            //Render model bounding box and box outline.
            RenderSystem.lineWidth(2);
            renderLineBox(matrix4f, model.rotation, cornerOne, cornerTwo, new Vector4i(255, 255, 255, 255));
            renderBox(matrix4f, cornerOne, cornerTwo, new Vector4i(255, 255, 255, 64));
        }

        RenderSystem.enableCull();
    }

    /**
     * Renders the circles that make up the rotation guides.
     * The circles are formed by rendering lines connecting points around a circle.
     * @param matrix4f The transformation matrix.
     */
    private static void renderRotateGuides(Matrix4f matrix4f, Quaternionf rotation) {
        LineBuffer buffer = new LineBuffer(matrix4f, rotation);
        for (int i = 0; i < numCirclePoints; ++i) {
            //Get adjacent points in circle.
            Vector2f p1 = new Vector2f(circlePoints.get(i)).mul(size.get(size.maxComponent()) + 1);
            Vector2f p2 = new Vector2f(circlePoints.get((i + 1) % numCirclePoints)).mul(size.get(size.maxComponent()) + 1);

            //Render red, green, and blue lines of circle.
            buffer.setColor(255,0,0, alpha.x).beginLine(center.x, center.y + p1.x, center.z + p1.y).endLine(center.x, center.y + p2.x, center.z + p2.y);
            buffer.setColor(0,255,0, alpha.y).beginLine(center.x + p1.x, center.y, center.z + p1.y).endLine(center.x + p2.x, center.y, center.z + p2.y);
            buffer.setColor(0,0,255, alpha.z).beginLine(center.x + p1.x, center.y + p1.y, center.z).endLine(center.x + p2.x, center.y + p2.y, center.z);
        }
        buffer.draw();
    }

    /**
     * Renders the lines and boxes that make up the scale guides.
     * @param matrix4f The transformation matrix.
     */
    private static void renderScaleGuides(Matrix4f matrix4f, Quaternionf rotation) {
        //Size of scale guide boxes.
        float boxSize = Math.max(.5f, Math.max(Math.min(size.x, size.y), Math.max(Math.min(size.x, size.z), Math.min(size.y, size.z))))/4;

        //Distance of scale guide boxes from center
        Vector3f offSet = new Vector3f(size).add(boxSize, boxSize, boxSize).add(1,1,1);

        //Render scale guide boxes.
        renderCube(matrix4f, new Vector3f(center).sub(offSet.x, 0,0), boxSize, new Vector4i(255,0,0, alpha.x));
        renderCube(matrix4f, new Vector3f(center).add(offSet.x, 0,0), boxSize, new Vector4i(255,0,0, alpha.x));
        renderCube(matrix4f, new Vector3f(center).sub(0, offSet.y,0), boxSize, new Vector4i(0,255,0, alpha.y));
        renderCube(matrix4f, new Vector3f(center).add(0, offSet.y,0), boxSize, new Vector4i(0,255,0, alpha.y));
        renderCube(matrix4f, new Vector3f(center).sub(0,0, offSet.z), boxSize, new Vector4i(0,0,255, alpha.z));
        renderCube(matrix4f, new Vector3f(center).add(0,0, offSet.z), boxSize, new Vector4i(0,0,255, alpha.z));

        //Render scale guide lines.
        LineBuffer buffer = new LineBuffer(matrix4f, rotation);
        buffer.setColor(255,0,0, alpha.x).beginLine(center.x - offSet.x, center.y, center.z).endLine(center.x + offSet.x, center.y, center.z);
        buffer.setColor(0,255,0, alpha.y).beginLine(center.x, center.y - offSet.y, center.z).endLine(center.x, center.y + offSet.y, center.z);
        buffer.setColor(0,0,255, alpha.z).beginLine(center.x, center.y, center.z - offSet.z).endLine(center.x, center.y, center.z + offSet.z);
        buffer.draw();
    }

    /**
     * Renders the lines that make up the translation guides.
     * @param matrix4f The transformation matrix.
     * @param rotation The rotation of the Model.
     */
    private static void renderTranslationGuides(Matrix4f matrix4f, Quaternionf rotation) {
        //Find the minimum and maximum corners of model after rotated and scaled.
        Vector3f minCorner = new Vector3f();
        Vector3f maxCorner = new Vector3f();
        for (int mask = 0; mask < 8; ++mask) {
            Vector3f corner = new Vector3f(size.x, size.y, size.z).mul((mask & 1) == 1 ? -1 : 1, (mask & 2) == 2 ? -1 : 1, (mask & 4) == 4 ? -1 : 1).rotate(rotation);
            minCorner.min(corner);
            maxCorner.max(corner);
        }

        //Length of translation guidelines.
        float size = (maxCorner.sub(minCorner).get(maxCorner.maxComponent()) + 2) * .75f;

        //Render translation guide lines.
        LineBuffer buffer = new LineBuffer(matrix4f, rotation);
        buffer.setColor(255, 0,0, alpha.x).beginLine(center.x - size, center.y, center.z).endLine(center.x + size, center.y, center.z);
        buffer.setColor(0,255,0, alpha.y).beginLine(center.x, center.y - size, center.z).endLine(center.x, center.y + size, center.z);
        buffer.setColor(0,0,255, alpha.z).beginLine(center.x, center.y, center.z - size).endLine(center.x, center.y, center.z + size);
        buffer.draw();
    }

    /**
     * Renders a preview of the Model as blocks.
     * @param matrix4f The transformation matrix.
     * @param camera The camera position.
     * @param blocks A map of block names to sets of Points
     */
    private static void renderBlocksPreview(Matrix4f matrix4f, Vector3f camera, ConcurrentMap<String, Set<Point>> blocks) {
        //RenderSystem settings.
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();

        //Calculating where to cull faces that are facing away from the camera.
        Vector3f cullNegative = new Vector3f(camera);
        Vector3f cullPositive = new Vector3f(cullNegative);

        //Two points for minimum and maximum corners of blocks.
        Vector3f p1 = new Vector3f();
        Vector3f p2 = new Vector3f();

        for (Map.Entry<String, Set<Point>> blockEntry: blocks.entrySet()) {
            RenderSystem.setShaderTexture(0, ResourceLocation.withDefaultNamespace("textures/block/" + blockEntry.getKey() + ".png"));

            //Getting and starting buffer
            BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

            for (Point p: blockEntry.getValue()) {
                //Setting the two corners.
                p1.set(p.x-.4995f, p.y-.4995f, p.z-.4995f).add(center);
                p2.set(p1).add(.999f,.999f,.999f);

                //Render each of the six faces if it is facing towards the camera, and if its corresponding bit is a 1.
                // - x
                if (p1.x > cullNegative.x) {
                    buffer.addVertex(matrix4f, p1.x, p1.y, p1.z).setUv(1,1);
                    buffer.addVertex(matrix4f, p1.x, p1.y, p2.z).setUv(1,0);
                    buffer.addVertex(matrix4f, p1.x, p2.y, p2.z).setUv(0,0);
                    buffer.addVertex(matrix4f, p1.x, p2.y, p1.z).setUv(0,1);
                }
                // + x
                if (p2.x < cullPositive.x ) {
                    buffer.addVertex(matrix4f, p2.x, p1.y, p1.z).setUv(0,0);
                    buffer.addVertex(matrix4f, p2.x, p2.y, p1.z).setUv(0,1);
                    buffer.addVertex(matrix4f, p2.x, p2.y, p2.z).setUv(1,1);
                    buffer.addVertex(matrix4f, p2.x, p1.y, p2.z).setUv(1,0);
                }
                // - y
                if (p1.y > cullNegative.y) {
                    buffer.addVertex(matrix4f, p1.x, p1.y, p1.z).setUv(0,0);
                    buffer.addVertex(matrix4f, p2.x, p1.y, p1.z).setUv(0,1);
                    buffer.addVertex(matrix4f, p2.x, p1.y, p2.z).setUv(1,1);
                    buffer.addVertex(matrix4f, p1.x, p1.y, p2.z).setUv(1,0);
                }
                // + y
                if (p2.y < cullPositive.y) {
                    buffer.addVertex(matrix4f, p1.x, p2.y, p1.z).setUv(1,1);
                    buffer.addVertex(matrix4f, p1.x, p2.y, p2.z).setUv(1,0);
                    buffer.addVertex(matrix4f, p2.x, p2.y, p2.z).setUv(0,0);
                    buffer.addVertex(matrix4f, p2.x, p2.y, p1.z).setUv(0,1);
                }
                // - z
                if (p1.z > cullNegative.z) {
                    buffer.addVertex(matrix4f, p1.x, p1.y, p1.z).setUv(0,0);
                    buffer.addVertex(matrix4f, p1.x, p2.y, p1.z).setUv(0,1);
                    buffer.addVertex(matrix4f, p2.x, p2.y, p1.z).setUv(1,1);
                    buffer.addVertex(matrix4f, p2.x, p1.y, p1.z).setUv(1,0);
                }
                // + z
                if (p2.z < cullPositive.z) {
                    buffer.addVertex(matrix4f, p1.x, p1.y, p2.z).setUv(1,1);
                    buffer.addVertex(matrix4f, p2.x, p1.y, p2.z).setUv(1,0);
                    buffer.addVertex(matrix4f, p2.x, p2.y, p2.z).setUv(0,0);
                    buffer.addVertex(matrix4f, p1.x, p2.y, p2.z).setUv(0,1);
                }
            }
            try {
                BufferUploader.drawWithShader(buffer.buildOrThrow());
            } catch (IllegalStateException ignored) {}
        }
    }


    /**
     * Renders the outline of a box.
     * @param matrix4f The transformation matrix.
     * @param cornerOne The first corner of the box.
     * @param cornerTwo The second corner of the box.
     * @param color The color of the box.
     */
    public static void renderLineBox(Matrix4f matrix4f, Quaternionf rotation, Vector3f cornerOne, Vector3f cornerTwo, Vector4i color) {
        //Get minimum and maximum corners of the box.
        Vector3f p1 = new Vector3f(cornerOne).min(cornerTwo);
        Vector3f p2 = new Vector3f(cornerTwo).max(cornerTwo);

        //Render three lines from four non-adjacent corners.
        //Render three lines from four non-adjacent corners.
        LineBuffer buffer = new LineBuffer(matrix4f, rotation);
        buffer.setColor(color.x, color.y, color.z, color.w);
        buffer.beginLine(p1.x, p1.y, p1.z).endLine(p2.x, p1.y, p1.z).endLine(p1.x, p2.y, p1.z).endLine(p1.x, p1.y, p2.z);
        buffer.beginLine(p2.x, p1.y, p2.z).endLine(p1.x, p1.y, p2.z).endLine(p2.x, p2.y, p2.z).endLine(p2.x, p1.y, p1.z);
        buffer.beginLine(p2.x, p2.y, p1.z).endLine(p1.x, p2.y, p1.z).endLine(p2.x, p1.y, p1.z).endLine(p2.x, p2.y, p2.z);
        buffer.beginLine(p1.x, p2.y, p2.z).endLine(p1.x, p2.y, p1.z).endLine(p1.x, p1.y, p2.z).endLine(p2.x, p2.y, p2.z);
        buffer.draw();
    }

    /**
     * Renders a filled box.
     * @param matrix4f The transformation matrix.
     * @param cornerOne The first corner of the box.
     * @param cornerTwo The second corner of the box.
     * @param color The color of the box.
     */
    public static void renderBox(Matrix4f matrix4f, Vector3f cornerOne, Vector3f cornerTwo, Vector4i color) {
        //RenderSystem settings.
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();

        //Get minimum and maximum corners of the box.
        Vector3f p1 = new Vector3f(cornerOne).min(cornerTwo);
        Vector3f p2 = new Vector3f(cornerTwo).max(cornerTwo);

        //Render each side of the box.
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        //-x
        buffer.addVertex(matrix4f, p1.x, p1.y, p1.z).setColor(color.x, color.y, color.z, color.w);
        buffer.addVertex(matrix4f, p1.x, p1.y, p2.z).setColor(color.x, color.y, color.z, color.w);
        buffer.addVertex(matrix4f, p1.x, p2.y, p2.z).setColor(color.x, color.y, color.z, color.w);
        buffer.addVertex(matrix4f, p1.x, p2.y, p1.z).setColor(color.x, color.y, color.z, color.w);

        //+x
        buffer.addVertex(matrix4f, p2.x, p1.y, p1.z).setColor(color.x, color.y, color.z, color.w);
        buffer.addVertex(matrix4f, p2.x, p2.y, p1.z).setColor(color.x, color.y, color.z, color.w);
        buffer.addVertex(matrix4f, p2.x, p2.y, p2.z).setColor(color.x, color.y, color.z, color.w);
        buffer.addVertex(matrix4f, p2.x, p1.y, p2.z).setColor(color.x, color.y, color.z, color.w);

        //-y
        buffer.addVertex(matrix4f, p1.x, p1.y, p1.z).setColor(color.x, color.y, color.z, color.w);
        buffer.addVertex(matrix4f, p2.x, p1.y, p1.z).setColor(color.x, color.y, color.z, color.w);
        buffer.addVertex(matrix4f, p2.x, p1.y, p2.z).setColor(color.x, color.y, color.z, color.w);
        buffer.addVertex(matrix4f, p1.x, p1.y, p2.z).setColor(color.x, color.y, color.z, color.w);

        //+y
        buffer.addVertex(matrix4f, p1.x, p2.y, p1.z).setColor(color.x, color.y, color.z, color.w);
        buffer.addVertex(matrix4f, p1.x, p2.y, p2.z).setColor(color.x, color.y, color.z, color.w);
        buffer.addVertex(matrix4f, p2.x, p2.y, p2.z).setColor(color.x, color.y, color.z, color.w);
        buffer.addVertex(matrix4f, p2.x, p2.y, p1.z).setColor(color.x, color.y, color.z, color.w);

        //-z
        buffer.addVertex(matrix4f, p1.x, p1.y, p1.z).setColor(color.x, color.y, color.z, color.w);
        buffer.addVertex(matrix4f, p1.x, p2.y, p1.z).setColor(color.x, color.y, color.z, color.w);
        buffer.addVertex(matrix4f, p2.x, p2.y, p1.z).setColor(color.x, color.y, color.z, color.w);
        buffer.addVertex(matrix4f, p2.x, p1.y, p1.z).setColor(color.x, color.y, color.z, color.w);

        //+z
        buffer.addVertex(matrix4f, p1.x, p1.y, p2.z).setColor(color.x, color.y, color.z, color.w);
        buffer.addVertex(matrix4f, p2.x, p1.y, p2.z).setColor(color.x, color.y, color.z, color.w);
        buffer.addVertex(matrix4f, p2.x, p2.y, p2.z).setColor(color.x, color.y, color.z, color.w);
        buffer.addVertex(matrix4f, p1.x, p2.y, p2.z).setColor(color.x, color.y, color.z, color.w);

        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    /**
     * Renders a filled cube.
     * @param matrix4f The transformation matrix.
     * @param center The center of the cube.
     * @param s The side length of the cube.
     * @param color The color of the cube.
     */
    public static void renderCube(Matrix4f matrix4f, Vector3f center, float s, Vector4i color) {
        renderBox(matrix4f, new Vector3f(center).sub(s/2,s/2,s/2), new Vector3f(center).add(s/2,s/2,s/2), color);
    }
}