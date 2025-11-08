package com.example.hellow.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface UserDao {

    // Lệnh thêm một người dùng mới. Nếu username đã tồn tại, nó sẽ bị bỏ qua.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void registerUser(User user);

    // Lệnh tìm một người dùng theo username.
    // Dùng để kiểm tra khi đăng nhập.
    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    User findByUsername(String username);
}
