package com.example.storemanagement;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {
    private EditText etUsername, etPassword;
    private Button btnLogin;

    // ExecutorService để thực hiện tác vụ DB ở background
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    // Cấu hình kết nối MariaDB (điều chỉnh theo cấu hình của bạn)
    private static final String DB_URL = "jdbc:mysql://pvl.vn:3306/admin_db";
    private static final String DB_USER = "raspberry";
    private static final String DB_PASSWORD = "admin6789@";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Hiển thị giao diện đăng nhập (login_activity.xml)
        setContentView(R.layout.login_activity);
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                final String username = etUsername.getText().toString().trim();
                final String password = etPassword.getText().toString().trim();

                if (username.isEmpty() || password.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                } else {
                    login(username, password);
                }
            }
        });
    }

    // Hàm kiểm tra đăng nhập từ MariaDB
    private void login(final String username, final String password) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                Connection connection = null;
                PreparedStatement stmt = null;
                ResultSet rs = null;
                try {
                    // Load driver JDBC (với MySQL Connector/J; nếu dùng MariaDB Connector/J, hãy thay đổi driver tương ứng)
                    Class.forName("com.mysql.jdbc.Driver");
                    connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);

                    String sql = "SELECT * FROM quanlykho_users WHERE UserName = ? AND PassWord = ?";
                    stmt = connection.prepareStatement(sql);
                    stmt.setString(1, username);
                    stmt.setString(2, password);
                    rs = stmt.executeQuery();

                    if (rs.next()) {
                        // Nếu tài khoản tồn tại, kiểm tra nếu là admin hay user
                        if ("admin".equalsIgnoreCase(username)) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(LoginActivity.this, "Đăng nhập admin thành công", Toast.LENGTH_SHORT).show();
                                    // Chuyển sang AdminActivity
                                    Intent intent = new Intent(LoginActivity.this, AdminActivity.class);
                                    startActivity(intent);
                                    finish();
                                }
                            });
                        } else {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(LoginActivity.this, "Đăng nhập user thành công", Toast.LENGTH_SHORT).show();
                                    // Chuyển sang UserActivity
                                    Intent intent = new Intent(LoginActivity.this, UserActivity.class);
                                    startActivity(intent);
                                    finish();
                                }
                            });
                        }
                    } else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(LoginActivity.this, "Tên đăng nhập hoặc mật khẩu không đúng", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                } catch (final Exception e) {
                    Log.e("LoginError", "Lỗi kết nối DB", e);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(LoginActivity.this, "Lỗi kết nối DB: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                } finally {
                    try {
                        if (rs != null) rs.close();
                        if (stmt != null) stmt.close();
                        if (connection != null) connection.close();
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}
