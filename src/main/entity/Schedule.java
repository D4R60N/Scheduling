package main.entity;

import java.util.*;

public class Schedule {
    private final List<Course> courses;
    private final List<Student> students;
    private final List<Activity> allActivities;
    private final Map<Student, Map<Course, Activity>> assignments;
    private final Map<Activity, Integer> activityOccupancy;
    private final int totalSlots;

    public Schedule(List<Course> courses, List<Student> students) {
        this.courses = courses;
        this.students = students;
        this.allActivities = new ArrayList<>();
        this.assignments = new HashMap<>();
        this.activityOccupancy = new HashMap<>();

        int maxPrefs = 0;
        for (Student s : students) {
            maxPrefs = Math.max(maxPrefs, s.getPreferences().length);
        }
        this.totalSlots = maxPrefs;

        for (Course course : courses) {
            int numStudents = students.size();
            int maxPerActivity = course.getMaxActivitySize();
            int activitiesNeeded = (int) Math.ceil((double) numStudents / maxPerActivity);

            for (int i = 0; i < activitiesNeeded; i++) {
                Activity a = new Activity(course, i);
                allActivities.add(a);
                activityOccupancy.put(a, 0);
            }
        }

        // Error if activities cannot fit into the available slots
        if (allActivities.size() > totalSlots) {
            throw new IllegalArgumentException("Error: Cannot fit " + allActivities.size() + 
                " required activities into " + totalSlots + " available time slots.");
        }
    }

    public List<Course> getCourses() { return courses; }
    public List<Student> getStudents() { return students; }
    public List<Activity> getAllActivities() { return allActivities; }
    public int getTotalSlots() { return totalSlots; }

    public boolean assignStudentToActivity(Student s, Activity a) {
        if (activityOccupancy.get(a) >= a.getCourse().getMaxActivitySize()) {
            return false;
        }
        assignments.computeIfAbsent(s, k -> new HashMap<>()).put(a.getCourse(), a);
        activityOccupancy.put(a, activityOccupancy.get(a) + 1);
        return true;
    }

    public void unassignStudentFromActivity(Student s, Activity a) {
        Map<Course, Activity> sMap = assignments.get(s);
        if (sMap != null && sMap.get(a.getCourse()) == a) {
            sMap.remove(a.getCourse());
            activityOccupancy.put(a, activityOccupancy.get(a) - 1);
        }
    }

    public void clearAssignments() {
        assignments.clear();
        for (Activity a : activityOccupancy.keySet()) {
            activityOccupancy.put(a, 0);
        }
    }

    public Map<Course, Activity> getStudentSchedule(Student s) {
        return assignments.getOrDefault(s, Collections.emptyMap());
    }

    public List<Activity> getActivitiesInSlot(int slotIndex) {
        List<Activity> inSlot = new ArrayList<>();
        for (Activity a : allActivities) {
            if (a.getTimeSlot() == slotIndex) {
                inSlot.add(a);
            }
        }
        return inSlot;
    }

    public float calculateAverageSatisfaction() {
        if (courses.isEmpty() || students.isEmpty()) return 0;
        int totalSatisfaction = 0;
        for (Student s : students) {
            Map<Course, Activity> sAssign = assignments.get(s);
            if (sAssign == null) continue;
            for (Activity a : sAssign.values()) {
                int slot = a.getTimeSlot();
                if (slot >= 0 && slot < s.getPreferences().length) {
                    totalSatisfaction += s.getPreferences()[slot];
                }
            }
        }
        return (float) totalSatisfaction / (students.size() * courses.size());
    }
}
