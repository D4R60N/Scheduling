package main;

import main.entity.Course;
import main.entity.Student;
import main.entity.Schedule;
import main.io.ScheduleReader;
import main.algorithm.Scheduler;
import main.algorithm.GameTheoryScheduler;

import java.io.InputStream;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        // Run with Graph Coloring
        System.out.println("=== SCHEDULING METHOD 1: GRAPH COLORING ===");
        runScheduling(true);

        System.out.println("\n" + "=".repeat(40) + "\n");

        // Run with Game Theory
        System.out.println("=== SCHEDULING METHOD 2: GAME THEORY (VOTING/AUCTION) ===");
        runScheduling(false);
    }

    private static void runScheduling(boolean useGraphColoring) {
        // Reload resources to ensure fresh data
        InputStream coursesIs = Main.class.getClassLoader().getResourceAsStream("courses.csv");
        InputStream studentsIs = Main.class.getClassLoader().getResourceAsStream("students.csv");

        if (coursesIs == null || studentsIs == null) {
            System.err.println("CSV files not found!");
            return;
        }

        List<Course> courses = ScheduleReader.readCourses(coursesIs);
        List<Student> students = ScheduleReader.readStudents(studentsIs);

        if (courses.isEmpty() || students.isEmpty()) {
            System.err.println("Incomplete or empty input data!");
            return;
        }

        Schedule schedule = new Schedule(courses, students);

        if (useGraphColoring) {
            Scheduler.scheduleActivities(schedule);
            Scheduler.assignStudents(schedule);
        } else {
            GameTheoryScheduler.scheduleActivities(schedule);
            GameTheoryScheduler.assignStudentsAuction(schedule);
        }

        printResults(schedule);
    }

    private static void printResults(Schedule schedule) {
        System.out.println("\nActivity Layout (Slot assignments):");
        for (int i = 0; i < schedule.getTotalSlots(); i++) {
            System.out.print("Slot " + (i + 1) + ": ");
            System.out.println(schedule.getActivitiesInSlot(i));
        }

        System.out.println("\nSummary:");
        System.out.println("Total Student Satisfaction (Higher is better): " + schedule.calculateTotalSatisfaction());
    }
}
