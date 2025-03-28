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
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AdminActivity extends AppCompatActivity {
    private EditText etUserName, etPassWord, etPhone, etEmail, etIDRole;
    private Button btnPushData;

    // ExecutorService để thực hiện tác vụ DB ở background
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    // Cấu hình kết nối MariaDB (điều chỉnh theo cấu hình của bạn)
    private static final String DB_URL = "jdbc:mysql://pvl.vn:3306/admin_db";
    private static final String DB_USER = "raspberry";
    private static final String DB_PASSWORD = "admin6789@";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Hiển thị giao diện admin (admin_layout.xml)
        setContentView(R.layout.admin_layout);
        etUserName = findViewById(R.id.etUserName);
        etPassWord = findViewById(R.id.etPassWord);
        etPhone    = findViewById(R.id.etPhone);
        etEmail    = findViewById(R.id.etEmail);
        etIDRole   = findViewById(R.id.etIDRole);
        btnPushData = findViewById(R.id.btnPushData);

        btnPushData.setOnClickListener(new View.OnClickListener(){
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

        if (userName.isEmpty() || passWord.isEmpty() || phone.isEmpty() || email.isEmpty() || idRole.isEmpty()) {
            Toast.makeText(AdminActivity.this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
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
                                Toast.makeText(AdminActivity.this, "Dữ liệu đã được đẩy lên thành công!", Toast.LENGTH_SHORT).show();
                                clearFields();
                            } else {
                                Toast.makeText(AdminActivity.this, "Đẩy dữ liệu thất bại!", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } catch (final Exception e) {
                    Log.e("PushDataError", "Lỗi đẩy dữ liệu", e);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(AdminActivity.this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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

    private void clearFields() {
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
