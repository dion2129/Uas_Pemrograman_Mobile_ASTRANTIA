package com.example.astrantia;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class HomeScheduleAdapter extends RecyclerView.Adapter<HomeScheduleAdapter.ViewHolder> {

    private List<Schedule> list;

    public HomeScheduleAdapter(List<Schedule> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Kita gunakan layout item_schedule yang sudah ada, tapi nanti kita sembunyikan tombol hapusnya
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_schedule, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Schedule schedule = list.get(position);
        holder.tvSubject.setText(schedule.getSubjectName());
        holder.tvTime.setText(schedule.getStartTime() + " - " + schedule.getEndTime());
        holder.tvRoom.setText("Ruangan " + schedule.getRoom());

        // Sembunyikan tombol hapus karena ini di Beranda
        holder.btnDelete.setVisibility(View.GONE);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSubject, tvTime, tvRoom;
        View btnDelete; // Menggunakan View umum agar bisa di-gone

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSubject = itemView.findViewById(R.id.tvSubjectName);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvRoom = itemView.findViewById(R.id.tvRoom);
            btnDelete = itemView.findViewById(R.id.btnDeleteSchedule);
        }
    }
}