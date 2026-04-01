package main.algorithm;

import main.entity.Activity;
import main.entity.Course;
import main.entity.Schedule;
import main.entity.Student;

import java.util.*;

public class GameTheoryScheduler {

    public static void scheduleActivities(Schedule schedule) {
        List<Activity> activities = new ArrayList<>(schedule.getAllActivities());
        int totalSlots = schedule.getTotalSlots();
        List<Student> students = schedule.getStudents();

        // 1. Voting phase: Sum preferences across all students for each slot
        double[] slotVotes = new double[totalSlots];
        for (Student s : students) {
            int[] prefs = s.getPreferences();
            for (int i = 0; i < Math.min(totalSlots, prefs.length); i++) {
                slotVotes[i] += prefs[i];
            }
        }

        // 2. Rank slots by preference (highest first)
        Integer[] rankedSlots = new Integer[totalSlots];
        for (int i = 0; i < totalSlots; i++) rankedSlots[i] = i;
        Arrays.sort(rankedSlots, (a, b) -> Double.compare(slotVotes[b], slotVotes[a]));

        // 3. Round-robin layout: Assign best slots across all courses fairly
        Map<Course, List<Activity>> courseToActivities = new LinkedHashMap<>();
        for (Activity a : activities) {
            courseToActivities.computeIfAbsent(a.getCourse(), k -> new ArrayList<>()).add(a);
        }

        int slotIdx = 0;
        boolean activityScheduled;
        do {
            activityScheduled = false;
            for (Course course : courseToActivities.keySet()) {
                List<Activity> group = courseToActivities.get(course);
                if (!group.isEmpty() && slotIdx < totalSlots) {
                    Activity a = group.remove(0);
                    a.setTimeSlot(rankedSlots[slotIdx++]);
                    activityScheduled = true;
                }
            }
        } while (activityScheduled && slotIdx < totalSlots);
    }

    public static void assignStudentsAuction(Schedule schedule) {
        schedule.clearAssignments();
        List<Student> students = new ArrayList<>(schedule.getStudents());
        // For game theory, fairness can be improved by sorting students 
        // who might have harder constraints or higher needs first,
        // but shuffle is a standard "Serial Dictatorship" lottery.
        Collections.shuffle(students);

        for (Student student : students) {
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
                    // Correct backtracking rollback
                    usedSlots.remove(slot);
                    schedule.unassignStudentFromActivity(student, a);
                }
            }
        }
        return false;
    }
}
