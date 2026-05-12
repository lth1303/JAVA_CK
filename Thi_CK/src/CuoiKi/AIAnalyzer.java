package CuoiKi;

import okhttp3.*;
import org.json.*;

import java.util.concurrent.TimeUnit;

public class AIAnalyzer {

    private static final String API_KEY =
    		"";

    private static final String API_URL =
            "https://api.groq.com/openai/v1/chat/completions";

    private static final OkHttpClient client =
            new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build();

    public static JSONObject analyzeCode(String code) {

        if (code == null || code.isBlank()) {
            return null;
        }

        try {

            String prompt =
                    "Analyze the following competitive programming code.\n" +
                    "Return ONLY valid JSON.\n\n" +

                    "Required JSON format:\n" +
                    "{\n" +
                    "  \"algorithm\": \"...\",\n" +
                    "  \"data_structure\": \"...\",\n" +
                    "  \"time_complexity\": \"...\",\n" +
                    "  \"ai_generated\": true/false,\n" +
                    "  \"explanation\": \"short explanation\"\n" +
                    "}\n\n" +

                    "Code:\n" + code;

            JSONObject json = new JSONObject();

            json.put("model", "llama-3.1-8b-instant");

            json.put("temperature", 0);

            json.put("max_tokens", 300);

            json.put(
                    "response_format",
                    new JSONObject()
                            .put("type", "json_object")
            );

            JSONArray messages = new JSONArray();

            messages.put(
                    new JSONObject()
                            .put("role", "user")
                            .put("content", prompt)
            );

            json.put("messages", messages);

            RequestBody body = RequestBody.create(
                    json.toString(),
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(API_URL)
                    .addHeader(
                            "Authorization",
                            "Bearer " + API_KEY
                    )
                    .post(body)
                    .build();

            try (Response response =
                         client.newCall(request).execute()) {

                if (!response.isSuccessful()) {

                    System.out.println(
                            "API ERROR: " +
                                    response.code()
                    );

                    return null;
                }

                String res = response.body().string();

                JSONObject result = new JSONObject(res);

                String content =
                        result.getJSONArray("choices")
                                .getJSONObject(0)
                                .getJSONObject("message")
                                .getString("content");

                content = content
                        .replace("```json", "")
                        .replace("```", "")
                        .trim();

                return new JSONObject(content);
            }

        } catch (Exception e) {

            System.out.println(
                    "AI Analyze Error: " + e.getMessage()
            );
        }

        return null;
    }
}