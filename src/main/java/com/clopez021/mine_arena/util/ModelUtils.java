package com.clopez021.mine_arena.util;

import com.clopez021.mine_arena.model3d.Model;
import com.clopez021.mine_arena.model3d.ObjModel;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.io.FilenameUtils;
import org.openjdk.nashorn.internal.runtime.regexp.joni.exception.ValueException;

/** Utilities for loading models from files and resources. */
public final class ModelUtils {
  private ModelUtils() {}

  /**
   * Load a model from a file path.
   *
   * @param file file pointing to the model.
   */
  public static Model loadModel(File file) throws IOException, ValueException {
    String extension = FilenameUtils.getExtension(file.getName()).toLowerCase();
    if (extension.equals("obj")) {
      return new ObjModel(file);
    }
    throw new ValueException("Error: The file is not a valid OBJ model file.");
  }

  /**
   * Copy OBJ/MTL/textures from the classpath under assets into the local models/ folder, then load.
   * Example directory: mine_arena:models/fireball, baseName: fireball
   */
  public static Model loadModelFromResources(ResourceLocation directory, String baseName)
      throws IOException, ValueException {
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
      try {
        copyClasspathResource(texPath, texOut);
      } catch (IOException ignored) {
      }
    }

    // 4) Load via existing file-based loader
    return loadModel(objOut);
  }

  private static void copyClasspathResource(String classpathPath, File out) throws IOException {
    try (InputStream in = ModelUtils.class.getResourceAsStream(classpathPath)) {
      if (in == null) throw new IOException("Resource not found: " + classpathPath);
      if (out.getParentFile() != null) out.getParentFile().mkdirs();
      try (FileOutputStream fos = new FileOutputStream(out)) {
        in.transferTo(fos);
      }
    }
  }

  private static List<String> copyMtlAndCollectTexturesFromClasspath(String classpathPath, File out)
      throws IOException {
    List<String> textures = new ArrayList<>();
    if (out.getParentFile() != null) out.getParentFile().mkdirs();
    StringBuilder content = new StringBuilder();
    try (InputStream in = ModelUtils.class.getResourceAsStream(classpathPath)) {
      if (in == null) throw new IOException("Resource not found: " + classpathPath);
      try (BufferedReader br =
          new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
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
