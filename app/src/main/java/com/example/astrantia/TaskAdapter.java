package com.example.astrantia;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.ViewHolder> {

    private Context context;
    private List<Task> taskList;
    private OnItemClickListener listener;

    // Interface agar Fragment bisa menangani klik item (untuk Edit)
    public interface OnItemClickListener {
        void onItemClick(Task task);
    }

    public TaskAdapter(Context context, List<Task> taskList, OnItemClickListener listener) {
        this.context = context;
        this.taskList = taskList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_task, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Task task = taskList.get(position);

        // 1. Set Data Teks
        holder.tvTitle.setText(task.getTitle());
        holder.tvDesc.setText(task.getDescription());
        holder.tvDate.setText(task.getDisplayDate());

        // 2. Logika Tampilan Berdasarkan Status (Selesai / Belum)
        if (task.isCompleted()) {
            // Tampilan TUGAS SELESAI
            holder.imgStatus.setImageResource(R.drawable.ic_check_circle); // Icon Centang Hijau
            holder.imgStatus.setColorFilter(Color.parseColor("#4CAF50")); // Warna Hijau

            // Coret Judul (Strikethrough)
            holder.tvTitle.setPaintFlags(holder.tvTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvTitle.setTextColor(Color.GRAY);

            // Sembunyikan indikator sisa hari
            holder.tvDaysLeft.setVisibility(View.GONE);
        } else {
            // Tampilan TUGAS AKTIF
            holder.imgStatus.setImageResource(R.drawable.ic_radio_button_unchecked); // Lingkaran Kosong
            holder.imgStatus.setColorFilter(ContextCompat.getColor(context, R.color.mint_primary)); // Warna Tema

            // Hapus Coretan (Normal)
            holder.tvTitle.setPaintFlags(holder.tvTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            holder.tvTitle.setTextColor(ContextCompat.getColor(context, R.color.black));

            // Tampilkan Sisa Hari
            holder.tvDaysLeft.setVisibility(View.VISIBLE);
            setupDeadlineIndicator(holder.tvDaysLeft, task.getDeadlineDate());
        }

        // 3. Klik Item (Buka Detail/Edit)
        holder.itemView.setOnClickListener(v -> listener.onItemClick(task));

        // 4. Klik Status (Ubah Aktif <-> Selesai)
        holder.imgStatus.setOnClickListener(v -> toggleTaskStatus(task));

        // 5. Klik Hapus
        holder.btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Hapus Tugas")
                    .setMessage("Yakin ingin menghapus tugas ini?")
                    .setPositiveButton("Hapus", (dialog, which) -> deleteTask(task.getId()))
                    .setNegativeButton("Batal", null)
                    .show();
        });
    }

    // Fungsi Menghitung Sisa Hari dan Warna
    private void setupDeadlineIndicator(TextView tv, String deadlineDate) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        try {
            Date deadline = sdf.parse(deadlineDate);
            Date today = new Date(); // Hari ini

            // Hitung selisih waktu
            long diff = deadline.getTime() - today.getTime();
            long days = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);

            if (days < 0) {
                // Terlewat
                tv.setText("Terlewat");
                tv.setBackgroundTintList(ContextCompat.getColorStateList(context, android.R.color.holo_red_light));
                tv.setTextColor(Color.WHITE);
            } else if (days == 0) {
                // Hari ini
                tv.setText("Hari Ini");
                tv.setBackgroundTintList(ContextCompat.getColorStateList(context, android.R.color.holo_orange_light));
                tv.setTextColor(Color.BLACK);
            } else {
                // Masih ada waktu
                tv.setText(days + " hari");
                if (days <= 3) {
                    // Mendesak (Merah Muda)
                    tv.setBackgroundColor(Color.parseColor("#FFEBEE"));
                    tv.setTextColor(Color.parseColor("#D32F2F"));
                } else {
                    // Masih Lama (Hijau Muda/Abu)
                    tv.setBackgroundColor(Color.parseColor("#E8F5E9"));
                    tv.setTextColor(Color.parseColor("#2E7D32"));
                }
            }
        } catch (ParseException e) {
            tv.setVisibility(View.GONE);
        }
    }

    // Fungsi Ubah Status di Firebase
    private void toggleTaskStatus(Task task) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        DatabaseReference ref = FirebaseDatabase.getInstance("https://astrantia-aabb2-default-rtdb.asia-southeast1.firebasedatabase.app").getReference();

        // Ubah true jadi false, atau sebaliknya
        boolean newStatus = !task.isCompleted();

        ref.child("tasks").child(uid).child(task.getId()).child("completed").setValue(newStatus);
    }

    // Fungsi Hapus di Firebase
    private void deleteTask(String taskId) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        DatabaseReference ref = FirebaseDatabase.getInstance("https://astrantia-aabb2-default-rtdb.asia-southeast1.firebasedatabase.app").getReference();

        ref.child("tasks").child(uid).child(taskId).removeValue()
                .addOnSuccessListener(aVoid -> Toast.makeText(context, "Tugas dihapus", Toast.LENGTH_SHORT).show());
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDesc, tvDate, tvDaysLeft;
        ImageView imgStatus, btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTaskTitle);
            tvDesc = itemView.findViewById(R.id.tvTaskDesc);
            tvDate = itemView.findViewById(R.id.tvTaskDate);
            tvDaysLeft = itemView.findViewById(R.id.tvDaysLeft);
            imgStatus = itemView.findViewById(R.id.imgStatus);
            btnDelete = itemView.findViewById(R.id.btnDeleteTask);
        }
    }
}