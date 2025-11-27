package com.example.astrantia;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class TaskFragment extends Fragment {

    private RecyclerView rvActive, rvCompleted;
    private TaskAdapter activeAdapter, completedAdapter;
    private List<Task> activeList, completedList;

    private LinearLayout layoutEmpty, layoutContent;
    private ProgressBar progressBar;
    private TextView tvPercent, tvProgressText;
    private Button btnAdd;

    private DatabaseReference mDatabase;
    private String uid;

    private Calendar calendar = Calendar.getInstance();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_task, container, false);

        // Binding Views
        rvActive = view.findViewById(R.id.rvActiveTasks);
        rvCompleted = view.findViewById(R.id.rvCompletedTasks);
        layoutEmpty = view.findViewById(R.id.layoutEmptyTask);
        layoutContent = view.findViewById(R.id.layoutContentTask);
        progressBar = view.findViewById(R.id.progressBarTask);
        tvPercent = view.findViewById(R.id.tvPercent);
        tvProgressText = view.findViewById(R.id.tvTaskProgressText);
        btnAdd = view.findViewById(R.id.btnAddTask);

        // Setup Adapter
        activeList = new ArrayList<>();
        completedList = new ArrayList<>();

        // (Kita akan buat TaskAdapter sebentar lagi)
        activeAdapter = new TaskAdapter(getContext(), activeList, this::openDetail);
        completedAdapter = new TaskAdapter(getContext(), completedList, this::openDetail);

        rvActive.setLayoutManager(new LinearLayoutManager(getContext()));
        rvActive.setAdapter(activeAdapter);

        rvCompleted.setLayoutManager(new LinearLayoutManager(getContext()));
        rvCompleted.setAdapter(completedAdapter);

        // Firebase
        mDatabase = FirebaseDatabase.getInstance("https://astrantia-aabb2-default-rtdb.asia-southeast1.firebasedatabase.app").getReference();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            loadTasks();
        }

        btnAdd.setOnClickListener(v -> showAddTaskDialog(null));

        return view;
    }

    // Buka Detail / Edit
    private void openDetail(Task task) {
        // Karena ini hanya popup dialog, kita bisa pakai dialog yang sama dengan mode Edit
        showAddTaskDialog(task);
    }

    private void showAddTaskDialog(@Nullable Task taskToEdit) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_task, null);
        builder.setView(view);
        AlertDialog dialog = builder.create();

        EditText etTitle = view.findViewById(R.id.etTaskTitle);
        EditText etDesc = view.findViewById(R.id.etTaskDesc);
        EditText etDeadline = view.findViewById(R.id.etTaskDeadline);
        Button btnSave = view.findViewById(R.id.btnSaveTask);
        Button btnCancel = view.findViewById(R.id.btnCancelTask);
        TextView tvTitle = view.findViewById(R.id.tvDialogTitle);

        // Setup DatePicker
        final String[] selectedDate = {""}; // Format yyyy-MM-dd untuk database

        etDeadline.setOnClickListener(v -> {
            new DatePickerDialog(getContext(), (view1, year, month, dayOfMonth) -> {
                calendar.set(year, month, dayOfMonth);
                SimpleDateFormat displayFormat = new SimpleDateFormat("dd MMM yyyy", new Locale("id", "ID"));
                SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

                etDeadline.setText(displayFormat.format(calendar.getTime()));
                selectedDate[0] = dbFormat.format(calendar.getTime());
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
        });

        // Mode Edit
        if (taskToEdit != null) {
            tvTitle.setText("Edit Tugas");
            btnSave.setText("Simpan Perubahan");
            etTitle.setText(taskToEdit.getTitle());
            etDesc.setText(taskToEdit.getDescription());
            etDeadline.setText(taskToEdit.getDisplayDate());
            selectedDate[0] = taskToEdit.getDeadlineDate();
        }

        btnSave.setOnClickListener(v -> {
            String title = etTitle.getText().toString();
            String desc = etDesc.getText().toString();

            if (title.isEmpty() || selectedDate[0].isEmpty()) {
                Toast.makeText(getContext(), "Lengkapi data!", Toast.LENGTH_SHORT).show();
                return;
            }

            String id = (taskToEdit == null) ? mDatabase.child("tasks").child(uid).push().getKey() : taskToEdit.getId();
            boolean isDone = (taskToEdit != null) && taskToEdit.isCompleted();

            Task newTask = new Task(id, title, desc, selectedDate[0], etDeadline.getText().toString(), isDone);
            mDatabase.child("tasks").child(uid).child(id).setValue(newTask);

            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();
    }

    private void loadTasks() {
        mDatabase.child("tasks").child(uid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                activeList.clear();
                completedList.clear();

                for (DataSnapshot data : snapshot.getChildren()) {
                    Task task = data.getValue(Task.class);
                    if (task != null) {
                        if (task.isCompleted()) {
                            completedList.add(task);
                        } else {
                            activeList.add(task);
                        }
                    }
                }

                // Sorting Active Task (Deadline terdekat di atas)
                Collections.sort(activeList, (t1, t2) -> t1.getDeadlineDate().compareTo(t2.getDeadlineDate()));

                // Update UI
                if (activeList.isEmpty() && completedList.isEmpty()) {
                    layoutEmpty.setVisibility(View.VISIBLE);
                    layoutContent.setVisibility(View.GONE);
                } else {
                    layoutEmpty.setVisibility(View.GONE);
                    layoutContent.setVisibility(View.VISIBLE);
                }

                // Hitung Progress
                int total = activeList.size() + completedList.size();
                int done = completedList.size();
                int percent = (total > 0) ? (done * 100 / total) : 0;

                tvProgressText.setText(done + " dari " + total + " tugas selesai");
                tvPercent.setText(percent + "%");
                progressBar.setProgress(percent);

                activeAdapter.notifyDataSetChanged();
                completedAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}