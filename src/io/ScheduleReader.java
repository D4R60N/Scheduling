package io;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import entity.Schedule;

import java.io.*;


public class ScheduleReader {
    public static Schedule readSchedule(InputStream inputStream) {
        try (CSVReader reader = new CSVReader(new InputStreamReader(inputStream))) {
            String[] nextLine;
            while ((nextLine = reader.readNext()) != null) {
                System.out.println(nextLine[0] + " is from " + nextLine[1]);
            }
        } catch (CsvValidationException | IOException e) {
            throw new RuntimeException(e);
        }
        return new Schedule();
    }
}
