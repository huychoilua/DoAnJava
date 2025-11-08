package com.example.hellow.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "users") // Đặt tên cho bảng là "users"
public class User {

    @PrimaryKey    @NonNull // Đảm bảo username không bao giờ null và là khóa chính
    public String username;

    @NonNull
    public String password; // Mật khẩu (sẽ được mã hóa)

    // Constructor để dễ dàng tạo đối tượng User
    public User(@NonNull String username, @NonNull String password) {
        this.username = username;
        this.password = password;
    }
}
