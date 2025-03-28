package com.example.storemanagement;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.navigation.NavigationView;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UserManagementActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;
    private FrameLayout contentFrame;

    private ListView listViewAccounts;
    private Button btnAddAccount;
    private ArrayList<UserAccount> accountList = new ArrayList<>();
    private UserAccountAdapter adapter;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    // DB cấu hình
    private static final String DB_URL = "jdbc:mysql://pvl.vn:3306/admin_db";
    private static final String DB_USER = "raspberry";
    private static final String DB_PASSWORD = "admin6789@";

    private String adminName, adminEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_management_layout);

        // Thiết lập Toolbar và Navigation Drawer
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);
        contentFrame = findViewById(R.id.content_frame);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);

        // Lấy thông tin admin từ Intent extras (được truyền từ LoginActivity)
        Intent intent = getIntent();
        adminName = intent.getStringExtra("adminName");
        adminEmail = intent.getStringExtra("adminEmail");
        if (adminName == null) {
            adminName = "Admin Name";
        }
        if (adminEmail == null) {
            adminEmail = "admin@example.com";
        }

        View headerView = navigationView.getHeaderView(0);
        TextView tvAdminName = headerView.findViewById(R.id.tvAdminName);
        TextView tvAdminEmail = headerView.findViewById(R.id.tvAdminEmail);
        tvAdminName.setText(adminName);
        tvAdminEmail.setText(adminEmail);

        // Ánh xạ view cho danh sách tài khoản và nút thêm tài khoản
        listViewAccounts = findViewById(R.id.listViewAccounts);
        btnAddAccount = findViewById(R.id.btnAddAccount);
        // Set empty view nếu có (đảm bảo trong layout có tvEmpty)
        listViewAccounts.setEmptyView(findViewById(R.id.tvEmpty));

        adapter = new UserAccountAdapter(this, accountList);
        listViewAccounts.setAdapter(adapter);

        btnAddAccount.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                showAccountDialog(null);
            }
        });

        fetchAccounts();
    }

    // Xử lý lựa chọn mục từ Navigation Drawer
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            int id = item.getItemId();
            if (id == R.id.nav_admin_home) {
                Toast.makeText(this, "Home", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(UserManagementActivity.this, AdminActivity.class);
                intent.putExtra("adminName", adminName);
                intent.putExtra("adminEmail", adminEmail);
                startActivity(intent);
                // Xử lý chức năng quản lý sản phẩm
            } else if (id == R.id.nav_manage_products) {
                Toast.makeText(this, "Quản lý sản phẩm", Toast.LENGTH_SHORT).show();
                // Xử lý chức năng quản lý sản phẩm
            } else if (id == R.id.nav_inventory_management) {
                Toast.makeText(this, "Quản lý tồn kho", Toast.LENGTH_SHORT).show();
                // Xử lý chức năng quản lý tồn kho
            } else if (id == R.id.nav_in_out_stock) {
                Toast.makeText(this, "Nhập/Xuất kho", Toast.LENGTH_SHORT).show();
                // Xử lý chức năng nhập/xuất kho
            } else if (id == R.id.nav_reports) {
                Toast.makeText(this, "Báo cáo thống kê", Toast.LENGTH_SHORT).show();
                // Xử lý chức năng báo cáo thống kê
            } else if (id == R.id.nav_user_management) {
                Toast.makeText(this, "Quản lý người dùng", Toast.LENGTH_SHORT).show();
                // Chuyển sang giao diện quản lý người dùng và truyền thông tin admin
                Intent intent = new Intent(UserManagementActivity.this, UserManagementActivity.class);
                intent.putExtra("adminName", adminName);
                intent.putExtra("adminEmail", adminEmail);
                startActivity(intent);
            } else if (id == R.id.nav_user_logout) {
                Toast.makeText(this, "Đã đăng xuất", Toast.LENGTH_SHORT).show();
                // Chuyển sang giao diện quản lý người dùng và truyền thông tin admin
                Intent intent = new Intent(UserManagementActivity.this, LoginActivity.class);
                intent.putExtra("adminName", adminName);
                intent.putExtra("adminEmail", adminEmail);
                startActivity(intent);
            }
        drawerLayout.closeDrawers();
        return true;
    }

    // Lấy danh sách tài khoản từ DB
    public void fetchAccounts() {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                ArrayList<UserAccount> list = new ArrayList<>();
                Connection connection = null;
                PreparedStatement stmt = null;
                ResultSet rs = null;
                try {
                    Class.forName("com.mysql.jdbc.Driver");
                    connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                    String sql = "SELECT * FROM quanlykho_users";
                    stmt = connection.prepareStatement(sql);
                    rs = stmt.executeQuery();
                    int count = 0;
                    while(rs.next()){
                        count++;
                        String username = rs.getString("UserName");
                        String password = rs.getString("PassWord");
                        String phone = rs.getString("Phone");
                        String email = rs.getString("Email");
                        String idRole = rs.getString("IDRole");
                        list.add(new UserAccount(username, password, phone, email, idRole));
                    }
                    Log.d("FetchAccounts", "Số dòng nhận được: " + count);
                } catch(Exception e){
                    e.printStackTrace();
                } finally {
                    try{
                        if(rs != null) rs.close();
                        if(stmt != null) stmt.close();
                        if(connection != null) connection.close();
                    } catch(SQLException ex){}
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        accountList.clear();
                        accountList.addAll(list);
                        adapter.notifyDataSetChanged();
                    }
                });
            }
        });
    }

    // Hiển thị dialog để thêm mới hoặc chỉnh sửa tài khoản
    public void showAccountDialog(final UserAccount account) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(account == null ? "Thêm tài khoản mới" : "Chỉnh sửa tài khoản");

        View viewInflated = LayoutInflater.from(this).inflate(R.layout.dialog_account, null);
        final EditText inputUserName = viewInflated.findViewById(R.id.dialog_etUserName);
        final EditText inputPassWord = viewInflated.findViewById(R.id.dialog_etPassWord);
        final EditText inputPhone    = viewInflated.findViewById(R.id.dialog_etPhone);
        final EditText inputEmail    = viewInflated.findViewById(R.id.dialog_etEmail);
        final EditText inputIDRole   = viewInflated.findViewById(R.id.dialog_etIDRole);

        if(account != null) {
            inputUserName.setText(account.getUserName());
            inputPassWord.setText(account.getPassWord());
            inputPhone.setText(account.getPhone());
            inputEmail.setText(account.getEmail());
            inputIDRole.setText(account.getIDRole());
        }

        builder.setView(viewInflated);
        builder.setPositiveButton(account == null ? "Thêm" : "Cập nhật", new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String username = inputUserName.getText().toString().trim();
                String password = inputPassWord.getText().toString().trim();
                String phone = inputPhone.getText().toString().trim();
                String email = inputEmail.getText().toString().trim();
                String idRole = inputIDRole.getText().toString().trim();
                if(username.isEmpty() || password.isEmpty() || phone.isEmpty() || email.isEmpty() || idRole.isEmpty()){
                    Toast.makeText(UserManagementActivity.this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                    return;
                }
                if(account == null) {
                    addAccount(new UserAccount(username, password, phone, email, idRole));
                } else {
                    account.setUserName(username);
                    account.setPassWord(password);
                    account.setPhone(phone);
                    account.setEmail(email);
                    account.setIDRole(idRole);
                    updateAccount(account);
                }
            }
        });
        builder.setNegativeButton("Hủy", null);
        builder.show();
    }

    // Thêm tài khoản mới
    public void addAccount(final UserAccount account) {
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
                    stmt.setString(1, account.getUserName());
                    stmt.setString(2, account.getPassWord());
                    stmt.setString(3, account.getPhone());
                    stmt.setString(4, account.getEmail());
                    stmt.setString(5, account.getIDRole());
                    int rows = stmt.executeUpdate();
                    if(rows > 0){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(UserManagementActivity.this, "Thêm tài khoản thành công", Toast.LENGTH_SHORT).show();
                                fetchAccounts();
                            }
                        });
                    }
                } catch(Exception e) {
                    e.printStackTrace();
                } finally {
                    try{
                        if(stmt != null) stmt.close();
                        if(connection != null) connection.close();
                    } catch(SQLException ex){}
                }
            }
        });
    }

    // Cập nhật tài khoản (sử dụng UserName làm khóa)
    public void updateAccount(final UserAccount account) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                Connection connection = null;
                PreparedStatement stmt = null;
                try {
                    Class.forName("com.mysql.jdbc.Driver");
                    connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                    String sql = "UPDATE quanlykho_users SET PassWord = ?, Phone = ?, Email = ?, IDRole = ? WHERE UserName = ?";
                    stmt = connection.prepareStatement(sql);
                    stmt.setString(1, account.getPassWord());
                    stmt.setString(2, account.getPhone());
                    stmt.setString(3, account.getEmail());
                    stmt.setString(4, account.getIDRole());
                    stmt.setString(5, account.getUserName());
                    int rows = stmt.executeUpdate();
                    if(rows > 0){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(UserManagementActivity.this, "Cập nhật tài khoản thành công", Toast.LENGTH_SHORT).show();
                                fetchAccounts();
                            }
                        });
                    }
                } catch(Exception e){
                    e.printStackTrace();
                } finally {
                    try{
                        if(stmt != null) stmt.close();
                        if(connection != null) connection.close();
                    } catch(SQLException ex){}
                }
            }
        });
    }

    // Xóa tài khoản (sử dụng UserName làm khóa)
    public void deleteAccount(final String userName) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                Connection connection = null;
                PreparedStatement stmt = null;
                try {
                    Class.forName("com.mysql.jdbc.Driver");
                    connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                    String sql = "DELETE FROM quanlykho_users WHERE UserName = ?";
                    stmt = connection.prepareStatement(sql);
                    stmt.setString(1, userName);
                    int rows = stmt.executeUpdate();
                    if(rows > 0){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(UserManagementActivity.this, "Xóa tài khoản thành công", Toast.LENGTH_SHORT).show();
                                fetchAccounts();
                            }
                        });
                    }
                } catch(Exception e){
                    e.printStackTrace();
                } finally {
                    try{
                        if(stmt != null) stmt.close();
                        if(connection != null) connection.close();
                    } catch(SQLException ex){}
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
