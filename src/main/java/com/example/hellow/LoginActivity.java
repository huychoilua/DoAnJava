package com.example.hellow;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.hellow.data.AppDatabase;
import com.example.hellow.data.User;

public class LoginActivity extends AppCompatActivity {

    EditText edtUsername, edtPassword;
    Button btnLogin;
    TextView tvRegisterRedirect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        edtUsername = findViewById(R.id.edtUsername);
        edtPassword = findViewById(R.id.edtPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegisterRedirect = findViewById(R.id.tvRegister);

        btnLogin.setOnClickListener(v -> {    String username = edtUsername.getText().toString().trim();
            String password = edtPassword.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                return;
            }

            // --- PHẦN THAY ĐỔI CHÍNH BẮT ĐẦU TỪ ĐÂY ---

            // Chạy tác vụ truy vấn database trên một luồng nền để không làm treo giao diện
            new Thread(() -> {
                // Lấy thể hiện của database
                AppDatabase db = AppDatabase.getDatabase(getApplicationContext());
                // Tìm người dùng trong database theo username
                // DÒNG ĐÚNG
                User user = db.userDao().findByUsername(username);

                // Sau khi có kết quả, quay lại luồng UI để xử lý và cập nhật giao diện
                runOnUiThread(() -> {
                    // TODO: Trong ứng dụng thực tế, bạn cần so sánh mật khẩu đã được mã hóa
                    // Ví dụ: if (user != null && BCrypt.checkpw(password, user.password)) { ... }
                    if (user != null && user.password.equals(password)) {
                        // Đăng nhập thành công
                        Toast.makeText(LoginActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        // -----------------------
                        startActivity(intent);
                        finish(); // Đóng màn hình đăng nhập để người dùng không thể quay lại
                    } else {
                        // Đăng nhập thất bại (sai username hoặc password)
                        Toast.makeText(LoginActivity.this, "Sai tên đăng nhập hoặc mật khẩu", Toast.LENGTH_SHORT).show();
                    }
                });
            }).start();

            // --- KẾT THÚC PHẦN THAY ĐỔI ---
        });

        tvRegisterRedirect.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
    }
}
