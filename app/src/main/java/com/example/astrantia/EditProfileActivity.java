package com.example.astrantia;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private EditText etName, etNim, etEmail;
    private Button btnSave, btnCancel;
    private DatabaseReference mDatabase;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        // 1. Inisialisasi View
        etName = findViewById(R.id.etEditName);
        etNim = findViewById(R.id.etEditNim);
        etEmail = findViewById(R.id.etEditEmail);
        btnSave = findViewById(R.id.btnSaveProfile);
        btnCancel = findViewById(R.id.btnCancel);

        // 2. Setup Firebase (Gunakan URL Asia Southeast)
        mDatabase = FirebaseDatabase.getInstance("https://astrantia-aabb2-default-rtdb.asia-southeast1.firebasedatabase.app").getReference();

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            loadCurrentData();
        }

        // 3. Listener Tombol
        btnSave.setOnClickListener(v -> saveProfile());
        btnCancel.setOnClickListener(v -> finish());
    }

    private void loadCurrentData() {
        // Mengambil data user saat ini untuk ditampilkan di form
        mDatabase.child("users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.child("fullName").getValue(String.class);
                    String nim = snapshot.child("nim").getValue(String.class);
                    String email = snapshot.child("email").getValue(String.class);

                    etName.setText(name);
                    etNim.setText(nim);
                    etEmail.setText(email); // Email hanya read-only
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(EditProfileActivity.this, "Gagal memuat data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveProfile() {
        String newName = etName.getText().toString().trim();
        String newNim = etNim.getText().toString().trim();

        // Validasi input kosong
        if (newName.isEmpty() || newNim.isEmpty()) {
            Toast.makeText(this, "Nama dan NIM tidak boleh kosong", Toast.LENGTH_SHORT).show();
            return;
        }

        // Siapkan data yang akan diupdate (Hanya Nama dan NIM)
        Map<String, Object> updates = new HashMap<>();
        updates.put("fullName", newName);
        updates.put("nim", newNim);

        // Lakukan Update ke Firebase
        mDatabase.child("users").child(uid).updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(EditProfileActivity.this, "Profil berhasil diperbarui", Toast.LENGTH_SHORT).show();
                    finish(); // Tutup halaman edit dan kembali ke profil
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(EditProfileActivity.this, "Gagal update: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}