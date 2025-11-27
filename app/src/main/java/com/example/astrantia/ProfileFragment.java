package com.example.astrantia;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ProfileFragment extends Fragment {

    private TextView tvName, tvNim, tvEmail;
    private TextView tvStatMatkul, tvStatTotalTugas, tvStatTugasSelesai;
    private Button btnLogout, btnEditProfile;
    private DatabaseReference mDatabase;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        tvName = view.findViewById(R.id.tvProfileName);
        tvNim = view.findViewById(R.id.tvProfileNim);
        tvEmail = view.findViewById(R.id.tvProfileEmail);

        tvStatMatkul = view.findViewById(R.id.tvStatMatkul);
        tvStatTotalTugas = view.findViewById(R.id.tvStatTotalTugas);
        tvStatTugasSelesai = view.findViewById(R.id.tvStatTugasSelesai);

        btnLogout = view.findViewById(R.id.btnLogout);
        btnEditProfile = view.findViewById(R.id.btnEditProfile);

        mDatabase = FirebaseDatabase.getInstance("https://astrantia-aabb2-default-rtdb.asia-southeast1.firebasedatabase.app").getReference();

        loadProfileData();
        loadStatistics();

        btnEditProfile.setOnClickListener(v -> startActivity(new Intent(getActivity(), EditProfileActivity.class)));

        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadProfileData();
        loadStatistics();
    }

    private void loadProfileData() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        mDatabase.child("users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.child("fullName").getValue(String.class);
                    String nim = snapshot.child("nim").getValue(String.class);
                    String email = snapshot.child("email").getValue(String.class);

                    tvName.setText(name != null ? name : "User");
                    tvNim.setText("NIM: " + (nim != null ? nim : "-"));
                    tvEmail.setText(email != null ? email : "-");
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadStatistics() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // 1. Hitung Mata Kuliah (Global)
        mDatabase.child("global_schedules").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    tvStatMatkul.setText(String.valueOf(snapshot.getChildrenCount()));
                } else {
                    tvStatMatkul.setText("0");
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        // 2. Hitung Tugas (Pribadi) - BARU DITAMBAHKAN
        mDatabase.child("tasks").child(uid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long total = 0;
                long completed = 0;

                if (snapshot.exists()) {
                    total = snapshot.getChildrenCount();
                    for (DataSnapshot data : snapshot.getChildren()) {
                        Task task = data.getValue(Task.class);
                        if (task != null && task.isCompleted()) {
                            completed++;
                        }
                    }
                }

                tvStatTotalTugas.setText(String.valueOf(total));
                tvStatTugasSelesai.setText(String.valueOf(completed));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}