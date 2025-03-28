package com.example.storemanagement;

import androidx.appcompat.app.AppCompatActivity;
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

public class MainActivity extends AppCompatActivity {
    // --- Các biến cho giao diện đăng nhập ---
    private EditText etLoginUsername, etLoginPassword;
    private Button btnLogin;

    // --- Các biến cho giao diện Admin ---
    private EditText etUserName, etPassWord, etPhone, etEmail, etIDRole;
    private Button btnPushData;

    // ExecutorService để thực hiện các tác vụ DB ở background
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    // Cấu hình kết nối MariaDB (điều chỉnh theo cấu hình của bạn)
    private static final String DB_URL = "jdbc:mysql://pvl.vn:3306/admin_db";
    private static final String DB_USER = "raspberry";
    private static final String DB_PASSWORD = "admin6789@";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Ban đầu hiển thị giao diện đăng nhập (login_activity.xml)
        setContentView(R.layout.login_activity);
        initLoginViews();
    }

    // Ánh xạ view cho giao diện đăng nhập
    private void initLoginViews() {
        etLoginUsername = findViewById(R.id.etUsername);
        etLoginPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                final String username = etLoginUsername.getText().toString().trim();
                final String password = etLoginPassword.getText().toString().trim();

                if(username.isEmpty() || password.isEmpty()){
                    Toast.makeText(MainActivity.this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
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
                    // Load driver JDBC (có thể thay đổi thành "org.mariadb.jdbc.Driver" nếu dùng MariaDB Connector/J)
                    Class.forName("com.mysql.jdbc.Driver");
                    connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                    String sql = "SELECT * FROM quanlykho_users WHERE UserName = ? AND PassWord = ?";
                    stmt = connection.prepareStatement(sql);
                    stmt.setString(1, username);
                    stmt.setString(2, password);
                    rs = stmt.executeQuery();

                    if (rs.next()) {
                        // Nếu tài khoản tồn tại, kiểm tra nếu là admin
                        if ("admin".equalsIgnoreCase(username)) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(MainActivity.this, "Đăng nhập admin thành công", Toast.LENGTH_SHORT).show();
                                    switchToAdminLayout();
                                }
                            });
                        } else {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(MainActivity.this, "Tài khoản không phải admin", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    } else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, "Tên đăng nhập hoặc mật khẩu không đúng", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                } catch (final Exception e) {
                    Log.e("LoginError", "Lỗi kết nối DB", e);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "Lỗi kết nối DB: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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

    // Chuyển giao diện sang admin_layout và ánh xạ các view của admin
    private void switchToAdminLayout() {
        setContentView(R.layout.admin_layout);
        initAdminViews();
    }

    // Ánh xạ view cho giao diện Admin
    private void initAdminViews() {
        etUserName = findViewById(R.id.etUserName);
        etPassWord = findViewById(R.id.etPassWord);
        etPhone    = findViewById(R.id.etPhone);
        etEmail    = findViewById(R.id.etEmail);
        etIDRole   = findViewById(R.id.etIDRole);
        btnPushData = findViewById(R.id.btnPushData);

        btnPushData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pushDataToDB();
            }
        });
    }

    // Hàm đẩy dữ liệu từ giao diện admin lên MariaDB
    private void pushDataToDB() {
        final String userName = etUserName.getText().toString().trim();
        final String passWord = etPassWord.getText().toString().trim();
        final String phone = etPhone.getText().toString().trim();
        final String email = etEmail.getText().toString().trim();
        final String idRole = etIDRole.getText().toString().trim();

        if(userName.isEmpty() || passWord.isEmpty() || phone.isEmpty() || email.isEmpty() || idRole.isEmpty()){
            Toast.makeText(MainActivity.this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        executorService.execute(new Runnable() {
            @Override
            public void run() {
                Connection connection = null;
                PreparedStatement stmt = null;
                try {
                    Class.forName("com.mysql.jdbc.Driver");
                    connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                    String sql = "INSERT INTO quanlykho_users (UserName, PassWord, Phone, Email, IDRole) VALUES (?, ?, ?, ?, ?)";
                    stmt = connection.prepareStatement(sql);
                    stmt.setString(1, userName);
                    stmt.setString(2, passWord);
                    stmt.setString(3, phone);
                    stmt.setString(4, email);
                    stmt.setString(5, idRole);

                    final int rowsInserted = stmt.executeUpdate();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (rowsInserted > 0) {
                                Toast.makeText(MainActivity.this, "Dữ liệu đã được đẩy lên thành công!", Toast.LENGTH_SHORT).show();
                                clearAdminFields();
                            } else {
                                Toast.makeText(MainActivity.this, "Đẩy dữ liệu thất bại!", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } catch (final Exception e) {
                    Log.e("PushDataError", "Lỗi đẩy dữ liệu", e);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                } finally {
                    try {
                        if (stmt != null) stmt.close();
                        if (connection != null) connection.close();
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });
    }

    private void clearAdminFields() {
        etUserName.setText("");
        etPassWord.setText("");
        etPhone.setText("");
        etEmail.setText("");
        etIDRole.setText("");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}
