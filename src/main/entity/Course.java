package main.entity;

public class Course {
    private final String name;
    private final int maxActivitySize;

    public Course(String name, int maxActivitySize) {
        this.name = name;
        this.maxActivitySize = maxActivitySize;
    }

    public String getName() { return name; }
    public int getMaxActivitySize() { return maxActivitySize; }

    @Override
    public String toString() { return name; }
}
