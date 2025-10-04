package com.clopez021.mine_arena.model3d;

import com.clopez021.mine_arena.model3d.util.Palette;
import com.clopez021.mine_arena.model3d.util.Point;
import com.clopez021.mine_arena.model3d.util.Triangle;
import com.clopez021.mine_arena.model3d.util.VectorColors;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import javax.imageio.ImageIO;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;
import org.joml.Vector4i;

/** Represents a 3D Model created from an obj file. */
public class ObjModel extends Model {
  /** The default material for missing or unassigned textures. */
  public static final String DEFAULT_MATERIAL = "iron_block  ";

  public static final float MODEL_SCALE = 30;

  /** The default color for missing or unassigned textures. */
  public static final int DEFAULT_COLOR = (255 << 24) | (128 << 16) | (128 << 8) | 128;

  /**
   * @param file A file to the texture to be opened.
   * @return A BufferedImage representing the texture if opened successfully, null otherwise.
   */
  private static BufferedImage openTexture(File file) {
    try {
      return ImageIO.read(file);
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Rotate a map of local block positions around Y (yaw) and X (pitch) axes by the given degrees.
   * This mimics the renderer transform order: Y by -yaw, then X by +pitch. Positions are rotated as
   * floats and then rounded to nearest integer cell; state facings/axis are updated by rotating
   * their direction vectors and snapping to the nearest cardinal.
   */
  public static Map<BlockPos, BlockState> rotateBlocks3D(
      Map<BlockPos, BlockState> blocks, float yawDegrees, float pitchDegrees) {
    if (blocks.isEmpty()) return new HashMap<>();

    // Match in-game visuals: apply -yaw (clockwise when looking down from +Y)
    // then pitch in the same sign as the player's XRot (down is +, up is -).
    double yawRad = Math.toRadians(-yawDegrees);
    double pitchRad = Math.toRadians(pitchDegrees);

    // Compute bounds center to use as rotation pivot (prevents orbiting)
    int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
    int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
    for (BlockPos p : blocks.keySet()) {
      minX = Math.min(minX, p.getX());
      minY = Math.min(minY, p.getY());
      minZ = Math.min(minZ, p.getZ());
      maxX = Math.max(maxX, p.getX() + 1);
      maxY = Math.max(maxY, p.getY() + 1);
      maxZ = Math.max(maxZ, p.getZ() + 1);
    }
    double cx = (minX + maxX) * 0.5; // center in continuous space
    double cy = (minY + maxY) * 0.5;
    double cz = (minZ + maxZ) * 0.5;

    double cosY = Math.cos(yawRad), sinY = Math.sin(yawRad);
    double cosP = Math.cos(pitchRad), sinP = Math.sin(pitchRad);

    Map<BlockPos, BlockState> out = new HashMap<>(blocks.size());
    for (Map.Entry<BlockPos, BlockState> e : blocks.entrySet()) {
      BlockPos p = e.getKey();
      BlockState state = e.getValue();

      // Work with voxel centers to reduce rounding bias
      double px = p.getX() + 0.5;
      double py = p.getY() + 0.5;
      double pz = p.getZ() + 0.5;

      // Translate to pivot
      double dx = px - cx;
      double dy = py - cy;
      double dz = pz - cz;

      // Pitch first (about X)
      double x1 = dx;
      double y1 = dy * cosP - dz * sinP;
      double z1 = dy * sinP + dz * cosP;

      // Then yaw (about Y)
      double x2 = x1 * cosY + z1 * sinY;
      double y2 = y1;
      double z2 = -x1 * sinY + z1 * cosY;

      // Translate back from pivot
      double xc = cx + x2;
      double yc = cy + y2;
      double zc = cz + z2;

      // Map rotated center to integer cell (lower corner)
      int rx = (int) Math.floor(xc);
      int ry = (int) Math.floor(yc);
      int rz = (int) Math.floor(zc);

      out.put(new BlockPos(rx, ry, rz), state);
    }
    return out;
  }

  /**
   * @param img A BufferedImage representing a texture.
   * @param x A number 0-1 representing the horizontal position in the texture.
   * @param y A number 0-1 representing the vertical position in the texture.
   * @return The color at the location in the texture as an integer, 0 if the texture is null.
   */
  private static int getColor(BufferedImage img, float x, float y) {
    if (img == null) return DEFAULT_COLOR;
    int width = img.getWidth();
    int height = img.getHeight();
    y = 1 - y;

    int tx = (int) (x * width);
    int ty = (int) (y * height);

    tx = ((tx % width) + width) % width;
    ty = ((ty % height) + height) % height;

    return img.getRGB(tx, ty);
  }

  /** A Map that maps material names to a list of Faces that use that material. */
  private final HashMap<String, List<Face>> materialFaceMap = new HashMap<>();

  /** A Map that maps material names to Files to texture images. */
  private final HashMap<String, BufferedImage> materialFileMap = new HashMap<>();

  /** A Map that maps material names to colors. */
  private final HashMap<String, Integer> materialColorMap = new HashMap<>();

  /** A list of float arrays that represent xyz coordinates of a vertex. */
  private final List<float[]> positionVertices = new ArrayList<>();

  /** A list of float arrays that represent uv coordinates of a vertex. */
  private final List<float[]> textureVertices = new ArrayList<>();

  /** The name of the current material in the mtl or obj file. */
  private String currentMaterial;

  /** Base directory of the input OBJ/MTL files for resolving relative paths. */
  private final File baseDir;

  /**
   * @param file A file to the obj file.
   * @throws IOException The file could not be opened.
   */
  public ObjModel(File file) throws IOException {
    this.baseDir = file.getParentFile();
    readMtl(file);
    currentMaterial = DEFAULT_MATERIAL;
    materialFaceMap.put(currentMaterial, new ArrayList<>());
    readObj(file);
    setScale(MODEL_SCALE);
    centerModel();
    updateBlockFaces();
  }

  /**
   * @see Model#getTextureToBlocks()
   */
  public Map<BlockPos, BlockState> getTextureToBlocks() {
    Map<BlockPos, BlockState> blocks = new HashMap<>();
    for (String material : materialFaceMap.keySet()) {
      BufferedImage texture = materialFileMap.get(material);
      for (Face face : materialFaceMap.get(material)) {
        for (Point p : face.getBlockPoints()) {
          int color =
              texture != null
                  ? ObjModel.getColor(texture, p.tx, p.ty)
                  : materialColorMap.getOrDefault(material, -1);
          BlockState blockState = Palette.getNearestBlock(color);
          blocks.put(p.blockPos(), blockState);
        }
      }
    }
    return blocks;
  }

  /**
   * @see Model#centerModel()
   */
  protected void centerModel() {
    Vector3f center = maxCorner.sub(minCorner, new Vector3f()).div(2);
    for (float[] xyz : positionVertices) {
      xyz[0] -= minCorner.x + center.x;
      xyz[1] -= minCorner.y + center.y;
      xyz[2] -= minCorner.z + center.z;
    }
    center.mul(-1, minCorner);
    center.mul(1, maxCorner);
  }

  /**
   * @see Model#updateBlockFaces()
   */
  protected void updateBlockFaces() {
    textureToBlocks.clear();
    renderFaces.clear();
    for (Map.Entry<String, List<Face>> entry : materialFaceMap.entrySet()) {
      String material = entry.getKey();
      List<Face> faces = entry.getValue();
      BufferedImage texture = materialFileMap.get(material);
      for (Face face : faces) {
        for (Triangle triangle : face.getTriangles()) {
          for (Point p : triangle.getBlockPoints(new HashSet<>())) {
            int color =
                texture != null
                    ? ObjModel.getColor(texture, p.tx, p.ty)
                    : materialColorMap.getOrDefault(material, -1);
            String blockTexture = Palette.getNearestBlockTexture(color);
            if (!blockTexture.equals("air"))
              textureToBlocks
                  .computeIfAbsent(blockTexture, ignored -> ConcurrentHashMap.newKeySet())
                  .add(p);
            renderFaces.putIfAbsent(p, 0);
          }
        }
      }
    }
  }

  /**
   * Reads and parses the mtl file line by line.
   *
   * @param file A file to the mtl file.
   * @throws IOException The file could not be opened or read.
   */
  private void readMtl(File file) throws IOException {
    String objName = file.getName();
    String baseName = objName.substring(0, objName.length() - 4);
    // Only resolve MTL relative to the OBJ's directory
    File mtlFile = new File(baseDir, baseName + ".mtl");
    if (!mtlFile.exists()) return;

    try (BufferedReader bufferedReader = new BufferedReader(new FileReader(mtlFile))) {
      String line;
      while ((line = bufferedReader.readLine()) != null) {
        readMtlLine(line.strip().replaceAll(" +", " ").split(" ", 2));
      }
    }
  }

  /**
   * Reads the first string in the array and calls the appropriate method to parse the array of
   * strings.
   *
   * @param line An array of strings representing a line from the mtl file.
   */
  private void readMtlLine(String[] line) {
    try {
      switch (line[0]) {
        case "newmtl" -> readNewMtl(line);
        case "map_Kd" -> readMapKd(line);
        case "Kd" -> readKd(line);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Parses the array of strings and sets currentMaterial to the material referenced by the command.
   * Adds a new List to materialFaceMap for this material.
   *
   * @param line An array of strings representing a newmtl command.
   */
  private void readNewMtl(String[] line) {
    currentMaterial = line[1];
    materialFaceMap.put(currentMaterial, new ArrayList<>());
  }

  /**
   * Parses the array of strings and adds a File to materialFileMap
   *
   * @param line An array of strings representing a map_kd command.
   */
  private void readMapKd(String[] line) {
    // Resolve texture path strictly relative to the OBJ/MTL directory
    File texturePath = new File(baseDir, line[1]);
    if (texturePath.isFile()) {
      materialFileMap.put(currentMaterial, ObjModel.openTexture(texturePath));
    }
  }

  /**
   * Parses the array of strings and adds
   *
   * @param line An array of strings representing a Kd command.
   */
  private void readKd(String[] line) {
    String[] colors = line[1].split(" ");
    int red = (int) (Float.parseFloat(colors[0]) * 255),
        green = (int) (Float.parseFloat(colors[1]) * 255),
        blue = (int) (Float.parseFloat(colors[2]) * 255);
    int color = VectorColors.ARGBToInt(new Vector4i(255, red, green, blue));
    materialColorMap.put(currentMaterial, color);
  }

  /**
   * Reads and parses the obj file line by line.
   *
   * @param file A file to the obj file.
   * @throws IOException The file could not be opened or read.
   */
  private void readObj(File file) throws IOException {
    try (BufferedReader bufferedReader = new BufferedReader(new FileReader(file))) {
      String line;
      while ((line = bufferedReader.readLine()) != null) {
        readObjLine(line.strip().replaceAll(" +", " ").split(" "));
      }
    }
  }

  /**
   * Reads the first string in the array and calls the appropriate method to parse the array of
   * strings.
   *
   * @param line An array of strings representing a line from the obj file.
   */
  private void readObjLine(String[] line) {
    try {
      switch (line[0]) {
        case "v" -> readVertex(line);
        case "vt" -> readTextureVertex(line);
        case "usemtl" -> readUseMaterial(line);
        case "f" -> readFace(line);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Parses the array of strings and adds the position vertex as a float array to this model's list
   * of position vertices.
   *
   * @param line An array of strings representing a vertex command.
   */
  private void readVertex(String[] line) {
    float x = Float.parseFloat(line[1]),
        y = Float.parseFloat(line[2]),
        z = Float.parseFloat(line[3]);
    positionVertices.add(new float[] {x, y, z});
    Vector3f temp = new Vector3f(x, y, z);
    minCorner.min(temp);
    maxCorner.max(temp);
  }

  /**
   * Parses the array of strings and adds the texture vertex as a float array to this model's list
   * of texture vertices.
   *
   * @param line An array of strings representing a texture vertex command.
   */
  private void readTextureVertex(String[] line) {
    textureVertices.add(new float[] {Float.parseFloat(line[1]), Float.parseFloat(line[2])});
  }

  /**
   * Parses the array of strings and sets the current material if the material was defined in the
   * mtl file.
   *
   * @param line An array of strings representing a usemtl command.
   */
  private void readUseMaterial(String[] line) {
    currentMaterial = DEFAULT_MATERIAL;
    if (line.length > 1 && materialFaceMap.containsKey(line[1])) currentMaterial = line[1];
  }

  /**
   * Parses the array of strings and adds the face to the corresponding list of Faces based on the
   * current material.
   *
   * @param line An array of strings representing a face command.
   */
  private void readFace(String[] line) {
    materialFaceMap.get(currentMaterial).add(new Face(line));
  }

  /**
   * @see Model#getTriangles()
   */
  public List<Triangle> getTriangles() {
    List<Triangle> triangles = new ArrayList<>();
    for (Map.Entry<String, List<Face>> entry : materialFaceMap.entrySet()) {
      List<Face> faces = entry.getValue();
      for (Face face : faces) {
        face.getTriangles(triangles);
      }
    }
    return triangles;
  }

  /** Represents a face in an obj model. */
  private class Face {
    /** An array of indices corresponding to vertices in the obj model. */
    private final int[] vertexIndices;

    /** An array of indices corresponding to texture vertices in the obj model. */
    private final int[] textureIndices;

    /** The number of vertices that make up the Face. */
    private final int numVertices;

    /** True if there are indices to texture vertices, false otherwise. */
    private boolean textured;

    /**
     * @param line An array of strings containing face data.
     */
    private Face(String[] line) {
      numVertices = line.length - 1;
      vertexIndices = new int[numVertices];
      textureIndices = new int[numVertices];
      for (int i = 1; i < line.length; ++i) {
        String[] numbers = line[i].split("/");
        int vertexIndex = Integer.parseInt(numbers[0]);
        vertexIndex = vertexIndex < 0 ? positionVertices.size() + vertexIndex : vertexIndex - 1;
        vertexIndices[i - 1] = vertexIndex;
        if (numbers.length >= 2 && !numbers[1].isEmpty()) {
          int textureIndex = Integer.parseInt(numbers[1]);
          textureIndex =
              textureIndex < 0 ? textureVertices.size() + textureIndex : textureIndex - 1;
          textureIndices[i - 1] = textureIndex;
          textured = true;
        }
      }
    }

    /**
     * @return A list of Points representing the vertices that make up this Face, transformed into
     *     world coordinates.
     */
    private List<Point> getVertices() {
      List<Point> vertices = new ArrayList<>();
      for (int i = 0; i < numVertices; ++i) {
        float[] xyz = positionVertices.get(vertexIndices[i]);
        float[] uv = new float[2];
        if (textured) uv = textureVertices.get(textureIndices[i]);
        vertices.add(new Point(xyz, uv).transformed(ObjModel.this.getTransformationMatrix()));
      }
      return vertices;
    }

    /**
     * @return A list of Triangles that make up this Face.
     */
    private List<Triangle> getTriangles() {
      List<Triangle> triangles = new ArrayList<>();
      List<Point> vertices = getVertices();
      Point p1 = vertices.get(0);
      for (int i = 1; i < numVertices - 1; ++i) {
        Point p2 = vertices.get(i);
        Point p3 = vertices.get(i + 1);
        triangles.add(new Triangle(p1, p2, p3));
      }
      return triangles;
    }

    /**
     * @return A list of Triangles that make up this Face.
     */
    private List<Triangle> getTriangles(List<Triangle> triangles) {
      List<Point> vertices = getVertices();
      Point p1 = vertices.get(0);
      for (int i = 1; i < numVertices - 1; ++i) {
        Point p2 = vertices.get(i);
        Point p3 = vertices.get(i + 1);
        triangles.add(new Triangle(p1, p2, p3));
      }
      return triangles;
    }

    /**
     * @return A set of points inside the polygon described by this Face using integer coordinates.
     */
    private Set<Point> getBlockPoints() {
      Set<Point> blockPoints = new HashSet<>();
      for (Triangle triangle : getTriangles()) triangle.getBlockPoints(blockPoints);
      return blockPoints;
    }
  }
}
