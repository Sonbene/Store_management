package com.example.storemanagement;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class UserActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Hiển thị giao diện người dùng được định nghĩa trong user_layout.xml
        setContentView(R.layout.user_layout);

        // Nếu file user_layout.xml chưa có nội dung, bạn có thể thêm TextView để thông báo
        TextView tvWelcome = new TextView(this);
        tvWelcome.setText("Chào mừng User!");
        tvWelcome.setTextSize(24);
        setContentView(tvWelcome);
    }
}
