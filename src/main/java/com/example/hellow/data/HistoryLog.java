package com.example.hellow.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "history_logs")
public class HistoryLog {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @NonNull
    public String personId; // Mã số người được nhận diện

    @NonNull
    public String personName; // Tên người được nhận diện

    public long timestamp; // Lưu thời gian dưới dạng số (milliseconds) để dễ sắp xếp

    public HistoryLog(@NonNull String personId, @NonNull String personName, long timestamp) {
        this.personId = personId;
        this.personName = personName;
        this.timestamp = timestamp;
    }
}
