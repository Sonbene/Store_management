package com.example.storemanagement;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.navigation.NavigationView;

public class AdminActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;
    private String adminName, adminEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_layout);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
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

        // Lấy header view của NavigationView và cập nhật thông tin admin
        View headerView = navigationView.getHeaderView(0);
        TextView tvAdminName = headerView.findViewById(R.id.tvAdminName);
        TextView tvAdminEmail = headerView.findViewById(R.id.tvAdminEmail);
        tvAdminName.setText(adminName);
        tvAdminEmail.setText(adminEmail);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_admin_home) {
            Toast.makeText(this, "Home", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(AdminActivity.this, AdminActivity.class);
            intent.putExtra("adminName", adminName);
            intent.putExtra("adminEmail", adminEmail);
            startActivity(intent);
            // Xử lý chức năng quản lý sản phẩm
        } else if (id == R.id.nav_manage_products) {
            Toast.makeText(this, "Quản lý sản phẩm", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(AdminActivity.this, ProductListActivity.class);
            intent.putExtra("adminName", adminName);
            intent.putExtra("adminEmail", adminEmail);
            startActivity(intent);
            // Xử lý chức năng quản lý sản phẩm
        } else if (id == R.id.nav_inventory_management) {
            Toast.makeText(this, "Quản lý tồn kho", Toast.LENGTH_SHORT).show();

            // Xử lý chức năng quản lý tồn kho
        } else if (id == R.id.nav_in_out_stock) {
            Toast.makeText(this, "Nhập/Xuất kho", Toast.LENGTH_SHORT).show();
            // Xử lý chức năng nhập/xuất kho
            Intent intent = new Intent(AdminActivity.this, InventoryManagementActivity.class);
            intent.putExtra("adminName", adminName);
            intent.putExtra("adminEmail", adminEmail);
            startActivity(intent);
        } else if (id == R.id.nav_reports) {
            Toast.makeText(this, "Báo cáo thống kê", Toast.LENGTH_SHORT).show();
            // Xử lý chức năng báo cáo thống kê
        } else if (id == R.id.nav_user_management) {
            Toast.makeText(this, "Quản lý người dùng", Toast.LENGTH_SHORT).show();
            // Chuyển sang giao diện quản lý người dùng và truyền thông tin admin
            Intent intent = new Intent(AdminActivity.this, UserManagementActivity.class);
            intent.putExtra("adminName", adminName);
            intent.putExtra("adminEmail", adminEmail);
            startActivity(intent);
        } else if (id == R.id.nav_user_logout) {
            Toast.makeText(this, "Đã đăng xuất", Toast.LENGTH_SHORT).show();
            // Chuyển sang giao diện quản lý người dùng và truyền thông tin admin
            Intent intent = new Intent(AdminActivity.this, LoginActivity.class);
            intent.putExtra("adminName", adminName);
            intent.putExtra("adminEmail", adminEmail);
            startActivity(intent);
        }
        drawerLayout.closeDrawers();
        return true;
    }
}
