package com.knkevin.ai_builder.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.knkevin.ai_builder.AIBuilder;
import com.knkevin.ai_builder.models.ObjModel;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.function.IntConsumer;

import static com.knkevin.ai_builder.Config.meshyApiKey;

public class Meshy {
    public static void textTo3D(String prompt, IntConsumer updateProgress) throws Exception {
        String previewTaskId = createPreviewTask(prompt);
        waitForTask(previewTaskId, 0, updateProgress);

        String refineTaskId = createRefineTask(previewTaskId);
        waitForTask(refineTaskId, 50, updateProgress);

        String[] modelUrls = retrieveTextTo3dTask(refineTaskId, updateProgress);
        String objUrl = modelUrls[0];
        String mtlUrl = modelUrls[1];
        String textureUrl = modelUrls[2];
        String objPath = "models/" + refineTaskId + ".obj";
        String mtlPath = "models/" + refineTaskId + ".mtl";
        String texturePath = "models/texture_0.png";
        try {
            downloadFile(objUrl, objPath);
            downloadFile(mtlUrl, mtlPath);
            downloadFile(textureUrl, texturePath);
            AIBuilder.model = new ObjModel(new File(objPath));
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            new File(objPath).delete();
            new File(mtlPath).delete();
        }
    }

    public static String createPreviewTask(String prompt) {
        try (HttpClient client = HttpClient.newHttpClient()) {
            String body = "{"
                + "\"mode\":\"preview\","
                + "\"prompt\":" + "\"" + prompt + "\""
                + "}";

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.meshy.ai/openapi/v2/text-to-3d"))
                .header("Authorization", "Bearer " + meshyApiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body))
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

    public static void waitForTask(String taskId, int offset, IntConsumer updateProgress) throws Exception {
        String apiUrl = "https://api.meshy.ai/openapi/v2/text-to-3d/" + taskId;
        HttpClient client = HttpClient.newHttpClient();

        while (true) {
            HttpRequest request = HttpRequest.newBuilder()
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
            updateProgress.accept(progress/2 + offset);

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
            String body = "{"
                + "\"mode\":\"refine\","
                + "\"preview_task_id\":" + "\"" + previewTaskId + "\""
                + "}";

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.meshy.ai/openapi/v2/text-to-3d"))
                .header("Authorization", "Bearer " + meshyApiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body))
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
            HttpRequest request = HttpRequest.newBuilder()
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
            String textureUrl = task.getAsJsonArray("texture_urls").get(0).getAsJsonObject().get("base_color").getAsString();
            return new String[]{objUrl, mtlUrl, textureUrl};
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static void downloadFile(String fileUrl, String destinationFilePath) throws Exception {
        HttpResponse<InputStream> response;
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(fileUrl))
                    .GET()
                    .build();

            // Send the request and get the file content as InputStream
            response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

            // Open an OutputStream to write the content to the destination file
            try (InputStream in = response.body();
                 FileOutputStream out = new FileOutputStream(destinationFilePath)
            ) {
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
