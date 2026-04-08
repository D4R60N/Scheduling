package main.algorithm;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.cdimascio.dotenv.Dotenv;
import main.entity.Activity;
import main.entity.Course;
import main.entity.Schedule;
import main.entity.Student;
import okhttp3.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class AIScheduler {

    private static final String API_KEY = System.getenv("GOOGLE_AI_KEY") == null ? Dotenv.load().get("API_KEY") : System.getenv("GOOGLE_AI_KEY");
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite-preview:generateContent?key=" + API_KEY;

    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();
    private static final Gson gson = new Gson();

    public static void scheduleAndAssign(Schedule schedule) {
        if (API_KEY == null) {
            System.err.println("Error: API Key not set. Set GOOGLE_AI_KEY env variable.");
            return;
        }

        try {
            System.out.println("... Waiting for Full AI response (Layout + Assignment) ...");
            String prompt = generateFullAIPrompt(schedule);
            String aiResponse = callGeminiAPI(prompt);
            
            if (aiResponse != null) {
                parseAndApplyFullAIResponse(aiResponse, schedule);
                System.out.println("... Full AI response received and applied.");
            }
            
        } catch (Exception e) {
            System.err.println("Full AI Call failed: " + e.getMessage());
        }
    }

    private static String generateFullAIPrompt(Schedule schedule) {
        JsonObject data = new JsonObject();
        data.addProperty("totalStudents", schedule.getStudents().size());
        data.addProperty("totalSlotsAvailable", schedule.getTotalSlots());
        
        JsonArray coursesArr = new JsonArray();
        for (Course c : schedule.getCourses()) {
            JsonObject co = new JsonObject();
            co.addProperty("name", c.getName());
            co.addProperty("maxCapacityPerActivity", c.getMaxActivitySize());
            int needed = (int) Math.ceil((double) schedule.getStudents().size() / c.getMaxActivitySize());
            co.addProperty("requiredNumberOfActivities", needed);
            coursesArr.add(co);
        }
        data.add("courses", coursesArr);

        JsonArray studentsArr = new JsonArray();
        for (Student s : schedule.getStudents()) {
            JsonObject st = new JsonObject();
            st.addProperty("name", s.getName());
            JsonArray pr = new JsonArray();
            for (int p : s.getPreferences()) pr.add(p);
            st.add("preferences", pr);
            studentsArr.add(st);
        }
        data.add("students", studentsArr);

        return "You are a scheduling expert. Optimize a schedule based on this JSON data. \n" +
               "IMPORTANT RULES:\n" +
               "1. Every course MUST have exactly the 'requiredNumberOfActivities' mentioned in the data.\n" +
               "2. Each activity must be assigned to a UNIQUE time slot (0 to " + (schedule.getTotalSlots()-1) + "). One activity per slot ONLY.\n" +
               "3. Every student MUST be assigned to exactly one activity for EVERY course.\n" +
               "4. An activity cannot exceed its 'maxCapacityPerActivity'.\n" +
               "5. Maximize the global satisfaction (sum of preferences for assigned slots).\n\n" +
               "Output ONLY raw JSON format:\n" +
               "{\n" +
               "  \"activities\": [ { \"course\": \"Math\", \"group\": 1, \"slot\": 0 }, ... ],\n" +
               "  \"assignments\": [ { \"student\": \"Student1\", \"course\": \"Math\", \"group\": 1 }, ... ]\n" +
               "}\n\n" +
               "Input Data: " + data.toString();
    }

    private static String callGeminiAPI(String prompt) throws IOException {
        JsonObject body = new JsonObject();
        JsonArray contents = new JsonArray();
        JsonObject content = new JsonObject();
        JsonArray parts = new JsonArray();
        JsonObject part = new JsonObject();
        part.addProperty("text", prompt);
        parts.add(part);
        content.add("parts", parts);
        contents.add(content);
        body.add("contents", contents);

        Request request = new Request.Builder()
                .url(API_URL)
                .post(RequestBody.create(body.toString(), MediaType.get("application/json")))
                .build();

        try (Response response = client.newCall(request).execute()) {
            String respStr = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) throw new IOException("HTTP " + response.code() + ": " + respStr);
            
            JsonObject respJson = gson.fromJson(respStr, JsonObject.class);
            return respJson.getAsJsonArray("candidates").get(0).getAsJsonObject()
                    .getAsJsonObject("content").getAsJsonArray("parts").get(0).getAsJsonObject()
                    .get("text").getAsString().trim();
        }
    }

    private static void parseAndApplyFullAIResponse(String response, Schedule schedule) {
        try {
            String cleanJson = response.replaceAll("(?s)```json\\s*(.*?)\\s*```", "$1")
                                       .replaceAll("(?s)```\\s*(.*?)\\s*```", "$1").trim();
            JsonObject result = gson.fromJson(cleanJson, JsonObject.class);

            JsonArray activitiesArr = result.getAsJsonArray("activities");
            for (int i = 0; i < activitiesArr.size(); i++) {
                JsonObject act = activitiesArr.get(i).getAsJsonObject();
                String courseName = act.get("course").getAsString();
                int group = act.get("group").getAsInt();
                int slot = act.get("slot").getAsInt();
                for (Activity a : schedule.getAllActivities()) {
                    if (a.getCourse().getName().equalsIgnoreCase(courseName) && a.getIndex() == (group - 1)) {
                        a.setTimeSlot(slot);
                        break;
                    }
                }
            }

            schedule.clearAssignments();
            JsonArray assignmentsArr = result.getAsJsonArray("assignments");
            for (int i = 0; i < assignmentsArr.size(); i++) {
                JsonObject ass = assignmentsArr.get(i).getAsJsonObject();
                String studentName = ass.get("student").getAsString();
                String courseName = ass.get("course").getAsString();
                int group = ass.get("group").getAsInt();

                Student s = schedule.getStudents().stream()
                        .filter(std -> std.getName().equalsIgnoreCase(studentName)).findFirst().orElse(null);
                Activity a = schedule.getAllActivities().stream()
                        .filter(act -> act.getCourse().getName().equalsIgnoreCase(courseName) && act.getIndex() == (group - 1))
                        .findFirst().orElse(null);

                if (s != null && a != null) schedule.assignStudentToActivity(s, a);
            }
        } catch (Exception e) {
            System.err.println("Parse failed: " + e.getMessage() + ". Response: " + response);
        }
    }
}
