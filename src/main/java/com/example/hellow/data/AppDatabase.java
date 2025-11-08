package com.example.hellow.data;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// 1. THÊM HistoryLog.class và 2. TĂNG version lên 2
@Database(entities = {User.class, HistoryLog.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    // Cung cấp một phương thức trừu tượng để lấy UserDao
    public abstract UserDao userDao();

    // 3. THÊM phương thức trừu tượng để lấy HistoryLogDao
    public abstract HistoryLogDao historyLogDao();

    // Sử dụng Singleton Pattern để đảm bảo chỉ có một thể hiện của database
    private static volatile AppDatabase INSTANCE;

    // Thêm một ExecutorService để chạy các tác vụ database trên luồng nền
    public static final ExecutorService databaseWriteExecutor = 
            Executors.newFixedThreadPool(4);

    // Phương thức tĩnh để lấy thể hiện của database
    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    // Tạo database nếu nó chưa tồn tại
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "app_database")
                            // Thêm dòng này để xử lý việc nâng cấp version.
                            // Nó sẽ xóa và tạo lại database khi version thay đổi.
                            // (Chỉ nên dùng trong quá trình phát triển)
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
