package com.example.astrantia;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScheduleFragment extends Fragment {

    private RecyclerView rvSchedules;
    private ScheduleAdapter adapter;
    private List<Object> groupedList;
    private Button btnAdd;
    private TextView tvTotal;

    private DatabaseReference mDatabase;
    private boolean isAdmin = false; // Default bukan admin

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_schedule, container, false);

        rvSchedules = view.findViewById(R.id.rvSchedules);
        btnAdd = view.findViewById(R.id.btnAddSchedule);
        tvTotal = view.findViewById(R.id.tvTotalSchedules);

        groupedList = new ArrayList<>();
        rvSchedules.setLayoutManager(new LinearLayoutManager(getContext()));

        // Inisialisasi Database
        mDatabase = FirebaseDatabase.getInstance("https://astrantia-aabb2-default-rtdb.asia-southeast1.firebasedatabase.app").getReference();

        checkUserRole(); // Cek dulu dia Raja atau Rakyat Jelata

        btnAdd.setOnClickListener(v -> showScheduleDialog(null));

        return view;
    }

    private void checkUserRole() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Cek Role di Database Users
        mDatabase.child("users").child(uid).child("role").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String role = snapshot.getValue(String.class);

                // Jika role adalah "admin", set true
                isAdmin = "admin".equalsIgnoreCase(role);

                setupUIBasedOnRole();
                loadGlobalSchedules();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private void setupUIBasedOnRole() {
        if (isAdmin) {
            btnAdd.setVisibility(View.VISIBLE); // Raja boleh nambah jadwal
        } else {
            btnAdd.setVisibility(View.GONE); // Rakyat hanya boleh melihat
        }

        // Pasang Adapter setelah tahu Role-nya
        adapter = new ScheduleAdapter(getContext(), groupedList, isAdmin, schedule -> {
            if (isAdmin) {
                showScheduleDialog(schedule); // Hanya admin yang bisa buka dialog edit
            }
        });
        rvSchedules.setAdapter(adapter);
    }

    private void loadGlobalSchedules() {
        // BACA DARI 'global_schedules' (Bukan per UID lagi)
        mDatabase.child("global_schedules").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                groupedList.clear();
                List<Schedule> rawList = new ArrayList<>();

                for (DataSnapshot data : snapshot.getChildren()) {
                    Schedule schedule = data.getValue(Schedule.class);
                    if (schedule != null) rawList.add(schedule);
                }

                // Grouping Logic (Sama seperti sebelumnya)
                String[] daysOfWeek = {"Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu"};
                int totalMatkul = 0;

                for (String day : daysOfWeek) {
                    List<Schedule> schedulesForDay = new ArrayList<>();
                    for (Schedule s : rawList) {
                        if (s.getDay().equalsIgnoreCase(day)) schedulesForDay.add(s);
                    }
                    if (!schedulesForDay.isEmpty()) {
                        groupedList.add(day);
                        groupedList.addAll(schedulesForDay);
                        totalMatkul += schedulesForDay.size();
                    }
                }

                if (adapter != null) adapter.notifyDataSetChanged();
                tvTotal.setText("Total: " + totalMatkul + " mata kuliah");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    // --- DIALOG & VALIDASI (Hanya bisa diakses jika tombol Add muncul / Admin klik item) ---

    private void showScheduleDialog(@Nullable Schedule scheduleToEdit) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_schedule, null);
        builder.setView(view);
        AlertDialog dialog = builder.create();

        Spinner spinnerDay = view.findViewById(R.id.spinnerDay);
        EditText etSubject = view.findViewById(R.id.etSubjectDialog);
        EditText etStart = view.findViewById(R.id.etStartTime);
        EditText etEnd = view.findViewById(R.id.etEndTime);
        EditText etRoom = view.findViewById(R.id.etRoomDialog);
        Button btnSave = view.findViewById(R.id.btnSaveDialog);
        Button btnCancel = view.findViewById(R.id.btnCancelDialog);

        etStart.addTextChangedListener(new TimeTextWatcher(etStart));
        etEnd.addTextChangedListener(new TimeTextWatcher(etEnd));

        String[] days = {"Senin", "Selasa", "Rabu", "Kamis", "Jumat"};
        ArrayAdapter<String> adapterDay = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, days);
        adapterDay.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDay.setAdapter(adapterDay);

        if (scheduleToEdit != null) {
            btnSave.setText("Update");
            etSubject.setText(scheduleToEdit.getSubjectName());
            etStart.setText(scheduleToEdit.getStartTime());
            etEnd.setText(scheduleToEdit.getEndTime());
            etRoom.setText(scheduleToEdit.getRoom());
            int spinnerPosition = adapterDay.getPosition(scheduleToEdit.getDay());
            spinnerDay.setSelection(spinnerPosition);
        }

        btnSave.setOnClickListener(v -> {
            String day = spinnerDay.getSelectedItem().toString();
            String subject = etSubject.getText().toString();
            String start = etStart.getText().toString();
            String end = etEnd.getText().toString();
            String room = etRoom.getText().toString();

            if (subject.isEmpty() || start.isEmpty() || end.isEmpty() || room.isEmpty()) {
                Toast.makeText(getContext(), "Isi semua data!", Toast.LENGTH_SHORT).show();
            } else if (!isValidTime(start) || !isValidTime(end)) {
                Toast.makeText(getContext(), "Format jam salah!", Toast.LENGTH_SHORT).show();
            } else {
                if (scheduleToEdit == null) {
                    saveScheduleToFirebase(day, subject, start, end, room);
                } else {
                    updateScheduleInFirebase(scheduleToEdit.getId(), day, subject, start, end, room);
                }
                dialog.dismiss();
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();
    }

    private boolean isValidTime(String time) {
        if (time.length() != 5 || !time.contains(":")) return false;
        try {
            String[] parts = time.split(":");
            int h = Integer.parseInt(parts[0]);
            int m = Integer.parseInt(parts[1]);
            return (h>=0 && h<=23) && (m>=0 && m<=59);
        } catch (NumberFormatException e) { return false; }
    }

    // SIMPAN KE GLOBAL
    private void saveScheduleToFirebase(String day, String subject, String start, String end, String room) {
        String id = mDatabase.child("global_schedules").push().getKey(); // Ganti path
        Schedule schedule = new Schedule(id, day, subject, start, end, room);

        if (id != null) {
            mDatabase.child("global_schedules").child(id).setValue(schedule)
                    .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Jadwal Global ditambahkan", Toast.LENGTH_SHORT).show());
        }
    }

    // UPDATE KE GLOBAL
    private void updateScheduleInFirebase(String id, String day, String subject, String start, String end, String room) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("day", day);
        updates.put("subjectName", subject);
        updates.put("startTime", start);
        updates.put("endTime", end);
        updates.put("room", room);

        mDatabase.child("global_schedules").child(id).updateChildren(updates)
                .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Jadwal Global diperbarui", Toast.LENGTH_SHORT).show());
    }

    private static class TimeTextWatcher implements TextWatcher {
        private final EditText editText;
        private String current = "";
        public TimeTextWatcher(EditText editText) { this.editText = editText; }
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (!s.toString().equals(current)) {
                String clean = s.toString().replaceAll("[^\\d]", "");
                String cleanC = current.replaceAll("[^\\d]", "");
                int cl = clean.length();
                int sel = cl;
                for (int i = 2; i <= cl && i < 6; i += 2) sel++;
                if (clean.equals(cleanC)) sel--;
                if (clean.length() < 8){
                    String formatted = "";
                    if (clean.length() >= 4) formatted = clean.substring(0, 2) + ":" + clean.substring(2, 4);
                    else if (clean.length() >= 2) formatted = clean.substring(0, 2) + ":" + clean.substring(2);
                    else formatted = clean;
                    current = formatted;
                    editText.setText(formatted);
                    try { editText.setSelection(Math.min(formatted.length(), 5)); }
                    catch (IndexOutOfBoundsException e) { editText.setSelection(formatted.length()); }
                }
            }
        }
        @Override public void afterTextChanged(Editable s) { }
    }
}