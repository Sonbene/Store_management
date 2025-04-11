package com.example.storemanagement;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
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

public class InventoryManagementActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;

    // Các trường nhập liệu dạng AutoCompleteTextView
    private AutoCompleteTextView etID, etMakho, etMaVatTu, etTenVatTu, etDonViTinh, etViTri, etSoLuong, etGiaTien;
    private RadioGroup rgTransactionType;
    private RadioButton rbImport, rbExport;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    // DB configuration
    //private static final String DB_URL = "jdbc:mysql://pvl.vn:3306/admin_db";
    private static final String DB_URL = "jdbc:mysql://pvl.vn:3306/admin_db?useUnicode=true&characterEncoding=utf8";
    private static final String DB_USER = "raspberry";
    private static final String DB_PASSWORD = "admin6789@";
    private boolean autoFillState = false;

    private String adminName, adminEmail;
    private double basePrice = 0.0; // Giá cơ bản được lấy từ DB khi truy vấn theo ID

    private static final int REQUEST_CODE_QR_SCAN = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.inventory_management_layout);

        // Thiết lập Toolbar và Navigation Drawer
        toolbar = findViewById(R.id.toolbar_inventory);
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

        // Lấy thông tin admin từ Intent extras
        Intent intent = getIntent();
        adminName = intent.getStringExtra("adminName");
        adminEmail = intent.getStringExtra("adminEmail");
        if (adminName == null) adminName = "Admin Name";
        if (adminEmail == null) adminEmail = "admin@example.com";

        // Cập nhật header của NavigationView
        View headerView = navigationView.getHeaderView(0);
        ((android.widget.TextView) headerView.findViewById(R.id.tvAdminName)).setText(adminName);
        ((android.widget.TextView) headerView.findViewById(R.id.tvAdminEmail)).setText(adminEmail);

        // Ánh xạ các AutoCompleteTextView và RadioGroup từ layout
        etID = findViewById(R.id.etID);
        etMakho = findViewById(R.id.etMakho);
        etMaVatTu = findViewById(R.id.etMaVatTu);
        etTenVatTu = findViewById(R.id.etTenVatTu);
        etDonViTinh = findViewById(R.id.etDonViTinh);
        etViTri = findViewById(R.id.etViTri);
        etSoLuong = findViewById(R.id.etSoLuong);
        etGiaTien = findViewById(R.id.etGiaTien);

        rgTransactionType = findViewById(R.id.rgTransactionType);
        rbImport = findViewById(R.id.rbImport);
        rbExport = findViewById(R.id.rbExport);

        etID.setEnabled(false);
        etGiaTien.setEnabled(true);
        etMakho.setEnabled(true);
        etMaVatTu.setEnabled(true);
        etTenVatTu.setEnabled(true);
        etDonViTinh.setEnabled(true);
        etViTri.setEnabled(true);

        // Khi chọn loại giao dịch thay đổi
        rgTransactionType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbExport) {
                //etSoLuong.setHint("Số lượng xuất");
                etID.setEnabled(true);
                etGiaTien.setEnabled(false);
                etMakho.setEnabled(false);
                etMaVatTu.setEnabled(false);
                etTenVatTu.setEnabled(false);
                etDonViTinh.setEnabled(false);
                etViTri.setEnabled(false);
                autoFillState = true;
                clearAllFields();
            } else {
                //etSoLuong.setHint("Số lượng nhập");
                autoFillState = false;
                etID.setEnabled(false);
                etGiaTien.setEnabled(true);
                etMakho.setEnabled(true);
                etMaVatTu.setEnabled(true);
                etTenVatTu.setEnabled(true);
                etDonViTinh.setEnabled(true);
                etViTri.setEnabled(true);

                etID.setText("");
                clearAllFields();
            }
        });

        // Khi mất focus trên etID, truy vấn DB theo ID để lấy thông tin sản phẩm
        etID.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String idText = etID.getText().toString().trim();
                if (!idText.isEmpty()) {
                    if(autoFillState == true)
                    {
                        fetchProductInfoByID(idText);
                    }
                }
            }
        });

        // Thêm listener cho layout cha để clear focus khi bấm ra ngoài
        findViewById(R.id.parentLayout).setOnClickListener(v -> {
            // Nếu etID đang có focus, clear focus sẽ kích hoạt onFocusChangeListener
            if (etID.isFocused()) {
                etID.clearFocus();
            }
        });

        // Khi số lượng thay đổi, nếu đã truy vấn thành công thì tính tổng giá = số lượng * basePrice
        etSoLuong.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override
            public void afterTextChanged(Editable s) {
                String qtyStr = s.toString().trim();
                if (!qtyStr.isEmpty() && basePrice > 0) {
                    try {
                        double qty = Double.parseDouble(qtyStr);
                        double total = qty * basePrice;
                        etGiaTien.setText(String.valueOf(total));
                    } catch (NumberFormatException e) {
                        etGiaTien.setText("");
                    }
                }
            }
        });

        // Trong onCreate(), sau khi khởi tạo etID
        etID.setOnClickListener(v -> {
            if (rbExport.isChecked()) {
                // Khi chọn xuất, hiển thị danh sách ID gợi ý từ DB (không lọc ban đầu)
                fetchIdSuggestions("");
            } else {
                // Khi nhập hàng, tự động lấy ID lớn nhất và +1
                fetchMaxId();
            }
        });

// Nếu rbExport được chọn, khi người dùng nhập vào etID thì lọc danh sách ID
        etID.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override
            public void afterTextChanged(Editable s) {
                if (rbExport.isChecked()) {
                    String filter = s.toString().trim();
                    fetchIdSuggestions(filter);
                }
            }
        });


        // Thiết lập gợi ý cho các trường nhập liệu (truy vấn thực tế)
        fetchSuggestions("Makho", etMakho);
        fetchSuggestions("MaVatTu", etMaVatTu);
        fetchSuggestions("TenVatTu", etTenVatTu);
        fetchSuggestions("DonViTinh", etDonViTinh);
        fetchSuggestions("ViTri", etViTri);
        // Có thể thiết lập thêm cho SoLuong và GiaTien nếu cần

        // Thiết lập sự kiện cho nút xác nhận giao dịch
        findViewById(com.example.storemanagement.R.id.btnSubmit).setOnClickListener(v -> processTransaction());
    }

    private void clearAllFields() {
        etID.setText("");
        etGiaTien.setText("");
        etMakho.setText("");
        etMaVatTu.setText("");
        etTenVatTu.setText("");
        etDonViTinh.setText("");
        etViTri.setText("");
    }

    private void fetchIdSuggestions(final String filter) {
        executorService.execute(() -> {
            Connection connection = null;
            PreparedStatement stmt = null;
            ResultSet rs = null;
            try {
                Class.forName("com.mysql.jdbc.Driver");
                connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);

                // Nếu filter không rỗng, thêm điều kiện LIKE để lọc
                String sql = "SELECT ID FROM quanlykho_data";
                if (!filter.isEmpty()) {
                    sql += " WHERE ID LIKE ?";
                }
                stmt = connection.prepareStatement(sql);
                if (!filter.isEmpty()) {
                    stmt.setString(1, "%" + filter + "%");
                }
                rs = stmt.executeQuery();
                final ArrayList<String> suggestions = new ArrayList<>();
                while (rs.next()) {
                    String id = rs.getString("ID");
                    if (id != null && !suggestions.contains(id)) {
                        suggestions.add(id);
                    }
                }
                runOnUiThread(() -> {
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(InventoryManagementActivity.this,
                            android.R.layout.simple_dropdown_item_1line, suggestions);
                    etID.setAdapter(adapter);
                    etID.showDropDown();
                });
            } catch (Exception e) {
                e.printStackTrace();
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


    private void fetchMaxId() {
        executorService.execute(() -> {
            Connection connection = null;
            PreparedStatement stmt = null;
            ResultSet rs = null;
            try {
                Class.forName("com.mysql.jdbc.Driver");
                connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                // Giả sử cột ID có kiểu số, dùng CAST để đảm bảo so sánh số học
                String sql = "SELECT MAX(CAST(ID AS UNSIGNED)) AS maxID FROM quanlykho_data";
                stmt = connection.prepareStatement(sql);
                rs = stmt.executeQuery();
                int newId = 1;
                if (rs.next()) {
                    int maxId = rs.getInt("maxID");
                    newId = maxId + 1;
                }
                final int finalNewId = newId;
                runOnUiThread(() -> etID.setText(String.valueOf(finalNewId)));
            } catch (Exception e) {
                e.printStackTrace();
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
     * Xử lý giao dịch khi nút btnSubmit được bấm.
     * Nếu đang chọn xuất hàng, cập nhật giảm số lượng trên DB theo ID.
     * Nếu đang chọn nhập hàng, chèn một bản ghi mới vào DB.
     */
    private void processTransaction() {
        String idText = etID.getText().toString().trim();
        String makho = etMakho.getText().toString().trim();
        String maVatTu = etMaVatTu.getText().toString().trim();
        String tenVatTu = etTenVatTu.getText().toString().trim();
        String donViTinh = etDonViTinh.getText().toString().trim();
        String viTri = etViTri.getText().toString().trim();
        String qtyStr = etSoLuong.getText().toString().trim();
        String totalPriceStr = etGiaTien.getText().toString().trim();

        if (qtyStr.isEmpty() || totalPriceStr.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập số lượng", Toast.LENGTH_SHORT).show();
            return;
        }

        double qty;
        try {
            qty = Double.parseDouble(qtyStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Số lượng không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        // Kiểm tra giao dịch nhập hay xuất
        if (rbExport.isChecked()) {
            // Xuất hàng: cập nhật trừ số lượng trên DB theo ID
            if (idText.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập ID sản phẩm cần xuất", Toast.LENGTH_SHORT).show();
                return;
            }
            processExport(idText, qty);
        } else {
            // Nhập hàng: chèn một bản ghi mới vào DB
            // Lấy thời gian hiện tại (ở DB sẽ dùng NOW())
            // Giá tiền: nếu etGiaTien hiển thị total, thì đơn giá = total / qty (nếu qty > 0)
            double unitPrice = basePrice;
            try {
                double total = Double.parseDouble(totalPriceStr);
                if (qty > 0) {
                    unitPrice = total;
                }
            } catch (NumberFormatException e) {
                // Giữ unitPrice là basePrice
            }
            processImport(makho, maVatTu, tenVatTu, donViTinh, viTri, qty, unitPrice);
        }
    }

    /**
     * Xử lý xuất hàng: trừ số lượng sản phẩm trên DB theo ID.
     */
    private void processExport(final String idValue, final double qtyToExport) {
        executorService.execute(() -> {
            Connection connection = null;
            PreparedStatement stmt = null;
            try {
                Class.forName("com.mysql.jdbc.Driver");
                connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                // Giả sử bảng quanlykho_data có cột SoLuongNhap và ID là khóa chính.
                // Cập nhật giảm số lượng: SoLuongNhap = SoLuongNhap - qtyToExport, nếu kết quả >= 0
                String sql = "UPDATE quanlykho_data SET SoLuongNhap = SoLuongNhap - ? WHERE ID = ? AND SoLuongNhap >= ?";
                stmt = connection.prepareStatement(sql);
                stmt.setDouble(1, qtyToExport);
                stmt.setString(2, idValue);
                stmt.setDouble(3, qtyToExport);
                int rows = stmt.executeUpdate();
                runOnUiThread(() -> {
                    if (rows > 0) {
                        Toast.makeText(InventoryManagementActivity.this, "Xuất hàng thành công", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(InventoryManagementActivity.this, "Xuất hàng thất bại: Kiểm tra số lượng hoặc ID", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(InventoryManagementActivity.this, "Lỗi xuất hàng", Toast.LENGTH_SHORT).show());
            } finally {
                try {
                    if (stmt != null) stmt.close();
                    if (connection != null) connection.close();
                } catch (SQLException ex) { ex.printStackTrace(); }
            }
        });
    }

    /**
     * Xử lý nhập hàng: chèn một bản ghi mới vào DB với các thông tin nhập liệu.
     * ID tự tăng, ThoiGianNhap được lấy bằng NOW() trong SQL.
     */
    private void processImport(final String makho, final String maVatTu, final String tenVatTu,
                               final String donViTinh, final String viTri, final double qty, final double unitPrice) {
        executorService.execute(() -> {
            Connection connection = null;
            PreparedStatement stmt = null;
            try {
                Class.forName("com.mysql.jdbc.Driver");
                connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);

                // Đảm bảo phiên làm việc sử dụng mã hóa utf8mb4
                connection.createStatement().execute("SET NAMES 'utf8mb4'");

                String sql = "INSERT INTO quanlykho_data (MaKho, MaVatTu, TenVatTu, DonViTinh, ViTri, SoLuongNhap, ThoiGianNhap, GiaTien) " +
                        "VALUES (?, ?, ?, ?, ?, ?, NOW(), ?)";
                stmt = connection.prepareStatement(sql);
                stmt.setString(1, makho);
                stmt.setString(2, maVatTu);
                stmt.setString(3, tenVatTu);
                stmt.setString(4, donViTinh);
                stmt.setString(5, viTri);
                stmt.setDouble(6, qty);
                stmt.setDouble(7, unitPrice);
                int rows = stmt.executeUpdate();
                runOnUiThread(() -> {
                    if (rows > 0) {
                        Toast.makeText(InventoryManagementActivity.this, "Nhập hàng thành công", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(InventoryManagementActivity.this, "Nhập hàng thất bại", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(InventoryManagementActivity.this, "Lỗi nhập hàng", Toast.LENGTH_SHORT).show());
            } finally {
                try {
                    if (stmt != null) stmt.close();
                    if (connection != null) connection.close();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    /**
     * Phương thức truy vấn DB để lấy thông tin sản phẩm theo ID và cập nhật các trường nhập liệu tương ứng.
     */
    private void fetchProductInfoByID(final String idValue) {
        executorService.execute(() -> {
            Connection connection = null;
            PreparedStatement stmt = null;
            ResultSet rs = null;
            try {
                Class.forName("com.mysql.jdbc.Driver");
                connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                String sql = "SELECT Makho, MaVatTu, TenVatTu, DonViTinh, ViTri, GiaTien FROM quanlykho_data WHERE ID = ?";
                stmt = connection.prepareStatement(sql);
                stmt.setString(1, idValue);
                rs = stmt.executeQuery();
                if (rs.next()) {
                    final String makho = rs.getString("Makho");
                    final String maVatTu = rs.getString("MaVatTu");
                    final String tenVatTu = rs.getString("TenVatTu");
                    final String donViTinh = rs.getString("DonViTinh");
                    final String viTri = rs.getString("ViTri");
                    final double giaTien = rs.getDouble("GiaTien");
                    basePrice = giaTien;
                    runOnUiThread(() -> {
                        etMakho.setText(makho);
                        etMaVatTu.setText(maVatTu);
                        etTenVatTu.setText(tenVatTu);
                        etDonViTinh.setText(donViTinh);
                        etViTri.setText(viTri);
                        etGiaTien.setText(String.valueOf(giaTien));
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(InventoryManagementActivity.this,
                            "Không tìm thấy sản phẩm với ID: " + idValue, Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(InventoryManagementActivity.this,
                        "Lỗi truy vấn sản phẩm theo ID", Toast.LENGTH_SHORT).show());
            } finally {
                try {
                    if (rs != null) rs.close();
                    if (stmt != null) stmt.close();
                    if (connection != null) connection.close();
                } catch (SQLException ex) { ex.printStackTrace(); }
            }
        });
    }

    /**
     * Phương thức truy vấn DB để lấy danh sách gợi ý cho cột được chỉ định và cập nhật cho AutoCompleteTextView.
     */
    private void fetchSuggestions(final String column, final AutoCompleteTextView view) {
        executorService.execute(() -> {
            Connection connection = null;
            PreparedStatement stmt = null;
            ResultSet rs = null;
            try {
                Class.forName("com.mysql.jdbc.Driver");
                connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                String sql = "SELECT DISTINCT " + column + " FROM quanlykho_data";
                stmt = connection.prepareStatement(sql);
                rs = stmt.executeQuery();

                final ArrayList<String> suggestions = new ArrayList<>();
                while (rs.next()) {
                    String value = rs.getString(column);
                    if (value != null && !suggestions.contains(value)) {
                        suggestions.add(value);
                    }
                }
                final ArrayAdapter<String> adapter = new ArrayAdapter<>(InventoryManagementActivity.this,
                        android.R.layout.simple_dropdown_item_1line, suggestions);
                runOnUiThread(() -> {
                    view.setAdapter(adapter);
                    view.setThreshold(1);
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(InventoryManagementActivity.this,
                        "Lỗi truy vấn gợi ý cho " + column, Toast.LENGTH_SHORT).show());
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.inventory_menu, menu);
        return true;
    }


    private ActivityResultLauncher<Intent> qrScanLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    String qrResult = result.getData().getStringExtra("QR_RESULT");
                    etMaVatTu.setText(qrResult);

                    //Toast.makeText(InventoryManagementActivity.this, "Kết quả QR: " + qrResult, Toast.LENGTH_LONG).show();

                    //Xử lý kết quả quét QR
                    //etID.setText(qrResult);
                    //fetchProductInfoByID(qrResult);
                }
            }
    );

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_scan_qr) {
            Intent intent = new Intent(this, QRScanActivity.class);
            qrScanLauncher.launch(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_admin_home) {
            Toast.makeText(this, "Home", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(InventoryManagementActivity.this, AdminActivity.class);
            intent.putExtra("adminName", adminName);
            intent.putExtra("adminEmail", adminEmail);
            startActivity(intent);
        } else if (id == R.id.nav_manage_products) {
            Toast.makeText(this, "Quản lý sản phẩm", Toast.LENGTH_SHORT).show();
            Toast.makeText(this, "Quản lý sản phẩm", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(InventoryManagementActivity.this, ProductListActivity.class);
            intent.putExtra("adminName", adminName);
            intent.putExtra("adminEmail", adminEmail);
            startActivity(intent);
        } else if (id == R.id.nav_inventory_management) {
            Toast.makeText(this, "Quản lý tồn kho", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_in_out_stock) {
            Toast.makeText(this, "Nhập/Xuất kho", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(InventoryManagementActivity.this, InventoryManagementActivity.class);
            intent.putExtra("adminName", adminName);
            intent.putExtra("adminEmail", adminEmail);
            startActivity(intent);
        } else if (id == R.id.nav_reports) {
            Toast.makeText(this, "Báo cáo thống kê", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_user_management) {
            Toast.makeText(this, "Quản lý người dùng", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(InventoryManagementActivity.this, UserManagementActivity.class);
            intent.putExtra("adminName", adminName);
            intent.putExtra("adminEmail", adminEmail);
            startActivity(intent);
        } else if (id == R.id.nav_user_logout) {
            Toast.makeText(this, "Đã đăng xuất", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(InventoryManagementActivity.this, LoginActivity.class);
            startActivity(intent);
        }else if (id == R.id.nav_invoice) {
            Toast.makeText(this, "Hóa đơn bán hàng", Toast.LENGTH_SHORT).show();
            // Chuyển sang giao diện quản lý người dùng và truyền thông tin admin
            Intent intent = new Intent(InventoryManagementActivity.this, BillPrintActivity.class);
            intent.putExtra("adminName", adminName);
            intent.putExtra("adminEmail", adminEmail);
            startActivity(intent);
        }
        drawerLayout.closeDrawers();
        return true;
    }
}
