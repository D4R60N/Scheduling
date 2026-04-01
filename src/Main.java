import entity.Schedule;
import io.ScheduleReader;

import java.io.InputStream;

public class Main {
    public static void main(String[] args) {
        InputStream is = Main.class.getClassLoader().getResourceAsStream("file.csv");

        if (is == null) {
            System.err.println("File not found!");
            return;
        }

        Schedule schedule = ScheduleReader.readSchedule(is);
    }
}