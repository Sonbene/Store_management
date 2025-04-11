package com.example.storemanagement;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProductListActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;
    private TableLayout tableProducts;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    // DB configuration (sử dụng UTF-8)
    private static final String DB_URL = "jdbc:mysql://pvl.vn:3306/admin_db?useUnicode=true&characterEncoding=utf8";
    private static final String DB_USER = "raspberry";
    private static final String DB_PASSWORD = "admin6789@";

    // Thông tin admin (nếu cần truyền từ màn hình đăng nhập)
    private String adminName, adminEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_list);

        // Ánh xạ view và thiết lập Toolbar, Navigation Drawer
        toolbar = findViewById(R.id.toolbar_product);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);
        tableProducts = findViewById(R.id.tableProducts);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);

        // Lấy thông tin admin từ Intent extras nếu có
        Intent intent = getIntent();
        adminName = intent.getStringExtra("adminName");
        adminEmail = intent.getStringExtra("adminEmail");
        if (adminName == null) adminName = "Admin Name";
        if (adminEmail == null) adminEmail = "admin@example.com";

        // Cập nhật thông tin admin vào header của NavigationView
        View headerView = navigationView.getHeaderView(0);
        TextView tvAdminName = headerView.findViewById(R.id.tvAdminName);
        TextView tvAdminEmail = headerView.findViewById(R.id.tvAdminEmail);
        tvAdminName.setText(adminName);
        tvAdminEmail.setText(adminEmail);

        // Gọi phương thức truy vấn dữ liệu sản phẩm từ DB
        fetchProducts();
    }

    /**
     * Phương thức truy vấn dữ liệu sản phẩm từ bảng quanlykho_data và hiển thị lên TableLayout.
     */
    private void fetchProducts() {
        executorService.execute(() -> {
            Connection connection = null;
            PreparedStatement stmt = null;
            ResultSet rs = null;
            try {
                Class.forName("com.mysql.jdbc.Driver");
                connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                String sql = "SELECT * FROM quanlykho_data";
                stmt = connection.prepareStatement(sql);
                rs = stmt.executeQuery();

                // Cập nhật giao diện (UI thread)
                runOnUiThread(() -> {
                    tableProducts.removeAllViews();
                    TableRow headerRow = new TableRow(ProductListActivity.this);
                    headerRow.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
                    headerRow.addView(createHeaderCell("Mã kho"));
                    headerRow.addView(createHeaderCell("Mã vật tư"));
                    headerRow.addView(createHeaderCell("Tên vật tư"));
                    headerRow.addView(createHeaderCell("Đơn vị tính"));
                    headerRow.addView(createHeaderCell("Vị trí"));
                    headerRow.addView(createHeaderCell("SL nhập"));
                    headerRow.addView(createHeaderCell("Thời gian nhập"));
                    headerRow.addView(createHeaderCell("Giá tiền"));
                    tableProducts.addView(headerRow);
                });

                int count = 0;
                while (rs.next()) {
                    count++;
                    final String makho = rs.getString("MaKho");
                    final String maVatTu = rs.getString("MaVatTu");
                    final String tenVatTu = rs.getString("TenVatTu");
                    final String donViTinh = rs.getString("DonViTinh");
                    final String viTri = rs.getString("ViTri");
                    final int soLuongNhap = rs.getInt("SoLuongNhap");
                    final String thoiGianNhap = rs.getString("ThoiGianNhap");
                    final double giaTien = rs.getDouble("GiaTien");

                    runOnUiThread(() -> {
                        TableRow row = new TableRow(ProductListActivity.this);
                        row.setPadding(4, 4, 4, 4);
                        row.addView(createCellTextView(makho));
                        row.addView(createCellTextView(maVatTu));
                        row.addView(createCellTextView(tenVatTu));
                        row.addView(createCellTextView(donViTinh));
                        row.addView(createCellTextView(viTri));
                        row.addView(createCellTextView(String.valueOf(soLuongNhap)));
                        row.addView(createCellTextView(thoiGianNhap));
                        row.addView(createCellTextView(String.valueOf(giaTien)));
                        tableProducts.addView(row);
                    });
                }
                Log.d("FetchProducts", "Số dòng sản phẩm nhận được: " + count);
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(ProductListActivity.this, "Lỗi truy vấn sản phẩm", Toast.LENGTH_SHORT).show());
            } finally {
                try {
                    if (rs != null) rs.close();
                    if (stmt != null) stmt.close();
                    if (connection != null) connection.close();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    /**
     * Tạo ô header cho bảng.
     */
    private TextView createHeaderCell(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(14);
        tv.setTextColor(getResources().getColor(android.R.color.white));
        tv.setPadding(8, 8, 8, 8);
        tv.setGravity(Gravity.CENTER);
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        return tv;
    }

    /**
     * Tạo ô dữ liệu cho bảng.
     */
    private TextView createCellTextView(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(14);
        //tv.setTextColor(Color.BLACK);
        tv.setPadding(8, 8, 8, 8);
        tv.setGravity(Gravity.CENTER);
        return tv;
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Nếu cần, inflate thêm menu từ inventory_menu.xml
//        getMenuInflater().inflate(R.menu.inventory_menu, menu);
//        return true;
//    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // Xử lý các mục menu trong Toolbar, ví dụ: icon quét QR
        if (item.getItemId() == R.id.action_scan_qr) {
            Toast.makeText(this, "Quét QR", Toast.LENGTH_SHORT).show();
            // Logic mở màn hình quét QR (có thể dùng Activity mới hoặc dialog)
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Xử lý lựa chọn từ Navigation Drawer
        int id = item.getItemId();
        if (id == R.id.nav_admin_home) {
            Toast.makeText(this, "Home", Toast.LENGTH_SHORT).show();
            // Ví dụ: chuyển sang AdminActivity
            Intent intent = new Intent(ProductListActivity.this, AdminActivity.class);
            intent.putExtra("adminName", adminName);
            intent.putExtra("adminEmail", adminEmail);
            startActivity(intent);
        } else if (id == R.id.nav_inventory_management) {
            Toast.makeText(this, "Quản lý tồn kho", Toast.LENGTH_SHORT).show();

            // Xử lý chức năng quản lý tồn kho
        } else if (id == R.id.nav_in_out_stock) {
            Toast.makeText(this, "Nhập/Xuất kho", Toast.LENGTH_SHORT).show();
            // Ở đây có thể chuyển sang InventoryManagementActivity (ở trang này bạn đang ở)
            Toast.makeText(this, "Nhập/Xuất kho", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(ProductListActivity.this, InventoryManagementActivity.class);
            intent.putExtra("adminName", adminName);
            intent.putExtra("adminEmail", adminEmail);
            startActivity(intent);
        } else if (id == R.id.nav_manage_products) {
            Toast.makeText(this, "Quản lý sản phẩm", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(ProductListActivity.this, ProductListActivity.class);
            intent.putExtra("adminName", adminName);
            intent.putExtra("adminEmail", adminEmail);
            startActivity(intent);
            // Ở đây có thể chuyển sang trang khác nếu cần
        } else if (id == R.id.nav_reports) {
            Toast.makeText(this, "Báo cáo thống kê", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_user_management) {
            Toast.makeText(this, "Quản lý người dùng", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(ProductListActivity.this, UserManagementActivity.class);
            intent.putExtra("adminName", adminName);
            intent.putExtra("adminEmail", adminEmail);
            startActivity(intent);
        } else if (id == R.id.nav_user_logout) {
            Toast.makeText(this, "Đã đăng xuất", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(ProductListActivity.this, LoginActivity.class);
            startActivity(intent);
        }else if (id == R.id.nav_invoice) {
            Toast.makeText(this, "Hóa đơn bán hàng", Toast.LENGTH_SHORT).show();
            // Chuyển sang giao diện quản lý người dùng và truyền thông tin admin
            Intent intent = new Intent(ProductListActivity.this, BillPrintActivity.class);
            intent.putExtra("adminName", adminName);
            intent.putExtra("adminEmail", adminEmail);
            startActivity(intent);
        }
        drawerLayout.closeDrawers();
        return true;
    }
}
