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

import static com.knkevin.ai_builder.Config.meshyApiKey;

public class Meshy {
    public static void textTo3D(String prompt) {
        String previewTaskId = createPreviewTask(prompt);
        String refineTaskId = createRefineTask(previewTaskId);
        String[] modelUrls = streamTextTo3DTask(refineTaskId);
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

    public static void waitForPreviewTask(String previewTaskId) throws Exception {
        String apiUrl = "https://api.meshy.ai/openapi/v2/text-to-3d/" + previewTaskId;
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

            JsonObject previewTask = JsonParser.parseString(response.body()).getAsJsonObject();
            String status = previewTask.get("status").getAsString();

            if ("SUCCEEDED".equals(status)) {
                System.out.println("Preview task finished.");
                break;
            }

            System.out.println("Preview task status: " + status +
                    " | Progress: " + previewTask.get("progress") +
                    " | Retrying in 5 seconds...");
            Thread.sleep(5000);
        }
    }

    public static String createRefineTask(String previewTaskId) {
        try (HttpClient client = HttpClient.newHttpClient()) {
            waitForPreviewTask(previewTaskId);
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

    public static String[] streamTextTo3DTask(String taskId) {
        HttpResponse<InputStream> response;
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.meshy.ai/openapi/v2/text-to-3d/" + taskId + "/stream"))
                .header("Authorization", "Bearer " + meshyApiKey)
                .header("Accept", "text/event-stream")
                .GET()
                .build();
            response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

            BufferedReader reader = new BufferedReader(new InputStreamReader(response.body()));
            String line;
            StringBuilder eventData = new StringBuilder();

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("data:")) {
                    eventData.append(line.substring(5).trim());
                } else if (line.isEmpty() && !eventData.isEmpty()) {
                    // Parse JSON using JsonElement and JsonObject from Gson
                    JsonElement element = JsonParser.parseString(eventData.toString());
                    JsonObject data = element.getAsJsonObject();

                    String status = data.get("status").getAsString();
                    if (status.equals("SUCCEEDED") || status.equals("FAILED") || status.equals("CANCELED")) {
                        if (status.equals("SUCCEEDED")) {
                            System.out.println("Refine task finished.");
                            String objUrl = data.getAsJsonObject("model_urls").get("obj").getAsString();
                            String mtlUrl = data.getAsJsonObject("model_urls").get("mtl").getAsString();
                            String textureUrl = data.getAsJsonArray("texture_urls").get(0).getAsJsonObject().get("base_color").getAsString();
                            return new String[]{objUrl, mtlUrl, textureUrl};
                        }
                        break;
                    }

                    System.out.println("Refine task status: " + status +
                        " | Progress: " + data.get("progress").getAsString() +
                        " | Retrying in 5 seconds...");

                    eventData.setLength(0); // Reset buffer
                }
            }
            throw new RuntimeException("An error occurred while generating the 3D model.");
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
