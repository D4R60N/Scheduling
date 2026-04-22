package main;

import main.entity.Course;
import main.entity.Student;
import main.entity.Schedule;
import main.io.ScheduleReader;
import main.algorithm.Scheduler;
import main.algorithm.GameTheoryScheduler;
import main.algorithm.AILayoutOnlyScheduler;
import main.algorithm.AIScheduler;

import java.io.InputStream;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        System.out.println("=== Barvení grafu ===");
        runScheduling(1);

        printSeparator();

        System.out.println("=== Teorie her: aukce ===");
        runScheduling(2);

        printSeparator();

        System.out.println("=== AI + deterministické přiřazování ===");
        runScheduling(3);

        printSeparator();

        System.out.println("=== AI + AI přiřazování ===");
        runScheduling(4);
    }

    private static void printSeparator() {
        System.out.println();
        for (int i = 0; i < 40; i++) System.out.print("=");
        System.out.println("\n");
    }

    private static void runScheduling(int method) {
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

        switch (method) {
            case 1:
                Scheduler.scheduleActivities(schedule);
                Scheduler.assignStudents(schedule);
                break;
            case 2:
                GameTheoryScheduler.scheduleActivities(schedule);
                GameTheoryScheduler.assignStudentsAuction(schedule);
                break;
            case 3:
                AILayoutOnlyScheduler.scheduleAndAssign(schedule);
                break;
            case 4:
                AIScheduler.scheduleAndAssign(schedule);
                break;
        }

        printResults(schedule);
    }

    private static void printResults(Schedule schedule) {
        System.out.println("\nRozvržení:");
        for (int i = 0; i < schedule.getTotalSlots(); i++) {
            System.out.print("Slot " + (i + 1) + ": ");
            System.out.println(schedule.getActivitiesInSlot(i));
        }

        System.out.println("\nSouhrn:");
        System.out.println("Celková spokojenost studentů: " + schedule.calculateAverageSatisfaction());
    }
}
