package main.algorithm;

import main.entity.Activity;
import main.entity.Course;
import main.entity.Schedule;
import main.entity.Student;

import java.util.*;

public class Scheduler {
    public static void scheduleActivities(Schedule schedule) {
        List<Activity> all = schedule.getAllActivities();
        int totalSlots = schedule.getTotalSlots();
        for (Activity a : all) a.setTimeSlot(-1);
        for (int i = 0; i < Math.min(all.size(), totalSlots); i++) {
            all.get(i).setTimeSlot(i);
        }
    }

    public static void assignStudents(Schedule schedule) {
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

        options.sort((a1, a2) -> {
            int p1 = student.getPreferences().length > a1.getTimeSlot() ? student.getPreferences()[a1.getTimeSlot()] : 0;
            int p2 = student.getPreferences().length > a2.getTimeSlot() ? student.getPreferences()[a2.getTimeSlot()] : 0;
            return p2 - p1;
        });

        for (Activity activity : options) {
            int slot = activity.getTimeSlot();
            if (!usedSlots.contains(slot)) {
                if (schedule.assignStudentToActivity(student, activity)) {
                    usedSlots.add(slot);
                    if (findBestStudentAssignment(student, schedule, courseIndex + 1, usedSlots)) return true;
                    // Backtrack
                    usedSlots.remove(slot);
                    schedule.unassignStudentFromActivity(student, activity);
                }
            }
        }
        return false;
    }
}
