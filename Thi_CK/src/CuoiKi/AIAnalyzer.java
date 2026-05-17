package CuoiKi;

import okhttp3.*;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

public class AIAnalyzer {
    private static final String API_KEY = "----";
    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .build();
    public static JSONObject analyzeCode(String code) {

        try {
            if (code == null || code.isBlank()) {
                return null;
            }
            if (code.length() > 5000) {
                code =code.substring(0, 5000);
            }
            String prompt = """
            		You are an expert competitive programming code analyzer.
            		TASK:
            		Analyze ONLY the provided source code.
            		STRICT RULES:
            		- Detect algorithms ONLY if explicitly implemented in code.
            		- Detect data structures ONLY if explicitly used.
            		- NEVER guess.
            		- NEVER infer hidden logic.
            		- NEVER hallucinate.
            		- NEVER explain.
            		- NEVER summarize.
            		- Return VALID RAW JSON ONLY.
            		- If uncertain, return empty array [].
            		- Very important:
            		  Only detect an algorithm if there is strong code evidence.

            		DETECTION RULES:

            		Binary Search:
            		- detect ONLY if:
            		  while(l <= r)
            		  mid = (l+r)/2
            		  lower_bound / upper_bound

            		DFS:
            		- detect ONLY if recursive traversal on graph/tree exists

            		BFS:
            		- detect ONLY if queue-based graph traversal exists

            		Dijkstra:
            		- detect ONLY if priority_queue + graph relaxation exists

            		Dynamic Programming:
            		- detect ONLY if dp array/table is used for state transition

            		Prefix Sum:
            		- detect ONLY if prefix accumulation array exists

            		Fenwick Tree:
            		- detect ONLY if lowbit(x) or BIT operations exist

            		Segment Tree:
            		- detect ONLY if tree node recursion/update/query exists

            		KMP:
            		- detect ONLY if lps/pi table exists

            		String Hashing:
            		- detect ONLY if rolling hash/mod hash exists

            		Two Pointers:
            		- detect ONLY if two moving indices are used together

            		Sliding Window:
            		- detect ONLY if window expands/shrinks dynamically

            		Sorting:
            		- detect ONLY if sort(...) exists

            		Math:
            		- detect ONLY if formulas/number theory/combinatorics are core logic

            		
            		TIME COMPLEXITY:
            		- Return only the MAIN overall complexity.
            		- Use standard Big-O notation only.
            		
            		AI GENERATED:
            		Estimate whether the code is AI-generated based on:
            		- unnatural naming
            		- repetitive structure
            		- excessive comments
            		- suspicious consistency
            		- verbose template

            		OUTPUT FORMAT:

            		{
            		  "algorithm": [],
            		  "data_structure": [],
            		  "time_complexity": "",
            		  "ai_generated": false,
            		  "ai_probability": 0
            		}

            		Return RAW JSON ONLY.

            		SOURCE CODE:
            		""" + code;
            JSONObject bodyJson =new JSONObject();
            bodyJson.put( "model","llama-3.3-70b-versatile");
            bodyJson.put("temperature", 0 );
            bodyJson.put( "max_tokens",200 );
            bodyJson.put( "response_format",new JSONObject() .put("type", "json_object"));
            JSONArray messages =new JSONArray();
            messages.put(new JSONObject() .put("role", "user") .put("content", prompt) );
            bodyJson.put("messages",messages);

            RequestBody body =RequestBody.create( bodyJson.toString(), MediaType.parse("application/json" ));
            Request request = new Request.Builder()
                            .url(API_URL)
                            .addHeader("Authorization", "Bearer " + API_KEY)
                            .addHeader( "Content-Type","application/json" )
                            .post(body)
                            .build();

            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) {
                System.out.println("AI API ERROR: "+ response.code() );
                System.out.println( response.body().string() );
                return null;
            }
            String responseText = response.body().string();
            JSONObject json = new JSONObject(responseText);
            String content = json.getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content");
            
            content = content.replace("```json", "") .replace("```", "").trim();

            System.out.println(
                    "\n===== PHÂN TÍCH AI ====="
            );
            System.out.println(content);
            JSONObject result = new JSONObject(content);
            if (!result.has("algorithm")) {
            	result.put( "algorithm", new JSONArray());
            }
            if (!result.has("data_structure")) {
                result.put( "data_structure", new JSONArray() );
            }

            if (!result.has("time_complexity")) {
                result.put("time_complexity", "Unknown");
            }

            if (!result.has("ai_generated")) {
                result.put( "ai_generated", "false");
            }

            double prob = result.optDouble(  "ai_probability", 0 );

            if (prob <= 1) {
                prob *= 100;
            }

            result.put("ai_probability",(int) prob );
            return result;

        } catch (Exception e) {
            System.out.println("AI Analyze ERROR: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }
}