package com.clopez021.mine_arena.integration.meshy;

import static com.clopez021.mine_arena.config.ServerConfig.meshyApiKey;

import com.clopez021.mine_arena.model3d.Model;
import com.clopez021.mine_arena.model3d.ObjModel;
import com.clopez021.mine_arena.util.ModelUtils;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.function.IntConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class MeshyClient {

  /**
   * Generate a model from a prompt and return the voxelized blocks. Temp files are automatically
   * deleted.
   *
   * @param prompt The text description for model generation
   * @return Map of BlockPos to BlockState representing the voxelized model
   * @throws Exception if generation fails
   */
  public static Map<BlockPos, BlockState> buildBlocksFromPrompt(String prompt) throws Exception {
    if (prompt == null || prompt.isEmpty())
      throw new IllegalArgumentException("prompt cannot be empty");
    IntConsumer noop = p -> {};
    String previewTaskId = createPreviewTask(prompt);
    waitForTask(previewTaskId, 0, noop);
    String refineTaskId = createRefineTask(previewTaskId);
    waitForTask(refineTaskId, 50, noop);

    String[] urls = retrieveTextTo3dTask(refineTaskId, noop);
    String objUrl = urls[0];
    String mtlUrl = urls[1];
    String textureUrl = urls[2];

    String modelName = refineTaskId;
    String textureName = modelName;

    String objPath = "models/" + modelName + ".obj";
    String mtlPath = "models/" + modelName + ".mtl";
    String texturePath = "models/" + textureName + ".png";

    try {
      Files.createDirectories(Paths.get("models"));
      downloadFile(objUrl, objPath);
      downloadFile(mtlUrl, mtlPath);
      downloadFile(textureUrl, texturePath);
      renameInFile(mtlPath, "texture_0", textureName);

      Model model = new ObjModel(new File(objPath));
      return ModelUtils.buildVoxels(model);
    } finally {
      try {
        new File(objPath).delete();
      } catch (Exception ignored) {
      }
      try {
        new File(mtlPath).delete();
      } catch (Exception ignored) {
      }
      try {
        new File(texturePath).delete();
      } catch (Exception ignored) {
      }
    }
  }

  public static void renameInFile(String filePath, String oldStr, String newStr) throws Exception {
    Path path = Paths.get(filePath);
    Charset charset = StandardCharsets.ISO_8859_1;
    String content = Files.readString(path, charset);
    content = content.replace(oldStr, newStr);
    Files.writeString(path, content);
  }

  public static String createPreviewTask(String prompt) {
    try (HttpClient client = HttpClient.newHttpClient()) {
      // Build JSON safely to handle quotes and special characters in prompt
      JsonObject obj = new JsonObject();
      obj.addProperty("mode", "preview");
      obj.addProperty("prompt", prompt);
      obj.addProperty("ai_model", "meshy-4");

      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create("https://api.meshy.ai/openapi/v2/text-to-3d"))
              .header("Authorization", "Bearer " + meshyApiKey)
              .header("Content-Type", "application/json")
              .POST(HttpRequest.BodyPublishers.ofString(obj.toString()))
              .build();

      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      String responseBody = response.body();
      JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();

      int status = response.statusCode();
      switch (status) {
        case 401 -> throw new MeshyExceptions.InvalidApiKeyException();
        case 402 -> throw new MeshyExceptions.PaymentRequiredException();
        case 403 -> throw new MeshyExceptions.TooManyRequestsException();
      }
      if (status >= 500) throw new MeshyExceptions.ServerErrorException();
      if (status >= 400) throw new RuntimeException(json.get("message").getAsString());

      return json.get("result").getAsString();
    } catch (IOException | InterruptedException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  public static void waitForTask(String taskId, int offset, IntConsumer updateProgress)
      throws Exception {
    String apiUrl = "https://api.meshy.ai/openapi/v2/text-to-3d/" + taskId;
    HttpClient client = HttpClient.newHttpClient();

    while (true) {
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create(apiUrl))
              .header("Authorization", "Bearer " + meshyApiKey)
              .GET()
              .build();

      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() != 200) {
        throw new RuntimeException("HTTP error code: " + response.statusCode());
      }

      JsonObject task = JsonParser.parseString(response.body()).getAsJsonObject();
      String status = task.get("status").getAsString();

      int progress = task.get("progress").getAsInt();
      updateProgress.accept(progress / 2 + offset);

      if (status.equals("SUCCEEDED")) {
        System.out.println("Task status: " + status + " | Progress: " + progress);
        System.out.println("Task finished.");
        break;
      }
      if (status.equals("FAILED")) {
        throw new RuntimeException(task.get("message").getAsString());
      }

      System.out.println("Task status: " + status + " | Progress: " + task.get("progress"));
      Thread.sleep(1000);
    }
  }

  public static String createRefineTask(String previewTaskId) {
    try (HttpClient client = HttpClient.newHttpClient()) {
      JsonObject obj = new JsonObject();
      obj.addProperty("mode", "refine");
      obj.addProperty("preview_task_id", previewTaskId);

      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create("https://api.meshy.ai/openapi/v2/text-to-3d"))
              .header("Authorization", "Bearer " + meshyApiKey)
              .header("Content-Type", "application/json")
              .POST(HttpRequest.BodyPublishers.ofString(obj.toString()))
              .build();

      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      String responseBody = response.body();
      JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();

      int status = response.statusCode();
      switch (status) {
        case 401 -> throw new MeshyExceptions.InvalidApiKeyException();
        case 402 -> throw new MeshyExceptions.PaymentRequiredException();
        case 403 -> throw new MeshyExceptions.TooManyRequestsException();
      }
      if (status >= 500) throw new MeshyExceptions.ServerErrorException();
      if (status >= 400) throw new RuntimeException(json.get("message").getAsString());

      return json.get("result").getAsString();
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  public static String[] retrieveTextTo3dTask(String taskId, IntConsumer updateProgress) {
    try (HttpClient client = HttpClient.newHttpClient()) {
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create("https://api.meshy.ai/openapi/v2/text-to-3d/" + taskId))
              .header("Authorization", "Bearer " + meshyApiKey)
              .GET()
              .build();

      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      JsonObject task = JsonParser.parseString(response.body()).getAsJsonObject();
      String status = task.get("status").getAsString();

      if (status.equals("FAILED")) {
        throw new RuntimeException(task.get("message").getAsString());
      }
      if (!status.equals("SUCCEEDED")) {
        throw new RuntimeException("Attempted to retrieve Text to 3D Task before it finished.");
      }

      String objUrl = task.getAsJsonObject("model_urls").get("obj").getAsString();
      String mtlUrl = task.getAsJsonObject("model_urls").get("mtl").getAsString();
      String textureUrl =
          task.getAsJsonArray("texture_urls")
              .get(0)
              .getAsJsonObject()
              .get("base_color")
              .getAsString();
      return new String[] {objUrl, mtlUrl, textureUrl};
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  public static void downloadFile(String fileUrl, String destinationFilePath) throws Exception {
    HttpResponse<InputStream> response;
    try (HttpClient client = HttpClient.newHttpClient()) {
      HttpRequest request = HttpRequest.newBuilder().uri(URI.create(fileUrl)).GET().build();

      // Send the request and get the file content as InputStream
      response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

      // Open an OutputStream to write the content to the destination file
      try (InputStream in = response.body();
          FileOutputStream out = new FileOutputStream(destinationFilePath)) {
        byte[] buffer = new byte[1024];
        int bytesRead;

        // Read from the input stream and write to the output file
        while ((bytesRead = in.read(buffer)) != -1) {
          out.write(buffer, 0, bytesRead);
        }
      }
    }
  }
}
