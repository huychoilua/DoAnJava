package com.example.hellow.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface HistoryLogDao {

    @Insert
    void insertLog(HistoryLog log);

    @Query("SELECT * FROM history_logs ORDER BY timestamp DESC") // Lấy tất cả và sắp xếp theo thời gian mới nhất
    List<HistoryLog> getAllLogs();
}
