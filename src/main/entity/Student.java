package main.entity;

public class Student {
    private final String name;
    private final int[] preferences;

    public Student(String name, int[] preferences) {
        this.name = name;
        this.preferences = preferences;
    }

    public String getName() { return name; }
    public int[] getPreferences() { return preferences; }

    @Override
    public String toString() { return name; }
}
