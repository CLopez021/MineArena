package com.clopez021.mine_arena.command;

import com.clopez021.mine_arena.MineArena;
import com.clopez021.mine_arena.models.Model;
import com.clopez021.mine_arena.models.ObjModel;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.apache.commons.io.FilenameUtils;
import org.openjdk.nashorn.internal.runtime.regexp.joni.exception.ValueException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles logic dealing with loading a new Model through a command.
 */
public class LoadCommand {
    /**
     * Attempts to load a Model from the file specified by the command.
     * @param command The executed command.
     * @return A 1 or 0 representing the success of the command.
     */
    protected static int load(CommandContext<CommandSourceStack> command) {
        if (!Minecraft.getInstance().isSingleplayer()) {
            command.getSource().sendFailure(Component.literal("Error: Models can only be loaded in single-player!"));
            return 0;
        }
        try {
            String fileName = StringArgumentType.getString(command, "filename");
            MineArena.model = loadModel(new File("models/" + fileName));
            command.getSource().sendSystemMessage(Component.literal(fileName + " loaded successfully."));
            return 1;
        } catch (Exception e) {
            command.getSource().sendFailure(Component.literal("Error: The model could not be loaded."));
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * @param file A File to a 3D model.
     * @return A Model constructed from the file.
     * @throws IOException The File could not be opened.
     * @throws ValueException The File was not an OBJ file.
     */
    public static Model loadModel(File file) throws IOException, ValueException {
        String extension = FilenameUtils.getExtension(file.getName()).toLowerCase();
        if (extension.equals("obj")) {
            return new ObjModel(file);
        }
        throw new ValueException("Error: The file is not a valid OBJ model file.");
    }

    /**
     * Server-side helper: copy OBJ/MTL/textures from assets (resource pack) into the local
     * "models/" folder, then load with the existing file-based parser.
     * Example directory: mine_arena:models/fireball, baseName: example_model
     */
    public static Model loadModelFromResources(ResourceLocation directory, String baseName) throws IOException, ValueException {
        // Build classpath resource roots
        String root = "/assets/" + directory.getNamespace() + "/" + directory.getPath() + "/";

        // 1) Copy OBJ
        String objPath = root + baseName + ".obj";
        File objOut = new File("models/" + baseName + ".obj");
        copyClasspathResource(objPath, objOut);

        // 2) Copy MTL + collect textures
        String mtlPath = root + baseName + ".mtl";
        File mtlOut = new File("models/" + baseName + ".mtl");
        List<String> textures = copyMtlAndCollectTexturesFromClasspath(mtlPath, mtlOut);

        // 3) Copy textures to models/<path>
        for (String tex : textures) {
            File texOut = new File("models/" + tex);
            String texPath = root + tex;
            try { copyClasspathResource(texPath, texOut); } catch (IOException ignored) {}
        }

        // 4) Load via existing file-based loader
        return loadModel(objOut);
    }

    private static void copyClasspathResource(String classpathPath, File out) throws IOException {
        try (InputStream in = LoadCommand.class.getResourceAsStream(classpathPath)) {
            if (in == null) throw new IOException("Resource not found: " + classpathPath);
            out.getParentFile().mkdirs();
            try (FileOutputStream fos = new FileOutputStream(out)) {
                in.transferTo(fos);
            }
        }
    }

    private static List<String> copyMtlAndCollectTexturesFromClasspath(String classpathPath, File out) throws IOException {
        List<String> textures = new ArrayList<>();
        out.getParentFile().mkdirs();
        StringBuilder content = new StringBuilder();
        try (InputStream in = LoadCommand.class.getResourceAsStream(classpathPath)) {
            if (in == null) throw new IOException("Resource not found: " + classpathPath);
            try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    content.append(line).append('\n');
                    String s = line.strip();
                    if (s.isEmpty()) continue;
                    String[] parts = s.replaceAll(" +", " ").split(" ", 2);
                    if (parts.length == 2 && parts[0].equals("map_Kd")) textures.add(parts[1]);
                }
            }
        }
        try (FileOutputStream fos = new FileOutputStream(out)) {
            fos.write(content.toString().getBytes(StandardCharsets.UTF_8));
        }
        return textures;
    }
}
