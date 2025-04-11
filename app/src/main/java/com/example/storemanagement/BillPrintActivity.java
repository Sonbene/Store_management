package com.example.storemanagement;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class BillPrintActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_SCAN_BARCODE = 101;
    // Thông tin kết nối MariaDB lấy từ InventoryManagementActivity.java
    private static final String DB_URL = "jdbc:mysql://pvl.vn:3306/admin_db?useUnicode=true&characterEncoding=utf8";
    private static final String DB_USER = "raspberry";
    private static final String DB_PASSWORD = "admin6789@";

    private TextView tvBillTitle, tvBillNumber, tvDate, tvTotalAmount;
    private Button btnPrint;
    private TableLayout productTable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bill_print_layout);

        // Ánh xạ các View
        tvBillTitle = findViewById(R.id.tvBillTitle);
        tvBillNumber = findViewById(R.id.tvBillNumber);
        tvDate = findViewById(R.id.tvDate);
        tvTotalAmount = findViewById(R.id.tvTotalAmount);
        productTable = findViewById(R.id.productTable);
        btnPrint = findViewById(R.id.btnPrint);

        // Gán sự kiện cho nút in hóa đơn
        btnPrint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                printBill();
            }
        });

        // Cập nhật dữ liệu mặc định của hóa đơn theo ngày hôm nay.
        Date now = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        SimpleDateFormat billNoFormat = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault());
        String currentDate = dateFormat.format(now);
        String billNo = "Bill No: " + billNoFormat.format(now);
        tvBillNumber.setText(billNo);
        tvDate.setText("Ngày: " + currentDate);

        // Nếu có sản phẩm ban đầu, bạn có thể thêm dòng sản phẩm vào hóa đơn
        // Ví dụ: addProductRow("VT001", "Sản phẩm A", 10, 100000);
    }

    // Hàm thêm dòng sản phẩm vào bảng (TableLayout)
    private void addProductRow(String maVatTu, String tenVatTu, int soLuong, int donGia) {
        TableRow row = new TableRow(this);

        TextView tvMaVatTu = new TextView(this);
        tvMaVatTu.setText(maVatTu);
        tvMaVatTu.setPadding(4, 4, 4, 4);

        TextView tvTenVatTu = new TextView(this);
        tvTenVatTu.setText(tenVatTu);
        tvTenVatTu.setPadding(4, 4, 4, 4);

        TextView tvSoLuong = new TextView(this);
        tvSoLuong.setText(String.valueOf(soLuong));
        tvSoLuong.setPadding(4, 4, 4, 4);

        TextView tvDonGia = new TextView(this);
        tvDonGia.setText(String.valueOf(donGia));
        tvDonGia.setPadding(4, 4, 4, 4);

        TextView tvThanhTien = new TextView(this);
        int thanhTien = soLuong * donGia;
        tvThanhTien.setText(String.valueOf(thanhTien));
        tvThanhTien.setPadding(4, 4, 4, 4);

        row.addView(tvMaVatTu);
        row.addView(tvTenVatTu);
        row.addView(tvSoLuong);
        row.addView(tvDonGia);
        row.addView(tvThanhTien);

        productTable.addView(row);
    }

    // Khi bấm nút Quét Barcode, chuyển sang InvoiceActivity
    public void onScanBarcode(View view) {
        Intent intent = new Intent(this, InvoiceActivity.class);
        startActivityForResult(intent, REQUEST_CODE_SCAN_BARCODE);
    }

    // Nhận kết quả từ InvoiceActivity sau khi quét barcode
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_SCAN_BARCODE && resultCode == RESULT_OK && data != null) {
            final String scannedProductCode = data.getStringExtra("QR_RESULT");
            // Tra cứu sản phẩm từ MariaDB theo mã vật tư quét được (chạy ở background thread)
            new Thread(() -> {
                final Product product = lookupProduct(scannedProductCode);
                runOnUiThread(() -> {
                    if (product == null) {
                        Toast.makeText(BillPrintActivity.this, "Không tìm thấy sản phẩm với mã " + scannedProductCode, Toast.LENGTH_SHORT).show();
                    } else {
                        // Nếu tìm thấy, cập nhật hóa đơn
                        updateInvoiceWithProduct(product);
                    }
                });
            }).start();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    // Phương thức cập nhật hóa đơn với thông tin sản phẩm tra cứu được
    private void updateInvoiceWithProduct(Product product) {
        boolean found = false;
        int rowCount = productTable.getChildCount();
        // Giả sử dòng đầu tiên của TableLayout là tiêu đề nên duyệt từ index 1
        for (int i = 1; i < rowCount; i++) {
            TableRow row = (TableRow) productTable.getChildAt(i);
            TextView tvCode = (TextView) row.getChildAt(0);
            if (tvCode.getText().toString().equals(product.code)) {
                found = true;
                TextView tvQuantity = (TextView) row.getChildAt(2);
                int quantity = Integer.parseInt(tvQuantity.getText().toString());
                quantity++;
                tvQuantity.setText(String.valueOf(quantity));

                // Cập nhật "Thành tiền" dựa trên đơn giá và số lượng
                TextView tvThanhTien = (TextView) row.getChildAt(4);
                int newThanhTien = quantity * product.unitPrice;
                tvThanhTien.setText(String.valueOf(newThanhTien));
                break;
            }
        }
        if (!found) {
            // Nếu sản phẩm chưa có trong hóa đơn, thêm dòng mới
            addProductRow(product.code, product.name, 1, product.unitPrice);
        }
        recalcTotalAmount();
    }

    // Hàm tính lại tổng số tiền hóa đơn dựa trên cột "Thành tiền" của từng dòng sản phẩm
    private void recalcTotalAmount() {
        int total = 0;
        int rowCount = productTable.getChildCount();
        for (int i = 1; i < rowCount; i++) {
            TableRow row = (TableRow) productTable.getChildAt(i);
            TextView tvThanhTien = (TextView) row.getChildAt(4);
            total += Integer.parseInt(tvThanhTien.getText().toString());
        }
        tvTotalAmount.setText("Tổng cộng: " + total);
    }

    // Hàm tra cứu sản phẩm trong MariaDB dựa trên mã vật tư
    private Product lookupProduct(String productCode) {
        Product product = null;
        Connection connection = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            // Khởi tạo driver
            Class.forName("com.mysql.jdbc.Driver");
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            // Giả sử thông tin sản phẩm cần lấy gồm: MaVatTu, TenVatTu, GiaTien
            String sql = "SELECT MaVatTu, TenVatTu, GiaTien FROM quanlykho_data WHERE MaVatTu = ?";
            stmt = connection.prepareStatement(sql);
            stmt.setString(1, productCode);
            rs = stmt.executeQuery();
            if (rs.next()) {
                String code = rs.getString("MaVatTu");
                String name = rs.getString("TenVatTu");
                int unitPrice = rs.getInt("GiaTien");
                product = new Product(code, name, unitPrice);
            }
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
        return product;
    }

    // Lớp mô phỏng thông tin sản phẩm
    private static class Product {
        String code;
        String name;
        int unitPrice;

        Product(String code, String name, int unitPrice) {
            this.code = code;
            this.name = name;
            this.unitPrice = unitPrice;
        }
    }

    // Phương thức in hóa đơn: ẩn các thành phần không cần thiết (như nút bấm) trước khi in
    private void printBill() {
        // Lấy view chứa hóa đơn (toàn bộ ScrollView)
        final View content = findViewById(R.id.billPrintContainer);
        // Các view cần ẩn: phần quét barcode và nút in
        final View barcodeSection = findViewById(R.id.barcodeScanningSection);
        final View btnPrintView = findViewById(R.id.btnPrint);

        // Lưu trạng thái ban đầu
        final int oldBarcodeVisibility = barcodeSection.getVisibility();
        final int oldPrintVisibility = btnPrintView.getVisibility();

        // Ẩn các view không cần in
        barcodeSection.setVisibility(View.GONE);
        btnPrintView.setVisibility(View.GONE);

        // Yêu cầu layout cập nhật và chờ 300ms (có thể điều chỉnh)
        content.postDelayed(new Runnable() {
            @Override
            public void run() {
                PrintManager printManager = (PrintManager) getSystemService(Context.PRINT_SERVICE);
                PrintDocumentAdapter printAdapter = new ViewPrintAdapter(BillPrintActivity.this, content);
                String jobName = getString(R.string.app_name) + " Hóa Đơn";
                printManager.print(jobName, printAdapter, new PrintAttributes.Builder().build());

                // Khôi phục lại các view sau khi in (nếu có thể đảm bảo sau khi snapshot được lấy)
                barcodeSection.setVisibility(oldBarcodeVisibility);
                btnPrintView.setVisibility(oldPrintVisibility);
            }
        }, 300);  // delay 300ms
    }

}
