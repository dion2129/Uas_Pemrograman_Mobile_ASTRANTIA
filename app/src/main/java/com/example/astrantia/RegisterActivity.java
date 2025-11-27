package com.example.astrantia;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
// IMPORT BARU UNTUK REALTIME DATABASE
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RegisterActivity extends AppCompatActivity {
    private EditText etName, etNim, etEmail, etPass;
    private Button btnRegister;
    private TextView tvLoginLink;
    private FirebaseAuth mAuth;

    // Ganti Firestore dengan DatabaseReference
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();

        // INISIALISASI REALTIME DATABASE (Sesuaikan URL dengan punya Anda)
        mDatabase = FirebaseDatabase.getInstance("https://astrantia-aabb2-default-rtdb.asia-southeast1.firebasedatabase.app").getReference();

        etName = findViewById(R.id.etName);
        etNim = findViewById(R.id.etNim);
        etEmail = findViewById(R.id.etEmail);
        etPass = findViewById(R.id.etPass);
        btnRegister = findViewById(R.id.btnRegister);
        tvLoginLink = findViewById(R.id.tvLoginLink);

        btnRegister.setOnClickListener(v -> registerUser());

        tvLoginLink.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void registerUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPass.getText().toString().trim();
        String name = etName.getText().toString().trim();
        String nim = etNim.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty() || name.isEmpty()) {
            Toast.makeText(this, "Isi semua data!", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String uid = mAuth.getCurrentUser().getUid();
                        User newUser = new User(uid, name, nim, email, "user");

                        // SIMPAN KE REALTIME DATABASE
                        // Strukturnya: Root -> users -> UID -> Data
                        mDatabase.child("users").child(uid).setValue(newUser)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Register Berhasil", Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(this, MainActivity.class));
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Gagal simpan data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });

                    } else {
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "Gagal";
                        Toast.makeText(this, "Gagal Auth: " + errorMsg, Toast.LENGTH_SHORT).show();
                    }
                });
    }
}