package com.knkevin.ai_builder.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.knkevin.ai_builder.AIBuilder;
import com.knkevin.ai_builder.models.Model;
import com.knkevin.ai_builder.models.ObjModel;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class Meshy {
    private static final String apiKey = "msy_dummy_api_key_for_test_mode_12345678";

    public static void textTo3D(String prompt) {
        String previewTaskId = createPreviewTask(prompt);
        String refineTaskId = createRefineTask(previewTaskId);
        String[] modelUrls = streamTextTo3DTask(refineTaskId);
        String objUrl = modelUrls[0];
        String mtlUrl = modelUrls[1];
        String objPath = "models/" + refineTaskId + ".obj";
        String mtlPath = "models/" + refineTaskId + ".mtl";
        try {
            downloadFile(objUrl, objPath);
            downloadFile(mtlUrl, mtlPath);
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
            .header("Authorization", "Bearer " + apiKey)
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body();
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
            return json.get("result").getAsString();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
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
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body();
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
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
                .header("Authorization", "Bearer " + apiKey)
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
                            String objUrl = data.getAsJsonObject("model_urls").get("obj").getAsString();
                            String mtlUrl = data.getAsJsonObject("model_urls").get("mtl").getAsString();
                            return new String[]{objUrl, mtlUrl};
                        }
                    }

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
