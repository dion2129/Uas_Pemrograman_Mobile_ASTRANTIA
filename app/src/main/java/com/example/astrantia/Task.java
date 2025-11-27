package com.example.astrantia;

public class Task {
    private String id;
    private String title;
    private String description;
    private String deadlineDate; // Format: yyyy-MM-dd (untuk sorting)
    private String displayDate;  // Format: dd MMM yyyy (untuk display)
    private boolean isCompleted;

    public Task() {}

    public Task(String id, String title, String description, String deadlineDate, String displayDate, boolean isCompleted) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.deadlineDate = deadlineDate;
        this.displayDate = displayDate;
        this.isCompleted = isCompleted;
    }

    // Getter & Setter
    public String getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getDeadlineDate() { return deadlineDate; }
    public void setDeadlineDate(String deadlineDate) { this.deadlineDate = deadlineDate; }
    public String getDisplayDate() { return displayDate; }
    public void setDisplayDate(String displayDate) { this.displayDate = displayDate; }
    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) { isCompleted = completed; }
}