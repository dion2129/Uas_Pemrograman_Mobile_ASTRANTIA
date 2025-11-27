package com.example.astrantia;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

public class HomeFragment extends Fragment {
    private TextView tvWelcomeName, tvCurrentDay;
    private TextView tvHomeTaskTitle, tvHomeTaskDate;
    private ImageView imgHomeTaskStatus;
    private Button btnHomeGoToTask;

    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;

    private RecyclerView rvHomeSchedule;
    private LinearLayout layoutEmpty;
    private HomeScheduleAdapter adapter;
    private List<Schedule> todayList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        tvWelcomeName = view.findViewById(R.id.tvWelcomeName);
        tvCurrentDay = view.findViewById(R.id.tvCurrentDay);

        tvHomeTaskTitle = view.findViewById(R.id.tvHomeTaskTitle);
        tvHomeTaskDate = view.findViewById(R.id.tvHomeTaskDate);
        imgHomeTaskStatus = view.findViewById(R.id.imgHomeTaskStatus);
        btnHomeGoToTask = view.findViewById(R.id.btnHomeGoToTask);

        rvHomeSchedule = view.findViewById(R.id.rvHomeSchedule);
        layoutEmpty = view.findViewById(R.id.layoutEmptySchedule);

        todayList = new ArrayList<>();
        adapter = new HomeScheduleAdapter(todayList);
        rvHomeSchedule.setLayoutManager(new LinearLayoutManager(getContext()));
        rvHomeSchedule.setAdapter(adapter);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance("https://astrantia-aabb2-default-rtdb.asia-southeast1.firebasedatabase.app").getReference();

        String todayName = getTodayName();
        tvCurrentDay.setText(todayName);

        loadUserData();
        loadTodaySchedule(todayName);
        loadUpcomingTask();

        btnHomeGoToTask.setOnClickListener(v -> {
            if (getActivity() != null) {
                BottomNavigationView bottomNav = getActivity().findViewById(R.id.bottom_navigation);
                if (bottomNav != null) bottomNav.setSelectedItemId(R.id.nav_task);
            }
        });

        return view;
    }

    private void loadUpcomingTask() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        mDatabase.child("tasks").child(uid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // --- PENGECEKAN PENTING ---
                if (!isAdded() || getContext() == null) return;
                // --------------------------

                List<Task> activeTasks = new ArrayList<>();

                for (DataSnapshot data : snapshot.getChildren()) {
                    Task task = data.getValue(Task.class);
                    if (task != null && !task.isCompleted()) {
                        activeTasks.add(task);
                    }
                }

                if (!activeTasks.isEmpty()) {
                    Collections.sort(activeTasks, (t1, t2) -> t1.getDeadlineDate().compareTo(t2.getDeadlineDate()));
                    Task nearestTask = activeTasks.get(0);

                    tvHomeTaskTitle.setText(nearestTask.getTitle());
                    tvHomeTaskDate.setText("Deadline: " + nearestTask.getDisplayDate());
                    tvHomeTaskDate.setVisibility(View.VISIBLE);

                    imgHomeTaskStatus.setImageResource(R.drawable.ic_radio_button_unchecked);
                    // Aman menggunakan ContextCompat karena sudah dicek di atas
                    imgHomeTaskStatus.setColorFilter(ContextCompat.getColor(getContext(), R.color.mint_primary));

                    btnHomeGoToTask.setText("Lihat detail");

                } else {
                    tvHomeTaskTitle.setText("Tidak ada tugas mendatang");
                    tvHomeTaskDate.setVisibility(View.GONE);

                    imgHomeTaskStatus.setImageResource(R.drawable.ic_check_box);
                    imgHomeTaskStatus.setColorFilter(ContextCompat.getColor(getContext(), R.color.black));
                    imgHomeTaskStatus.setAlpha(0.3f);

                    btnHomeGoToTask.setText("Buat tugas baru");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // ... (Sisa method getTodayName, loadUserData, loadTodaySchedule tetap sama) ...

    private String getTodayName() {
        // Ambil waktu saat ini
        Date date = new Date();

        // Format "EEEE" artinya Nama Hari Lengkap (Monday, Tuesday, dst)
        // Locale("id", "ID") memaksa outputnya jadi Bahasa Indonesia (Senin, Selasa, dst)
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE", new Locale("id", "ID"));

        return sdf.format(date);
    }

    private void loadUserData() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();
        mDatabase.child("users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return; // Cek juga disini
                if (snapshot.exists()) {
                    String name = snapshot.child("fullName").getValue(String.class);
                    if (name != null) {
                        String[] splitName = name.split(" ");
                        tvWelcomeName.setText("Selamat datang,\n" + splitName[0] + "!");
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadTodaySchedule(String today) {
        mDatabase.child("global_schedules").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return; // Cek juga disini

                todayList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Schedule schedule = data.getValue(Schedule.class);
                    if (schedule != null && schedule.getDay().equalsIgnoreCase(today)) {
                        todayList.add(schedule);
                    }
                }
                if (todayList.isEmpty()) {
                    rvHomeSchedule.setVisibility(View.GONE);
                    layoutEmpty.setVisibility(View.VISIBLE);
                } else {
                    rvHomeSchedule.setVisibility(View.VISIBLE);
                    layoutEmpty.setVisibility(View.GONE);
                    todayList.sort((s1, s2) -> s1.getStartTime().compareTo(s2.getStartTime()));
                }
                adapter.notifyDataSetChanged();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}