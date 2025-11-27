package com.example.astrantia;

public class User {
    private String uid;
    private String fullName;
    private String nim;
    private String email;
    private String role; // "admin" atau "user"

    // Konstruktor kosong (diperlukan untuk Firestore)
    public User() {}

    // Konstruktor lengkap
    public User(String uid, String fullName, String nim, String email, String role) {
        this.uid = uid;
        this.fullName = fullName;
        this.nim = nim;
        this.email = email;
        this.role = role;
    }

    // Getter dan Setter (Enkapsulasi)
    public String getFullName() { return fullName; }
    public String getNim() { return nim; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
}
