package service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class AiService {

    // *** PUT YOUR GEMINI API KEY HERE ***
    private static final String API_KEY = "AIzaSyABTokPqWS68PcwYdI-mKr9tA8fTP5QxYc";
    private static final String API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + API_KEY;

    public static String askGemini(String prompt) {
        try {
            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String body = "{\"contents\":[{\"parts\":[{\"text\":\"" + escapeJson(prompt) + "\"}]}]}";
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes("UTF-8"));
            }

            int code = conn.getResponseCode();
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    code == 200 ? conn.getInputStream() : conn.getErrorStream(), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            br.close();

            if (code == 200) {
                return extractText(sb.toString());
            } else {
                return "AI error (code " + code + "): " + sb.toString();
            }
        } catch (Exception e) {
            return "Could not connect to AI: " + e.getMessage();
        }
    }

    private static String extractText(String json) {
        int i = json.indexOf("\"text\"");
        if (i == -1) return "No response from AI.";
        i = json.indexOf("\"", json.indexOf(":", i) + 1);
        if (i == -1) return "No response from AI.";
        int end = i + 1;
        while (end < json.length()) {
            if (json.charAt(end) == '\\') { end += 2; continue; }
            if (json.charAt(end) == '"') break;
            end++;
        }
        String t = json.substring(i + 1, end);
        t = t.replace("\\n", "\n").replace("\\\"", "\"")
             .replace("\\\\", "\\").replace("\\t", "\t");
        return t;
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
}
