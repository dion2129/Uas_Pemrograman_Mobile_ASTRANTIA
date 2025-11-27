package com.example.astrantia;

public class Schedule {
    private String id;
    private String day;
    private String subjectName;
    private String startTime;
    private String endTime;
    private String room;

    public Schedule() {
        // Constructor kosong diperlukan untuk Firebase
    }

    public Schedule(String id, String day, String subjectName, String startTime, String endTime, String room) {
        this.id = id;
        this.day = day;
        this.subjectName = subjectName;
        this.startTime = startTime;
        this.endTime = endTime;
        this.room = room;
    }

    // Getter
    public String getId() { return id; }
    public String getDay() { return day; }
    public String getSubjectName() { return subjectName; }
    public String getStartTime() { return startTime; }
    public String getEndTime() { return endTime; }
    public String getRoom() { return room; }
}