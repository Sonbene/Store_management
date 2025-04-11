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
    // Thông tin kết nối MariaDB
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

        // Cập nhật dữ liệu mặc định của hóa đơn theo ngày hôm nay:
        // Số hóa đơn tạo theo thời gian hiện tại để đảm bảo tính duy nhất.
        Date now = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        SimpleDateFormat billNoFormat = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault());
        String currentDate = dateFormat.format(now);
        String billNo = "Bill No: " + billNoFormat.format(now);
        tvBillNumber.setText(billNo);
        tvDate.setText("Ngày: " + currentDate);

        // Nếu có sản phẩm ban đầu, bạn có thể thêm dòng sản phẩm vào hóa đơn (ví dụ cách dưới đây).
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

    // Khi bấm nút Quét Barcode, chuyển sang InvoiceActivity để quét
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
            new Thread(new Runnable() {
                @Override
                public void run() {
                    final Product product = lookupProduct(scannedProductCode);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (product == null) {
                                Toast.makeText(BillPrintActivity.this, "Không tìm thấy sản phẩm với mã " + scannedProductCode, Toast.LENGTH_SHORT).show();
                            } else {
                                updateInvoiceWithProduct(product);
                            }
                        }
                    });
                }
            }).start();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    // Cập nhật hóa đơn với thông tin sản phẩm tra cứu được
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
                int invoiceQuantity = Integer.parseInt(tvQuantity.getText().toString());
                // Nếu số lượng trên hóa đơn đã bằng số lượng tồn kho, không cho thêm nữa
                if (invoiceQuantity >= product.availableQuantity) {
                    Toast.makeText(BillPrintActivity.this, "Sản phẩm " + product.code + " đã đạt giới hạn tồn kho (" + product.availableQuantity + ")", Toast.LENGTH_SHORT).show();
                } else {
                    invoiceQuantity++;
                    tvQuantity.setText(String.valueOf(invoiceQuantity));
                    TextView tvThanhTien = (TextView) row.getChildAt(4);
                    int newThanhTien = invoiceQuantity * product.unitPrice;
                    tvThanhTien.setText(String.valueOf(newThanhTien));
                }
                break;
            }
        }
        if (!found) {
            // Nếu sản phẩm chưa có, chỉ thêm nếu còn hàng
            if (product.availableQuantity > 0) {
                addProductRow(product.code, product.name, 1, product.unitPrice);
            } else {
                Toast.makeText(BillPrintActivity.this, "Sản phẩm " + product.code + " không còn hàng", Toast.LENGTH_SHORT).show();
            }
        }
        recalcTotalAmount();
    }

    // Tính lại tổng số tiền của hóa đơn
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

    // Tra cứu sản phẩm trong MariaDB dựa trên mã vật tư, lấy thông tin và số lượng tồn kho (SoLuongNhap)
    private Product lookupProduct(String productCode) {
        Product product = null;
        Connection connection = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            Class.forName("com.mysql.jdbc.Driver");
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            String sql = "SELECT MaVatTu, TenVatTu, GiaTien, SoLuongNhap FROM quanlykho_data WHERE MaVatTu = ?";
            stmt = connection.prepareStatement(sql);
            stmt.setString(1, productCode);
            rs = stmt.executeQuery();
            if (rs.next()) {
                String code = rs.getString("MaVatTu");
                String name = rs.getString("TenVatTu");
                int unitPrice = rs.getInt("GiaTien");
                int availableQuantity = rs.getInt("SoLuongNhap");
                product = new Product(code, name, unitPrice, availableQuantity);
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

    // Lớp Product có thêm thuộc tính availableQuantity
    private static class Product {
        String code;
        String name;
        int unitPrice;
        int availableQuantity;

        Product(String code, String name, int unitPrice, int availableQuantity) {
            this.code = code;
            this.name = name;
            this.unitPrice = unitPrice;
            this.availableQuantity = availableQuantity;
        }
    }

    // Khi in hóa đơn (PDF icon được chọn), sau in sẽ giảm số lượng sản phẩm đó trong MariaDB
    // Sau khi ẩn các view không cần in, in hóa đơn và cập nhật số lượng tồn kho.
//    private void printBill() {
//        final View content = findViewById(R.id.billPrintContainer);
//        final View barcodeSection = findViewById(R.id.barcodeScanningSection);
//        final View btnPrintView = findViewById(R.id.btnPrint);
//
//        final int oldBarcodeVisibility = barcodeSection.getVisibility();
//        final int oldPrintVisibility = btnPrintView.getVisibility();
//
//        barcodeSection.setVisibility(View.GONE);
//        btnPrintView.setVisibility(View.GONE);
//
//        content.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                PrintManager printManager = (PrintManager) getSystemService(Context.PRINT_SERVICE);
//                PrintDocumentAdapter printAdapter = new ViewPrintAdapter(BillPrintActivity.this, content);
//                String jobName = getString(R.string.app_name) + " Hóa Đơn";
//                printManager.print(jobName, printAdapter, new PrintAttributes.Builder().build());
//
//                // Sau khi gọi in, cập nhật số lượng tồn kho trong MariaDB
//                new Thread(new Runnable() {
//                    @Override
//                    public void run() {
//                        updateDatabaseAfterPrint();
//                    }
//                }).start();
//
//                barcodeSection.setVisibility(oldBarcodeVisibility);
//                btnPrintView.setVisibility(oldPrintVisibility);
//            }
//        }, 300);
//    }


    private void printBill() {
        try {
            // Inflate layout dùng riêng cho in (chỉ chứa bảng hóa đơn)
            View printView = getLayoutInflater().inflate(R.layout.invoice_print_only, null);

            // Lấy và cập nhật dữ liệu từ giao diện hiện tại
            TextView tvBillTitlePrint = printView.findViewById(R.id.tvBillTitlePrint);
            TextView tvBillNumberPrint = printView.findViewById(R.id.tvBillNumberPrint);
            TextView tvDatePrint = printView.findViewById(R.id.tvDatePrint);
            TableLayout productTablePrint = printView.findViewById(R.id.productTablePrint);
            TextView tvTotalAmountPrint = printView.findViewById(R.id.tvTotalAmountPrint);

            tvBillTitlePrint.setText(tvBillTitle.getText());
            tvBillNumberPrint.setText(tvBillNumber.getText());
            tvDatePrint.setText(tvDate.getText());
            tvTotalAmountPrint.setText(tvTotalAmount.getText());

            // Sao chép các dòng sản phẩm từ productTable sang productTablePrint
            int rowCount = productTable.getChildCount();
            for (int i = 0; i < rowCount; i++) {
                View row = productTable.getChildAt(i);
                if (row instanceof TableRow) {
                    TableRow newRow = new TableRow(this);
                    newRow.setLayoutParams(row.getLayoutParams());
                    TableRow originalRow = (TableRow) row;
                    int colCount = originalRow.getChildCount();
                    for (int j = 0; j < colCount; j++) {
                        TextView originalTv = (TextView) originalRow.getChildAt(j);
                        TextView newTv = new TextView(this);
                        newTv.setLayoutParams(originalTv.getLayoutParams());
                        newTv.setPadding(
                                originalTv.getPaddingLeft(),
                                originalTv.getPaddingTop(),
                                originalTv.getPaddingRight(),
                                originalTv.getPaddingBottom()
                        );
                        newTv.setText(originalTv.getText());
                        newRow.addView(newTv);
                    }
                    productTablePrint.addView(newRow);
                }
            }

            // Ép view được in đo và layout trước khi in
            // Lấy chiều rộng của thiết bị nếu width của printView bằng 0
            int width = printView.getWidth();
            if (width <= 0) {
                width = getResources().getDisplayMetrics().widthPixels;
            }
            printView.measure(
                    View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            printView.layout(0, 0, printView.getMeasuredWidth(), printView.getMeasuredHeight());

            // Gọi PrintManager để in
            PrintManager printManager = (PrintManager) getSystemService(Context.PRINT_SERVICE);
            PrintDocumentAdapter printAdapter = new ViewPrintAdapter(this, printView);
            String jobName = getString(R.string.app_name) + " Hóa Đơn";
            printManager.print(jobName, printAdapter, new PrintAttributes.Builder().build());

            // Nếu có cần cập nhật DB sau in thì chạy đoạn code update ở đây...

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi khi in hóa đơn: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }



    // Duyệt qua từng sản phẩm trong hóa đơn và giảm số lượng tồn kho ở MariaDB
    private void updateDatabaseAfterPrint() {
        int rowCount = productTable.getChildCount();
        for (int i = 1; i < rowCount; i++) {
            TableRow row = (TableRow) productTable.getChildAt(i);
            TextView tvCode = (TextView) row.getChildAt(0);
            TextView tvQuantity = (TextView) row.getChildAt(2);
            String productCode = tvCode.getText().toString();
            int printedQuantity = Integer.parseInt(tvQuantity.getText().toString());
            updateProductQuantityInDB(productCode, printedQuantity);
        }
    }

    // Hàm cập nhật DB: trừ số lượng đã in từ SoLuongNhap nếu tồn kho đủ
    private void updateProductQuantityInDB(String productCode, int printedQuantity) {
        Connection connection = null;
        PreparedStatement stmt = null;
        try {
            Class.forName("com.mysql.jdbc.Driver");
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            String sql = "UPDATE quanlykho_data SET SoLuongNhap = SoLuongNhap - ? WHERE MaVatTu = ? AND SoLuongNhap >= ?";
            stmt = connection.prepareStatement(sql);
            stmt.setInt(1, printedQuantity);
            stmt.setString(2, productCode);
            stmt.setInt(3, printedQuantity);
            int rows = stmt.executeUpdate();
            if (rows <= 0) {
                runOnUiThread(() -> Toast.makeText(BillPrintActivity.this, "Cập nhật DB thất bại cho sản phẩm " + productCode, Toast.LENGTH_SHORT).show());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (stmt != null)
                    stmt.close();
                if (connection != null)
                    connection.close();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

}
