package main.io;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import main.entity.Course;
import main.entity.Student;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ScheduleReader {
    public static List<Course> readCourses(InputStream inputStream) {
        List<Course> courses = new ArrayList<>();
        try (CSVReader reader = new CSVReader(new InputStreamReader(inputStream))) {
            String[] nextLine;
            while ((nextLine = reader.readNext()) != null) {
                if (nextLine.length < 2) continue;
                courses.add(new Course(nextLine[0].trim(), Integer.parseInt(nextLine[1].trim())));
            }
        } catch (IOException | CsvValidationException | NumberFormatException e) {
            e.printStackTrace();
        }
        return courses;
    }

    public static List<Student> readStudents(InputStream inputStream) {
        List<Student> students = new ArrayList<>();
        try (CSVReader reader = new CSVReader(new InputStreamReader(inputStream))) {
            String[] nextLine;
            reader.readNext();
            while ((nextLine = reader.readNext()) != null) {
                if (nextLine.length < 2) continue; // At least name and one pref
                int numPrefs = nextLine.length - 1;
                int[] prefs = new int[numPrefs];
                for (int i = 0; i < numPrefs; i++) {
                    prefs[i] = Integer.parseInt(nextLine[i + 1].trim());
                }
                students.add(new Student(nextLine[0].trim(), prefs));
            }
        } catch (IOException | CsvValidationException | NumberFormatException e) {
            e.printStackTrace();
        }
        return students;
    }
}
