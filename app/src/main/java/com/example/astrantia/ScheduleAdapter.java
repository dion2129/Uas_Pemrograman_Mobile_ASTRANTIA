package com.example.astrantia;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class ScheduleAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    private Context context;
    private List<Object> itemList;
    private OnItemClickListener listener;
    private boolean isAdmin; // Variabel baru untuk cek status Admin

    public interface OnItemClickListener {
        void onItemClick(Schedule schedule);
    }

    // Update Constructor: Terima parameter isAdmin
    public ScheduleAdapter(Context context, List<Object> itemList, boolean isAdmin, OnItemClickListener listener) {
        this.context = context;
        this.itemList = itemList;
        this.isAdmin = isAdmin; // Simpan status
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        if (itemList.get(position) instanceof String) return TYPE_HEADER;
        else return TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_schedule_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_schedule, parent, false);
            return new ItemViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == TYPE_HEADER) {
            String dayName = (String) itemList.get(position);
            ((HeaderViewHolder) holder).tvHeader.setText(dayName);

        } else {
            Schedule schedule = (Schedule) itemList.get(position);
            ItemViewHolder itemHolder = (ItemViewHolder) holder;

            itemHolder.tvSubject.setText(schedule.getSubjectName());
            itemHolder.tvTime.setText(schedule.getStartTime() + " - " + schedule.getEndTime());
            itemHolder.tvRoom.setText("Ruangan " + schedule.getRoom());

            // --- LOGIKA HAK AKSES ---
            if (isAdmin) {
                // Jika Admin: Tampilkan tombol hapus & aktifkan klik edit
                itemHolder.btnDelete.setVisibility(View.VISIBLE);

                itemHolder.itemView.setOnClickListener(v -> {
                    if (listener != null) listener.onItemClick(schedule);
                });

                itemHolder.btnDelete.setOnClickListener(v -> {
                    new AlertDialog.Builder(context)
                            .setTitle("Hapus Jadwal Global")
                            .setMessage("Hapus " + schedule.getSubjectName() + "? Ini akan hilang untuk semua user.")
                            .setPositiveButton("Hapus", (dialog, which) -> deleteSchedule(schedule.getId()))
                            .setNegativeButton("Batal", null)
                            .show();
                });
            } else {
                // Jika User Biasa: Sembunyikan hapus & matikan klik
                itemHolder.btnDelete.setVisibility(View.GONE);
                itemHolder.itemView.setOnClickListener(null); // Tidak bisa diklik
            }
        }
    }

    private void deleteSchedule(String scheduleId) {
        // Hapus dari GLOBAL_SCHEDULES
        DatabaseReference ref = FirebaseDatabase.getInstance("https://astrantia-aabb2-default-rtdb.asia-southeast1.firebasedatabase.app").getReference();

        ref.child("global_schedules").child(scheduleId).removeValue()
                .addOnSuccessListener(aVoid -> Toast.makeText(context, "Jadwal dihapus", Toast.LENGTH_SHORT).show());
    }

    @Override
    public int getItemCount() { return itemList.size(); }

    public static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvHeader;
        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvHeader = itemView.findViewById(R.id.tvHeaderDay);
        }
    }

    public static class ItemViewHolder extends RecyclerView.ViewHolder {
        TextView tvSubject, tvTime, tvRoom;
        ImageView btnDelete;
        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSubject = itemView.findViewById(R.id.tvSubjectName);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvRoom = itemView.findViewById(R.id.tvRoom);
            btnDelete = itemView.findViewById(R.id.btnDeleteSchedule);
        }
    }
}