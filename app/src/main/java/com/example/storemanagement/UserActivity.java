package com.example.storemanagement;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.navigation.NavigationView;

public class UserActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;
    private TextView tvUserWelcome;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Sử dụng layout user_layout.xml có cấu trúc DrawerLayout, Toolbar, NavigationView...
        setContentView(R.layout.user_layout);

        // Ánh xạ các thành phần giao diện
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Thiết lập ActionBarDrawerToggle để hiển thị icon điều hướng
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Cập nhật lời chào cho user (có thể truyền thông tin từ Intent)
        tvUserWelcome = findViewById(R.id.tvUserWelcome);
        Intent intent = getIntent();
        String userName = intent.getStringExtra("userName");
        if (userName == null || userName.isEmpty()) {
            userName = "User";
        }
        tvUserWelcome.setText("Chào mừng " + userName + "!");
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Xử lý sự kiện khi chọn các mục trong Navigation Drawer
        int id = item.getItemId();
//        switch (id) {
//            case R.id.nav_user_home:
//                Toast.makeText(this, "Trang chủ", Toast.LENGTH_SHORT).show();
//                break;
//            case R.id.nav_user_orders:
//                Toast.makeText(this, "Đơn hàng của bạn", Toast.LENGTH_SHORT).show();
//                // Ví dụ: mở activity quản lý đơn hàng của user
//                // startActivity(new Intent(this, UserOrdersActivity.class));
//                break;
//            case R.id.nav_user_products:
//                Toast.makeText(this, "Sản phẩm của cửa hàng", Toast.LENGTH_SHORT).show();
//                // Ví dụ: mở activity danh sách sản phẩm
//                // startActivity(new Intent(this, ProductListActivity.class));
//                break;
//            case R.id.nav_user_logout:
//                Toast.makeText(this, "Đăng xuất", Toast.LENGTH_SHORT).show();
//                // Xử lý đăng xuất, ví dụ:
//                // startActivity(new Intent(this, LoginActivity.class));
//                finish();
//                break;
//            default:
//                Toast.makeText(this, "Mục chưa được định nghĩa", Toast.LENGTH_SHORT).show();
//                break;
//        }
        drawerLayout.closeDrawers();
        return true;
    }
}
