package com.example.hellow.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.hellow.R;
import com.example.hellow.data.HistoryLog;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    private List<HistoryLog> logList;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault());

    public HistoryAdapter(List<HistoryLog> logList) {
        this.logList = logList;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Tạo view cho một hàng từ file history_item_layout.xml
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.history_item_layout, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        // Lấy dữ liệu từ một item và gán vào các view
        HistoryLog currentLog = logList.get(position);
        holder.tvPersonName.setText(currentLog.personName);
        holder.tvPersonId.setText(currentLog.personId);
        holder.tvDetectionDate.setText(dateFormat.format(new Date(currentLog.timestamp)));
    }

    @Override
    public int getItemCount() {
        // Trả về tổng số item trong danh sách
        return logList.size();
    }

    // Lớp ViewHolder chứa các view của một hàng
    public static class HistoryViewHolder extends RecyclerView.ViewHolder {
        public TextView tvPersonName;
        public TextView tvPersonId;
        public TextView tvDetectionDate;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPersonName = itemView.findViewById(R.id.tvPersonName);
            tvPersonId = itemView.findViewById(R.id.tvPersonId);
            tvDetectionDate = itemView.findViewById(R.id.tvDetectionDate);
        }
    }
}
