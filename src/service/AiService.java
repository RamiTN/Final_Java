package service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class AiService {

    private static final String API_KEY = loadApiKey();
    private static final String API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=";
    private static final Gson GSON = new Gson();

    /**
     * Reads API_KEY from system env first, then falls back to .env file in project root.
     */
    private static String loadApiKey() {
        // 1. Try system environment variable first
        String key = System.getenv("API_KEY");
        if (key != null && !key.isBlank()) return key;

        // 2. Fall back to .env file — check working dir and one level up
        for (String path : new String[]{".env", "../.env"}) {
            File envFile = new File(path);
            if (envFile.exists()) {
                try (BufferedReader br = new BufferedReader(new FileReader(envFile))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        line = line.trim();
                        if (line.startsWith("API_KEY=")) {
                            String value = line.substring("API_KEY=".length()).trim();
                            if (!value.isBlank()) return value;
                        }
                    }
                } catch (Exception e) {
                    System.err.println("[AiService] Could not read .env: " + e.getMessage());
                }
            }
        }

        System.err.println("[AiService] API_KEY not found in environment or .env file.");
        return null;
    }

    /**
     * Sends a full prompt to Gemini and returns the response text.
     */
    public static String askGemini(String prompt) {
        if (API_KEY == null || API_KEY.isBlank()) {
            return "Error: API_KEY not found. Add it to your .env file as: API_KEY=your_key_here";
        }
        if (prompt == null || prompt.isBlank()) {
            return "Please provide a valid prompt.";
        }

        String requestBody = buildRequestBody(prompt);
        int maxRetries = 2;

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection) new URL(API_URL + API_KEY).openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setDoOutput(true);
                conn.setConnectTimeout(10_000);
                conn.setReadTimeout(30_000);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(requestBody.getBytes(StandardCharsets.UTF_8));
                }

                int code = conn.getResponseCode();

                String responseBody;
                try (BufferedReader br = new BufferedReader(new InputStreamReader(
                        code == 200 ? conn.getInputStream() : conn.getErrorStream(),
                        StandardCharsets.UTF_8))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                    responseBody = sb.toString();
                }

                if (code == 200) {
                    return extractText(responseBody);
                }

                if (code == 503 && attempt < maxRetries) {
                    Thread.sleep(2000);
                    continue;
                }

                return switch (code) {
                    case 400 -> "Bad request. The prompt could not be processed. (HTTP 400)";
                    case 403 -> "Access denied. Check your API key. (HTTP 403)";
                    case 429 -> "Rate limit exceeded. Please wait and try again. (HTTP 429)";
                    case 503 -> "AI is currently busy. Please try again in a few seconds. (HTTP 503)";
                    default  -> "Unexpected error (HTTP " + code + "). Try again.";
                };

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return "Request interrupted.";
            } catch (Exception e) {
                if (attempt == maxRetries) {
                    return "Connection error: " + e.getMessage();
                }
            } finally {
                if (conn != null) conn.disconnect();
            }
        }

        return "Something went wrong after multiple attempts.";
    }

    /**
     * Builds the Gemini request body safely using Gson.
     * Structure: { "contents": [ { "parts": [ { "text": "..." } ] } ] }
     */
    private static String buildRequestBody(String prompt) {
        JsonObject textPart = new JsonObject();
        textPart.addProperty("text", prompt);

        JsonArray parts = new JsonArray();
        parts.add(textPart);

        JsonObject content = new JsonObject();
        content.add("parts", parts);

        JsonArray contents = new JsonArray();
        contents.add(content);

        JsonObject body = new JsonObject();
        body.add("contents", contents);

        return GSON.toJson(body);
    }

    /**
     * Navigates the Gemini response: candidates[0].content.parts[0].text
     */
    private static String extractText(String json) {
        try {
            JsonObject root = GSON.fromJson(json, JsonObject.class);
            return root
                    .getAsJsonArray("candidates")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("content")
                    .getAsJsonArray("parts")
                    .get(0).getAsJsonObject()
                    .get("text").getAsString();
        } catch (Exception e) {
            return "Could not parse AI response.";
        }
    }
}