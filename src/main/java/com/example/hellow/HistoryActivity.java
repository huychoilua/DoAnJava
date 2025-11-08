package com.example.hellow;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.hellow.adapter.HistoryAdapter;
import com.example.hellow.data.AppDatabase;
import com.example.hellow.data.HistoryLog;
import java.util.ArrayList;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerViewHistory;
    private Button btnBackToMain;
    private HistoryAdapter historyAdapter;
    private List<HistoryLog> historyLogList = new ArrayList<>();

    // Thêm một Tag để lọc Logcat cho dễ dàng hơn
    private static final String TAG = "HistoryActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        Log.d(TAG, "onCreate: Activity đã được tạo.");

        // --- PHẦN BỔ SUNG: THÊM BƯỚC KIỂM TRA AN TOÀN ---

        // Ánh xạ các View
        recyclerViewHistory = findViewById(R.id.recyclerViewHistory);
        btnBackToMain = findViewById(R.id.btnBackToMain);

        // KIỂM TRA NULL NGAY LẬP TỨC ĐỂ CHẨN ĐOÁN LỖI LAYOUT
        // Nếu một trong các View này là null, ứng dụng sẽ không crash mà sẽ hiển thị lỗi và đóng lại.
        // Điều này cho bạn biết vấn đề nằm ở file activity_history.xml hoặc lỗi cache.
        if (recyclerViewHistory == null || btnBackToMain == null) {
            Toast.makeText(this, "Lỗi nghiêm trọng: Không tìm thấy View trong layout!", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Lỗi findViewById: RecyclerView hoặc Button là null. Hãy kiểm tra lại ID trong file activity_history.xml và thực hiện Clean/Rebuild Project.");
            finish(); // Đóng Activity để tránh crash
            return;  // Dừng thực thi hàm onCreate tại đây
        }
        // Nếu code chạy đến đây, nghĩa là việc ánh xạ View đã thành công.
        Log.d(TAG, "onCreate: Ánh xạ View thành công.");
        // --- KẾT THÚC PHẦN KIỂM TRA ---

        // Thiết lập RecyclerView
        setupRecyclerView();

        // Tải dữ liệu lịch sử từ database
        loadHistoryData();

        // Thiết lập sự kiện cho nút quay về
        btnBackToMain.setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        // Tạo adapter với một danh sách rỗng ban đầu
        historyAdapter = new HistoryAdapter(historyLogList);
        // Thiết lập layout manager cho RecyclerView (sắp xếp theo chiều dọc)
        recyclerViewHistory.setLayoutManager(new LinearLayoutManager(this));
        // Gắn adapter vào RecyclerView
        recyclerViewHistory.setAdapter(historyAdapter);
        Log.d(TAG, "setupRecyclerView: RecyclerView đã được thiết lập.");
    }

    private void loadHistoryData() {
        Log.d(TAG, "loadHistoryData: Bắt đầu tải dữ liệu từ database...");
        // Chạy tác vụ lấy dữ liệu trên luồng nền
        new Thread(() -> {
            try {
                AppDatabase db = AppDatabase.getDatabase(getApplicationContext());
                List<HistoryLog> logsFromDb = db.historyLogDao().getAllLogs();
                Log.d(TAG, "loadHistoryData: Đã lấy được " + logsFromDb.size() + " bản ghi từ DB.");

                // Quay lại luồng UI để cập nhật danh sách
                runOnUiThread(() -> {
                    if (logsFromDb.isEmpty()) {
                        Toast.makeText(HistoryActivity.this, "Chưa có lịch sử nhận diện.", Toast.LENGTH_SHORT).show();
                    }
                    historyLogList.clear(); // Xóa dữ liệu cũ
                    historyLogList.addAll(logsFromDb); // Thêm dữ liệu mới
                    historyAdapter.notifyDataSetChanged(); // Báo cho adapter biết dữ liệu đã thay đổi để nó vẽ lại
                    Log.d(TAG, "loadHistoryData: Đã cập nhật giao diện RecyclerView.");
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Log.e(TAG, "Lỗi khi tải dữ liệu từ DB: " + e.getMessage());
                    Toast.makeText(HistoryActivity.this, "Lỗi khi tải lịch sử từ database.", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
}
