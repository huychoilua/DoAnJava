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

public class RegisterActivity extends AppCompatActivity {

    EditText edtUsernameRegister, edtPasswordRegister, edtConfirmPassword;
    Button btnRegister;
    TextView tvLoginRedirect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        edtUsernameRegister = findViewById(R.id.edtUsernameRegister);
        edtPasswordRegister = findViewById(R.id.edtPasswordRegister);
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        tvLoginRedirect = findViewById(R.id.tvLoginRedirect);

        btnRegister.setOnClickListener(v -> {
            String username = edtUsernameRegister.getText().toString().trim();
            String password = edtPasswordRegister.getText().toString().trim();
            String confirm = edtConfirmPassword.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!password.equals(confirm)) {
                Toast.makeText(this, "Mật khẩu nhập lại không khớp", Toast.LENGTH_SHORT).show();
                return;
            }

            // --- PHẦN THAY ĐỔI CHÍNH BẮT ĐẦU TỪ ĐÂY ---

            // TODO: BƯỚC BẢO MẬT QUAN TRỌNG
            // Trong ứng dụng thực tế, bạn PHẢI mã hóa mật khẩu trước khi lưu.
            // Ví dụ: String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
            // Hiện tại, chúng ta sẽ tạm bỏ qua để đơn giản hóa.
            User newUser = new User(username, password);

            // Chạy tác vụ lưu vào database trên một luồng nền để không làm treo UI
            new Thread(() -> {
                // Lấy thể hiện của database
                AppDatabase db = AppDatabase.getDatabase(getApplicationContext());
                // Thực hiện lệnh thêm người dùng
                db.userDao().registerUser(newUser);

                // Sau khi lưu xong, quay lại luồng UI để thông báo và đóng Activity
                runOnUiThread(() -> {
                    Toast.makeText(RegisterActivity.this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show();
                    // Chuyển về màn hình đăng nhập sau khi đăng ký
                    startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                    finish(); // Đóng màn hình đăng ký
                });
            }).start();

            // --- KẾT THÚC PHẦN THAY ĐỔI ---
        });

        tvLoginRedirect.setOnClickListener(v ->
                startActivity(new Intent(this, LoginActivity.class)));
    }
}
