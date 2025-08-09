package com.clopez021.mine_arena.key_bindings;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public final class KeyBindings {
    public static final String KEY_CATEGORY = "key.category.mine_arena.mine_arena";
    public static final String KEY_PLACE = "key.mine_arena.place";
    public static final String KEY_UNDO = "key.mine_arena.undo";
    public static final String KEY_SCALE = "key.mine_arena.scale";
    public static final String KEY_ROTATE = "key.mine_arena.rotate";
    public static final String KEY_TRANSLATE = "key.mine_arena.translate";
    public static final String KEY_X_AXIS = "key.mine_arena.x_axis";
    public static final String KEY_Y_AXIS = "key.mine_arena.y_axis";
    public static final String KEY_Z_AXIS = "key.mine_arena.z_axis";
    public static final String KEY_TOGGLE_VIEW = "key.mine_arena.toggle_view";

    public static final KeyMapping PLACE_KEY = new KeyMapping(KEY_PLACE, KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_P, KEY_CATEGORY);
    public static final KeyMapping UNDO_KEY = new KeyMapping(KEY_UNDO, KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_U, KEY_CATEGORY);
    public static final KeyMapping SCALE_KEY = new KeyMapping(KEY_SCALE, KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_S, KEY_CATEGORY);
    public static final KeyMapping ROTATE_KEY = new KeyMapping(KEY_ROTATE, KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, KEY_CATEGORY);
    public static final KeyMapping TRANSLATE_KEY = new KeyMapping(KEY_TRANSLATE, KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_M, KEY_CATEGORY);
    public static final KeyMapping X_AXIS_KEY = new KeyMapping(KEY_X_AXIS, KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_X, KEY_CATEGORY);
    public static final KeyMapping Y_AXIS_KEY = new KeyMapping(KEY_Y_AXIS, KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Y, KEY_CATEGORY);
    public static final KeyMapping Z_AXIS_KEY = new KeyMapping(KEY_Z_AXIS, KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Z, KEY_CATEGORY);
    public static final KeyMapping TOGGLE_VIEW_KEY = new KeyMapping(KEY_TOGGLE_VIEW, KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, KEY_CATEGORY);
}