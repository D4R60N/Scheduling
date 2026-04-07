package main.entity;

public class Activity {
    private final Course course;
    private final int index;
    private int timeSlot = -1;

    public Activity(Course course, int index) {
        this.course = course;
        this.index = index;
    }

    public Course getCourse() { return course; }
    public int getIndex() { return index; }
    public int getTimeSlot() { return timeSlot; }
    public void setTimeSlot(int timeSlot) { this.timeSlot = timeSlot; }

    @Override
    public String toString() {
        return String.format("%s (Grp %d)", course.getName(), index + 1);
    }
}
