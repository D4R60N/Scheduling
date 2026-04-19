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

        double[] slotVotes = new double[totalSlots];
        for (Student s : students) {
            int[] prefs = s.getPreferences();
            for (int i = 0; i < Math.min(totalSlots, prefs.length); i++) {
                slotVotes[i] += prefs[i];
            }
        }

        Integer[] rankedSlots = new Integer[totalSlots];
        for (int i = 0; i < totalSlots; i++) rankedSlots[i] = i;
        Arrays.sort(rankedSlots, (a, b) -> Double.compare(slotVotes[b], slotVotes[a]));

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
        List<Student> students = schedule.getStudents();
        List<Course> courses = schedule.getCourses();

        List<Bid> allBids = new ArrayList<>();
        for (Student s : students) {
            for (Activity a : schedule.getAllActivities()) {
                if (a.getTimeSlot() != -1) {
                    int pref = s.getPreferences().length > a.getTimeSlot() ? s.getPreferences()[a.getTimeSlot()] : 0;
                    if (pref > 0) {
                        allBids.add(new Bid(s, a, pref));
                    }
                }
            }
        }

        allBids.sort((b1, b2) -> Double.compare(b2.value, b1.value));

        Set<String> studentCoursePairAssigned = new HashSet<>();

        for (Bid bid : allBids) {
            String assignmentKey = bid.student.getName() + "_" + bid.activity.getCourse().getName();

            if (studentCoursePairAssigned.contains(assignmentKey)) continue;

            if (isStudentFreeInSlot(bid.student, schedule, bid.activity.getTimeSlot())) {
                if (schedule.assignStudentToActivity(bid.student, bid.activity)) {
                    studentCoursePairAssigned.add(assignmentKey);
                }
            }
        }

        for (Student s : students) {
            for (Course c : courses) {
                if (!studentCoursePairAssigned.contains(s.getName() + "_" + c.getName())) {
                    fillBestEffort(s, c, schedule);
                }
            }
        }
    }

    private static boolean isStudentFreeInSlot(Student s, Schedule schedule, int slot) {
        Map<Course, Activity> current = schedule.getStudentSchedule(s);
        for (Activity a : current.values()) {
            if (a.getTimeSlot() == slot) return false;
        }
        return true;
    }

    private static void fillBestEffort(Student s, Course c, Schedule schedule) {
        for (Activity a : schedule.getAllActivities()) {
            if (a.getCourse().equals(c) && a.getTimeSlot() != -1) {
                if (isStudentFreeInSlot(s, schedule, a.getTimeSlot())) {
                    if (schedule.assignStudentToActivity(s, a)) return;
                }
            }
        }
    }

    private static class Bid {
        Student student;
        Activity activity;
        double value;
        Bid(Student s, Activity a, double v) {
            this.student = s;
            this.activity = a;
            this.value = v;
        }
    }
}
