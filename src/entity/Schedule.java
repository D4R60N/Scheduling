package entity;

import java.util.ArrayList;
import java.util.List;

public class Schedule {
    private List<TimeSlot> timeSlots;
    private List<Course> courses;

    public Schedule() {
        timeSlots = new ArrayList<>();
    }
}
