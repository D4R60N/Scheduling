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

public class AILayoutOnlyScheduler {

    private static final String API_KEY = System.getenv("GOOGLE_AI_KEY") == null ? Dotenv.load().get("API_KEY") : System.getenv("GOOGLE_AI_KEY");
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite-preview:generateContent?key=" + API_KEY;
    
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();
    private static final Gson gson = new Gson();

    public static void scheduleAndAssign(Schedule schedule) {
        if (API_KEY == null) {
            System.err.println("Error: API Key not set. Set GOOGLE_AI_KEY env variable for AILayoutOnlyScheduler.");
            return;
        }

        try {
            String prompt = generateLayoutPrompt(schedule);
            String aiResponse = callGeminiAPI(prompt);
            applyAILayout(aiResponse, schedule);

            assignStudentsDeterministic(schedule);
            
        } catch (Exception e) {
            System.err.println("AI Layout Call failed: " + e.getMessage());
        }
    }

    private static String generateLayoutPrompt(Schedule schedule) {
        JsonObject data = new JsonObject();
        data.addProperty("totalSlots", schedule.getTotalSlots());
        
        JsonArray coursesArr = new JsonArray();
        for (Course c : schedule.getCourses()) {
            JsonObject co = new JsonObject();
            co.addProperty("name", c.getName());
            int needed = (int) Math.ceil((double) schedule.getStudents().size() / c.getMaxActivitySize());
            co.addProperty("activityCount", needed);
            coursesArr.add(co);
        }
        data.add("courses", coursesArr);

        // Send a summary of student preferences per slot so AI can decide on layout
        double[] totalPrefPerSlot = new double[schedule.getTotalSlots()];
        for (Student s : schedule.getStudents()) {
            for (int i = 0; i < Math.min(s.getPreferences().length, totalPrefPerSlot.length); i++) {
                totalPrefPerSlot[i] += s.getPreferences()[i];
            }
        }
        data.add("aggregatedSlotPreferences", gson.toJsonTree(totalPrefPerSlot));

        return "You are a scheduling expert. Decide which activity goes into which time slot (0 to " + (schedule.getTotalSlots() - 1) + ").\n" +
               "Rules:\n" +
               "1. Each activity must have a UNIQUE slot. One activity per slot ONLY.\n" +
               "2. A course has multiple activities (groups). Assign each group to a unique slot.\n" +
               "3. Use the aggregatedSlotPreferences to place activities in slots students actually like.\n\n" +
               "Output ONLY raw JSON:\n" +
               "{ \"activities\": [ { \"course\": \"Math\", \"group\": 1, \"slot\": 0 }, ... ] }\n\n" +
               "Data: " + data.toString();
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

    private static void applyAILayout(String response, Schedule schedule) {
        try {
            String cleanJson = response.replaceAll("(?s)```json\\s*(.*?)\\s*```", "$1")
                                       .replaceAll("(?s)```\\s*(.*?)\\s*```", "$1").trim();
            JsonObject result = gson.fromJson(cleanJson, JsonObject.class);
            JsonArray activitiesArr = result.getAsJsonArray("activities");
            
            for (Activity a : schedule.getAllActivities()) a.setTimeSlot(-1);

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
        } catch (Exception e) {
            System.err.println("Parse Layout failed: " + e.getMessage());
        }
    }

    private static void assignStudentsDeterministic(Schedule schedule) {
        schedule.clearAssignments();
        for (Student student : schedule.getStudents()) {
            findBestStudentAssignment(student, schedule, 0, new HashSet<>());
        }
    }

    private static boolean findBestStudentAssignment(
            Student student,
            Schedule schedule,
            int courseIndex,
            Set<Integer> usedSlots) {

        if (courseIndex == schedule.getCourses().size()) return true;

        Course currentCourse = schedule.getCourses().get(courseIndex);
        List<Activity> options = new ArrayList<>();
        for (Activity a : schedule.getAllActivities()) {
            if (a.getCourse().equals(currentCourse) && a.getTimeSlot() != -1) options.add(a);
        }

        options.sort((a, b) -> {
            int pA = student.getPreferences().length > a.getTimeSlot() ? student.getPreferences()[a.getTimeSlot()] : 0;
            int pB = student.getPreferences().length > b.getTimeSlot() ? student.getPreferences()[b.getTimeSlot()] : 0;
            return pB - pA;
        });

        for (Activity a : options) {
            int slot = a.getTimeSlot();
            if (!usedSlots.contains(slot)) {
                if (schedule.assignStudentToActivity(student, a)) {
                    usedSlots.add(slot);
                    if (findBestStudentAssignment(student, schedule, courseIndex + 1, usedSlots)) return true;
                    usedSlots.remove(slot);
                    schedule.unassignStudentFromActivity(student, a);
                }
            }
        }
        return false;
    }
}
